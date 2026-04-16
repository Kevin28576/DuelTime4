package com.kevin.dueltime4.data.mapper;

import com.kevin.dueltime4.arena.base.BaseRecordData;
import com.kevin.dueltime4.data.pojo.ClassicArenaRecordData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ClassicArenaRecordDataMapper {
    List<BaseRecordData> getAll(@Param("playerName") String playerName);

    void add(ClassicArenaRecordData arenaRecordData);

    void createTableIfNotExists();
}
