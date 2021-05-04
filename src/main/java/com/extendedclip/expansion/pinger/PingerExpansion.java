package com.extendedclip.expansion.pinger;

import com.extendedclip.expansion.pinger.ping.DefaultPing;
import com.extendedclip.expansion.pinger.ping.Ping;
import com.extendedclip.expansion.pinger.ping.WebPing;
import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;

import java.util.*;

public class PingerExpansion extends PlaceholderExpansion implements Cacheable, Taskable {

    public static Settings settings;

    public static String online = "&aOnline";
    public static String offline = "&cOffline";
    public static int cachetime = 20;
    public static int interval = 30;

    private int mainTask = -1;
    private final Map<String, Ping> mainPings = new HashMap<>();

    private final Map<String, Integer> pingTasks = new HashMap<>();
    private final Map<String, Ping> pings = new HashMap<>();

    private final Map<String, String> cache = new HashMap<>();

    @Override
    public void start() {
        settings = new Settings(getVersion());
        online = settings.getString("text.online", "&aOnline");
        offline = settings.getString("text.offline", "&cOffline");
        interval = settings.getInt("defaults.check-interval", 30);
        if (interval < 1) {
            interval = 30;
        }
        cachetime = settings.getInt("defaults.cache-time", 20);
        if (cachetime < 1) {
            cachetime = 20;
        }

        mainTask = Bukkit.getScheduler().runTaskTimerAsynchronously(getPlaceholderAPI(), () -> {
            if (mainPings.isEmpty()) return;

            final List<String> toDelete = new ArrayList<>();

            mainPings.forEach((ip, ping) -> {
                if (((System.currentTimeMillis() / 1000) - ping.getLastRequest()) > (interval + 10)) {
                    toDelete.add(ip);
                } else {
                    ping.fetchData();
                }
            });

            toDelete.forEach(mainPings::remove);
        }, 20L, 20L * interval).getTaskId();
    }

    @Override
    public void stop() {
        if (mainTask != -1) Bukkit.getScheduler().cancelTask(mainTask);
        pingTasks.values().forEach(task -> Bukkit.getScheduler().cancelTask(task));
    }

    @Override
    public void clear() {
        mainPings.clear();
        pingTasks.clear();
        pings.clear();
        cache.clear();
        settings.getCache().clear();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "clip & Rubenicos";
    }

    @Override
    public String getIdentifier() {
        return "pinger";
    }

    @Override
    public String getVersion() {
        return "1.1.0";
    }

    @Override
    public String onRequest(org.bukkit.OfflinePlayer p, String params) {
        String[] args = params.split("_", 4);
        if (args.length < 2) return "Unknown IP";
        String identifier = args[0] + "_" + args[1];
        if (cache.containsKey(identifier)) return cache.get(identifier);

        boolean save = args.length > 2 && args[args.length - 1].toLowerCase().startsWith("cache");
        int time = 0;
        if (save) {
            String[] s = args[args.length - 1].split(":");
            time = (s.length > 1 ? parseInt(s[1], 20) : 20);
            args = Arrays.copyOf(args, args.length - 1);
        }

        String result;
        if (args[1].toLowerCase().startsWith("iptable:")) {
            final String[] table = args[1].split(":", 3);
            if (table.length < 2) {
                return "Invalid Table";
            } else if (table.length > 2 && table[2].toLowerCase().equals("sum")) {
                int total = 0;
                for (String ip : settings.getIpTable(table[1])) {
                    total = total + parseInt(getPing(args, ip).getData(args[0]), 0);
                }
                result = String.valueOf(total);
            } else {
                List<String> data = new ArrayList<>();
                for (String ip : settings.getIpTable(table[1])) {
                    data.add(getPing(args, ip).getData(args[0]));
                }
                result = String.join((table.length > 2 ? table[2] : ", "), data);
            }
        } else {
            Ping ping = getPing(args, args[1]);
            result = ping.getData(args[0]);
        }

        return (save ? cache(identifier, result, time) : result);
    }

    private String cache(String identifier, String result, int time) {
        if (time < 1) {
            time = 20;
        }
        if (!cache.containsKey(identifier)) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(getPlaceholderAPI(), () -> cache.remove(identifier), 20L * time);
        }
        cache.put(identifier, result);
        return result;
    }

    private Ping getPing(String[] args, String ip) {
        Ping ping;
        if (args[0].toLowerCase().startsWith("api:")) {
            if (mainPings.containsKey(ip) || pings.containsKey(ip)) {
                String[] api = args[0].split(":");
                args[0] = (api.length > 2 ? api[2] : "nothing");
                ping = mainPings.getOrDefault(ip, pings.get(ip));
            } else {
                ping = null;
            }
        } else {
            ping = mainPings.getOrDefault(ip, pings.getOrDefault(ip, null));
        }

        int refresh = (args.length > 2 ? parseInt(args[2], interval) : interval);
        if (ping == null) {
            return createPing(ip, args, refresh);
        } else {
            resolveInterval(ip, ping, refresh);
            return ping;
        }
    }

    private Ping createPing(String identifier, String[] args, int refresh) {
        String[] ip = identifier.split(":");
        Ping ping;
        if (args[0].toLowerCase().startsWith("api:")) {
            String[] api = args[0].split(":");
            ping = new WebPing(ip[0], (ip.length > 1 ? parseInt(ip[1], 25565) : 25565), (ip.length > 2 ? parseInt(ip[2], 2000) : 2000), (api.length > 1 ? api[1] : "mcapi"));
            args[0] = (api.length > 2 ? api[2] : "nothing");
        } else {
            ping = new DefaultPing(ip[0], (ip.length > 1 ? parseInt(ip[1], 25565) : 25565), (ip.length > 2 ? parseInt(ip[2], 2000) : 2000));
        }
        resolveInterval(identifier, ping, refresh);
        return ping;
    }

    private void resolveInterval(String identifier, Ping ping, int refresh) {
        if (refresh != interval) {
            mainPings.remove(identifier);
            if (!pings.containsKey(identifier)) {
                pings.put(identifier, ping);
                pingTasks.put(identifier, Bukkit.getScheduler().runTaskTimerAsynchronously(getPlaceholderAPI(), () -> {
                    if (((System.currentTimeMillis() / 1000) - ping.getLastRequest()) > (interval + 10)) {
                        stopPing(identifier);
                    } else {
                        pings.get(identifier).fetchData();
                    }
                }, 20L, 20L * refresh).getTaskId());
            }
        } else {
            if (!mainPings.containsKey(identifier)) {
                mainPings.put(identifier, ping);
            }
            stopPing(identifier);
        }
    }

    private void stopPing(String identifier) {
        if (pings.containsKey(identifier)) {
            pings.remove(identifier);
            Bukkit.getScheduler().cancelTask(pingTasks.get(identifier));
            pingTasks.remove(identifier);
        }
    }

    private int parseInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
