package com.kevin.dueltime4.ranking.hologram;

import com.kevin.dueltime4.DuelTimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HologramInstance {
    private Object hologram;
    private final HologramPluginType hologramPluginType;

    public HologramInstance(HologramPluginType hologramPluginType, Location location, String rankingId, List<String> content, Material material) {
        this.hologramPluginType = hologramPluginType;
        try {
            switch (hologramPluginType) {
                case HOLOGRAPHIC_DISPLAY:
                    this.hologram = createHolographicDisplay(location);
                    refreshHolographicDisplay(content, material);
                    break;
                case CMI:
                    this.hologram = createCMIHologram(rankingId, location);
                    refreshCMIHologram(content);
                    break;
                case DECENT_HOLOGRAM:
                    this.hologram = createDecentHologram(rankingId, location);
                    refreshDecentHologram(content, material);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            DuelTimePlugin.getInstance().getLogger().warning("Failed to create hologram (" + hologramPluginType + "): " + e.getMessage());
            this.hologram = null;
        }
    }

    public void destroy() {
        if (hologram == null) {
            return;
        }
        try {
            switch (hologramPluginType) {
                case HOLOGRAPHIC_DISPLAY:
                case DECENT_HOLOGRAM:
                    invokeMethod(hologram, "delete");
                    break;
                case CMI:
                    invokeMethod(hologram, "remove");
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            DuelTimePlugin.getInstance().getLogger().warning("Failed to destroy hologram (" + hologramPluginType + "): " + e.getMessage());
        }
    }

    public void refresh(List<String> content, Material material) {
        if (hologram == null) {
            return;
        }
        switch (hologramPluginType) {
            case HOLOGRAPHIC_DISPLAY:
                Bukkit.getScheduler().runTask(DuelTimePlugin.getInstance(), () -> {
                    try {
                        refreshHolographicDisplay(content, material);
                    } catch (Exception e) {
                        DuelTimePlugin.getInstance().getLogger().warning("Failed to refresh holographic display: " + e.getMessage());
                    }
                });
                break;
            case CMI:
                try {
                    refreshCMIHologram(content);
                } catch (Exception e) {
                    DuelTimePlugin.getInstance().getLogger().warning("Failed to refresh CMI hologram: " + e.getMessage());
                }
                break;
            case DECENT_HOLOGRAM:
                Bukkit.getScheduler().runTask(DuelTimePlugin.getInstance(), () -> {
                    try {
                        refreshDecentHologram(content, material);
                    } catch (Exception e) {
                        DuelTimePlugin.getInstance().getLogger().warning("Failed to refresh DecentHolograms hologram: " + e.getMessage());
                    }
                });
                break;
            default:
                break;
        }
    }

    public void move(Location location) {
        if (hologram == null) {
            return;
        }
        try {
            switch (hologramPluginType) {
                case HOLOGRAPHIC_DISPLAY:
                    invokeMethod(hologram, "teleport", location);
                    break;
                case CMI:
                    invokeMethod(hologram, "setLoc", location);
                    invokeMethod(hologram, "refresh");
                    break;
                case DECENT_HOLOGRAM:
                    Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
                    invokeStaticMethod(dhapiClass, "moveHologram", hologram, location);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            DuelTimePlugin.getInstance().getLogger().warning("Failed to move hologram (" + hologramPluginType + "): " + e.getMessage());
        }
    }

    private Object createHolographicDisplay(Location location) throws Exception {
        Class<?> hologramsAPI = Class.forName("com.gmail.filoghost.holographicdisplays.api.HologramsAPI");
        return invokeStaticMethod(hologramsAPI, "createHologram", DuelTimePlugin.getInstance(), location);
    }

    private Object createCMIHologram(String rankingId, Location location) throws Exception {
        Class<?> hologramClass = Class.forName("com.Zrips.CMI.Modules.Holograms.CMIHologram");
        Constructor<?> constructor = hologramClass.getConstructor(String.class, Location.class);
        Object cmiHologram = constructor.newInstance(rankingId, location);
        invokeMethod(cmiHologram, "setLoc", location);
        return cmiHologram;
    }

    private Object createDecentHologram(String rankingId, Location location) throws Exception {
        Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
        String cleanId = rankingId.replace("dueltime4:", "");
        return invokeStaticMethod(dhapiClass, "createHologram", cleanId, location, new ArrayList<String>());
    }

    private void refreshHolographicDisplay(List<String> content, Material material) throws Exception {
        invokeMethod(hologram, "clearLines");
        if (material != null) {
            invokeMethod(hologram, "appendItemLine", new ItemStack(material));
        }
        for (String line : content) {
            invokeMethod(hologram, "appendTextLine", line);
        }
    }

    private void refreshCMIHologram(List<String> content) throws Exception {
        invokeMethod(hologram, "setLines", content);
        invokeMethod(hologram, "refresh");
    }

    private void refreshDecentHologram(List<String> content, Material material) throws Exception {
        Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
        invokeStaticMethod(dhapiClass, "setHologramLines", hologram, new ArrayList<>());
        if (material != null) {
            invokeStaticMethod(dhapiClass, "addHologramLine", hologram, material);
        }
        for (String line : content) {
            invokeStaticMethod(dhapiClass, "addHologramLine", hologram, line);
        }
    }

    private Object invokeStaticMethod(Class<?> clazz, String methodName, Object... args) throws Exception {
        Method method = findCompatibleMethod(clazz, methodName, args);
        return method.invoke(null, args);
    }

    private Object invokeMethod(Object target, String methodName, Object... args) throws Exception {
        Method method = findCompatibleMethod(target.getClass(), methodName, args);
        return method.invoke(target, args);
    }

    private Method findCompatibleMethod(Class<?> clazz, String methodName, Object... args) throws NoSuchMethodException {
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != args.length) {
                continue;
            }
            boolean compatible = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!isCompatible(parameterTypes[i], args[i])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                return method;
            }
        }
        throw new NoSuchMethodException("Method not found: " + clazz.getName() + "#" + methodName);
    }

    private boolean isCompatible(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return !parameterType.isPrimitive();
        }
        Class<?> argClass = arg.getClass();
        if (parameterType.isAssignableFrom(argClass)) {
            return true;
        }
        if (parameterType.isPrimitive()) {
            if (parameterType == boolean.class) {
                return argClass == Boolean.class;
            }
            if (parameterType == byte.class) {
                return argClass == Byte.class;
            }
            if (parameterType == short.class) {
                return argClass == Short.class;
            }
            if (parameterType == int.class) {
                return argClass == Integer.class;
            }
            if (parameterType == long.class) {
                return argClass == Long.class;
            }
            if (parameterType == float.class) {
                return argClass == Float.class;
            }
            if (parameterType == double.class) {
                return argClass == Double.class;
            }
            if (parameterType == char.class) {
                return argClass == Character.class;
            }
        }
        return false;
    }
}
