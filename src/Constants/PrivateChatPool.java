package Constants;

import Client.PrivateChatFrame;

import java.util.HashMap;

public class PrivateChatPool {
    private static final HashMap<String, PrivateChatFrame> pool = new HashMap<>();

    public static void put(String key, PrivateChatFrame frame) {
        pool.put(key, frame);
    }

    public static PrivateChatFrame get(String key) {
        return pool.get(key);
    }
}
