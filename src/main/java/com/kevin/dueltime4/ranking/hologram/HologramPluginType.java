package com.kevin.dueltime4.ranking.hologram;

import org.bukkit.Bukkit;

public enum HologramPluginType {
    HOLOGRAPHIC_DISPLAY("HolographicDisplay", "HolographicDisplays"),
    CMI("CMI"),
    DECENT_HOLOGRAM("DecentHolograms");

    private final String[] pluginNames;

    HologramPluginType(String... pluginNames) {
        this.pluginNames = pluginNames;
    }

    public String[] getPluginNames() {
        return pluginNames;
    }

    public String getPluginName() {
        for (String realName : pluginNames) {
            if (Bukkit.getPluginManager().isPluginEnabled(realName)) {
                return realName;
            }
        }
        return pluginNames[0];
    }
}
