package Client;

import Constants.PrivateChatPool;
import Model.Message;
import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSON;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class Main extends JFrame {
    private static final long serialVersionUID = 1L;

    private static PrintStream ps;
    private static OutputStream os;
    private static InputStream is;
    private static InputStreamReader isr;
    private static BufferedReader br;


    private static GroupChatFrame groupChatFrame;
    private static Socket cSocket = null;

    static void safeExit(Socket socket) {
        IoUtil.close(socket);
        IoUtil.close(os);
        IoUtil.close(is);
        IoUtil.close(ps);
        IoUtil.close(isr);
        IoUtil.close(br);
        System.exit(0);
    }


    public static void handle_message(String json) throws Exception {
        if (json == null) {
            return;
        }
        System.out.println("handle: " + json);
        Message msg = JSON.parseObject(json, Message.class);
        if (msg.toUser != null) {
            // 私聊消息
            String key = msg.fromUser.userName;
            if (key.equals(Config.getInstance().getUserName())) {
                key = msg.toUser.userName;
            }
            PrivateChatFrame pc = PrivateChatPool.get(key);
            if (pc == null) {
                pc = new PrivateChatFrame(msg.fromUser);
                pc.setPrintStream(ps);
                pc.setVisible(true);
                PrivateChatPool.put(key, pc);
            }
            if (!pc.isActive()) pc.setVisible(true);
            pc.addRecords(msg);

        } else {
            // 群聊消息
            if (!groupChatFrame.isActive()) groupChatFrame.setVisible(true);
            groupChatFrame.addRecords(msg);
        }
    }


    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        EventQueue.invokeLater(() -> {
            try {
                groupChatFrame = new GroupChatFrame();
                groupChatFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent windowEvent) {
                        if (JOptionPane.showConfirmDialog(null, "你确定要退出吗?",
                                "退出聊天室", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                            safeExit(cSocket);
                        }
                    }
                });
                groupChatFrame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("用户名：", SwingConstants.RIGHT));
        label.add(new JLabel("密  码：", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField userNameField = new JTextField();
        controls.add(userNameField);
        JPasswordField passwordField = new JPasswordField();
        controls.add(passwordField);
        panel.add(controls, BorderLayout.CENTER);

        String username = "";
        String password = "";
        while (password.isEmpty() || username.isEmpty()) {
            int s = JOptionPane.showConfirmDialog(
                    groupChatFrame, panel, "登录", JOptionPane.OK_CANCEL_OPTION);
            if (s != JOptionPane.OK_OPTION) {
                // 用户点击了取消或关闭
                groupChatFrame.dispose();
                return;
            }
            username = userNameField.getText().strip();
            password = String.valueOf(passwordField.getPassword()).strip();
        }

        Config.getInstance().setUserName(username);
        groupChatFrame.setTitle(username + " | " + Config.getInstance().getAppName());

        try {
            cSocket = new Socket(Config.getInstance().getServerHost(), Config.getInstance().getServerPort());
            os = cSocket.getOutputStream();
            ps = new PrintStream(os);
            is = cSocket.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);

            groupChatFrame.setPrintStream(ps);

            System.out.println(username + "向服务器发送用户名密码");
            ps.println(username + ";" + password);
            String ack = br.readLine();
            if (ack.equals("ack_" + username)) {
                System.out.println("服务器响应：" + ack);
                ps.println("ack_" + username);
                System.out.println("连接服务器成功");
                String json;
                while ((json = br.readLine()) != null) {
                    handle_message(json);
                    System.out.println("handled.");
                }
            } else if (ack.equals("403")) {
                JOptionPane.showMessageDialog(null, "用户名或密码错误", "登录错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
        } finally {
            safeExit(cSocket);
        }
    }
}
