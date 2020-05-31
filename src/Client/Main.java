package Client;

import java.awt.*;
import java.io.*;
import java.net.Socket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;

import Common.User;
import Utils.ByteUtils;
import Utils.FileUtils;
import Utils.SocketUtils;
import Common.Message;
import com.alibaba.fastjson.JSON;

public class Main extends JFrame {
    private static final long serialVersionUID = 1L;

    private static PrintStream ps;
    private static Main frame;
    private static JEditorPane recordsPane;
    private static Socket cSocket = null;
    private static JTextArea sendTextArea;
    private static JEditorPane onlineUserPane;
    HyperlinkListener hyperlinkListener = new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    String x = e.getDescription();
                    if (x.startsWith("user://")) {
                        String atPerson = x.replace("user://", "");
                        sendTextArea.setText(sendTextArea.getText() + " @" + atPerson);
                    } else if (x.startsWith("img://")) {
                        String imgPath = x.replace("img://", "");
                        EventQueue.invokeLater(() -> {
                            try {
                                ImageFrame imgFrame = new ImageFrame(imgPath);
                                imgFrame.setVisible(true);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        });
                    } else if (x.startsWith("file://")) {
                        String content = x.substring(7);
                        int r = content.lastIndexOf(";");
                        String filename = "";
                        if (r != -1) {
                            filename = content.substring(0, r);
                            content = content.substring(r + 1);
                        }
                        System.out.println(content);
                        byte[] fileSrc = ByteUtils.decodeBase64StringToByte(content);
                        fileSrc = ByteUtils.unGZip(fileSrc);
                        File file = FileUtils.fileChooser(null, new File(filename));
                        if (file != null) {
                            String filePath = file.getAbsolutePath();
                            System.out.println(filePath);
                            FileUtils.saveContent(fileSrc, file);
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    };

    public static boolean handle_message(String json) throws Exception {
        if (json == null) {
            return false;
        }
        System.out.println("handle: " + json);
        Message msg = JSON.parseObject(json, Message.class);
        switch (msg.type) {
            case "text":
                recordsPane.setText(Records.parseText(msg));
                break;
            case "event":
                switch (msg.msg) {
                    case "join":
                    case "left":
                        recordsPane.setText(Records.parseJoinOrLeft(msg));
                        onlineUserPane.setText(Records.parseOnlineUsers(msg));
                        break;
                }
                break;
            case "img":
                recordsPane.setText(Records.parseImg(msg, frame));
                break;
            case "file":
                recordsPane.setText(Records.parseFile(msg));
                break;
        }
        return true;
    }

    static class SendUtils {
        private static User getUser() {
            return new User(Config.getInstance().getUserName());
        }

        public static void sendText() {
            String text = sendTextArea.getText();
            if (text.equals("")) {
                return;
            }
            String jsonString = JSON.toJSONString(new Message(getUser(), text));
            ps.println(jsonString);
            sendTextArea.setText("");

        }

        private static void _sendFile(File file, String type) {
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

        public static void sendFile() {
            File file = FileUtils.fileChooser(null, null);
            if (file != null) {
                _sendFile(file, "file");
            }

        }

        public static void sendImg() {
            File file = FileUtils.fileChooser(new FileNameExtensionFilter("图片", "jpg", "jpeg", "png", "gif"), null);
            if (file != null) {
                _sendFile(file, "img");
            }

        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                frame = new Main();
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                        if (JOptionPane.showConfirmDialog(frame, "Are you sure you want to close this window?",
                                "Close Window?", JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                            SocketUtils.safeExit(cSocket);
                        }
                    }
                });
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        String userName = JOptionPane.showInputDialog(frame, "请输入用户名:");
        if (userName == null) {
            frame.dispose();
            return;
        }
        while (userName.isEmpty()) {
            userName = JOptionPane.showInputDialog(frame, "请重新输入用户名:");
            if (userName == null) {
                frame.dispose();
                return;
            }
        }
        Config.getInstance().setUserName(userName);

        String password = JOptionPane.showInputDialog(frame, "请输入密码:");
        if (password == null) {
            frame.dispose();
            return;
        }
        while (password.isEmpty()) {
            password = JOptionPane.showInputDialog(frame, "请重新输入密码:");
            if (password == null) {
                frame.dispose();
                return;
            }
        }
        frame.setTitle(Config.getInstance().getAppName() + " - User：" + userName);

        try {
            cSocket = new Socket(Config.getInstance().getServerHost(), Config.getInstance().getServerPort());
            OutputStream os = cSocket.getOutputStream();
            InputStream is = cSocket.getInputStream();
            ps = new PrintStream(os);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            System.out.println(userName + "向服务器发送用户名密码");
            ps.println(userName + ";" + password);
            String ack = br.readLine();
            if (ack.equals("ack" + userName)) {
                System.out.println("服务器响应：" + ack);
                ps.println("ack" + userName);
                System.out.println("连接服务器成功");
                while (true) {
                    String json = br.readLine();
                    if (handle_message(json)) {
                        System.out.println("handled.");
                    } else {
                        JOptionPane.showMessageDialog(frame, json);
                    }
                }
            } else if (ack.equals("403")) {
                JOptionPane.showMessageDialog(frame, "密码错误", "Error", JOptionPane.ERROR_MESSAGE);
                SocketUtils.safeExit(cSocket);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
        } finally {
            SocketUtils.safeExit(cSocket);
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
        setLocation(d.width / 2 - width / 2, 0);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

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
        sendImgBtn.addActionListener(e -> SendUtils.sendImg());
        menuBar.add(sendImgBtn);
        JButton sendFileBtn = new JButton("发送文件");
        sendFileBtn.addActionListener(e -> SendUtils.sendFile());
        menuBar.add(sendFileBtn);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> SendUtils.sendText());
        controlPanel.add(sendButton, BorderLayout.EAST);
    }


}
