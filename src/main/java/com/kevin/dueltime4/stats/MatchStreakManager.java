package com.kevin.dueltime4.stats;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MatchStreakManager {
    private final Map<String, Integer> winStreakMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> loseStreakMap = new ConcurrentHashMap<>();

    public int recordWin(String playerName) {
        int newValue = winStreakMap.getOrDefault(playerName, 0) + 1;
        winStreakMap.put(playerName, newValue);
        loseStreakMap.remove(playerName);
        return newValue;
    }

    public int recordLose(String playerName) {
        int newValue = loseStreakMap.getOrDefault(playerName, 0) + 1;
        loseStreakMap.put(playerName, newValue);
        winStreakMap.remove(playerName);
        return newValue;
    }

    public void recordDraw(String playerName) {
        winStreakMap.remove(playerName);
        loseStreakMap.remove(playerName);
    }

    public int getWinStreak(String playerName) {
        return winStreakMap.getOrDefault(playerName, 0);
    }

    public int getLoseStreak(String playerName) {
        return loseStreakMap.getOrDefault(playerName, 0);
    }

    public String getDisplay(String playerName) {
        int win = getWinStreak(playerName);
        if (win > 0) {
            return "W" + win;
        }
        int lose = getLoseStreak(playerName);
        if (lose > 0) {
            return "L" + lose;
        }
        return "-";
    }

    public void clear() {
        winStreakMap.clear();
        loseStreakMap.clear();
    }
}
