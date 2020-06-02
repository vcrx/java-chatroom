package Client;

import Common.Message;
import Common.User;
import Utils.ByteUtils;
import Utils.FileUtils;
import Utils.LinkUtils;
import com.alibaba.fastjson.JSON;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Random;

public class Main extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final Records rc = new Records();

    private static PrintStream ps;
    private static OutputStream os;
    private static InputStream is;
    private static InputStreamReader isr;
    private static BufferedReader br;


    private static Main chatRoomFrame;
    private static Socket cSocket = null;
    private static final HashMap<String, PrivateChatFrame> privateChatFrameMap = new HashMap<>();
    private final JEditorPane recordsPane;
    private final JTextArea sendTextArea;
    private final JEditorPane onlineUserPane;


    static void safeExit(Socket socket) {
        try {
            if (socket != null) socket.close();
            if (ps != null) ps.close();
            if (os != null) os.close();
            if (is != null) is.close();
            if (isr != null) isr.close();
            if (br != null) br.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    HyperlinkListener hyperlinkListener = new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    String x = e.getDescription();
                    if (x.startsWith("user://")) {
                        String toUserString = x.replace("user://", "");
                        EventQueue.invokeLater(() -> {
                            try {
                                User toUser = JSON.parseObject(toUserString, User.class);
                                PrivateChatFrame privateChatFrame = new PrivateChatFrame(ps, toUser);
                                privateChatFrame.setVisible(true);
                                privateChatFrameMap.put(toUser.userName, privateChatFrame);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        });
                    } else if (x.startsWith("img://")) {
                        LinkUtils.ShowImage(x);
                    } else if (x.startsWith("file://")) {
                        LinkUtils.SaveFileWithFilename(x);
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    };

    public static void handle_message(String json) throws Exception {
        if (json == null) {
            return;
        }
        System.out.println("handle: " + json);
        Message msg = JSON.parseObject(json, Message.class);
        if (msg.toUser != null) {
            String key = msg.fromUser.userName;
            if (key.equals(Config.getInstance().getUserName())) {
                key = msg.toUser.userName;
            }
            PrivateChatFrame pc = privateChatFrameMap.get(key);
            if (pc == null) {
                pc = new PrivateChatFrame(ps, msg.fromUser);
                pc.setVisible(true);
                privateChatFrameMap.put(key, pc);
            }
            if (!pc.isActive()) pc.setVisible(true);
            pc.addRecords(msg);
            return;
        }
        if (!chatRoomFrame.isActive()) chatRoomFrame.setVisible(true);
        chatRoomFrame.addRecords(msg);
    }

    public void addRecords(Message msg) throws Exception {
        switch (msg.type) {
            case "text":
                recordsPane.setText(rc.parseText(msg));
                break;
            case "event":
                switch (msg.msg) {
                    case "join":
                    case "left":
                        recordsPane.setText(rc.parseJoinOrLeft(msg));
                        onlineUserPane.setText(rc.parseOnlineUsers(msg));
                        break;
                }
                break;
            case "img":
                recordsPane.setText(rc.parseImg(msg, chatRoomFrame));
                break;
            case "file":
                recordsPane.setText(rc.parseFile(msg));
                break;
        }
    }

    private User getUser() {
        return new User(Config.getInstance().getUserName());
    }

    public void sendText() {
        String text = sendTextArea.getText();
        if (text.equals("")) {
            return;
        }
        String jsonString = JSON.toJSONString(new Message(getUser(), text));
        ps.println(jsonString);
        sendTextArea.setText("");
    }

    private void _sendFile(File file, String type) {
        String filePath = file.getAbsolutePath();
        byte[] data = FileUtils.readContent(filePath);
        if (data != null) {
            data = ByteUtils.GZip(data);
            String dataB64 = ByteUtils.encodeByteToBase64String(data);
            Message msg = new Message(getUser(), dataB64, type, file.getName());
            String jsonString = JSON.toJSONString(msg);
            ps.println(jsonString);
        }
    }

    public void sendFile() {
        File file = FileUtils.fileChooser(null, null);
        if (file != null) {
            _sendFile(file, "file");
        }
    }

    public void sendImg() {
        File file = FileUtils.fileChooser(new FileNameExtensionFilter("图片", "jpg", "jpeg", "png", "gif"), null);
        if (file != null) {
            _sendFile(file, "img");
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
                chatRoomFrame = new Main();
                chatRoomFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                        if (JOptionPane.showConfirmDialog(null, "你确定要退出吗?",
                                "退出聊天室", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                            safeExit(cSocket);
                        }
                    }
                });
                chatRoomFrame.setVisible(true);
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
                    chatRoomFrame, panel, "登录", JOptionPane.OK_CANCEL_OPTION);
            if (s != JOptionPane.OK_OPTION) {
                // 用户点击了取消或关闭
                chatRoomFrame.dispose();
                return;
            }
            username = userNameField.getText().strip();
            password = String.valueOf(passwordField.getPassword()).strip();
        }

        Config.getInstance().setUserName(username);
        chatRoomFrame.setTitle(username + " | " + Config.getInstance().getAppName());

        try {
            cSocket = new Socket(Config.getInstance().getServerHost(), Config.getInstance().getServerPort());
            os = cSocket.getOutputStream();
            ps = new PrintStream(os);
            is = cSocket.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);

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


    public Main() {
        setTitle(Config.getInstance().getAppName());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 700, 500);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.max(d.width / 2, getSize().width);
        int height = Math.max(d.height / 2, getSize().height);
        width = Math.min(width, d.width * 2 / 3);
        height = Math.min(height, d.height * 2 / 3);
        setSize(width, height);
        setLocation(d.width / 2 - width / 2, new Random().nextInt(height / 2) + 1);

        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JPanel upPanel = new JPanel();
        upPanel.setLayout(new BorderLayout(0, 0));
        contentPane.add(upPanel, BorderLayout.CENTER);

        recordsPane = new JEditorPane();
        recordsPane.setEditable(false);
        recordsPane.setContentType("text/html");
        recordsPane.addHyperlinkListener(hyperlinkListener);
        recordsPane.addMouseListener(new LinkUtils.HyperlinkMouseListener());
        DefaultCaret caret = (DefaultCaret) recordsPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        upPanel.add(new JScrollPane(recordsPane), BorderLayout.CENTER);

        JPanel onlineUserPanel = new JPanel();
        onlineUserPanel.setLayout(new BorderLayout(0, 0));
        onlineUserPanel.setPreferredSize(new Dimension(120, 0));
        upPanel.add(onlineUserPanel, BorderLayout.EAST);

        onlineUserPane = new JEditorPane();
        onlineUserPane.setContentType("text/html");
        onlineUserPane.addHyperlinkListener(hyperlinkListener);
        onlineUserPane.setEditable(false);
        DefaultCaret caret1 = (DefaultCaret) onlineUserPane.getCaret();
        caret1.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        onlineUserPanel.add(new JScrollPane(onlineUserPane));

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout(0, 0));
        contentPane.add(controlPanel, BorderLayout.SOUTH);
        JScrollPane inputScrollPane = new JScrollPane();
        controlPanel.add(inputScrollPane);

        sendTextArea = new JTextArea();
        sendTextArea.setRows(3);
        sendTextArea.setLineWrap(true);
        inputScrollPane.setViewportView(sendTextArea);
        JMenuBar menuBar = new JMenuBar();
        inputScrollPane.setColumnHeaderView(menuBar);
        JButton sendImgBtn = new JButton("发送图片");
        sendImgBtn.addActionListener(e -> sendImg());
        menuBar.add(sendImgBtn);
        JButton sendFileBtn = new JButton("发送文件");
        sendFileBtn.addActionListener(e -> sendFile());
        menuBar.add(sendFileBtn);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendText());
        controlPanel.add(sendButton, BorderLayout.EAST);
    }
}
