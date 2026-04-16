package com.kevin.dueltime4.level;

public class Tier {
    private final String id;
    private final int level;
    private final String title;
    private final double expForLevelUp;

    public Tier(String id, int level, String title, double expForLevelUp) {
        this.id = id;
        this.level = level;
        this.title = title;
        this.expForLevelUp = expForLevelUp;
    }

    public String getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public double getExpForLevelUp() {
        return expForLevelUp;
    }

    public int compare(Tier tier) {
        if (level > tier.getLevel()) return 1;
        else if (level < tier.getLevel()) return -1;
        return 0;
    }
}
