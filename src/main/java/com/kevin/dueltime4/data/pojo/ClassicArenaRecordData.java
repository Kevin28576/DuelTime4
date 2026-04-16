package com.kevin.dueltime4.data.pojo;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.arena.base.BaseRecordData;
import com.kevin.dueltime4.util.UtilFormat;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ClassicArenaRecordData extends BaseRecordData {
    private final String opponentName;
    private final Result result;
    private final int time;
    private final double expChange;
    private final int hitTime;
    private final double totalDamage;
    private final double maxDamage;
    private final double averageDamage;

    public ClassicArenaRecordData(String playerName, String arenaId, String opponentName, Result result, int time, double expChange, int hitTime, double totalDamage, double maxDamage, double averageDamage, String date) {
        super(playerName, arenaId, date);
        this.opponentName = opponentName;
        this.result = result;
        this.time = time;
        this.expChange = expChange;
        this.hitTime = hitTime;
        this.totalDamage = totalDamage;
        this.maxDamage = maxDamage;
        this.averageDamage = averageDamage;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public Result getResult() {
        return result;
    }

    public int getTime() {
        return time;
    }

    public double getExpChange() {
        return expChange;
    }

    public int getHitTime() {
        return hitTime;
    }

    public double getTotalDamage() {
        return totalDamage;
    }

    public double getMaxDamage() {
        return maxDamage;
    }

    public double getAverageDamage() {
        return averageDamage;
    }

    @Override
    public String getItemStackTitle() {
        return MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_TITLE, getPlayer());
    }

    @Override
    public List<String> getItemStackContent() {
        List<String> content = new ArrayList<>();
        Player player = getPlayer();
        BaseArena arena = DuelTimePlugin.getInstance().getArenaManager().get(getArenaId());
        content.add(MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_CONTENT_ARENA_NAME, player,
                arena != null ? arena.getArenaData().getName() : getArenaId()));
        content.add(MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_CONTENT_RESULT, player,
                MsgBuilder.get(result.msg, player)));
        content.add(MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_CONTENT_OPPONENT, player,
                opponentName));
        content.add(MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_CONTENT_TIME, player,
                "" + time));
        content.add(MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_CONTENT_EXP_CHANGE, player,
                UtilFormat.distinguishPositiveNumber(expChange)));
        content.add(MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_CONTENT_TOTAL_DAMAGE, player,
                "" + UtilFormat.round(totalDamage, 2)));
        content.add(MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_CONTENT_MAX_DAMAGE, player,
                "" + UtilFormat.round(maxDamage, 2)));
        content.add(MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_CONTENT_AVERAGE_DAMAGE, player,
                "" + UtilFormat.round(averageDamage, 2)));
        content.add(MsgBuilder.get(Msg.RECORD_TYPE_CLASSIC_CONTENT_DATE, player,
                getDate()));
        return content;
    }

    public enum Result {
        WIN(Msg.STRING_WIN), LOSE(Msg.STRING_LOSE), DRAW(Msg.STRING_DRAW);

        private final Msg msg;

        Result(Msg msg) {
            this.msg = msg;
        }

        public Msg getMsg() {
            return msg;
        }
    }
}
