package com.kevin.dueltime4.record;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseRecordData;
import com.kevin.dueltime4.data.pojo.ClassicArenaRecordData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class BattleReportExportService {
    private static final DateTimeFormatter FILE_NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter TEXT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DuelTimePlugin plugin;

    public BattleReportExportService(DuelTimePlugin plugin) {
        this.plugin = plugin;
    }

    public ExportResult export(String playerName, ExportFormat format, int limit) {
        if (playerName == null || playerName.isBlank()) {
            return ExportResult.error("player name is empty");
        }
        if (limit <= 0) {
            return ExportResult.error("limit must be > 0");
        }
        if (plugin.getCacheManager() == null || plugin.getCacheManager().getArenaRecordCache() == null) {
            return ExportResult.error("record cache unavailable");
        }

        List<BaseRecordData> allRecords = plugin.getCacheManager().getArenaRecordCache().getAnyway(playerName);
        if (allRecords == null) {
            allRecords = Collections.emptyList();
        }
        if (allRecords.isEmpty()) {
            return ExportResult.success(null, 0, 0, format);
        }

        int exportedCount = Math.min(limit, allRecords.size());
        List<BaseRecordData> selectedRecords = new ArrayList<>(allRecords.subList(0, exportedCount));
        String generatedAt = LocalDateTime.now().format(TEXT_TIME_FORMAT);

        String extension = format == ExportFormat.JSON ? "json" : "csv";
        String fileName = "battle-report_" + sanitizeFileSegment(playerName) + "_"
                + LocalDateTime.now().format(FILE_NAME_TIME_FORMAT) + "." + extension;

        Path exportFolder = plugin.getDataFolder().toPath().resolve("exports").resolve("battle-report");
        Path outputPath = exportFolder.resolve(fileName);
        try {
            Files.createDirectories(exportFolder);
            String content = format == ExportFormat.JSON
                    ? buildJson(playerName, generatedAt, allRecords.size(), selectedRecords)
                    : buildCsv(selectedRecords);
            Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return ExportResult.error("io error: " + exception.getMessage());
        }

        return ExportResult.success(outputPath.toAbsolutePath(), allRecords.size(), exportedCount, format);
    }

    private String buildJson(String playerName, String generatedAt, int totalRecords, List<BaseRecordData> records) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"player\": \"").append(jsonEscape(playerName)).append("\",\n");
        builder.append("  \"generated_at\": \"").append(jsonEscape(generatedAt)).append("\",\n");
        builder.append("  \"total_records\": ").append(totalRecords).append(",\n");
        builder.append("  \"exported_records\": ").append(records.size()).append(",\n");
        builder.append("  \"records\": [\n");
        for (int i = 0; i < records.size(); i++) {
            appendJsonRecord(builder, records.get(i));
            if (i < records.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private void appendJsonRecord(StringBuilder builder, BaseRecordData record) {
        builder.append("    {");
        appendJsonField(builder, "type", record instanceof ClassicArenaRecordData ? "classic" : "unknown", true);
        appendJsonField(builder, "player", record.getPlayerName(), true);
        appendJsonField(builder, "arena_id", record.getArenaId(), true);
        appendJsonField(builder, "date", record.getDate(), record instanceof ClassicArenaRecordData);
        if (record instanceof ClassicArenaRecordData classic) {
            appendJsonField(builder, "opponent", classic.getOpponentName(), true);
            appendJsonField(builder, "result", classic.getResult().name().toLowerCase(Locale.ROOT), true);
            appendJsonField(builder, "time_seconds", classic.getTime(), true);
            appendJsonField(builder, "exp_change", classic.getExpChange(), true);
            appendJsonField(builder, "hit_count", classic.getHitTime(), true);
            appendJsonField(builder, "total_damage", classic.getTotalDamage(), true);
            appendJsonField(builder, "max_damage", classic.getMaxDamage(), true);
            appendJsonField(builder, "average_damage", classic.getAverageDamage(), false);
        }
        builder.append("}");
    }

    private void appendJsonField(StringBuilder builder, String key, String value, boolean withComma) {
        builder.append("\"").append(jsonEscape(key)).append("\": \"").append(jsonEscape(value)).append("\"");
        if (withComma) {
            builder.append(", ");
        }
    }

    private void appendJsonField(StringBuilder builder, String key, Number value, boolean withComma) {
        builder.append("\"").append(jsonEscape(key)).append("\": ").append(value);
        if (withComma) {
            builder.append(", ");
        }
    }

    private String buildCsv(List<BaseRecordData> records) {
        StringBuilder builder = new StringBuilder();
        builder.append("type,player,arena_id,date,opponent,result,time_seconds,exp_change,hit_count,total_damage,max_damage,average_damage\n");
        for (BaseRecordData record : records) {
            if (record instanceof ClassicArenaRecordData classic) {
                builder.append(csvEscape("classic")).append(",")
                        .append(csvEscape(classic.getPlayerName())).append(",")
                        .append(csvEscape(classic.getArenaId())).append(",")
                        .append(csvEscape(classic.getDate())).append(",")
                        .append(csvEscape(classic.getOpponentName())).append(",")
                        .append(csvEscape(classic.getResult().name().toLowerCase(Locale.ROOT))).append(",")
                        .append(classic.getTime()).append(",")
                        .append(classic.getExpChange()).append(",")
                        .append(classic.getHitTime()).append(",")
                        .append(classic.getTotalDamage()).append(",")
                        .append(classic.getMaxDamage()).append(",")
                        .append(classic.getAverageDamage())
                        .append("\n");
            } else {
                builder.append(csvEscape("unknown")).append(",")
                        .append(csvEscape(record.getPlayerName())).append(",")
                        .append(csvEscape(record.getArenaId())).append(",")
                        .append(csvEscape(record.getDate()))
                        .append(",,,,,,,,\n");
            }
        }
        return builder.toString();
    }

    private String sanitizeFileSegment(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    public enum ExportFormat {
        JSON,
        CSV;

        public static ExportFormat fromToken(String token) {
            if (token == null) {
                return null;
            }
            return switch (token.toLowerCase(Locale.ROOT)) {
                case "json" -> JSON;
                case "csv" -> CSV;
                default -> null;
            };
        }
    }

    public static class ExportResult {
        private final boolean success;
        private final String errorMessage;
        private final Path outputPath;
        private final int totalRecordCount;
        private final int exportedRecordCount;
        private final ExportFormat format;

        private ExportResult(boolean success,
                             String errorMessage,
                             Path outputPath,
                             int totalRecordCount,
                             int exportedRecordCount,
                             ExportFormat format) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.outputPath = outputPath;
            this.totalRecordCount = totalRecordCount;
            this.exportedRecordCount = exportedRecordCount;
            this.format = format;
        }

        public static ExportResult success(Path outputPath, int totalRecordCount, int exportedRecordCount, ExportFormat format) {
            return new ExportResult(true, "", outputPath, totalRecordCount, exportedRecordCount, format);
        }

        public static ExportResult error(String errorMessage) {
            return new ExportResult(false, errorMessage == null ? "unknown" : errorMessage, null, 0, 0, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Path getOutputPath() {
            return outputPath;
        }

        public int getTotalRecordCount() {
            return totalRecordCount;
        }

        public int getExportedRecordCount() {
            return exportedRecordCount;
        }

        public ExportFormat getFormat() {
            return format;
        }
    }
}
