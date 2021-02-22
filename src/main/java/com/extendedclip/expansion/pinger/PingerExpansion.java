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
            final List<String> toDelete = new ArrayList<>();
            final List<String> toPing = new ArrayList<>();
            mainPings.forEach((ip, ping) -> {
                if ((current - lastRequest.get(ip)) > (interval * 2)) {
                    toDelete.add(ip);
                } else {
                    toPing.add(ip);
                }
            });
            toPing.forEach(ip -> {
                try {
                    if (!mainPings.get(ip).fetchData()) toDelete.add(ip);
                } catch (Exception ignored) { }
            });
            toDelete.forEach(ip -> {
                lastRequest.remove(ip);
                mainPings.remove(ip);
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
        if (cache.containsKey(params)) return cache.get(params);

        final String[] args = params.split("_", 3);
        if (args.length < 2) return "Enough args";

        final int num = parseInt(args[0], getEnum(args[0]));
        if (num > 6) return "Invalid Placeholder";

        if (args[1].toLowerCase().startsWith("iptable:")) {
            final String[] table = args[1].split(":", 3);
            if (iptables.containsKey(table[1].toLowerCase())) {
                final List<Ping> pingList = new ArrayList<>();
                iptables.get(table[1].toLowerCase()).forEach(ip -> {
                    final Ping ping = getPing(ip, interval);
                    if (ping.getData().length > 1) pingList.add(ping);
                });
                String result;
                if (table.length > 2 && table[2].toLowerCase().equals("sum")) {
                    if (num == 2) {
                        result = "Game version is not a Integer";
                    } else if (num == 3) {
                        result = "Server motd is not a Integer";
                    } else {
                        int count = 0;
                        for (Ping ping : pingList) {
                            count += (int) ping.getData()[num];
                        }
                        result = String.valueOf(count);
                    }
                } else {
                    final List<String> list = new ArrayList<>();
                    if (num == 6) {
                        pingList.forEach(ping -> list.add((ping.isOnline()) ? online : offline));
                    } else {
                        pingList.forEach(ping -> list.add(String.valueOf(ping.getData()[num])));
                    }
                    result = String.join((table.length > 2 ? table[2] : ", "), list);
                }
                cache.put(params, result);
                Bukkit.getScheduler().runTaskLaterAsynchronously(getPlaceholderAPI(), () -> cache.remove(params), (args.length > 2 ? parseInt(args[2], interval) : interval));
                return result;
            } else {
                return "Invalid Table";
            }
        }

        Ping ping = getPing(args[1], (args.length > 2 ? parseInt(args[2], interval) : interval));
        if (num == 6) return (ping.isOnline()) ? online : offline;

        return String.valueOf(ping.getData()[num]);
    }

    private Ping getPing(String arg, int interval) {
        lastRequest.put(arg, System.currentTimeMillis() / 1000);
        if (mainPings.containsKey(arg)) return mainPings.get(arg);
        if (pings.containsKey(arg)) return pings.get(arg);

        final String[] ip = arg.split(":", 3);
        final Ping ping = new Ping(ip[0], (ip.length > 1 ? parseInt(ip[1], 25565) : 25565), (ip.length > 2 ? parseInt(ip[2], 2000) : 2000));
        if (interval != this.interval) {
            if (ping.fetchData()) {
                pings.put(arg, ping);
                Bukkit.getScheduler().runTaskLaterAsynchronously(getPlaceholderAPI(), () -> pings.remove(arg), interval * 20);
            }
        } else {
            if (ping.fetchData()) mainPings.put(arg, ping);
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
            case "online":
                return 6;
            default:
                return 7;
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

        private boolean online = false;

        private Object[] data = {-1, -1, "unknown", "", 0, 0};

        public Ping(String address, int port, int timeout) {
            this.address = address;
            this.port = port;
            this.timeout = timeout;
        }

        public String getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public int getTimeout() {
            return timeout;
        }

        public boolean isOnline() {
            return online;
        }

        public void setOnline() {
            online = true;
        }

        public Object[] getData() {
            return data;
        }

        public void setData(Object[] data) {
            this.data = data;
        }

        public boolean fetchData() {
            try {
                Socket socket = new Socket();
                socket.setSoTimeout(getTimeout());
                socket.connect(new InetSocketAddress(getAddress(), getPort()), getTimeout());

                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_16BE);

                dataOutputStream.write(new byte[] { -2, 1 });

                int packetId = inputStream.read();
                if (packetId == -1) {
                    try {
                        socket.close();
                    } catch (IOException ignored) {}
                    socket = null;
                    return false;
                }

                if (packetId != 255) {
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
                if (string.startsWith("§")) {
                    String[] split = string.split("\000");
                    setData(new Object[] {
                            Integer.parseInt(split[0].substring(1)), // Ping version
                            Integer.parseInt(split[1]), // Protocol version
                            split[2], // Game version
                            split[3], // Motd
                            Integer.parseInt(split[4]), // Players online
                            Integer.parseInt(split[5])}); // Max players
                } else {
                    String[] split = string.split("§");
                    setData(new Object[] {"-1", "-1", "-1", // Ping, protocol and game version are invalid on this case
                            split[0], // Motd
                            Integer.parseInt(split[1]), // Players online
                            Integer.parseInt(split[2])}); // Max players
                }
                setOnline();

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
