package com.extendedclip.expansion.pinger.ping;

import com.extendedclip.expansion.pinger.PingerExpansion;

public abstract class Ping {

    String address;
    int port;
    int timeout;

    long lastRequest;

    private boolean online = false;

    public boolean isActive(long time) {
        return ((time - lastRequest)) < (PingerExpansion.interval + 10);
    }

    public void update() {
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
