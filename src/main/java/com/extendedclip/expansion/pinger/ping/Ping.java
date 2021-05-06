package com.extendedclip.expansion.pinger.ping;

public abstract class Ping {

    String address;
    int port;
    int timeout;

    long lastRequest;
    int pingTask;
    int interval;

    private boolean online = false;

    public long getLastRequest() {
        return lastRequest;
    }

    public void onRequest() {
        lastRequest = System.currentTimeMillis() / 1000;
    }

    public int getPingTask() {
        return pingTask;
    }

    public void setPingTask(int pingTask) {
        this.pingTask = pingTask;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
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
