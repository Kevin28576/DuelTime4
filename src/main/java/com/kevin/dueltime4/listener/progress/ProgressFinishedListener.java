package com.kevin.dueltime4.listener.progress;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.arena.ClassicArena;
import com.kevin.dueltime4.arena.base.BaseArenaData;
import com.kevin.dueltime4.arena.type.ArenaType;
import com.kevin.dueltime4.data.pojo.ClassicArenaData;
import com.kevin.dueltime4.event.progress.ProgressFinishedEvent;
import com.kevin.dueltime4.progress.Progress;
import com.kevin.dueltime4.progress.ProgressType;
import com.kevin.dueltime4.progress.Step;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.util.UtilGeometry;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.kevin.dueltime4.progress.ProgressType.InternalType.*;
import static com.kevin.dueltime4.arena.type.ArenaType.FunctionInternalType.*;

public class ProgressFinishedListener implements Listener {
    private final Map<String, String> progressTypeToFunctionType = new HashMap<String, String>() {{
        put(ADD_FUNCTION_CLASSIC_TIME_LIMIT.getId(), CLASSIC_TIME_LIMIT.getId());
        put(ADD_FUNCTION_CLASSIC_COUNTDOWN.getId(), CLASSIC_COUNTDOWN.getId());
        put(ADD_FUNCTION_CLASSIC_INVENTORY_CHECK_KEYWORD.getId(), CLASSIC_INVENTORY_CHECK_KEYWORD.getId());
        put(ADD_FUNCTION_CLASSIC_INVENTORY_CHECK_TYPE.getId(), CLASSIC_INVENTORY_CHECK_TYPE.getId());
        put(ADD_FUNCTION_CLASSIC_PRE_GAME_COMMAND.getId(), CLASSIC_PRE_GAME_COMMAND.getId());
        put(ADD_FUNCTION_CLASSIC_SPECTATE.getId(), CLASSIC_SPECTATE.getId());
        put(ADD_FUNCTION_CLASSIC_BAN_ENTITY_SPAWN.getId(), CLASSIC_BAN_ENTITY_SPAWN.getId());
    }};

    @EventHandler
    public void onProgressFinishedToApplyData(ProgressFinishedEvent event) {
        Progress progress = event.getProgress();
        String progressId = progress.getId();
        Step[] steps = progress.getSteps();
        if (progressId.equals(CREATE_CLASSIC_ARENA.getId())) {
            // 建立經典型別競技場過程
            UtilGeometry.buildCubicLine(progress.getPlayer(),(Location) steps[0].getData(), (Location) steps[1].getData(), 0.3, 60, 179, 113);
            DuelTimePlugin.getInstance().getArenaManager().add(
                    new ClassicArena(
                            new ClassicArenaData(
                                    (String) steps[4].getData(),
                                    (String) steps[5].getData(),
                                    (Location) steps[0].getData(),
                                    (Location) steps[1].getData(),
                                    null,
                                    (Location) steps[2].getData(),
                                    (Location) steps[3].getData()
                            )
                    )
            );
        }
        if (progressTypeToFunctionType.containsKey(progressId)) {
            // 為經典型別競技場新增附加功能過程
            String arenaId = (String) progress.getData();
            ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
            BaseArenaData arenaData = arenaManager.get(arenaId).getArenaData();
            HashMap<String, Object[]> functions = arenaData.getFunctions();
            if (functions == null) {
                functions = new HashMap<>();
            }
            functions.put(progressTypeToFunctionType.get(progressId), Arrays.stream(steps).map(Step::getData).toArray());
            arenaData.setFunctions(functions);
            arenaManager.update(arenaData);
        }
    }
}
