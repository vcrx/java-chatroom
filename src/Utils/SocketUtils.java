package Utils;

import Common.User;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class SocketUtils {
    public static void safeExit(Socket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.exit(0);
    }
}
