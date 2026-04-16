package com.kevin.dueltime4.viaversion;

import com.kevin.dueltime4.DuelTimePlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static com.kevin.dueltime4.viaversion.ViaVersion.TitleType.*;

public class ViaVersion {

    private static Class<?> getNmsClass(String name)
            throws ClassNotFoundException {
        if (DuelTimePlugin.serverVersionInt >= 17) {
            return Class.forName("net.minecraft.server." + name);
        } else {
            return Class.forName("net.minecraft.server." + DuelTimePlugin.serverVersion + "." + name);
        }
    }

    private static Class<?> getCbClass(String name)
            throws ClassNotFoundException {
        if (DuelTimePlugin.serverVersion == null || DuelTimePlugin.serverVersion.isEmpty()) {
            return Class.forName("org.bukkit.craftbukkit." + name);
        }
        return Class.forName("org.bukkit.craftbukkit."
                + DuelTimePlugin.serverVersion + "." + name);
    }

    private static Class<?> iChatBaseComponent;
    private static Class<?> chatComponentText;
    private static Class<?> packet;
    private static Class<?> packetPlayOutTitle;
    private static Class<?> enumTitleAction;
    private static boolean particleWarningLogged = false;

    public static void getClassesForTitleAndAction() {
        try {
            iChatBaseComponent = getNmsClass("IChatBaseComponent");
            chatComponentText = getNmsClass("ChatComponentText");
            packet = getNmsClass("Packet");
            packetPlayOutTitle = getNmsClass("PacketPlayOutTitle");
            enumTitleAction = getNmsClass("PacketPlayOutTitle$EnumTitleAction");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public enum TitleType {
        TITLE, SUBTITLE, PARALLEL, LINE
    }

    /*
    ?喲?芸?蝢拇楚?乓??楚?箸???Title??嚗??1.8隞乩????嚗隞仿??虫誑?Ｗ????耦撘?蜓title?title??喲?銝西?嚗?     */
    public static void sendTitle(Player player, String title, String subTitle,
                                 int fadeIn, int stay, int fadeOut, TitleType titleTypeAsMessage) {
        int version = DuelTimePlugin.serverVersionInt;
        if (DuelTimePlugin.serverVersionInt <= 7) {
            if (titleTypeAsMessage != null) {
                if (titleTypeAsMessage == TITLE)
                    player.sendMessage(title);
                else if (titleTypeAsMessage == SUBTITLE)
                    player.sendMessage(subTitle);
                else if (titleTypeAsMessage == PARALLEL) {
                    player.sendMessage(title);
                    player.sendMessage(subTitle);
                } else
                    player.sendMessage(title + " " + subTitle);
            }
        } else if (version <= 9) {
            try {
                Enum<?>[] enumConstants = (Enum<?>[]) enumTitleAction
                        .getEnumConstants();
                Object enumTITLE = null;
                Object enumSUBTITLE = null;
                Object enumTIMES = null;
                for (Enum<?> enum1 : enumConstants) {
                    String name = enum1.name();
                    if (name.equals("TITLE")) {
                        enumTITLE = enum1;
                    }
                    if (name.equals("SUBTITLE")) {
                        enumSUBTITLE = enum1;
                    }
                    if (name.equals("TIMES")) {
                        enumTIMES = enum1;
                    }
                }
                Object chatComponentTextTitleInstance = chatComponentText
                        .getConstructor(String.class).newInstance(title);
                Object chatComponentTextSubTitleInstance = chatComponentText
                        .getConstructor(String.class).newInstance(subTitle);
                Object packetPlayOutTitleTitleInstance = packetPlayOutTitle
                        .getConstructor(enumTitleAction, iChatBaseComponent)
                        .newInstance(enumTITLE, chatComponentTextTitleInstance);
                Object packetPlayOutTitleSubTitleInstance = packetPlayOutTitle
                        .getConstructor(enumTitleAction, iChatBaseComponent)
                        .newInstance(enumSUBTITLE,
                                chatComponentTextSubTitleInstance);
                Object packetPlayOutTitleTimeInstance = packetPlayOutTitle
                        .getConstructor(enumTitleAction, iChatBaseComponent,
                                int.class, int.class, int.class).newInstance(
                                enumTIMES, null, fadeIn, stay, fadeOut);
                Object craftPlayer = getCbClass("entity.CraftPlayer").cast(
                        player);
                Object entityPlayer = craftPlayer.getClass()
                        .getMethod("getHandle").invoke(craftPlayer);
                Object playerConnection = entityPlayer.getClass()
                        .getField("playerConnection").get(entityPlayer);
                playerConnection
                        .getClass()
                        .getMethod("sendPacket", packet)
                        .invoke(playerConnection,
                                packetPlayOutTitleTitleInstance);
                playerConnection
                        .getClass()
                        .getMethod("sendPacket", packet)
                        .invoke(playerConnection,
                                packetPlayOutTitleSubTitleInstance);
                playerConnection
                        .getClass()
                        .getMethod("sendPacket", packet)
                        .invoke(playerConnection,
                                packetPlayOutTitleTimeInstance);
            } catch (Exception ignored) {
            }
        } else {
            player.sendTitle(title, subTitle, fadeIn, stay, fadeOut);
        }
    }


    /*
    ?喲ctionBar??嚗??1.8隞乩????嚗隞仿??虫誑?Ｗ????耦撘?蜓title
    擃??祉?API??鈭?亙?ctionBar?瘜?雿??臬??.12.2?PI????隞仿ㄐ?函??舐撠敺??喲瘜?     */
    public static void sendActionBar(Player player, String actionbar, boolean considerLowVersion) {
        int version = DuelTimePlugin.serverVersionInt;
        if (version <= 7) {
            if (considerLowVersion) {
                player.sendMessage(actionbar);
            }
        } else if (version <= 9) {
            try {
                Class<?> craftPlayerClass = getCbClass("entity.CraftPlayer");
                Class<?> packetPlayOutChatClass = getNmsClass("PacketPlayOutChat");
                Class<?> iChatBaseComponentClass = getNmsClass("IChatBaseComponent");
                Class<?> chatSerializerClass = getNmsClass("ChatSerializer");
                Class<?> packetClass = getNmsClass("Packet");
                Object craftPlayer = craftPlayerClass.cast(player);
                Object entityPlayer = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
                Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
                Object chatBaseComponent = chatSerializerClass.getMethod("a", String.class)
                        .invoke(null, "{\"text\":\"" + actionbar + "\"}");
                Object packet = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, byte.class)
                        .newInstance(chatBaseComponent, (byte) 2);
                playerConnection.getClass().getMethod("sendPacket", packetClass).invoke(playerConnection, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(actionbar));
        }
    }

    // ?冽???蝎????輯??銝??撅望瘜?雿ms???砍榆?啣祕?典云憭找?嚗誑敺?Ｘ
    public static void spawnRedstoneParticle(Player viewer, Location location, float colorR, float colorG, float colorB) {
        try {
            if (DuelTimePlugin.serverVersionInt >= 17) {
                Color particleColor = Color.fromRGB(clampColor(colorR), clampColor(colorG), clampColor(colorB));
                viewer.getWorld().spawnParticle(
                        Particle.DUST,
                        location,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0,
                        new Particle.DustOptions(particleColor, 1.0f)
                );
                return;
            }

            Object packet;
            Class<?> packetClass = getNmsClass("Packet");
            try {
                Class<?> packetPlayOutWorldParticlesClass = getNmsClass("PacketPlayOutWorldParticles");
                try {
                    Class<?> particleParamRedstoneClass = getNmsClass("ParticleParamRedstone");
                    Class<?> particleParamClass = getNmsClass("ParticleParam");
                    Constructor<?> particleParamRedstoneConstructor = particleParamRedstoneClass.getConstructor(float.class, float.class, float.class, float.class);
                    Object particleParamRedstone = particleParamRedstoneConstructor.newInstance(
                            colorR / 255.0f, colorG / 255.0f, colorB / 255.0f, 1.0f);
                    Object particleParam = particleParamClass.cast(particleParamRedstone);
                    packet = packetPlayOutWorldParticlesClass.getConstructor(
                            particleParamClass, boolean.class, double.class, double.class, double.class, float.class, float.class, float.class, float.class, int.class
                    ).newInstance(
                            particleParam, false, location.getX(), location.getY(), location.getZ(), 0, 0, 0, 1, 0);
                } catch (NoSuchMethodException ignored) {
                    packet = packetPlayOutWorldParticlesClass.getConstructor(
                            String.class,
                            float.class,
                            float.class,
                            float.class,
                            float.class,
                            float.class,
                            float.class,
                            float.class,
                            int.class
                    ).newInstance(
                            "reddust",
                            (float) location.getX(),
                            (float) location.getY(),
                            (float) location.getZ(),
                            colorR / 255,
                            colorG / 255,
                            colorB / 255,
                            1.0f,
                            0
                    );
                }
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                Class<?> packetPlayOutWorldParticlesClass = getNmsClass("PacketPlayOutWorldParticles");
                Class<?> enumParticleClass = getNmsClass("EnumParticle");
                Object reddustEnum = enumParticleClass.getField("REDSTONE").get(null);
                packet = packetPlayOutWorldParticlesClass.getConstructor(
                        enumParticleClass,
                        boolean.class,
                        float.class,
                        float.class,
                        float.class,
                        float.class,
                        float.class,
                        float.class,
                        float.class,
                        int.class,
                        int[].class
                ).newInstance(
                        reddustEnum,
                        true,
                        (float) location.getX(),
                        (float) location.getY(),
                        (float) location.getZ(),
                        colorR / 255,
                        colorG / 255,
                        colorB / 255,
                        1.0f,
                        0,
                        new int[0]
                );
            }
            Class<?> craftPlayerClass = getCbClass("entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(viewer);
            Object entityPlayer = craftPlayerClass.getMethod("getHandle").invoke(craftPlayer);
            Object playerConnection = entityPlayer.getClass().getField("playerConnection").get(entityPlayer);
            playerConnection.getClass().getMethod("sendPacket", packetClass).invoke(playerConnection, packet);
        } catch (Exception e) {
            logParticleWarning(e);
        }
    }

    private static int clampColor(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    private static void logParticleWarning(Exception exception) {
        if (particleWarningLogged) {
            return;
        }
        particleWarningLogged = true;
        String message = exception.getMessage() == null ? "" : " - " + exception.getMessage();
        if (DuelTimePlugin.getInstance() != null) {
            DuelTimePlugin.getInstance().getLogger().warning("Failed to spawn arena preview particles" + message);
            return;
        }
        Bukkit.getLogger().warning("Failed to spawn arena preview particles" + message);
    }

    public static Object getCraftSplashPotion() {
        try {
            return getCbClass("entity.CraftSplashPotion");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getCraftProjectile() {
        try {
            return getCbClass("entity.CraftProjectile");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object getCraftProjectileSource(Entity entity,
                                                  String entityName) {
        Object ps = null;
        try {
            Class<?> clazz = getCbClass("entity." + entityName);
            Method method = clazz.getMethod("getShooter");
            ps = method.invoke(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ps;
    }

    public static Sound getSound(String... soundNames) {
        for (String soundName : soundNames) {
            Sound sound = getSoundByRegistryKey(soundName);
            if (sound != null) {
                return sound;
            }
            sound = getSoundByStaticField(soundName);
            if (sound != null) {
                return sound;
            }
        }
        return null;
    }

    private static Sound getSoundByRegistryKey(String soundName) {
        try {
            NamespacedKey namespacedKey;
            if (soundName.contains(":")) {
                namespacedKey = NamespacedKey.fromString(soundName.toLowerCase(Locale.ROOT));
            } else {
                namespacedKey = NamespacedKey.minecraft(soundName.toLowerCase(Locale.ROOT));
            }
            if (namespacedKey == null) {
                return null;
            }
            return Registry.SOUNDS.get(namespacedKey);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Sound getSoundByStaticField(String soundName) {
        Sound sound = getSoundByStaticFieldRaw(soundName);
        if (sound != null) {
            return sound;
        }
        return getSoundByStaticFieldRaw(soundName.toUpperCase(Locale.ROOT));
    }

    private static Sound getSoundByStaticFieldRaw(String soundName) {
        try {
            Field field = Sound.class.getField(soundName);
            Object soundObj = field.get(null);
            if (soundObj instanceof Sound) {
                return (Sound) soundObj;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }


    /**
     * 隞?.7?誑敺?API嚗瘜ukkit.getOnlinePlayers()?????亥?銋????砌???     *
     * @return ?拙振??
     */
    public static List<Player> getOnlinePlayers() {
        List<Player> players = null;
        try {
            Class<?> clazz = Class.forName("org.bukkit.Bukkit");
            Method method = clazz.getMethod("getOnlinePlayers");
            if (method.getReturnType().equals(Collection.class)) {
                Collection<?> rawPlayers = (Collection<?>) (method
                        .invoke(Bukkit.getServer()));
                players = new ArrayList<>();
                for (Object o : rawPlayers) {
                    if (o instanceof Player) {
                        players.add((Player) o);
                    }
                }
            } else {
                players = Arrays.asList((Player[]) method.invoke(Bukkit
                        .getServer()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return players;
    }
}
