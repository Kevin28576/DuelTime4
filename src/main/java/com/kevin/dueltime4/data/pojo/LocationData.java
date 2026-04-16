package com.kevin.dueltime4.data.pojo;

import org.bukkit.Location;

public class LocationData {
    private final String id;
    private Location location;

    public LocationData(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
