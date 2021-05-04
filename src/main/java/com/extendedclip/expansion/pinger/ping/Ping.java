package com.extendedclip.expansion.pinger.ping;

import com.extendedclip.expansion.pinger.PingerExpansion;

public abstract class Ping {

    String address;
    int port;
    int timeout;

    long lastRequest;

    private boolean online = false;

    public long getLastRequest() {
        return lastRequest;
    }

    public void onRequest() {
        lastRequest = System.currentTimeMillis() / 1000;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public abstract void fetchData();

    public abstract String getData(String type);
}
