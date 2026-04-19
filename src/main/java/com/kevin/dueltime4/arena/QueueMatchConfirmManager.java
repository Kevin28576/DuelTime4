package com.kevin.dueltime4.arena;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.gui.simple.QueueMatchConfirmInventoryHolder;
import com.kevin.dueltime4.util.UtilItemBuilder;
import com.kevin.dueltime4.viaversion.ViaVersionItem;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class QueueMatchConfirmManager {
    public static final String ACCEPT_COMMAND = "/dueltime accept";
    public static final String DECLINE_COMMAND = "/dueltime decline";
    public static final int ACCEPT_SLOT = 11;
    public static final int INFO_SLOT = 13;
    public static final int DECLINE_SLOT = 15;

    private final ArenaManager arenaManager;
    private final Map<String, PendingMatch> pendingByArena = new HashMap<>();
    private final Map<String, String> pendingArenaByPlayer = new HashMap<>();

    public QueueMatchConfirmManager(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    public void tryCreatePendingMatch(BaseArena arena) {
        if (arena == null) {
            return;
        }
        String arenaId = arena.getId();
        if (pendingByArena.containsKey(arenaId)) {
            return;
        }

        int requiredCount = Math.max(2, arena.getArenaData().getMinPlayerNumber());
        List<String> waitingPlayers = arenaManager.getWaitingPlayers(arenaId);
        if (waitingPlayers.size() < requiredCount) {
            return;
        }

        List<String> candidates = new ArrayList<>();
        for (String waitingPlayerName : waitingPlayers) {
            if (candidates.size() >= requiredCount) {
                break;
            }
            Player player = Bukkit.getPlayerExact(waitingPlayerName);
            if (player == null || !player.isOnline()) {
                continue;
            }
            if (!arenaManager.isWaitingPlayerForArena(waitingPlayerName, arenaId)) {
                continue;
            }
            candidates.add(waitingPlayerName);
        }

        if (candidates.size() < requiredCount) {
            return;
        }

        int timeoutSeconds = Math.max(5, DuelTimePlugin.getInstance().getCfgManager().getArenaClassicMatchConfirmTimeout());
        PendingMatch pendingMatch = new PendingMatch(arenaId, arena.getName(), requiredCount, timeoutSeconds, candidates);
        pendingMatch.timeoutTask = Bukkit.getScheduler().runTaskLater(
                DuelTimePlugin.getInstance(),
                () -> onTimeout(arenaId),
                timeoutSeconds * 20L
        );
        pendingMatch.countdownTask = Bukkit.getScheduler().runTaskTimer(
                DuelTimePlugin.getInstance(),
                () -> onCountdownTick(arenaId),
                0L,
                20L
        );

        pendingByArena.put(arenaId, pendingMatch);
        for (String playerName : candidates) {
            pendingArenaByPlayer.put(playerName, arenaId);
        }

        broadcastPrompt(pendingMatch);
    }

    public boolean accept(Player player) {
        if (player == null) {
            return false;
        }

        PendingMatch pendingMatch = getPendingMatchByPlayer(player.getName());
        if (pendingMatch == null) {
            return false;
        }

        if (!pendingMatch.acceptedPlayers.add(player.getName())) {
            DynamicLang.send(player, true,
                    "Dynamic.queue.confirm.already-accepted",
                    "&7你已接受這場匹配確認。");
            return true;
        }

        broadcastAcceptStatus(pendingMatch, player.getName());
        if (pendingMatch.acceptedPlayers.size() >= pendingMatch.requiredCount) {
            startPendingMatch(pendingMatch);
        }
        return true;
    }

    public boolean decline(Player player) {
        if (player == null) {
            return false;
        }

        PendingMatch pendingMatch = getPendingMatchByPlayer(player.getName());
        if (pendingMatch == null) {
            return false;
        }

        String declinedPlayerName = player.getName();
        cancelPendingMatch(
                pendingMatch,
                "Dynamic.queue.confirm.cancel-declined",
                "&c由於 &e{player} &c拒絕，匹配確認已取消。",
                "player", declinedPlayerName
        );

        arenaManager.removeWaitingPlayer(declinedPlayerName);
        arenaManager.tryCreatePendingMatch(pendingMatch.arenaId);
        return true;
    }

    public void onWaitingPlayerRemoved(String playerName) {
        PendingMatch pendingMatch = getPendingMatchByPlayer(playerName);
        if (pendingMatch == null) {
            return;
        }

        cancelPendingMatch(
                pendingMatch,
                "Dynamic.queue.confirm.cancel-left",
                "&c由於 &e{player} &c離開佇列，匹配確認已取消。",
                "player", playerName
        );

        arenaManager.tryCreatePendingMatch(pendingMatch.arenaId);
    }

    public void cancelForArenaSilently(String arenaId) {
        PendingMatch pendingMatch = pendingByArena.remove(arenaId);
        if (pendingMatch == null) {
            return;
        }
        if (pendingMatch.timeoutTask != null) {
            pendingMatch.timeoutTask.cancel();
        }
        if (pendingMatch.countdownTask != null) {
            pendingMatch.countdownTask.cancel();
        }
        for (String playerName : pendingMatch.playerNames) {
            pendingArenaByPlayer.remove(playerName);
        }
    }

    public void cancelAll() {
        for (String arenaId : new ArrayList<>(pendingByArena.keySet())) {
            cancelForArenaSilently(arenaId);
        }
    }

    public boolean hasPendingForArena(String arenaId) {
        return pendingByArena.containsKey(arenaId);
    }

    public int getAcceptedCount(String arenaId) {
        PendingMatch pendingMatch = pendingByArena.get(arenaId);
        return pendingMatch == null ? 0 : pendingMatch.acceptedPlayers.size();
    }

    public int getRequiredCount(String arenaId) {
        PendingMatch pendingMatch = pendingByArena.get(arenaId);
        return pendingMatch == null ? 0 : pendingMatch.requiredCount;
    }

    public int getPendingArenaCount() {
        return pendingByArena.size();
    }

    public boolean hasPendingForPlayer(String playerName) {
        return getPendingMatchByPlayer(playerName) != null;
    }

    private void startPendingMatch(PendingMatch pendingMatch) {
        cancelForArenaSilently(pendingMatch.arenaId);

        List<Player> startPlayers = new ArrayList<>();
        for (String playerName : pendingMatch.playerNames) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player == null || !player.isOnline() || !arenaManager.isWaitingPlayerForArena(playerName, pendingMatch.arenaId)) {
                arenaManager.removeWaitingPlayer(playerName);
                continue;
            }
            startPlayers.add(player);
            closeConfirmGuiIfOpen(player);
        }

        if (startPlayers.size() < pendingMatch.requiredCount) {
            for (Player startPlayer : startPlayers) {
                DynamicLang.send(startPlayer, true,
                        "Dynamic.queue.confirm.start-failed-unavailable",
                        "&c匹配確認失敗，部分玩家已不可用。");
            }
            arenaManager.tryCreatePendingMatch(pendingMatch.arenaId);
            return;
        }

        arenaManager.recordQueueMatchStartSample(pendingMatch.arenaId, pendingMatch.playerNames);
        for (Player startPlayer : startPlayers) {
            DynamicLang.send(startPlayer, true,
                    "Dynamic.queue.confirm.starting",
                    "&a所有玩家都已接受，正在開始比賽...");
            MsgBuilder.sendActionBar(" ", startPlayer, true);
        }
        arenaManager.start(pendingMatch.arenaId, null, startPlayers.toArray(Player[]::new));
    }

    private void onTimeout(String arenaId) {
        PendingMatch pendingMatch = pendingByArena.get(arenaId);
        if (pendingMatch == null) {
            return;
        }

        Set<String> missingPlayers = new HashSet<>(pendingMatch.playerNames);
        missingPlayers.removeAll(pendingMatch.acceptedPlayers);
        String missingText = missingPlayers.isEmpty()
                ? "-"
                : missingPlayers.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.joining(", "));

        cancelPendingMatch(
                pendingMatch,
                "Dynamic.queue.confirm.timeout",
                "&c匹配確認逾時，未確認玩家：&e{players}",
                "players", missingText
        );

        for (String missingPlayer : missingPlayers) {
            arenaManager.removeWaitingPlayer(missingPlayer);
        }
        arenaManager.tryCreatePendingMatch(arenaId);
    }

    private void onCountdownTick(String arenaId) {
        PendingMatch pendingMatch = pendingByArena.get(arenaId);
        if (pendingMatch == null) {
            return;
        }
        int secondsLeft = pendingMatch.getRemainingSeconds();
        broadcastCountdown(pendingMatch, secondsLeft);
        if (secondsLeft <= 0 && pendingMatch.countdownTask != null) {
            pendingMatch.countdownTask.cancel();
            pendingMatch.countdownTask = null;
        }
    }

    private PendingMatch getPendingMatchByPlayer(String playerName) {
        String arenaId = pendingArenaByPlayer.get(playerName);
        if (arenaId == null) {
            return null;
        }
        return pendingByArena.get(arenaId);
    }

    private void cancelPendingMatch(PendingMatch pendingMatch, String reasonKey, String reasonFallback, String... replacements) {
        cancelForArenaSilently(pendingMatch.arenaId);
        for (String playerName : pendingMatch.playerNames) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null && player.isOnline()) {
                closeConfirmGuiIfOpen(player);
                MsgBuilder.sendActionBar(" ", player, true);
                DynamicLang.send(player, true, reasonKey, reasonFallback, replacements);
            }
        }
    }

    private void broadcastPrompt(PendingMatch pendingMatch) {
        String countdownText = String.valueOf(pendingMatch.timeoutSeconds);
        for (String playerName : pendingMatch.playerNames) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player == null || !player.isOnline()) {
                continue;
            }

            boolean openedGui = openConfirmGui(player, pendingMatch);
            if (openedGui) {
                DynamicLang.send(player, true,
                        "Dynamic.queue.confirm.gui-hint",
                        "&7請在確認介面中選擇：&a接受 &7或 &c拒絕&7。");
                continue;
            }

            DynamicLang.send(player, true,
                    "Dynamic.queue.confirm.prompt-title",
                    "&e競技場 &f{arena} &e已有可開始的對戰。",
                    "arena", pendingMatch.arenaName);
            DynamicLang.send(player, true,
                    "Dynamic.queue.confirm.prompt-action",
                    "&7輸入 &a{accept} &7接受（&f{seconds} 秒&7），或輸入 &c{decline} &7拒絕。",
                    "accept", ACCEPT_COMMAND,
                    "seconds", countdownText,
                    "decline", DECLINE_COMMAND);
        }
    }

    private void broadcastCountdown(PendingMatch pendingMatch, int secondsLeft) {
        String seconds = String.valueOf(secondsLeft);
        String accepted = String.valueOf(pendingMatch.acceptedPlayers.size());
        String required = String.valueOf(pendingMatch.requiredCount);
        for (String playerName : pendingMatch.playerNames) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player == null || !player.isOnline()) {
                continue;
            }
            String actionBar = DynamicLang.get(player,
                    "Dynamic.queue.confirm.countdown-action-bar",
                    "&e匹配確認剩餘 &f{seconds}&e 秒 &8| &7已接受：&a{accepted}&7/&f{required}",
                    "seconds", seconds,
                    "accepted", accepted,
                    "required", required);
            MsgBuilder.sendActionBar(actionBar, player, true);
            updateConfirmGuiState(player, pendingMatch, secondsLeft);
        }
    }

    private void broadcastAcceptStatus(PendingMatch pendingMatch, String acceptedPlayerName) {
        int acceptedCount = pendingMatch.acceptedPlayers.size();
        int requiredCount = pendingMatch.requiredCount;
        for (String playerName : pendingMatch.playerNames) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player == null || !player.isOnline()) {
                continue;
            }

            if (playerName.equalsIgnoreCase(acceptedPlayerName)) {
                DynamicLang.send(player, true,
                        "Dynamic.queue.confirm.accept-self",
                        "&a你已接受（&f{accepted}&7/&f{required}&a）。",
                        "accepted", String.valueOf(acceptedCount),
                        "required", String.valueOf(requiredCount));
            } else {
                DynamicLang.send(player, true,
                        "Dynamic.queue.confirm.accept-other",
                        "&b{player} &7已接受（&f{accepted}&7/&f{required}&7）。",
                        "player", acceptedPlayerName,
                        "accepted", String.valueOf(acceptedCount),
                        "required", String.valueOf(requiredCount));
            }

            updateConfirmGuiState(player, pendingMatch, pendingMatch.getRemainingSeconds());
        }
    }

    private boolean openConfirmGui(Player player, PendingMatch pendingMatch) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        if (DuelTimePlugin.serverVersionInt <= 8) {
            return false;
        }
        try {
            Inventory inventory = Bukkit.createInventory(
                    new QueueMatchConfirmInventoryHolder(pendingMatch.arenaId),
                    27,
                    DynamicLang.get(player,
                            "Dynamic.queue.confirm.gui-title",
                            "&8匹配確認")
            );

            ItemStack filler = new UtilItemBuilder(ViaVersionItem.getGlassPaneType(15))
                    .setDisplayName(" ")
                    .build();
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }

            int secondsLeft = pendingMatch.getRemainingSeconds();
            inventory.setItem(ACCEPT_SLOT, buildAcceptItem(player, pendingMatch, secondsLeft));
            inventory.setItem(INFO_SLOT, buildInfoItem(player, pendingMatch, secondsLeft));
            inventory.setItem(DECLINE_SLOT, buildDeclineItem(player));
            player.openInventory(inventory);
            return true;
        } catch (Throwable throwable) {
            DuelTimePlugin.getInstance().getLogger().warning(
                    "Queue confirm GUI unavailable for " + player.getName()
                            + ", fallback to command mode. reason="
                            + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            return false;
        }
    }

    private void updateConfirmGuiState(Player player, PendingMatch pendingMatch, int secondsLeft) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (player.getOpenInventory() == null) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof QueueMatchConfirmInventoryHolder holder)) {
            return;
        }
        if (!pendingMatch.arenaId.equals(holder.getArenaId())) {
            return;
        }
        top.setItem(ACCEPT_SLOT, buildAcceptItem(player, pendingMatch, secondsLeft));
        top.setItem(INFO_SLOT, buildInfoItem(player, pendingMatch, secondsLeft));
    }

    private ItemStack buildAcceptItem(Player player, PendingMatch pendingMatch, int secondsLeft) {
        Material acceptMaterial = resolveMaterial("LIME_WOOL", "WOOL", "EMERALD_BLOCK", "EMERALD", "PAPER");
        String accepted = String.valueOf(pendingMatch.acceptedPlayers.size());
        String required = String.valueOf(pendingMatch.requiredCount);
        return new UtilItemBuilder(acceptMaterial)
                .setDisplayName(DynamicLang.get(player,
                        "Dynamic.queue.confirm.gui-accept-name",
                        "&a&l接受匹配"))
                .setLore(
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-accept-lore-1",
                                "&7競技場：&f{arena}",
                                "arena", pendingMatch.arenaName),
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-accept-lore-2",
                                "&7請在 &f{seconds} 秒 &7內確認",
                                "seconds", String.valueOf(secondsLeft)),
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-accept-lore-3",
                                "&7目前已接受：&a{accepted}&7/&f{required}",
                                "accepted", accepted,
                                "required", required),
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-accept-lore-4",
                                "&e點擊後接受，並等待其他玩家確認")
                )
                .setLight()
                .build();
    }

    private ItemStack buildDeclineItem(Player player) {
        Material declineMaterial = resolveMaterial("RED_WOOL", "WOOL", "REDSTONE_BLOCK", "REDSTONE", "PAPER");
        return new UtilItemBuilder(declineMaterial)
                .setDisplayName(DynamicLang.get(player,
                        "Dynamic.queue.confirm.gui-decline-name",
                        "&c&l拒絕匹配"))
                .setLore(
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-decline-lore-1",
                                "&7點擊後會取消本次匹配"),
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-decline-lore-2",
                                "&7你會離開目前佇列")
                )
                .build();
    }

    private ItemStack buildInfoItem(Player player, PendingMatch pendingMatch, int secondsLeft) {
        Material infoMaterial = ViaVersionItem.getWatchMaterial();
        if (infoMaterial == null) {
            infoMaterial = resolveMaterial("CLOCK", "WATCH", "PAPER");
        }
        String accepted = String.valueOf(pendingMatch.acceptedPlayers.size());
        String required = String.valueOf(pendingMatch.requiredCount);
        return new UtilItemBuilder(infoMaterial)
                .setDisplayName(DynamicLang.get(player,
                        "Dynamic.queue.confirm.gui-info-name",
                        "&e匹配確認資訊"))
                .setLore(
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-info-lore-1",
                                "&7競技場：&f{arena}",
                                "arena", pendingMatch.arenaName),
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-info-lore-2",
                                "&7確認倒數：&f{seconds} 秒",
                                "seconds", String.valueOf(secondsLeft)),
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-info-lore-3",
                                "&7已接受：&a{accepted}&7/&f{required}",
                                "accepted", accepted,
                                "required", required),
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-info-lore-4",
                                "&7若介面無法操作，可改用指令："),
                        DynamicLang.get(player,
                                "Dynamic.queue.confirm.gui-info-lore-5",
                                "&a{accept} &7/ &c{decline}",
                                "accept", ACCEPT_COMMAND,
                                "decline", DECLINE_COMMAND)
                )
                .build();
    }

    private void closeConfirmGuiIfOpen(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null) {
                return;
            }
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof QueueMatchConfirmInventoryHolder) {
                player.closeInventory();
            }
        } catch (Throwable ignored) {
        }
    }

    private Material resolveMaterial(String... names) {
        for (String name : names) {
            Material material = Material.getMaterial(name);
            if (material != null) {
                return material;
            }
        }
        return Material.PAPER;
    }

    private static class PendingMatch {
        private final String arenaId;
        private final String arenaName;
        private final int requiredCount;
        private final int timeoutSeconds;
        private final long expireAtMs;
        private final List<String> playerNames;
        private final Set<String> acceptedPlayers = new HashSet<>();
        private BukkitTask timeoutTask;
        private BukkitTask countdownTask;

        private PendingMatch(String arenaId, String arenaName, int requiredCount, int timeoutSeconds, List<String> playerNames) {
            this.arenaId = arenaId;
            this.arenaName = arenaName;
            this.requiredCount = requiredCount;
            this.timeoutSeconds = timeoutSeconds;
            this.playerNames = new ArrayList<>(playerNames);
            this.expireAtMs = System.currentTimeMillis() + timeoutSeconds * 1000L;
        }

        private int getRemainingSeconds() {
            long remainingMs = expireAtMs - System.currentTimeMillis();
            if (remainingMs <= 0L) {
                return 0;
            }
            return (int) ((remainingMs + 999L) / 1000L);
        }
    }
}
