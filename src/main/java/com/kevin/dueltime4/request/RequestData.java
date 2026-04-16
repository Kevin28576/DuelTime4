package com.kevin.dueltime4.request;

public class RequestData {
    private final Long startTime;
    private final Long endTime;
    private final String arenaEditName;

    public RequestData(Long startTime, Long endTime, String arenaEditName) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.arenaEditName = arenaEditName;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public String getData() {
        return arenaEditName;
    }
}
