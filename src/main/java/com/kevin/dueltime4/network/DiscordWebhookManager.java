package com.kevin.dueltime4.network;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ClassicArena;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DiscordWebhookManager {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-ORX]");
    private static final int EMBED_COLOR_SUCCESS = 0x57F287;
    private static final int EMBED_COLOR_DRAW = 0xFEE75C;
    private static final int EMBED_COLOR_STOPPED = 0xED4245;
    private static final int EMBED_COLOR_PENALTY = 0xFF9F43;
    private final DuelTimePlugin plugin;

    public DiscordWebhookManager(DuelTimePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        CfgManager cfg = plugin.getCfgManager();
        return cfg != null
                && cfg.isDiscordWebhookEnabled()
                && cfg.getDiscordWebhookUrl() != null
                && !cfg.getDiscordWebhookUrl().isBlank();
    }

    public void sendBattleReport(String arenaId, String arenaName, ClassicArena.Result result,
                                 String winner, String loser, String player1, String player2, int durationSeconds) {
        CfgManager cfg = plugin.getCfgManager();
        if (cfg == null || !isEnabled() || !cfg.isDiscordWebhookBattleReportEnabled()) {
            return;
        }
        if (result == ClassicArena.Result.DRAW && !cfg.isDiscordWebhookBattleReportIncludeDraw()) {
            return;
        }

        String resultText = switch (result) {
            case CLEAR -> resolve("Dynamic.webhook.battle-report.result.clear", "clear");
            case DRAW -> resolve("Dynamic.webhook.battle-report.result.draw", "draw");
            case STOPPED -> resolve("Dynamic.webhook.battle-report.result.stopped", "stopped");
        };

        String safeArenaName = safe(arenaName);
        String safeArenaId = safe(arenaId);
        String safeWinner = safe(winner);
        String safeLoser = safe(loser);
        String safePlayer1 = safe(player1);
        String safePlayer2 = safe(player2);
        String safeDurationSeconds = String.valueOf(Math.max(0, durationSeconds));
        String safeSecondsUnit = resolve("String.seconds", "seconds");
        String content = resolveWebhookContent(
                "battle-report",
                "arena", safeArenaName,
                "id", safeArenaId,
                "result", resultText,
                "winner", safeWinner,
                "loser", safeLoser,
                "p1", safePlayer1,
                "p2", safePlayer2,
                "seconds", safeDurationSeconds,
                "unit", safeSecondsUnit);

        String description = resolveEmbedDescription(
                "battle-report",
                "Battle finished | Arena: {arena} ({id}) | Result: {result} | Winner: {winner} | Loser: {loser} | Players: {p1} vs {p2} | Duration: {seconds} {unit}",
                "arena", safeArenaName,
                "id", safeArenaId,
                "result", resultText,
                "winner", safeWinner,
                "loser", safeLoser,
                "p1", safePlayer1,
                "p2", safePlayer2,
                "seconds", safeDurationSeconds,
                "unit", safeSecondsUnit);

        WebhookEmbed embed = new WebhookEmbed(
                resolveEmbedTitle("battle-report", "DuelTime4 Battle Report"),
                description,
                colorForResult(result));
        embed.setFooter(resolve("Dynamic.webhook.footer", "DuelTime4"));
        embed.addField(
                resolveEmbedFieldName("battle-report", "arena", "Arena"),
                resolveEmbedFieldValue("battle-report", "arena", "{arena} ({id})",
                        "arena", safeArenaName,
                        "id", safeArenaId),
                true);
        embed.addField(
                resolveEmbedFieldName("battle-report", "result", "Result"),
                resolveEmbedFieldValue("battle-report", "result", "{result}",
                        "result", resultText),
                true);
        embed.addField(
                resolveEmbedFieldName("battle-report", "winner", "Winner"),
                resolveEmbedFieldValue("battle-report", "winner", "{winner}",
                        "winner", safeWinner),
                true);
        embed.addField(
                resolveEmbedFieldName("battle-report", "loser", "Loser"),
                resolveEmbedFieldValue("battle-report", "loser", "{loser}",
                        "loser", safeLoser),
                true);
        embed.addField(
                resolveEmbedFieldName("battle-report", "players", "Players"),
                resolveEmbedFieldValue("battle-report", "players", "{p1} vs {p2}",
                        "p1", safePlayer1,
                        "p2", safePlayer2),
                false);
        embed.addField(
                resolveEmbedFieldName("battle-report", "duration", "Duration"),
                resolveEmbedFieldValue("battle-report", "duration", "{seconds} {unit}",
                        "seconds", safeDurationSeconds,
                        "unit", safeSecondsUnit),
                true);
        sendAsync(content, embed);
    }

    public void sendLeavePenalty(String playerName, String opponentName, String reasonKey,
                                 double pointDeducted, double currentPoint, int queueCooldownSeconds) {
        CfgManager cfg = plugin.getCfgManager();
        if (cfg == null || !isEnabled() || !cfg.isDiscordWebhookLeavePenaltyEnabled()) {
            return;
        }

        String safePlayer = safe(playerName);
        String safeOpponent = safe(opponentName);
        String safeCooldownSeconds = String.valueOf(Math.max(0, queueCooldownSeconds));
        String safeSecondsUnit = resolve("String.seconds", "seconds");
        String reason = resolve(
                "Dynamic.webhook.leave-penalty.reason." + reasonKey,
                reasonKey);
        String content = resolveWebhookContent(
                "leave-penalty",
                "player", safePlayer,
                "opponent", safeOpponent,
                "reason", safe(reason),
                "point", String.valueOf(pointDeducted),
                "current", String.valueOf(currentPoint),
                "cooldown", safeCooldownSeconds,
                "unit", safeSecondsUnit);
        String description = resolveEmbedDescription(
                "leave-penalty",
                "Leave penalty | Player: {player} | Opponent: {opponent} | Reason: {reason} | Point: -{point} (current {current}) | Queue cooldown: {cooldown} {unit}",
                "player", safePlayer,
                "opponent", safeOpponent,
                "reason", safe(reason),
                "point", String.valueOf(pointDeducted),
                "current", String.valueOf(currentPoint),
                "cooldown", safeCooldownSeconds,
                "unit", safeSecondsUnit);

        WebhookEmbed embed = new WebhookEmbed(
                resolveEmbedTitle("leave-penalty", "DuelTime4 Leave Penalty"),
                description,
                EMBED_COLOR_PENALTY);
        embed.setFooter(resolve("Dynamic.webhook.footer", "DuelTime4"));
        embed.addField(
                resolveEmbedFieldName("leave-penalty", "player", "Player"),
                resolveEmbedFieldValue("leave-penalty", "player", "{player}",
                        "player", safePlayer),
                true);
        embed.addField(
                resolveEmbedFieldName("leave-penalty", "opponent", "Opponent"),
                resolveEmbedFieldValue("leave-penalty", "opponent", "{opponent}",
                        "opponent", safeOpponent),
                true);
        embed.addField(
                resolveEmbedFieldName("leave-penalty", "reason", "Reason"),
                resolveEmbedFieldValue("leave-penalty", "reason", "{reason}",
                        "reason", safe(reason)),
                true);
        embed.addField(
                resolveEmbedFieldName("leave-penalty", "point", "Point Deducted"),
                resolveEmbedFieldValue("leave-penalty", "point", "-{point}",
                        "point", String.valueOf(pointDeducted)),
                true);
        embed.addField(
                resolveEmbedFieldName("leave-penalty", "current", "Current Point"),
                resolveEmbedFieldValue("leave-penalty", "current", "{current}",
                        "current", String.valueOf(currentPoint)),
                true);
        embed.addField(
                resolveEmbedFieldName("leave-penalty", "cooldown", "Queue Cooldown"),
                resolveEmbedFieldValue("leave-penalty", "cooldown", "{cooldown} {unit}",
                        "cooldown", safeCooldownSeconds,
                        "unit", safeSecondsUnit),
                true);
        sendAsync(content, embed);
    }

    private int colorForResult(ClassicArena.Result result) {
        return switch (result) {
            case CLEAR -> EMBED_COLOR_SUCCESS;
            case DRAW -> EMBED_COLOR_DRAW;
            case STOPPED -> EMBED_COLOR_STOPPED;
        };
    }

    private void sendAsync(String content, WebhookEmbed embed) {
        if (embed == null || embed.isEmpty()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> post(content, embed));
    }

    private void post(String content, WebhookEmbed embed) {
        CfgManager cfg = plugin.getCfgManager();
        if (cfg == null) {
            return;
        }

        String payload = buildPayload(content, embed, cfg.getDiscordWebhookUsername(), cfg.getDiscordWebhookAvatarUrl());
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(cfg.getDiscordWebhookUrl()).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(cfg.getDiscordWebhookConnectTimeoutMs());
            connection.setReadTimeout(cfg.getDiscordWebhookReadTimeoutMs());
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            byte[] body = payload.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                String error = readStream(connection.getErrorStream());
                plugin.getLogger().warning("[Webhook] Discord webhook request failed. code=" + responseCode + ", error=" + error);
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("[Webhook] Discord webhook request failed: " + exception.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildPayload(String content, WebhookEmbed embed, String username, String avatarUrl) {
        StringBuilder builder = new StringBuilder("{");
        boolean hasPrevious = appendOptionalStringField(builder, false, "username", username);
        hasPrevious = appendOptionalStringField(builder, hasPrevious, "avatar_url", avatarUrl);
        hasPrevious = appendOptionalStringField(builder, hasPrevious, "content", content);
        if (hasPrevious) {
            builder.append(",");
        }
        builder.append("\"embeds\":[").append(embed.toJson()).append("]}");
        return builder.toString();
    }

    private boolean appendOptionalStringField(StringBuilder builder, boolean hasPrevious, String key, String value) {
        if (value == null || value.isBlank()) {
            return hasPrevious;
        }
        if (hasPrevious) {
            builder.append(",");
        }
        builder.append("\"").append(escapeJson(key)).append("\":\"").append(escapeJson(value)).append("\"");
        return true;
    }

    private String resolve(String key, String fallback, String... replacements) {
        CommandSender console = Bukkit.getConsoleSender();
        String text = DynamicLang.get(console, key, fallback, replacements);
        return stripColor(text);
    }

    private String resolveEmbedTitle(String type, String fallback) {
        return resolve(
                "Dynamic.webhook." + type + ".embed.title",
                resolve("Dynamic.webhook." + type + ".embed-title", fallback));
    }

    private String resolveEmbedDescription(String type, String fallback, String... replacements) {
        return resolve(
                "Dynamic.webhook." + type + ".embed.description",
                resolve("Dynamic.webhook." + type + ".embed-description",
                        resolve("Dynamic.webhook." + type + ".content", fallback, replacements),
                        replacements),
                replacements);
    }

    private String resolveEmbedFieldName(String type, String field, String fallback) {
        return resolve(
                "Dynamic.webhook." + type + ".embed.fields." + field + ".name",
                resolve("Dynamic.webhook." + type + ".field." + field, fallback));
    }

    private String resolveEmbedFieldValue(String type, String field, String fallback, String... replacements) {
        return resolve(
                "Dynamic.webhook." + type + ".embed.fields." + field + ".value",
                fallback,
                replacements);
    }

    private String resolveWebhookContent(String type, String... replacements) {
        String globalContent = resolve("Dynamic.webhook.content", "", replacements);
        String eventContent = resolve("Dynamic.webhook." + type + ".content", "", replacements);
        if (globalContent.isBlank()) {
            return eventContent;
        }
        if (eventContent.isBlank()) {
            return globalContent;
        }
        return globalContent + "\n" + eventContent;
    }

    private String stripColor(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = ChatColor.translateAlternateColorCodes('&', text);
        normalized = COLOR_PATTERN.matcher(normalized).replaceAll("");
        return normalized.trim();
    }

    private String readStream(InputStream stream) {
        if (stream == null) {
            return "-";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(line);
            }
        } catch (Exception ignored) {
            return "-";
        }
        String value = builder.toString().trim();
        return value.isEmpty() ? "-" : value;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private class WebhookEmbed {
        private final String title;
        private final String description;
        private final int color;
        private final String timestamp;
        private final List<EmbedField> fields = new ArrayList<>();
        private String footerText;

        private WebhookEmbed(String title, String description, int color) {
            this.title = safe(title);
            this.description = safe(description);
            this.color = Math.max(0, color);
            this.timestamp = Instant.now().toString();
        }

        private void setFooter(String footerText) {
            this.footerText = safe(footerText);
        }

        private void addField(String name, String value, boolean inline) {
            fields.add(new EmbedField(safe(name), safe(value), inline));
        }

        private boolean isEmpty() {
            return title.isBlank() && description.isBlank() && fields.isEmpty();
        }

        private String toJson() {
            StringBuilder builder = new StringBuilder("{");
            boolean hasPrevious = false;
            hasPrevious = appendStringJsonField(builder, hasPrevious, "title", title);
            hasPrevious = appendStringJsonField(builder, hasPrevious, "description", description);
            if (hasPrevious) {
                builder.append(",");
            }
            builder.append("\"color\":").append(color);
            hasPrevious = true;
            hasPrevious = appendStringJsonField(builder, hasPrevious, "timestamp", timestamp);
            if (footerText != null && !footerText.isBlank()) {
                if (hasPrevious) {
                    builder.append(",");
                }
                builder.append("\"footer\":{\"text\":\"").append(escapeJson(footerText)).append("\"}");
                hasPrevious = true;
            }
            if (!fields.isEmpty()) {
                if (hasPrevious) {
                    builder.append(",");
                }
                builder.append("\"fields\":[");
                for (int i = 0; i < fields.size(); i++) {
                    if (i > 0) {
                        builder.append(",");
                    }
                    builder.append(fields.get(i).toJson());
                }
                builder.append("]");
            }
            builder.append("}");
            return builder.toString();
        }
    }

    private boolean appendStringJsonField(StringBuilder builder, boolean hasPrevious, String key, String value) {
        if (value == null || value.isBlank()) {
            return hasPrevious;
        }
        if (hasPrevious) {
            builder.append(",");
        }
        builder.append("\"")
                .append(escapeJson(key))
                .append("\":\"")
                .append(escapeJson(value))
                .append("\"");
        return true;
    }

    private class EmbedField {
        private final String name;
        private final String value;
        private final boolean inline;

        private EmbedField(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }

        private String toJson() {
            return "{\"name\":\"" + escapeJson(name)
                    + "\",\"value\":\"" + escapeJson(value)
                    + "\",\"inline\":" + inline + "}";
        }
    }
}
