package com.kevin.dueltime4.cache;

public class CacheManager {
    PlayerDataCache playerDataCache;
    BlacklistCache blacklistCache;
    ShopCache shopCache;
    LocationCache locationCache;
    RecordCache recordCache;

    public void load() {
        playerDataCache = new PlayerDataCache();
        blacklistCache = new BlacklistCache();
        shopCache = new ShopCache();
        locationCache = new LocationCache();
        recordCache = new RecordCache();
        reload();
    }

    public void reload() {
        playerDataCache.reload();
        playerDataCache.reloadRefreshRankingTimer();
        blacklistCache.reload();
        shopCache.reload();
        locationCache.reload();
        recordCache.reload();
    }

    public PlayerDataCache getPlayerDataCache() {
        return playerDataCache;
    }

    public BlacklistCache getBlacklistCache() {
        return blacklistCache;
    }

    public ShopCache getShopCache() {
        return shopCache;
    }

    public LocationCache getLocationCache() {
        return locationCache;
    }

    public RecordCache getArenaRecordCache() {
        return recordCache;
    }
}
