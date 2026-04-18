package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.QueueWatchdogService;
import com.kevin.dueltime4.cache.BlacklistCache;
import com.kevin.dueltime4.cache.LocationCache;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.cache.RecordCache;
import com.kevin.dueltime4.cache.ShopCache;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.data.MyBatisManager;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Locale;

public class CMDDoctor extends SubCommand {
    public CMDDoctor() {
        super("doctor", "doc", "health");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(CommandPermission.ADMIN)) {
            MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
            return true;
        }

        String scope = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "all";
        if (!isAllowedScope(scope)) {
            DynamicLang.send(sender, true,
                    "Dynamic.doctor.usage",
                    "&cUsage: /{label} doctor [all|services|database|queue]",
                    "label", label);
            return true;
        }
        scope = normalizeScope(scope);

        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        DynamicLang.send(sender, true,
                "Dynamic.doctor.separator",
                "&8&m---------------------------");
        DynamicLang.send(sender, true,
                "Dynamic.doctor.title",
                "&aDuelTime4 Doctor");
        DynamicLang.send(sender, true,
                "Dynamic.doctor.runtime",
                "&7Plugin: &f{plugin} &7| Bukkit: &f{bukkit}",
                "plugin", plugin.getDescription().getVersion(),
                "bukkit", Bukkit.getBukkitVersion());

        if (scope.equals("all") || scope.equals("services")) {
            sendServiceSection(sender, plugin);
        }
        if (scope.equals("all") || scope.equals("database")) {
            sendDatabaseSection(sender, plugin);
        }
        if (scope.equals("all") || scope.equals("queue")) {
            sendQueueSection(sender, plugin);
        }

