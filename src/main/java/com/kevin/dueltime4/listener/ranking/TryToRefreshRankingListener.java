package com.kevin.dueltime4.listener.ranking;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.cache.PlayerDataCache;
import com.kevin.dueltime4.data.MyBatisManager;
import com.kevin.dueltime4.data.mapper.PlayerDataMapper;
import com.kevin.dueltime4.event.ranking.TryToRefreshRankingEvent;
import com.kevin.dueltime4.level.LevelManager;
import com.kevin.dueltime4.ranking.Ranking;
import com.kevin.dueltime4.ranking.RankingData;
import com.kevin.dueltime4.ranking.RankingManager;
import com.kevin.dueltime4.ranking.hologram.HologramManager;
import com.kevin.dueltime4.yaml.message.Msg;
import com.kevin.dueltime4.yaml.message.MsgBuilder;
import org.apache.ibatis.session.SqlSession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TryToRefreshRankingListener implements Listener {
    @EventHandler
    public void tryToRefreshRankingListener(TryToRefreshRankingEvent e) {
        Ranking ranking = e.getRanking();
        String rankingId = ranking.getId();
        if (!Arrays.stream(RankingManager.InternalType.values()).map(RankingManager.InternalType::getId).collect(Collectors.toList()).contains(rankingId)) {
            return;
        }
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        MyBatisManager myBatisManager = plugin.getMyBatisManager();
        try (SqlSession sqlSession = myBatisManager.getFactory(PlayerDataCache.class).openSession(true)) {
            PlayerDataMapper mapper = sqlSession.getMapper(PlayerDataMapper.class);
            List<RankingData> rankingDataList = null;
            if (rankingId.equals(RankingManager.InternalType.CLASSIC_WIN_NUMBER.getId())) {
                rankingDataList = mapper.selectWinsRanking();
                for (RankingData data : rankingDataList) {
                    data.setExtraStr(Msg.STRING_GAMES);
                }
            } else if (rankingId.equals(RankingManager.InternalType.CLASSIC_WIN_RATE.getId())) {
                rankingDataList = mapper.selectWinRateRanking();
                for (RankingData data : rankingDataList) {
                    data.setExtraStr("%");
                }
            } else if (rankingId.equals(RankingManager.InternalType.EXP.getId())) {
                rankingDataList = mapper.selectExpRanking();
                LevelManager levelManager = DuelTimePlugin.getInstance().getLevelManager();
                for (RankingData data : rankingDataList) {
                    data.setExtraStr("§7 (" + levelManager.calculateTier(levelManager.calculateLevel((double) data.getData())).getTitle() + "§7)");
                }
            } else if (rankingId.equals(RankingManager.InternalType.TOTAL_GAME_NUMBER.getId())) {
                rankingDataList = mapper.selectTotalGameNumberRanking();
                for (RankingData data : rankingDataList) {
                    data.setExtraStr(Msg.STRING_GAMES);
                }
            } else if (rankingId.equals(RankingManager.InternalType.TOTAL_GAME_TIME.getId())) {
                rankingDataList = mapper.selectTotalGameTimeRanking();
                for (RankingData data : rankingDataList) {
                    data.setExtraStr(Msg.STRING_SECOND);
                }
            } else if (rankingId.equals(RankingManager.InternalType.CLASSIC_GAME_NUMBER.getId())) {
                rankingDataList = mapper.selectClassicGameNumberRanking();
                for (RankingData data : rankingDataList) {
                    data.setExtraStr(Msg.STRING_GAMES);
                }
            } else if (rankingId.equals(RankingManager.InternalType.CLASSIC_GAME_TIME.getId())) {
                rankingDataList = mapper.selectClassicGameTimeRanking();
                for (RankingData data : rankingDataList) {
                    data.setExtraStr(Msg.STRING_SECOND);
                }
            }
            plugin.getRankingManager().updateRanking(rankingId, rankingDataList, plugin);
            HologramManager hologramManager = plugin.getHologramManager();
            if (hologramManager.isEnabled() && hologramManager.getHologramInstanceMap().containsKey(rankingId)) {
                hologramManager.refresh(ranking);
            }
        }
        if (e.getSender() != null) {
            MsgBuilder.send(Msg.COMMAND_SUB_RANK_REFRESH_INTERNAL_SUCCESSFULLY, e.getSender(),
                    ranking.getName(e.getSender()),
                    rankingId);
        }
    }
}
