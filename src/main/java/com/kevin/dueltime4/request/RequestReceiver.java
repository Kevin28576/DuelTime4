package com.kevin.dueltime4.request;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestReceiver {
    private final String receiverName;
    private final Map<String, RequestData> requestDataMap;


    public RequestReceiver(String receiverName) {
        this.receiverName = receiverName;
        this.requestDataMap = new HashMap<>();
    }

    /**
     * @return 未超時請求的玩家名列表
     */
    public List<String> getValidSenderNames() {
        List<String> validRequests = new ArrayList<>();
        for (Map.Entry<String, RequestData> kv : requestDataMap.entrySet()) {
            RequestData requestData = kv.getValue();
            if (System.currentTimeMillis() > requestData.getEndTime()) {
                // 超時請求則跳過
                continue;
            }
            String senderName = kv.getKey();
            if (Bukkit.getPlayerExact(senderName) == null ||
                    !Bukkit.getPlayerExact(senderName).isOnline()) {
                // 請求方下線了則跳過
                continue;
            }
            validRequests.add(kv.getKey());
        }
        return validRequests;
    }

    /**
     * @return 返回無法接受某玩家請求的原因（列舉）
     */
    public InvalidReason getInvalidReason(String senderNameEntered) {
        if (requestDataMap.containsKey(senderNameEntered)) {
            if (System.currentTimeMillis() > requestDataMap.get(senderNameEntered).getEndTime()) {
                return InvalidReason.TIME_OUT;
            } else {
                return InvalidReason.OFFLINE;
            }
        } else {
            return InvalidReason.HAS_NOT_SENT;
        }
    }

    public enum InvalidReason {
        TIME_OUT,// 請求超時
        OFFLINE,// 請求方下線了
        HAS_NOT_SENT// 對方沒給自己傳送過請求
    }


    /**
     * 在請求列表中新增請求方的玩家名
     *
     * @param senderName 請求方玩家名
     */
    public void add(String senderName, String arenaEditName) {
        long startTime = System.currentTimeMillis();
        requestDataMap.put(senderName, new RequestData(startTime, startTime + 120 * 1000, arenaEditName));
    }

    /**
     * @return 某個請求方的請求資料
     */
    public RequestData get(String senderName) {
        return requestDataMap.get(senderName);
    }

    /**
     * 移除某個玩家的請求，一般應用於接收方拒絕了該玩家的請求
     */
    public void remove(String senderName) {
        requestDataMap.remove(senderName);
    }

    /**
     * 清空請求列表。一般應用於接受方開始了比賽時
     */
    public void clear() {
        requestDataMap.clear();
    }
}