        DynamicLang.send(sender, true,
                "Dynamic.doctor.separator",
                "&8&m---------------------------");
        return true;
    }

    private void sendServiceSection(CommandSender sender, DuelTimePlugin plugin) {
        DynamicLang.send(sender, true,
                "Dynamic.doctor.services-title",
                "&e[Services]");
        DynamicLang.send(sender, true,
                "Dynamic.doctor.services-line",
                "&7cfg={cfg} &7msg={msg} &7db={db} &7cache={cache} &7arena={arena} &7queue-watchdog={watchdog}",
                "cfg", status(plugin.getCfgManager() != null),
                "msg", status(plugin.getMsgManager() != null),
                "db", status(plugin.getMyBatisManager() != null),
                "cache", status(plugin.getCacheManager() != null),
                "arena", status(plugin.getArenaManager() != null),
                "watchdog", status(plugin.getQueueWatchdogService() != null));
    }

    private void sendDatabaseSection(CommandSender sender, DuelTimePlugin plugin) {
        DynamicLang.send(sender, true,
                "Dynamic.doctor.database-title",
                "&e[Database]");
        sendModuleStatusLine(sender, plugin, "arena", ArenaManager.class);
        sendModuleStatusLine(sender, plugin, "location", LocationCache.class);
        sendModuleStatusLine(sender, plugin, "shop", ShopCache.class);
        sendModuleStatusLine(sender, plugin, "record", RecordCache.class);
        sendModuleStatusLine(sender, plugin, "player-data", PlayerDataCache.class);
        sendModuleStatusLine(sender, plugin, "blacklist", BlacklistCache.class);
    }

    private void sendQueueSection(CommandSender sender, DuelTimePlugin plugin) {
        ArenaManager arenaManager = plugin.getArenaManager();
        QueueWatchdogService watchdog = plugin.getQueueWatchdogService();
        int waitingCount = arenaManager == null ? 0 : arenaManager.getTotalWaitingCount();
        int pendingCount = arenaManager == null ? 0 : arenaManager.getQueueMatchConfirmManager().getPendingArenaCount();
        String running = watchdog != null && watchdog.isRunning() ? "&aON" : "&cOFF";
        int interval = watchdog == null ? 0 : watchdog.getConfiguredIntervalSeconds();

        DynamicLang.send(sender, true,
                "Dynamic.doctor.queue-title",
                "&e[Queue]");
        DynamicLang.send(sender, true,
                "Dynamic.doctor.queue-summary",
                "&7waiting=&f{waiting} &7| pending-confirm=&f{pending} &7| watchdog={state} &7({interval}s)",
                "waiting", String.valueOf(waitingCount),
                "pending", String.valueOf(pendingCount),
                "state", running,
                "interval", String.valueOf(interval));

        if (watchdog != null) {
            QueueWatchdogService.Snapshot snapshot = watchdog.getSnapshot();
            String ago = snapshot.getLastRunAt() <= 0
                    ? "-"
                    : String.valueOf(Math.max(0, System.currentTimeMillis() - snapshot.getLastRunAt()));
            DynamicLang.send(sender, true,
                    "Dynamic.doctor.queue-watchdog",
                    "&7runs=&f{runs} &7| last-run-ago=&f{ago}ms &7| duration=&f{duration}ms",
                    "runs", String.valueOf(snapshot.getTotalRuns()),
                    "ago", ago,
                    "duration", String.valueOf(snapshot.getLastDurationMs()));
            DynamicLang.send(sender, true,
                    "Dynamic.doctor.queue-watchdog-details",
                    "&7cleaned-offline=&f{offline} &7| cleaned-invalid=&f{invalid} &7| match-attempts=&f{attempts} &7| pending-created=&f{created}",
                    "offline", String.valueOf(snapshot.getRemovedOfflinePlayers()),
                    "invalid", String.valueOf(snapshot.getRemovedInvalidPlayers()),
                    "attempts", String.valueOf(snapshot.getMatchAttempts()),
                    "created", String.valueOf(snapshot.getPendingCreated()));
            if (!snapshot.getLastError().isEmpty()) {
                DynamicLang.send(sender, true,
                        "Dynamic.doctor.queue-watchdog-error",
                        "&cLast watchdog error: &f{error}",
                        "error", snapshot.getLastError());
            }
        }
    }

    private void sendModuleStatusLine(CommandSender sender, DuelTimePlugin plugin, String moduleName, Class<?> moduleClass) {
        MyBatisManager myBatisManager = plugin.getMyBatisManager();
        if (myBatisManager == null) {
            DynamicLang.send(sender, true,
                    "Dynamic.doctor.database-line",
                    "&7- {module}: &f{storage} / {state}",
                    "module", moduleName,
                    "storage", "-",
                    "state", "&cNO_MANAGER");
            return;
        }

        MyBatisManager.DatabaseType type = myBatisManager.getType(moduleClass);
        String storage = type == null ? "-" : type.name().toLowerCase(Locale.ROOT);
        SqlSessionFactory factory = myBatisManager.getFactory(moduleClass);
        String state;
        if (factory == null) {
            state = "&cNO_FACTORY";
        } else {
            try (var ignored = factory.openSession()) {
                state = "&aOK";
            } catch (Exception exception) {
                state = "&cFAIL(" + exception.getClass().getSimpleName() + ")";
            }
        }

        DynamicLang.send(sender, true,
                "Dynamic.doctor.database-line",
                "&7- {module}: &f{storage} / {state}",
                "module", moduleName,
                "storage", storage,
                "state", state);
    }

    private boolean isAllowedScope(String scope) {
        return isAlias(scope, "all", "a", "run", "full")
                || isAlias(scope, "services", "service", "svc")
                || isAlias(scope, "database", "db")
                || isAlias(scope, "queue", "q");
    }

    private String normalizeScope(String scope) {
        if (isAlias(scope, "all", "a", "run", "full")) {
            return "all";
        }
        if (isAlias(scope, "services", "service", "svc")) {
            return "services";
        }
        if (isAlias(scope, "database", "db")) {
            return "database";
        }
        return "queue";
    }

    private boolean isAlias(String entered, String... aliases) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(entered)) {
                return true;
            }
        }
        return false;
    }

    private String status(boolean value) {
        return value ? "&aOK" : "&cMissing";
    }
}
