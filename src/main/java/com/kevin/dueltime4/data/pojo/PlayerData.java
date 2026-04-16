package com.kevin.dueltime4.data.pojo;

public class PlayerData implements Cloneable {
    private final String id;
    private double exp;
    private double point;
    private String language;
    private int totalGameNumber;
    private int totalGameTime;
    private int arenaClassicWins;
    private int arenaClassicDraws;
    private int arenaClassicLoses;
    private int arenaClassicTime;

    public PlayerData(String id, double exp, double point, String language, int totalGameNumber, int totalGameTime, int arenaClassicWins,int arenaClassicDraws, int arenaClassicLoses, int arenaClassicTime) {
        this.id = id;
        this.exp = exp;
        this.point = point;
        this.language = language;
        this.totalGameNumber = totalGameNumber;
        this.totalGameTime = totalGameTime;
        this.arenaClassicWins = arenaClassicWins;
        this.arenaClassicDraws = arenaClassicDraws;
        this.arenaClassicLoses = arenaClassicLoses;
        this.arenaClassicTime = arenaClassicTime;
    }

    public String getId() {
        return id;
    }

    public double getExp() {
        return exp;
    }

    public double getPoint() {
        return point;
    }

    public String getLanguage() {
        return language;
    }

    public int getTotalGameNumber() {
        return totalGameNumber;
    }

    public int getTotalGameTime() {
        return totalGameTime;
    }

    public int getArenaClassicWins() {
        return arenaClassicWins;
    }

    public int getArenaClassicDraws() {
        return arenaClassicDraws;
    }

    public int getArenaClassicLoses() {
        return arenaClassicLoses;
    }

    public int getArenaClassicTime() {
        return arenaClassicTime;
    }

    public void setExp(double exp) {
        this.exp = exp;
    }

    public void setPoint(double point) {
        this.point = point;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void accumulateTotalGameNumber() {
        this.totalGameNumber++;
    }

    public void accumulateTotalGameTime(int second) {
        this.totalGameTime+=second;
    }

    public void accumulateArenaClassicWins() {
        this.arenaClassicWins++;
    }

    public void accumulateArenaClassicDraws() {
        this.arenaClassicDraws++;
    }

    public void accumulateArenaClassicLoses() {
        this.arenaClassicLoses++;
    }

    public void accumulateArenaClassicTime(int second) {
        this.arenaClassicTime+=second;
    }

    @Override
    public PlayerData clone() {
        try {
            return (PlayerData) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "id='" + id + '\'' +
                ", exp=" + exp +
                ", point=" + point +
                ", language='" + language + '\'' +
                ", totalGameNumber=" + totalGameNumber +
                ", totalGameTime=" + totalGameTime +
                ", arenaClassicWins=" + arenaClassicWins +
                ", arenaClassicDraws=" + arenaClassicDraws +
                ", arenaClassicLoses=" + arenaClassicLoses +
                ", arenaClassicTime=" + arenaClassicTime +
                '}';
    }
}
