package Client;

import Common.Message;
import Common.User;
import Utils.ByteUtils;
import Utils.FileUtils;
import com.alibaba.fastjson.JSON;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class PrivateChatFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private JEditorPane recordsPane;
    private JTextArea sendTextArea;
    private JEditorPane onlineUserPane;
    private PrintStream ps;
    private User toUser;


    HyperlinkListener hyperlinkListener = e -> {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                String x = e.getDescription();
                if (x.startsWith("img://")) {
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
                    byte[] fileSrc = ByteUtils.decodeBase64StringToByte(content);
                    fileSrc = ByteUtils.unGZip(fileSrc);
                    File file = FileUtils.fileChooser(null, new File(filename));
                    if (file != null) {
                        String filePath = file.getAbsolutePath();
                        FileUtils.saveContent(fileSrc, file);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    };

    public boolean handle_message(String json) throws Exception {
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
                recordsPane.setText(Records.parseImg(msg, this));
                break;
            case "file":
                recordsPane.setText(Records.parseFile(msg));
                break;
        }
        return true;
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
        File file = FileUtils.fileChooser(new FileNameExtensionFilter("Í¼Æ¬", "jpg", "jpeg", "png", "gif"), null);
        if (file != null) {
            _sendFile(file, "img");
        }

    }


    public PrivateChatFrame(PrintStream ps, String toUserJson) {
        this.ps = ps;
        this.toUser = JSON.parseObject(toUserJson, User.class);

        setTitle("Óë " + toUser.userName + " Ë½ÁÄ");
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
        JButton sendImgBtn = new JButton("·¢ËÍÍ¼Æ¬");
        sendImgBtn.addActionListener(e -> sendImg());
        menuBar.add(sendImgBtn);
        JButton sendFileBtn = new JButton("·¢ËÍÎÄ¼þ");
        sendFileBtn.addActionListener(e -> sendFile());
        menuBar.add(sendFileBtn);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendText());
        controlPanel.add(sendButton, BorderLayout.EAST);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                dispose();
            }

            @Override
            public void windowStateChanged(WindowEvent e) {
                super.windowStateChanged(e);
                repaint();
            }
        });
    }
}
