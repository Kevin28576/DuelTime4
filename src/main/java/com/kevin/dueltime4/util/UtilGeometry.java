package com.kevin.dueltime4.util;

import com.kevin.dueltime4.DuelTimePlugin;
import com.kevin.dueltime4.arena.base.BaseArena;
import com.kevin.dueltime4.viaversion.ViaVersion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class UtilGeometry {
    public static void buildCubicLine(Player viewer, Location diagonalPointA, Location diagonalPointB, double interval, float colorR, float colorG, float colorB) {
        int axLog = diagonalPointA.getBlockX();
        int ayLog = diagonalPointA.getBlockY();
        int azLog = diagonalPointA.getBlockZ();
        int bxLog = diagonalPointB.getBlockX();
        int byLog = diagonalPointB.getBlockY();
        int bzLog = diagonalPointB.getBlockZ();
        int ax = Math.max(axLog, bxLog) + 1;
        int ay = Math.max(ayLog, byLog) + 1;
        int az = Math.max(azLog, bzLog) + 1;
        int bx = Math.min(axLog, bxLog);
        int by = Math.min(ayLog, byLog);
        int bz = Math.min(azLog, bzLog);
        World world = diagonalPointA.getWorld();
        Location startPointFromXPlane1 = new Location(world, bx, ay, az);
        Location startPointFromXPlane2 = new Location(world, bx, ay, bz);
        Location startPointFromXPlane3 = new Location(world, bx, by, az);
        Location startPointFromXPlane4 = new Location(world, bx, by, bz);

        Location startPointFromYPlane1 = new Location(world, ax, by, az);
        Location startPointFromYPlane2 = new Location(world, ax, by, bz);
        Location startPointFromYPlane3 = new Location(world, bx, by, az);
        Location startPointFromYPlane4 = new Location(world, bx, by, bz);

        Location startPointFromZPlane1 = new Location(world, ax, ay, bz);
        Location startPointFromZPlane2 = new Location(world, ax, by, bz);
        Location startPointFromZPlane3 = new Location(world, bx, ay, bz);
        Location startPointFromZPlane4 = new Location(world, bx, by, bz);
        for (double dx = 0, dy = 0, dz = 0;
             !(dx == -1 && dy == -1 && dz == -1);
             dx = (dx < ax - bx && dx != -1) ? (dx + interval > ax - bx ? ax - bx : dx + interval) : -1,
                     dy = (dy < ay - by && dy != -1) ? (dy + interval > ay - by ? ay - by : dy + interval) : -1,
                     dz = (dz < az - bz && dz != -1) ? (dz + interval > az - bz ? az - bz : dz + interval) : -1) {
            Vector vectorX = new Vector(dx, 0, 0);
            Location nowPointFromXPlane1 = (dx != -1) ? startPointFromXPlane1.clone().add(vectorX) : null;
            Location nowPointFromXPlane2 = (dx != -1) ? startPointFromXPlane2.clone().add(vectorX) : null;
            Location nowPointFromXPlane3 = (dx != -1) ? startPointFromXPlane3.clone().add(vectorX) : null;
            Location nowPointFromXPlane4 = (dx != -1) ? startPointFromXPlane4.clone().add(vectorX) : null;
            Vector vectorY = new Vector(0, dy, 0);
            Location nowPointFromYPlane1 = (dy != -1) ? startPointFromYPlane1.clone().add(vectorY) : null;
            Location nowPointFromYPlane2 = (dy != -1) ? startPointFromYPlane2.clone().add(vectorY) : null;
            Location nowPointFromYPlane3 = (dy != -1) ? startPointFromYPlane3.clone().add(vectorY) : null;
            Location nowPointFromYPlane4 = (dy != -1) ? startPointFromYPlane4.clone().add(vectorY) : null;
            Vector vectorZ = new Vector(0, 0, dz);
            Location nowPointFromZPlane1 = (dz != -1) ? startPointFromZPlane1.clone().add(vectorZ) : null;
            Location nowPointFromZPlane2 = (dz != -1) ? startPointFromZPlane2.clone().add(vectorZ) : null;
            Location nowPointFromZPlane3 = (dz != -1) ? startPointFromZPlane3.clone().add(vectorZ) : null;
            Location nowPointFromZPlane4 = (dz != -1) ? startPointFromZPlane4.clone().add(vectorZ) : null;

            Location[] nowPoints = new Location[]{
                    nowPointFromXPlane1,
                    nowPointFromXPlane2,
                    nowPointFromXPlane3,
                    nowPointFromXPlane4,
                    nowPointFromYPlane1,
                    nowPointFromYPlane2,
                    nowPointFromYPlane3,
                    nowPointFromYPlane4,
                    nowPointFromZPlane1,
                    nowPointFromZPlane2,
                    nowPointFromZPlane3,
                    nowPointFromZPlane4};
            for (Location nowPoint : nowPoints) {
                if (nowPoint != null) {
                    ViaVersion.spawnRedstoneParticle(viewer, nowPoint, colorR, colorG, colorB);
                    /*world.spawnParticle(Particle.REDSTONE, nowPoint, 0, colorR / 255, colorG / 255, colorB / 255);*/
                }
            }
        }
    }


    public static boolean hasOverlap(Location a1, Location a2, Location b1, Location b2) {
        if (a1.getWorld()==null || b1.getWorld() ==null){
            return false;
        }
        if (!a1.getWorld().getName().equals(b1.getWorld().getName())) {
            return false;
        }
        double aXMin = Math.min(a1.getX(), a2.getX());
        double aYMin = Math.min(a1.getY(), a2.getY());
        double aZMin = Math.min(a1.getZ(), a2.getZ());
        double aXMax = Math.max(a1.getX(), a2.getX());
        double aYMax = Math.max(a1.getY(), a2.getY());
        double aZMax = Math.max(a1.getZ(), a2.getZ());

        double bXMin = Math.min(b1.getX(), b2.getX());
        double bYMin = Math.min(b1.getY(), b2.getY());
        double bZMin = Math.min(b1.getZ(), b2.getZ());
        double bXMax = Math.max(b1.getX(), b2.getX());
        double bYMax = Math.max(b1.getY(), b2.getY());
        double bZMax = Math.max(b1.getZ(), b2.getZ());

        return aXMin <= bXMax && aXMax >= bXMin &&
                aYMin <= bYMax && aYMax >= bYMin &&
                aZMin <= bZMax && aZMax >= bZMin;
    }

    public static BaseArena getArena(Location loc) {
        for (BaseArena arena : DuelTimePlugin.getInstance().getArenaManager().getMap().values()) {
            if (inArena(loc, arena)) {
                return arena;
            }
        }
        return null;
    }

    public static boolean inArena(Location loc, BaseArena arena) {
        return inZone(loc, arena.getArenaData().getDiagonalPointLocation1(), arena.getArenaData().getDiagonalPointLocation2());
    }


    public static boolean inZone(Location loc, Location d1, Location d2) {
        try {
            if (d1.getWorld() == null || d2.getWorld() == null) {
                return false;
            }
            if (!d1.getWorld().getName().equals(loc.getWorld().getName())) {
                return false;
            }
            double minX = Math.min(d1.getX(), d2.getX());
            double minY = Math.min(d1.getY(), d2.getY());
            double minZ = Math.min(d1.getZ(), d2.getZ());
            double maxX = Math.max(d1.getX(), d2.getX());
            double maxY = Math.max(d1.getY(), d2.getY());
            double maxZ = Math.max(d1.getZ(), d2.getZ());
            return loc.getX() >= minX && loc.getX() < maxX + 1 &&
                    loc.getY() >= minY && loc.getY() < maxY + 1 &&
                    loc.getZ() >= minZ && loc.getZ() < maxZ + 1;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static BaseArena getArenaBelow(Location loc) {
        for (BaseArena arena : DuelTimePlugin.getInstance().getArenaManager().getMap().values()) {
            Location d1 = arena.getArenaData().getDiagonalPointLocation1();
            if (!d1.getWorld().getName().equals(loc.getWorld().getName())) {
                continue;
            }
            Location d2 = arena.getArenaData().getDiagonalPointLocation2();
            double minX = Math.min(d1.getX(), d2.getX());
            double minZ = Math.min(d1.getZ(), d2.getZ());
            double maxX = Math.max(d1.getX(), d2.getX());
            double minY = Math.min(d1.getY(), d2.getY());
            double maxZ = Math.max(d1.getZ(), d2.getZ());
            if (loc.getX() >= minX && loc.getX() < maxX + 1 &&
                    loc.getY() >= minY &&
                    loc.getZ() >= minZ && loc.getZ() < maxZ + 1) {
                return arena;
            }
        }
        return null;
    }
}
