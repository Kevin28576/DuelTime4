package com.kevin.dueltime4.data.mapper;

import com.kevin.dueltime4.data.MyBatisManager;
import com.kevin.dueltime4.data.pojo.PlayerData;
import com.kevin.dueltime4.ranking.RankingData;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface PlayerDataMapper {
    @MapKey("id")
    Map<String, PlayerData> getMap();

    PlayerData get(String id);

    void insertOrUpdateSQLite(PlayerData playerData);

    void insertOrUpdateMySQL(PlayerData playerData);

    void createTableIfNotExists();

    List<RankingData> selectWinsRanking();

    List<RankingData> selectWinRateRanking();

    List<RankingData> selectTotalGameNumberRanking();

    List<RankingData> selectClassicGameNumberRanking();

    List<RankingData> selectTotalGameTimeRanking();

    List<RankingData> selectClassicGameTimeRanking();

    List<RankingData> selectExpRanking();
}
