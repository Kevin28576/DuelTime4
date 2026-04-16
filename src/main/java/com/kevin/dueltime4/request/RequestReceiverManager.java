package com.kevin.dueltime4.request;

import java.util.HashMap;
import java.util.Map;

public class RequestReceiverManager {
    private final Map<String, RequestReceiver> requestReceiverMap = new HashMap<>();

    public void add(String senderName, String receiverName, String arenaEditname) {
        RequestReceiver requestReceiver = requestReceiverMap.getOrDefault(receiverName, new RequestReceiver(receiverName));
        requestReceiver.add(senderName, arenaEditname);
        requestReceiverMap.put(receiverName, requestReceiver);
    }

    public RequestReceiver get(String receiverName) {
        if (requestReceiverMap.containsKey(receiverName)) {
            return requestReceiverMap.get(receiverName);
        } else {
            RequestReceiver requestReceiver = new RequestReceiver(receiverName);
            requestReceiverMap.put(receiverName,requestReceiver);
            return requestReceiver;
        }
    }
}
