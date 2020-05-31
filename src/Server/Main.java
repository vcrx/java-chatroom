package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import Common.Message;
import Common.User;
import Utils.SocketUtils;
import com.alibaba.fastjson.JSON;

public class Main {
    private final ArrayList<HandlerThread> sockets = new ArrayList<>();

    public static void main(String[] args) {
        Main server = new Main();
        System.out.println("服务器已启动...");
        server.init();
    }

    public void init() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(1002);
            while (true) {
                System.out.println("等待连接...");
                Socket client = serverSocket.accept();
                sockets.add(new HandlerThread(client));
            }
        } catch (Exception e) {
            System.out.println("服务器捕获异常: ");
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ArrayList<User> getClientUsernames() {
        ArrayList<User> users = new ArrayList<>();
        for (HandlerThread thread : sockets) {
            users.add(thread.getUser());
        }
        return users;
    }

    public void showClientCount() {
        System.out.println("当前客户端数量：" + sockets.size());
    }

    private class HandlerThread extends Thread {
        private final Socket socket;
        private PrintStream ps;

        public String userName;
        public String password;

        public HandlerThread(Socket socket) {
            this.socket = socket;
            this.start();
        }

        public User getUser() {
            return new User(userName);
        }

        public void send(String jsonString) {
            ps.println(jsonString);
        }

        public void sendAll(String jsonString) {
            for (HandlerThread thread : sockets) {
                thread.send(jsonString);
            }
        }

        public void sendAll(String jsonString, boolean skipSender) {
            if (!skipSender) {
                sendAll(jsonString);
            } else {
                for (HandlerThread thread : sockets) {
                    if (thread.userName.equals(this.userName)) continue;
                    thread.send(jsonString);
                }
            }

        }

        public void run() {
            OutputStream os = null;
            InputStream is = null;
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                os = socket.getOutputStream();
                ps = new PrintStream(os);
                is = socket.getInputStream();
                isr = new InputStreamReader(is);
                br = new BufferedReader(isr);
                String user = br.readLine();
                String[] tmp = user.split(";");
                userName = tmp[0];
                password = tmp[1];
                System.out.println(userName + "：请求连接");
                if (password.contains("a")) {
                    send("ack" + userName);
                } else {
                    send("403");
                    throw new Exception(userName + "password error");
                }
                String ack = br.readLine();
                if (ack.equals("ack" + userName)) {
                    System.out.println(userName + "：连接成功");
                    sendAll(JSON.toJSONString(new Message(getUser(), "join", getClientUsernames())));
                    showClientCount();
                    while (true) {
                        String str = br.readLine();
                        if (str == null) {
                            throw new Exception("receive null");
                        }
                        System.out.println("服务器收到消息：" + str);
                        sendAll(str);
                    }
                }
            } catch (Exception e) {
                System.out.println(userName + " 捕获异常：");
                e.printStackTrace();
            } finally {
                System.out.println("清理 " + userName);
                sockets.remove(this);
                showClientCount();
                sendAll(JSON.toJSONString(new Message(getUser(), "left", getClientUsernames())));
                try {
                    if (socket != null) socket.close();
                    if (os != null) os.close();
                    if (is != null) is.close();
                    if (ps != null) ps.close();
                    if (isr != null) isr.close();
                    if (br != null) br.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
