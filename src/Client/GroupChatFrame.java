package Client;

import Client.Constants.LinkPrefix;
import Client.Constants.MessageType;
import Model.Message;
import Client.Utils.FileUtils;
import Client.Utils.LinkUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.File;
import java.util.Random;

public class GroupChatFrame extends ChatFrame {
    private JEditorPane recordsPane;
    private JTextArea sendTextArea;
    private JEditorPane onlineUserPane;

    HyperlinkListener hyperlinkListener = e -> {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                String x = e.getDescription();
                if (x.startsWith(LinkPrefix.USER)) {
                    LinkUtils.showUser(x);
                } else if (x.startsWith(LinkPrefix.IMAGE)) {
                    LinkUtils.showImage(x);
                } else if (x.startsWith(LinkPrefix.FILE)) {
                    LinkUtils.saveFileWithFilename(x);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    };

    @Override
    public void initialize() {
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

    @Override
    public void addRecords(Message msg) throws Exception {
        switch (msg.type) {
            case MessageType.TEXT:
                recordsPane.setText(rc.parseText(msg));
                break;
            case MessageType.EVENT:
                switch (msg.msg) {
                    case "left":
                        PrivateChatFrame frame = PrivateChatPool.get(msg.fromUser.userName);
                        if (frame != null) frame.dispose();
                    case "join":
                        recordsPane.setText(rc.parseJoinOrLeft(msg));
                        onlineUserPane.setText(rc.parseOnlineUsers(msg));
                        break;
                }
                break;
            case MessageType.IMAGE:
                recordsPane.setText(rc.parseImg(msg, this));
                break;
            case MessageType.FILE:
                recordsPane.setText(rc.parseFile(msg));
                break;
        }
    }

    @Override
    public void sendText() {
        String text = sendTextArea.getText();
        sendTextArea.setText("");
        if (text.equals("")) return;
        sendMsg(new Message(CurrUser.getInstance().getUser(), text));
    }

    @Override
    public void sendFile() {
        File file = FileUtils.chooseFile();
        if (file == null) return;
        Message msg = FileUtils.genFileMsg(file, MessageType.FILE);
        sendMsg(msg);
    }

    @Override
    public void sendImg() {
        File file = FileUtils.chooseImage();
        if (file == null) return;
        Message msg = FileUtils.genFileMsg(file, MessageType.IMAGE);
        sendMsg(msg);
    }

    public GroupChatFrame() {
        initialize();
    }
}
