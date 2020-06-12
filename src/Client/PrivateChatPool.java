package Client;

import java.util.HashMap;

public class PrivateChatPool {
    private static final HashMap<String, PrivateChatFrame> pool = new HashMap<>();

    public static void put(String toUserName, PrivateChatFrame frame) {
        pool.put(toUserName, frame);
    }

    public static PrivateChatFrame get(String key) {
        return pool.get(key);
    }
}
