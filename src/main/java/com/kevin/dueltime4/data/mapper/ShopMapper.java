package com.kevin.dueltime4.data.mapper;

import com.kevin.dueltime4.data.pojo.ShopRewardData;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ShopMapper {
    List<ShopRewardData> getList();

    void add(ShopRewardData shopRewardData);

    void remove(int id);

    void set(ShopRewardData shopRewardData);

    void createTable(@Param("databaseType") String databaseType);
}
