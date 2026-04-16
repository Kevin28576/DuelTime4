package com.kevin.dueltime4.data.mapper;

import com.kevin.dueltime4.data.pojo.LocationData;
import org.apache.ibatis.annotations.MapKey;

import java.util.Map;

public interface LocationMapper {
    @MapKey("id")
    Map<String, LocationData> getMap();

    void add(LocationData locationData);

    void remove(String id);

    void update(LocationData locationData);

    void createTableIfNotExists();
}
