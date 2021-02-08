package com.extendedclip.expansion.pinger;

import me.clip.placeholderapi.expansion.Cacheable;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PingerExpansion extends PlaceholderExpansion implements Cacheable, Taskable, Configurable {

    private String online = "&aOnline";
    private String offline = "&cOffline";

    private int mainTask = -1;
    private final Map<String, Ping> mainPings = new HashMap<>();
    private final Map<String, Long> lastRequest = new HashMap<>();

    private final Map<String, Ping> pings = new HashMap<>();

    private final Map<String, List<String>> iptables = new HashMap<>();
    private final Map<String, String> cache = new HashMap<>();

    private int interval = 30;

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("check_interval", 30);
        defaults.put("online", "&aOnline");
        defaults.put("offline", "&cOffline");
        defaults.put("iptables.example", new ArrayList<String>());
        return defaults;
    }

    @Override
    public void start() {
        online = getString("online", "&aOnline");
        offline = getString("offline", "&cOffline");
        int time = getInt("check_interval", 30);
        if (time > 0) interval = time;
        getConfigSection("iptables").getKeys(false).forEach(key -> iptables.put(key, getStringList("iptables." + key)));
        mainTask = Bukkit.getScheduler().runTaskTimerAsynchronously(getPlaceholderAPI(), () -> {
            if (mainPings.isEmpty()) return;
            final Long current = System.currentTimeMillis() / 1000;
            mainPings.forEach((ip, ping) -> {
                if ((current - lastRequest.get(ip)) > 50) {
                    mainPings.remove(ip);
                    lastRequest.remove(ip);
                } else {
                    try {
                        if (!ping.fetchData()) mainPings.remove(ip);
                    } catch (Exception ignored) { }
                }
            });
        }, 20L, 20L * interval).getTaskId();
    }

    @Override
    public void stop() {
        if (mainTask != -1) Bukkit.getScheduler().cancelTask(mainTask);
    }

    @Override
    public void clear() {
        mainPings.clear();
        lastRequest.clear();
        pings.clear();
        iptables.clear();
        cache.clear();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "clip";
    }

    @Override
    public String getIdentifier() {
        return "pinger";
    }

    @Override
    public String getVersion() {
        return "1.0.1";
    }

    @Override
    public String onRequest(org.bukkit.OfflinePlayer p, String params) {
        if (cache.containsKey(params)) return cache.get(params);

        final String[] args = params.split("_", 3);
        if (args.length < 2) return "Enought args";

        final String[] data = args[0].split(":", 2);
        final int num = parseInt(data[0], getEnum(data[0]));
        if (num < 0 && !data[0].contains("online")) return "Invalid Placeholder";

        if (args[1].toLowerCase().startsWith("iptable:")) {
            if (data[0].contains("online")) return "IpTables doesn't support " + data[0] + " placeholder";

            final String table = args[1].split(":", 2)[1].toLowerCase();
            if (iptables.containsKey(table)) {
                final List<Ping> pingList = new ArrayList<>();
                iptables.get(table).forEach(ip -> {
                    final Ping ping = getPing(ip, 30);
                    if (ping.getData().length > 1) pingList.add(ping);
                });
                if (num == 4) {
                    int count = 0;
                    for (Ping ping : pingList) {
                        count += parseInt(ping.getData()[4], 0);
                    }
                    return String.valueOf(count);
                } else {
                    final List<String> list = new ArrayList<>();
                    pingList.forEach(ping -> list.add(ping.getData()[num]));
                    return String.join((data.length > 1 ? data[1] : ", "), list);
                }
            } else {
                return "Invalid Table";
            }
        }

        Ping ping = getPing(args[1], (args.length > 2 ? parseInt(args[2], 30) : 30));
        if (ping.getData().length < 1) ping = null;
        if (data[0].contains("online")) return (ping != null) ? online : offline;

        return (ping != null ? ping.getData()[num] : (data.length > 1 ? data[1] : ""));
    }

    private Ping getPing(String arg, int interval) {
        lastRequest.put(arg, System.currentTimeMillis() / 1000);
        if (mainPings.containsKey(arg)) return mainPings.get(arg);
        if (pings.containsKey(arg)) return pings.get(arg);

        final String[] ip = arg.split(":", 3);
        final Ping ping = new Ping(ip[0], (ip.length > 1 ? parseInt(ip[1], 25565) : 25565), (ip.length > 2 ? parseInt(ip[2], 2000) : 2000));
        if (interval != this.interval) {
            Bukkit.getScheduler().runTaskAsynchronously(getPlaceholderAPI(), () -> {
                if (ping.fetchData()) {
                    pings.put(arg, ping);
                    Bukkit.getScheduler().runTaskLaterAsynchronously(getPlaceholderAPI(), () -> pings.remove(arg), interval * 20);
                }
            });
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(getPlaceholderAPI(), () -> {
                if (ping.fetchData()) mainPings.put(arg, ping);
            });
        }
        return ping;
    }

    private int getEnum(String s) {
        switch (s.toLowerCase()) {
            case "pingversion":
            case "pingv":
                return 0;
            case "protocolversion":
            case "protocolv":
                return 1;
            case "gameversion":
            case "version":
                return 2;
            case "motd":
                return 3;
            case "count":
            case "players":
                return 4;
            case "max":
            case "maxplayers":
                return 5;
            default:
                return -1;
        }
    }

    private int parseInt(String str, int def) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static final class Ping {
        private final String address;

        private final int port;

        private final int timeout;

        private String[] data;

        public Ping(String address, int port, int timeout) {
            this.address = address;
            this.port = port;
            this.timeout = timeout;
        }

        public String[] getData() {
            return data;
        }

        public boolean fetchData() {
            try {
                Socket socket = new Socket();
                socket.setSoTimeout(timeout);
                socket.connect(new InetSocketAddress(address, port), timeout);

                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_16BE);

                dataOutputStream.write(new byte[] { -2, 1 });

                if (inputStream.read() != 255) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {}
                    socket = null;
                    return false;
                }

                final int length = inputStreamReader.read();
                if (length == -1 || length == 0) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {}
                    socket = null;
                    return false;
                }

                char[] chars = new char[length];
                if (inputStreamReader.read(chars, 0, length) != length) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {}
                    socket = null;
                    return false;
                }

                String string = new String(chars);
                if (string.startsWith("§")) data = string.split("\000");

                dataOutputStream.close();
                outputStream.close();
                inputStreamReader.close();
                inputStream.close();
                socket.close();
            } catch (IOException exception) {
                return false;
            }
            return true;
        }
    }
}
