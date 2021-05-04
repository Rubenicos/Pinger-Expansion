package com.extendedclip.expansion.pinger.ping;

import com.extendedclip.expansion.pinger.PingerExpansion;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class DefaultPing extends Ping {

    private String[] data = {"-1", "-1", "unknown", "", "0", "0"};

    public DefaultPing(String address, int port, int timeout) {
        this.address = address;
        this.port = port;
        this.timeout = timeout;
    }

    private void setData(String[] data) {
        this.data = data;
    }

    @Override
    public void fetchData() {
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
                return;
            }

            final int length = inputStreamReader.read();
            if (length == -1 || length == 0) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                socket = null;
                return;
            }

            char[] chars = new char[length];
            if (inputStreamReader.read(chars, 0, length) != length) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                socket = null;
                return;
            }

            String string = new String(chars);
            if (string.startsWith("§")) {
                setData(string.substring(1).split("\000"));
            } else {
                setData(string.split("§"));
            }

            setOnline(true);

            dataOutputStream.close();
            outputStream.close();
            inputStreamReader.close();
            inputStream.close();
            socket.close();
        } catch (IOException e) {
            setOnline(false);
        }
    }

    @Override
    public String getData(String type) {
        update();
        switch (type.toLowerCase()) {
            case "pingversion":
            case "pingv":
                return data[0];
            case "protocolversion":
            case "protocolv":
                return data[1];
            case "gameversion":
            case "version":
                return data[2];
            case "motd":
                return data[3];
            case "count":
            case "players":
                return data[4];
            case "max":
            case "maxplayers":
                return data[5];
            case "online":
                if (isOnline()) {
                    return PingerExpansion.online;
                } else {
                    return PingerExpansion.offline;
                }
            default:
                return "Invalid data type";
        }
    }
}
