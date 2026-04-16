package com.kevin.dueltime4.listener.progress;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.progress.Progress;
import com.kevin.dueltime4.progress.Step;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.util.UtilGeometry;
import com.kevin.dueltime4.viaversion.ViaVersion;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class ProgressAutoUploadListener implements Listener {
    protected static Map<String, List<String>> listEnteredCache = new HashMap<>();

    /**
     * 上傳字串、字串列表、數字
     */
    @EventHandler
    public void onSendMessage(PlayerChatEvent event) {
        Player player = event.getPlayer();
        Progress progress = DuelTimePlugin.getInstance().getProgressManager().getProgress(player.getName());
        if (progress == null) {
            return;
        }
        if (progress.isPaused()) {
            return;
        }
        String enter = event.getMessage();
        for (String operateStr : Arrays.asList("-r", "-reverse", "-p", "-pause", "-c", "-continue", "-e", "-exit", "-l", "-list")) {
            if (operateStr.equalsIgnoreCase(enter)) {
                return;
            }
        }
        Step step = progress.getNowStep();
        if (!step.isAutoUpload()) {
            return;
        }
        if (step.getDataType() == null) {
            return;
        }
        if (step.getDataType().equals(String.class)) {
            event.setCancelled(true);
            if (step.hasAutoUploadTags(Step.AutoUploadTag.STRING_CONDITION_ID_STYLE)) {
                if (!UtilFormat.isIDStyle(enter)) {
                    MsgBuilder.send(Msg.ERROR_INCORRECT_ID_FORMAT, player,
                            enter);
                    return;
                }
            }
            enter = dealString(enter, step);
            progress.next(enter);
        }
        if (step.getDataType().equals(List.class)) {
            event.setCancelled(true);
            boolean pair = step.hasAutoUploadTags(Step.AutoUploadTag.LIST_CONDITION_STRING_INTEGER_PAIR);
            boolean pairLoose = step.hasAutoUploadTags(Step.AutoUploadTag.LIST_CONDITION_STRING_INTEGER_PAIR_LOOSE);
            if (pair || pairLoose) {
                if (!enter.contains(":")) {
                    if (pair) {
                        MsgBuilder.send(Msg.ERROR_INCORRECT_STRING_INTEGER_PAIR_FORMAT, player);
                        return;
                    }
                } else {
                    if (!UtilFormat.isInt(enter.split(":")[1])) {
                        MsgBuilder.send(Msg.ERROR_INCORRECT_STRING_INTEGER_PAIR_FORMAT_INTEGER, player);
                        return;
                    }
                }
            }
            if (step.hasAutoUploadTags(Step.AutoUploadTag.LIST_CONDITION_IDENTITY_COMMAND_PAIR)) {
                String[] clips = enter.split(":");
                if (clips.length <= 1) {
                    MsgBuilder.send(Msg.ERROR_INCORRECT_EXECUTOR_COMMAND_PAIR_FORMAT, player);
                    return;
                }
                String identityEntered = clips[0];
                if (!identityEntered.equalsIgnoreCase("player") && !identityEntered.equalsIgnoreCase("op") &&
                        !identityEntered.equalsIgnoreCase("console") && !identityEntered.equalsIgnoreCase("console_single_time")) {
                    MsgBuilder.send(Msg.PROGRESS_TYPE_SET_FUNCTION_PRE_GAME_COMMAND_STEP_1_INCORRECT_EXECUTOR, player,
                            identityEntered);
                    return;
                }
                enter = identityEntered.toLowerCase() + ":" + clips[1];// 身份名統一轉換為小寫
            }
            enter = dealString(enter, step);
            List<String> listEntered = listEnteredCache.getOrDefault(player.getName(), new ArrayList<>());
            listEntered.add(enter);
            listEnteredCache.put(player.getName(), listEntered);
            MsgBuilder.sendTitle(Msg.PROGRESS_OPERATION_LIST_ENTER_TITLE, Msg.PROGRESS_OPERATION_LIST_ENTER_SUBTITLE, 0, 50, 5, player, ViaVersion.TitleType.SUBTITLE,
                    UtilFormat.toString(listEntered,player, UtilFormat.StringifyTag.LIST_LIMIT_LINE_LENGTH, UtilFormat.StringifyTag.LIST_LIMIT_LIST_SIZE));
        }
        if (step.getDataType().equals(Integer.class)) {
            event.setCancelled(true);
            if (!UtilFormat.isInt(enter)) {
                MsgBuilder.send(Msg.ERROR_INCORRECT_INTEGER_FORMAT, player,
                        enter);
                return;
            }
            if (step.hasAutoUploadTags(Step.AutoUploadTag.INTEGER_CONDITION_POSITIVE_VALUE)) {
                if (Integer.parseInt(enter) <= 0) {
                    MsgBuilder.send(Msg.ERROR_VALUE_IS_NOT_POSITIVE, player,
                            enter);
                    return;
                }
            }
            progress.next(Integer.parseInt(enter));
        }
        if (step.getDataType().equals(Double.class) || step.getDataType().equals(Float.class)) {
            event.setCancelled(true);
            if (!UtilFormat.isDouble(enter)) {
                MsgBuilder.send(Msg.ERROR_INCORRECT_NUMBER_FORMAT, player,
                        enter);
                return;
            }
            if (step.hasAutoUploadTags(Step.AutoUploadTag.DOUBLE_CONDITION_POSITIVE_VALUE)) {
                if (Double.parseDouble(enter) <= 0) {
                    MsgBuilder.send(Msg.ERROR_VALUE_IS_NOT_POSITIVE, player,
                            enter);
                    return;
                }
            }
            progress.next(Integer.parseInt(enter));
        }
        if (step.getDataType().equals(Boolean.class)) {
            event.setCancelled(true);
            if (enter.equalsIgnoreCase("T")) {
                progress.next(true);
            } else if (enter.equalsIgnoreCase("F")) {
                progress.next(false);
            } else {
                MsgBuilder.send(Msg.ERROR_INCORRECT_BOOLEAN, player);
            }
        }
    }

    // 根據步驟所具有的自動上傳標簽處理字串，包括替換顏色符號等
    private String dealString(String enter, Step step) {
        if (step.hasAutoUploadTags(Step.AutoUploadTag.STRING_FUNCTION_REPLACE_COLOR_SYMBOL)) {
            enter = enter.replace("&", "§");
        }
        if (step.hasAutoUploadTags(Step.AutoUploadTag.STRING_FUNCTION_REPLACE_BLANK)) {
            enter = enter.replace("_", " ");
        }
        if (step.hasAutoUploadTags(Step.AutoUploadTag.STRING_FUNCTION_TO_UPPERCASE)) {
            enter = enter.toUpperCase();
        }
        if (step.hasAutoUploadTags(Step.AutoUploadTag.STRING_FUNCTION_TO_LOWERCASE)) {
            enter = enter.toLowerCase();
        }
        return enter;
    }


    /**
     * 上傳位置
     */
    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Progress progress = DuelTimePlugin.getInstance().getProgressManager().getProgress(player.getName());
        if (progress == null) {
            return;
        }
        if (progress.isPaused()) {
            return;
        }
        Step step = progress.getNowStep();
        if (step.getDataType() == null) {
            return;
        }
        if (!step.isAutoUpload()) {
            return;
        }
        if (!step.getDataType().equals(Location.class)) {
            return;
        }
        // 記得補充忽略副手點選的程式碼
        event.setCancelled(true);
        Action action = event.getAction();
        Location location;
        if (step.hasAutoUploadTags(Step.AutoUploadTag.LOCATION_CONDITION_CLICK_AIR)) {
            if (!action.equals(Action.LEFT_CLICK_AIR) && !action.equals(Action.RIGHT_CLICK_AIR)) {
                return;
            }
            location = player.getLocation();
        } else {
            if (!action.equals(Action.LEFT_CLICK_BLOCK) && !action.equals(Action.RIGHT_CLICK_BLOCK)) {
                return;
            }
            location = event.getClickedBlock().getLocation();
        }
        if (step.hasAutoUploadTags(Step.AutoUploadTag.LOCATION_CONDITION_THE_SAME_WORLD)) {
            int finishedStepNumber = progress.getFinishedStep();
            if (finishedStepNumber > 0) {
                Object lastLocation = progress.getSteps()[finishedStepNumber - 1].getData();
                if (lastLocation instanceof Location && !((Location) lastLocation).getWorld().getName().equals(location.getWorld().getName())) {
                    MsgBuilder.send(Msg.PROGRESS_AUTO_UPLOAD_LOCATION_DIFFERENT_WORLD, player);
                    return;
                }
            }
        }
        if (step.hasAutoUploadTags(Step.AutoUploadTag.LOCATION_CONDITION_DIFFERENT_BLOCK)) {
            int finishedStepNumber = progress.getFinishedStep();
            if (finishedStepNumber > 0) {
                Object lastLocation = progress.getSteps()[finishedStepNumber - 1].getData();
                if (lastLocation instanceof Location && ((Location) lastLocation).getBlock().getLocation().equals(location.getBlock().getLocation())) {
                    MsgBuilder.send(Msg.PROGRESS_AUTO_UPLOAD_LOCATION_THE_SAME_BLOCK, player);
                    return;
                }
            }
        }
        if (step.hasAutoUploadTags(Step.AutoUploadTag.LOCATION_CONDITION_CANNOT_OVERLAP_WITH_OTHER_ARENA)) {
            int finishedStepNumber = progress.getFinishedStep();
            if (finishedStepNumber > 0) {
                Object lastLocation = progress.getSteps()[finishedStepNumber - 1].getData();
                if (lastLocation instanceof Location && !((Location) lastLocation).getBlock().getLocation().equals(location.getBlock().getLocation())) {
                    for (BaseArena arena : DuelTimePlugin.getInstance().getArenaManager().getMap().values()) {
                        Location arenaLoc1 = arena.getArenaData().getDiagonalPointLocation1();
                        Location arenaLoc2 = arena.getArenaData().getDiagonalPointLocation2();
                        if (UtilGeometry.hasOverlap(
                                (Location) lastLocation, location,
                                arenaLoc1, arenaLoc2)) {
                            UtilGeometry.buildCubicLine(player,arenaLoc1, arenaLoc2, 0.3, 220, 20, 60);
                            MsgBuilder.send(Msg.PROGRESS_AUTO_UPLOAD_LOCATION_OVERLAY_WITH_OTHER_ARENA, player,
                                    arena.getArenaData().getName());
                            return;
                        }
                    }
                }
            }
        }
        progress.next(location);
    }
}
