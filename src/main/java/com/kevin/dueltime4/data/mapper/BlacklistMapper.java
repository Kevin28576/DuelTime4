package com.kevin.dueltime4.data.mapper;

import java.util.List;

public interface BlacklistMapper {
    List<String> get();

    void createTableIfNotExists();

    void add(String id);

    void remove(String id);
}
