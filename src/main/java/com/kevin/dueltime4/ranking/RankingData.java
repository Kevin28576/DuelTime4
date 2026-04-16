package com.kevin.dueltime4.ranking;
public class RankingData {
    private final String playerName;
    private final Object data;
    private Object extraStr;

    public RankingData(String playerName, Object data, Object extraStr) {
        this.playerName = playerName;
        this.data = data;
        this.extraStr = extraStr;
    }

    public RankingData(String playerName, Object data) {
        this.playerName = playerName;
        this.data = data;
        this.extraStr = null;
    }
    public String getPlayerName() {
        return playerName;
    }
    public Object getData() {
        return data;
    }

    public Object getExtraStr() {
        return extraStr;
    }

    public void setExtraStr(Object extraStr) {
        this.extraStr = extraStr;
    }
}
