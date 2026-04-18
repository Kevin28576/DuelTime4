package com.kevin.dueltime4.command.sub;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.command.SubCommand;
import com.kevin.dueltime4.record.BattleReportExportService;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.yaml.message.DynamicLang;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMDRecord extends SubCommand {
    private static final int DEFAULT_EXPORT_LIMIT = 100;
    private static final int MAX_EXPORT_LIMIT = 1000;

    public CMDRecord() {
        super("record", "r");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                MsgBuilder.send(Msg.ERROR_NOT_PLAYER_EXECUTOR, sender);
                return true;
            }
            DuelTimePlugin.getInstance().getCustomInventoryManager().getArenaRecord().openFor((Player) sender);
            return true;
        }

        if (isAlias(args[1], "export", "exp", "dump")) {
            if (!sender.hasPermission(CommandPermission.ADMIN)) {
                MsgBuilder.send(Msg.ERROR_NO_PERMISSION, sender);
                return true;
            }
            handleExport(sender, label, args);
            return true;
        }

        sendUsage(sender, label);
        return true;
    }

    private void handleExport(CommandSender sender, String label, String[] args) {
        String targetName;
        int index = 2;
        if (args.length >= 3 && !isFormatToken(args[2]) && !UtilFormat.isInt(args[2])) {
            targetName = args[2];
            index = 3;
        } else {
            if (!(sender instanceof Player player)) {
                DynamicLang.send(sender, true,
                        "Dynamic.record-export.usage",
                        "&cUsage: /{label} record export [player] [json|csv] [limit]",
                        "label", label);
                return;
            }
            targetName = player.getName();
        }

        BattleReportExportService.ExportFormat format = BattleReportExportService.ExportFormat.JSON;
        Integer limit = null;

        if (args.length > index) {
            BattleReportExportService.ExportFormat parsedFormat = BattleReportExportService.ExportFormat.fromToken(args[index]);
            if (parsedFormat != null) {
                format = parsedFormat;
                index++;
            }
        }

        if (args.length > index) {
            String limitToken = args[index];
            if (!UtilFormat.isInt(limitToken)) {
                DynamicLang.send(sender, true,
                        "Dynamic.record-export.invalid-arg",
                        "&cInvalid argument: &f{arg}",
                        "arg", limitToken);
                sendUsage(sender, label);
                return;
            }

            int parsedLimit = Integer.parseInt(limitToken);
            if (parsedLimit <= 0) {
                DynamicLang.send(sender, true,
                        "Dynamic.record-export.limit-positive",
                        "&cLimit must be > 0.");
                return;
            }
            limit = Math.min(parsedLimit, MAX_EXPORT_LIMIT);
            index++;
        }

        if (args.length > index) {
            DynamicLang.send(sender, true,
                    "Dynamic.record-export.invalid-arg",
                    "&cInvalid argument: &f{arg}",
                    "arg", args[index]);
            sendUsage(sender, label);
            return;
        }

        int exportLimit = limit == null ? DEFAULT_EXPORT_LIMIT : limit;
        DynamicLang.send(sender, true,
                "Dynamic.record-export.exporting",
                "&7Exporting battle report... player=&f{player}&7, format=&f{format}&7, limit=&f{limit}",
                "player", targetName,
                "format", format.name().toLowerCase(),
                "limit", String.valueOf(exportLimit));

        BattleReportExportService exportService = new BattleReportExportService(DuelTimePlugin.getInstance());
        BattleReportExportService.ExportResult result = exportService.export(targetName, format, exportLimit);
        if (!result.isSuccess()) {
            DynamicLang.send(sender, true,
                    "Dynamic.record-export.failed",
                    "&cExport failed: &f{reason}",
                    "reason", result.getErrorMessage());
            return;
        }
        if (result.getExportedRecordCount() <= 0) {
            DynamicLang.send(sender, true,
                    "Dynamic.record-export.empty",
                    "&eNo records found for &f{player}&e.",
                    "player", targetName);
            return;
        }

        DynamicLang.send(sender, true,
                "Dynamic.record-export.success",
                "&aExport completed: &f{count}&a/{total} record(s), format=&f{format}&a.",
                "count", String.valueOf(result.getExportedRecordCount()),
                "total", String.valueOf(result.getTotalRecordCount()),
                "format", result.getFormat().name().toLowerCase());
        DynamicLang.send(sender, true,
                "Dynamic.record-export.path",
                "&7File: &f{path}",
                "path", result.getOutputPath().toString());
    }

    private boolean isFormatToken(String token) {
        return BattleReportExportService.ExportFormat.fromToken(token) != null;
    }

    private void sendUsage(CommandSender sender, String label) {
        DynamicLang.send(sender, true,
                "Dynamic.record-export.usage",
                "&cUsage: /{label} record export [player] [json|csv] [limit]",
                "label", label);
        DynamicLang.send(sender, true,
                "Dynamic.record-export.usage-example",
                "&7Example: /{label} record export kevin json 100",
                "label", label);
    }

    private boolean isAlias(String entered, String... aliases) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(entered)) {
                return true;
            }
        }
        return false;
    }
}
