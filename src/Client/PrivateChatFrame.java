package Client;

import Client.Constants.LinkPrefix;
import Client.Constants.MessageType;
import Model.Message;
import Model.User;
import Client.Utils.FileUtils;
import Client.Utils.LinkUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Random;

public class PrivateChatFrame extends ChatFrame {
    private static final long serialVersionUID = 2L;
    private JEditorPane recordsPane;
    private JTextArea sendTextArea;
    private final User toUser;

    @Override
    public void addRecords(Message msg) throws Exception {
        switch (msg.type) {
            case MessageType.TEXT:
                recordsPane.setText(rc.parseText(msg));
                break;
            case MessageType.IMAGE:
                recordsPane.setText(rc.parseImg(msg, this));
                break;
            case MessageType.FILE:
                recordsPane.setText(rc.parseFile(msg));
                break;
        }
    }

    HyperlinkListener hyperlinkListener = e -> {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                String x = e.getDescription();
                if (x.startsWith(LinkPrefix.USER)) {
                    String toUser = x.replace(LinkPrefix.USER, "");
                    sendTextArea.setText(sendTextArea.getText() + " @" + toUser);
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
    public void sendText() {
        String text = sendTextArea.getText();
        sendTextArea.setText("");
        if (text.equals("")) {
            return;
        }
        Message msg = new Message(CurrUser.getInstance().getUser(), text);
        msg.toUser = this.toUser;
        sendMsg(msg);
    }

    @Override
    public void sendFile() {
        File file = FileUtils.chooseFile();
        if (file != null) {
            Message msg = getSendFileMsg(file, MessageType.FILE);
            msg.toUser = this.toUser;
            sendMsg(msg);
        }
    }

    @Override
    public void sendImg() {
        File file = FileUtils.chooseImage();
        if (file != null) {
            Message msg = getSendFileMsg(file, MessageType.IMAGE);
            msg.toUser = this.toUser;
            sendMsg(msg);
        }
    }

    @Override
    public void initialize() {
        setTitle(CurrUser.getInstance().getUserName() + " 在与 " + toUser.userName + " 私聊");
        setBounds(100, 100, 700, 500);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.max(d.width / 2, getSize().width);
        int height = Math.max(d.height / 2, getSize().height);
        width = Math.min(width, d.width * 2 / 3);
        height = Math.min(height, d.height * 2 / 3);
        setSize(width, height);
        setLocation(d.width / 2 - width / 2, new Random().nextInt(height / 2) + 1);
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
        recordsPane.addMouseListener(new LinkUtils.HyperlinkMouseListener());
        DefaultCaret caret = (DefaultCaret) recordsPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        upPanel.add(new JScrollPane(recordsPane), BorderLayout.CENTER);

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

        recordsPane.setText(rc.content.toString());
    }

    public PrivateChatFrame(User user) {
        this.toUser = user;
        initialize();
    }
}
