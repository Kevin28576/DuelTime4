package com.kevin.dueltime4.listener;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.listener.arena.BaseArenaListener;
import com.kevin.dueltime4.listener.arena.ClassicArenaListener;
import com.kevin.dueltime4.listener.cache.CacheInitializedListener;
import com.kevin.dueltime4.listener.cache.JoinServerForLoadingPlayerDataCacheListener;
import com.kevin.dueltime4.listener.chat.ChatListener;
import com.kevin.dueltime4.listener.gui.*;
import com.kevin.dueltime4.listener.network.CheckVersionListener;
import com.kevin.dueltime4.listener.wait.*;
import com.kevin.dueltime4.listener.progress.*;
import com.kevin.dueltime4.listener.ranking.TryToRefreshRankingListener;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class ListenerManager {
    public static void register() {
        Listener[] listeners = {
                new ShopListener(),
                new JoinServerForLoadingPlayerDataCacheListener(),
                new ProgressAutoUploadListener(),
                new ProgressUploadListener(),
                new ProgressOperateListener(),
                new ProgressStartListener(),
                new ProgressFinishedListener(),
                new TryToRefreshRankingListener(),
                new ClassicArenaListener(),
                new ArenaRecordListener(),
                new ChatListener(),
                new StartListener(),
                new WaitingListener(),
                new CloseInventoryListener(),
                new BaseArenaListener(),
                new SimpleGUIListener(),
                new QueueMatchConfirmGUIListener(),
                new CacheInitializedListener(),
                new CheckVersionListener(),
        };
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener,
                    DuelTimePlugin.getInstance());
        }
    }
}
