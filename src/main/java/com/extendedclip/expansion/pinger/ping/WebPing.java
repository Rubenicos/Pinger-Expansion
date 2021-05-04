package com.extendedclip.expansion.pinger.ping;

import com.extendedclip.expansion.pinger.PingerExpansion;
import me.clip.placeholderapi.libs.gson.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WebPing extends Ping {

    private final Gson gson;
    private final String webapi;
    private JsonObject data;

    public WebPing(String address, int port, int timeout, String webapi) {
        gson = new Gson();
        this.webapi = webapi;
        this.address = address;
        this.port = port;
        this.timeout = timeout;
    }

    private void setData(String s) {
        data = gson.fromJson(s, JsonObject.class);
    }

    private String getURL() {
        return PingerExpansion.settings.getWebAPI(webapi).replace("{ip}", address).replace("{port}", String.valueOf(port));
    }

    @Override
    public void fetchData() {
        String content = "{}";
        try (InputStream in = new URL(getURL()).openStream(); BufferedInputStream buff = new BufferedInputStream(in)) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while ((len = buff.read(buf)) > 0) {
                stream.write(buf, 0, len);
            }
            content = new String(stream.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setData(content);
    }

    @Override
    public String getData(String type) {
        update();
        String[] parts = type.split("[.\\[\\]]");
        if (parts.length == 1) {
            return data.get(type).getAsString();
        }

        JsonElement result = data;

        for (String key : parts) {
            key = key.trim();
            if (key.isEmpty()) continue;

            if (result == null) {
                result = JsonNull.INSTANCE;
                break;
            }

            if (result.isJsonObject()) {
                result = ((JsonObject) result).get(key);
            } else if (result instanceof JsonArray) {
                int ix = Integer.parseInt(key) - 1;
                result = ((JsonArray) result).get(ix);
            } else {
                break;
            }
        }

        return result.getAsString();
    }
}