package com.kevin.dueltime4.cache;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.data.mapper.LocationMapper;
import com.kevin.dueltime4.data.pojo.LocationData;
import com.kevin.dueltime4.event.cache.CacheInitializedEvent;
import com.kevin.dueltime4.util.UtilSync;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.Location;

import java.util.*;

public class LocationCache {
    private Map<String, LocationData> locationDataMap = new HashMap<>();

    public void reload() {
        SqlSessionFactory sqlSessionFactory = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass());
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            LocationMapper mapper = sqlSession.getMapper(LocationMapper.class);
            mapper.createTableIfNotExists();
            locationDataMap = mapper.getMap();
            UtilSync.publishEvent(new CacheInitializedEvent(this.getClass()));
        }
    }

    public Location get(String id) {
        if (locationDataMap.containsKey(id)) {
            return locationDataMap.get(id).getLocation();
        } else {
            return null;
        }
    }

    public LocationData getData(String id) {
        return locationDataMap.get(id);
    }

    public void add(String id, Location location) {
        LocationData locationData = new LocationData(id, location);
        locationDataMap.put(id, locationData);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            sqlSession.getMapper(LocationMapper.class).add(locationData);
        }
    }

    public void set(String id, Location location) {
        LocationData locationData = locationDataMap.get(id);
        locationData.setLocation(location);
        locationDataMap.put(id, locationData);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            sqlSession.getMapper(LocationMapper.class).update(locationData);
        }
    }


    public void remove(String id) {
        locationDataMap.remove(id);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            sqlSession.getMapper(LocationMapper.class).remove(id);
        }
    }

    public enum InternalType {
        LOBBY("dueltime4:lobby");

        private final String id;

        InternalType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
