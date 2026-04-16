package com.kevin.dueltime4.data;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.ArenaManager;
import com.kevin.dueltime4.cache.*;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class MyBatisManager {
    private final HashMap<DatabaseType, SqlSessionFactory> factoryMap = new HashMap<>();
    private final HashMap<DatabaseType, PooledDataSource> dataSourceMap = new HashMap<>();
    private final HashMap<Class<?>, DatabaseType> moduleStorageMap = new HashMap<>();

    public MyBatisManager() {
        connectToDatabase();
    }

    public SqlSessionFactory getFactory(Class<?> clazz) {
        return factoryMap.get(moduleStorageMap.get(clazz));
    }

    public DatabaseType getType(Class<?> clazz) {
        return moduleStorageMap.get(clazz);
    }

    private DatabaseType getTypeFromConfig(FileConfiguration config, String key) {
        String path = "Database.type." + key;
        if (config.contains(path)) {
            return config.getString(path).equalsIgnoreCase("mysql") ? DatabaseType.MYSQL : DatabaseType.SQLITE;
        }
        return DatabaseType.SQLITE;
    }

    public void connectToDatabase() {
        DuelTimePlugin plugin = DuelTimePlugin.getInstance();
        FileConfiguration config = plugin.getCfgManager().getConfig();
        moduleStorageMap.put(ArenaManager.class, getTypeFromConfig(config, "arena"));
        moduleStorageMap.put(BlacklistCache.class, getTypeFromConfig(config, "blacklist"));
        moduleStorageMap.put(LocationCache.class, getTypeFromConfig(config, "location"));
        moduleStorageMap.put(PlayerDataCache.class, getTypeFromConfig(config, "player-data"));
        moduleStorageMap.put(RecordCache.class, getTypeFromConfig(config, "record"));
        moduleStorageMap.put(ShopCache.class, getTypeFromConfig(config, "shop"));
        for (DatabaseType type : DatabaseType.values()) {
            if (moduleStorageMap.containsValue(type)) { // 當存在某模組用到了該型別的資料庫，才初始化
                InputStream inputStream;
                try {
                    inputStream = Resources.getResourceAsStream("mybatis-config.xml");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, type.getEnvId());
                Configuration configuration = sqlSessionFactory.getConfiguration();
                Environment environment = configuration.getEnvironment();
                PooledDataSource dataSource = (PooledDataSource) environment.getDataSource();
                if (type.isRemote) {
                    dataSource.setUrl(config.getString("Database.url"));
                    dataSource.setUsername(config.getString("Database.username"));
                    dataSource.setPassword(config.getString("Database.password"));
                } else {
                    dataSource.setUrl(dataSource.getUrl().replace("{}", plugin.getDataFolder().getPath() + File.separator));
                }
                factoryMap.put(type, sqlSessionFactory);
                dataSourceMap.put(type, dataSource);
            }
        }
    }

    public void closeConnection() {
        dataSourceMap.values().forEach(PooledDataSource::forceCloseAll);
        dataSourceMap.clear();
        moduleStorageMap.clear();
        factoryMap.clear();
    }

    public enum DatabaseType {
        MYSQL("mysql", true),
        SQLITE("sqlite", false);

        private final String envId;
        private final boolean isRemote;

        DatabaseType(String envId, boolean isRemote) {
            this.envId = envId;
            this.isRemote = isRemote;
        }

        public String getEnvId() {
            return envId;
        }

        public boolean isRemote() {
            return isRemote;
        }
    }
}
