package com.kevin.dueltime4.arena;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.yaml.configuration.CfgManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class QueueWatchdogService {
    private final DuelTimePlugin plugin;
    private final AtomicLong totalRunCounter = new AtomicLong(0L);
    private volatile Snapshot snapshot = Snapshot.empty();
    private BukkitTask task;

    public QueueWatchdogService(DuelTimePlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void start() {
        if (task != null) {
            return;
        }
        CfgManager cfg = plugin.getCfgManager();
        if (cfg == null || !cfg.isArenaClassicMatchWatchdogEnabled()) {
            return;
        }

        long intervalTicks = Math.max(1L, cfg.getArenaClassicMatchWatchdogIntervalSeconds()) * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::runOnce, intervalTicks, intervalTicks);
    }

    public synchronized void restartFromConfig() {
        stop();
        start();
    }

    public synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public synchronized boolean isRunning() {
        return task != null;
    }

    public int getConfiguredIntervalSeconds() {
        CfgManager cfg = plugin.getCfgManager();
        if (cfg == null) {
            return 0;
        }
        return cfg.getArenaClassicMatchWatchdogIntervalSeconds();
    }

    public Snapshot getSnapshot() {
        return snapshot;
    }

    private void runOnce() {
        long startedAt = System.currentTimeMillis();
        int removedOfflinePlayers = 0;
        int removedInvalidPlayers = 0;
        int matchAttempts = 0;
        int pendingCreated = 0;
        String error = "";
        try {
            ArenaManager arenaManager = plugin.getArenaManager();
            CfgManager cfg = plugin.getCfgManager();
            if (arenaManager == null || cfg == null) {
                return;
            }

            if (cfg.isArenaClassicMatchWatchdogCleanupOfflinePlayers()) {
                Map<String, String> waitingMap = arenaManager.getWaitingPlayerToArenaMap();
                for (String playerName : waitingMap.keySet()) {
                    Player player = Bukkit.getPlayerExact(playerName);
                    if (player != null && player.isOnline()) {
                        continue;
                    }
                    arenaManager.removeWaitingPlayer(playerName);
                    removedOfflinePlayers++;
                }
            }

            if (cfg.isArenaClassicMatchWatchdogCleanupInvalidArena()) {
                Map<String, String> waitingMap = arenaManager.getWaitingPlayerToArenaMap();
                for (Map.Entry<String, String> entry : waitingMap.entrySet()) {
                    String playerName = entry.getKey();
                    String arenaId = entry.getValue();
                    BaseArena arena = arenaManager.get(arenaId);
                    if (arena != null && arena.getState() == BaseArena.State.WAITING) {
                        continue;
                    }
                    arenaManager.removeWaitingPlayer(playerName);
                    removedInvalidPlayers++;
                }
            }

            if (cfg.isArenaClassicMatchWatchdogTriggerMatchCheck()) {
                List<BaseArena> arenas = arenaManager.getList();
                for (BaseArena arena : arenas) {
                    if (arena.getState() != BaseArena.State.WAITING) {
                        continue;
                    }
                    String arenaId = arena.getId();
                    if (arenaManager.getWaitingPlayers(arenaId).isEmpty()) {
                        continue;
                    }
                    matchAttempts++;
                    boolean hadPending = arenaManager.getQueueMatchConfirmManager().hasPendingForArena(arenaId);
                    arenaManager.tryCreatePendingMatch(arenaId);
                    boolean hasPendingNow = arenaManager.getQueueMatchConfirmManager().hasPendingForArena(arenaId);
                    if (!hadPending && hasPendingNow) {
                        pendingCreated++;
                    }
                }
            }
        } catch (Exception exception) {
            error = exception.getClass().getSimpleName() + ": " + exception.getMessage();
            plugin.getLogger().warning("[QueueWatchdog] Run failed: " + error);
        } finally {
            long endedAt = System.currentTimeMillis();
            snapshot = new Snapshot(
                    totalRunCounter.incrementAndGet(),
                    endedAt,
                    endedAt - startedAt,
                    removedOfflinePlayers,
                    removedInvalidPlayers,
                    matchAttempts,
                    pendingCreated,
                    error);
        }
    }

    public static class Snapshot {
        private final long totalRuns;
        private final long lastRunAt;
        private final long lastDurationMs;
        private final int removedOfflinePlayers;
        private final int removedInvalidPlayers;
        private final int matchAttempts;
        private final int pendingCreated;
        private final String lastError;

        private Snapshot(long totalRuns,
                         long lastRunAt,
                         long lastDurationMs,
                         int removedOfflinePlayers,
                         int removedInvalidPlayers,
                         int matchAttempts,
                         int pendingCreated,
                         String lastError) {
            this.totalRuns = totalRuns;
            this.lastRunAt = lastRunAt;
            this.lastDurationMs = lastDurationMs;
            this.removedOfflinePlayers = removedOfflinePlayers;
            this.removedInvalidPlayers = removedInvalidPlayers;
            this.matchAttempts = matchAttempts;
            this.pendingCreated = pendingCreated;
            this.lastError = lastError == null ? "" : lastError;
        }

        public static Snapshot empty() {
            return new Snapshot(0L, 0L, 0L, 0, 0, 0, 0, "");
        }

        public long getTotalRuns() {
            return totalRuns;
        }

        public long getLastRunAt() {
            return lastRunAt;
        }

        public long getLastDurationMs() {
            return lastDurationMs;
        }

        public int getRemovedOfflinePlayers() {
            return removedOfflinePlayers;
        }

        public int getRemovedInvalidPlayers() {
            return removedInvalidPlayers;
        }

        public int getMatchAttempts() {
            return matchAttempts;
        }

        public int getPendingCreated() {
            return pendingCreated;
        }

        public String getLastError() {
            return lastError;
        }
    }
}
