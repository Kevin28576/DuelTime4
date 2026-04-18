package com.kevin.dueltime4.arena;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.gui.simple.QueueMatchConfirmInventoryHolder;
import com.kevin.dueltime4.util.UtilItemBuilder;
import com.kevin.dueltime4.viaversion.ViaVersionItem;
import com.kevin.dueltime4.yaml.message.DynamicLang;
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
                timeoutSeconds * 20L);

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
                    "&7You already accepted this queue match.");
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
                "&cQueue confirmation cancelled because &e{player} &cdeclined.",
                "player", declinedPlayerName);

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
                "&cQueue confirmation cancelled because &e{player} &cleft queue.",
                "player", playerName);

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
                        "&cQueue confirmation failed because some players were no longer available.");
            }
            arenaManager.tryCreatePendingMatch(pendingMatch.arenaId);
            return;
        }

        arenaManager.recordQueueMatchStartSample(pendingMatch.arenaId, pendingMatch.playerNames);
        for (Player startPlayer : startPlayers) {
            DynamicLang.send(startPlayer, true,
                    "Dynamic.queue.confirm.starting",
                    "&aAll players accepted. Starting match now...");
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
                "&cQueue confirmation timed out. Missing: &e{players}",
                "players", missingText);

        for (String missingPlayer : missingPlayers) {
            arenaManager.removeWaitingPlayer(missingPlayer);
        }
        arenaManager.tryCreatePendingMatch(arenaId);
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
                        "&7請使用確認介面選擇：&a接受 &7或 &c拒絕&7。");
                continue;
            }
            DynamicLang.send(player, true,
                    "Dynamic.queue.confirm.prompt-title",
                    "&eMatch ready in arena &f{arena}&e.",
                    "arena", pendingMatch.arenaName);
            DynamicLang.send(player, true,
                    "Dynamic.queue.confirm.prompt-action",
                    "&7Type &a{accept} &7to accept (&f{seconds}s&7), or &c{decline} &7to decline.",
                    "accept", ACCEPT_COMMAND,
                    "seconds", countdownText,
                    "decline", DECLINE_COMMAND);
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
                        "&aAccepted (&f{accepted}&7/&f{required}&a).",
                        "accepted", String.valueOf(acceptedCount),
                        "required", String.valueOf(requiredCount));
            } else {
                DynamicLang.send(player, true,
                        "Dynamic.queue.confirm.accept-other",
                        "&b{player} &7accepted (&f{accepted}&7/&f{required}&7).",
                        "player", acceptedPlayerName,
                        "accepted", String.valueOf(acceptedCount),
                        "required", String.valueOf(requiredCount));
            }
        }
    }

    private boolean openConfirmGui(Player player, PendingMatch pendingMatch) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        // 低版本直接回退指令操作
        if (DuelTimePlugin.serverVersionInt <= 8) {
            return false;
        }
        try {
            String seconds = String.valueOf(pendingMatch.timeoutSeconds);
            String title = DynamicLang.get(player,
                    "Dynamic.queue.confirm.gui-title",
                    "&8匹配確認");
            Inventory inventory = Bukkit.createInventory(
                    new QueueMatchConfirmInventoryHolder(pendingMatch.arenaId),
                    27,
                    title);

            ItemStack filler = new UtilItemBuilder(ViaVersionItem.getGlassPaneType(15))
                    .setDisplayName(" ")
                    .build();
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }

            Material acceptMaterial = resolveMaterial("LIME_WOOL", "WOOL", "EMERALD_BLOCK", "EMERALD", "PAPER");
            Material declineMaterial = resolveMaterial("RED_WOOL", "WOOL", "REDSTONE_BLOCK", "REDSTONE", "PAPER");
            Material infoMaterial = ViaVersionItem.getWatchMaterial();
            if (infoMaterial == null) {
                infoMaterial = resolveMaterial("CLOCK", "WATCH", "PAPER");
            }

            ItemStack acceptItem = new UtilItemBuilder(acceptMaterial)
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
                                    "seconds", seconds),
                            DynamicLang.get(player,
                                    "Dynamic.queue.confirm.gui-accept-lore-3",
                                    "&e點擊後接受，並等待其他玩家確認")
                    )
                    .setLight()
                    .build();

            ItemStack declineItem = new UtilItemBuilder(declineMaterial)
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

            ItemStack infoItem = new UtilItemBuilder(infoMaterial)
                    .setDisplayName(DynamicLang.get(player,
                            "Dynamic.queue.confirm.gui-info-name",
                            "&e匹配確認"))
                    .setLore(
                            DynamicLang.get(player,
                                    "Dynamic.queue.confirm.gui-info-lore-1",
                                    "&7競技場：&f{arena}",
                                    "arena", pendingMatch.arenaName),
                            DynamicLang.get(player,
                                    "Dynamic.queue.confirm.gui-info-lore-2",
                                    "&7確認倒數：&f{seconds} 秒",
                                    "seconds", seconds),
                            DynamicLang.get(player,
                                    "Dynamic.queue.confirm.gui-info-lore-3",
                                    "&7若介面無法使用，請改用指令："),
                            DynamicLang.get(player,
                                    "Dynamic.queue.confirm.gui-info-lore-4",
                                    "&a{accept} &7/ &c{decline}",
                                    "accept", ACCEPT_COMMAND,
                                    "decline", DECLINE_COMMAND)
                    )
                    .build();

            inventory.setItem(ACCEPT_SLOT, acceptItem);
            inventory.setItem(13, infoItem);
            inventory.setItem(DECLINE_SLOT, declineItem);
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
        private final List<String> playerNames;
        private final Set<String> acceptedPlayers = new HashSet<>();
        private BukkitTask timeoutTask;

        private PendingMatch(String arenaId, String arenaName, int requiredCount, int timeoutSeconds, List<String> playerNames) {
            this.arenaId = arenaId;
            this.arenaName = arenaName;
            this.requiredCount = requiredCount;
            this.timeoutSeconds = timeoutSeconds;
            this.playerNames = new ArrayList<>(playerNames);
        }
    }
}
