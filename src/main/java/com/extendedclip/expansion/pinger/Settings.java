package com.extendedclip.expansion.pinger;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Settings extends YamlConfiguration {

    private final Map<String, Object> cache = new HashMap<>();

    public Settings(String version) {
        reload(version);
    }

    public Map<String, Object> getCache() {
        return cache;
    }

    public void reload(String version) {
        cache.clear();
        File file = new File(PlaceholderAPIPlugin.getInstance().getDataFolder() + File.separator + "expansions" + File.separator + "Pinger" + File.separator + "settings.yml");
        if (file.exists()) {
            try {
                load(file);
            } catch (InvalidConfigurationException | IOException e) {
                e.printStackTrace();
            }
        }
        options().header("Pinger Expansion v" + version);
        if (!contains("text.online")) {
            set("text.online", "&aOnline");
        }
        if (!contains("text.offline")) {
            set("text.offline", "&cOffline");
        }
        if (!contains("defaults.check-interval")) {
            set("defaults.check-interval", 30);
        }
        if (!contains("defaults.cache-time")) {
            set("defaults.cache-time", 20);
        }
        if (!contains("iptable")) {
            set("iptable.atable", Arrays.asList("127.198.0.1", "18.264.98.50:25560"));
        }
        if (!contains("web-api")) {
            set("web-api.xdefcon", "http://mcapi.xdefcon.com/server/{ip}:{port}/full/json");
            set("web-api.mcapi", "https://mcapi.us/server/status?ip={ip}&port={port}");
            set("web-api.mcsrvstat", "https://api.mcsrvstat.us/2/{ip}:{port}");
            set("web-api.snowdev", "https://mcstatus.snowdev.com.br/api/query/v3/{ip}:{port}");
            set("web-api.mc-api", "https://eu.mc-api.net/v3/server/ping/{ip}:{port}");
        }
        try {
            save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getWebAPI(String identifier) {
        identifier = "web-api." + identifier;
        return String.valueOf(cache.getOrDefault(identifier, cache(identifier, get(identifier, "http://mcapi.xdefcon.com/server/{ip}:{port}/full/json"))));
    }

    @SuppressWarnings("unchecked")
    public List<String> getIpTable(String identifier) {
        identifier = "iptable." + identifier;
        return (List<String>) cache.getOrDefault(identifier, getIpTable0(identifier));
    }

    @SuppressWarnings("unchecked")
    private List<String> getIpTable0(String path) {
        final Object object = get(path);
        if (object instanceof List) {
            return (List<String>) cache(path, object);
        }
        return Collections.singletonList(String.valueOf(cache(path, object)));
    }

    private Object cache(String path, Object obj) {
        cache.put(path, obj);
        return obj;
    }
}
