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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.Random;

public class PrivateChatFrame extends JFrame {
    private static final long serialVersionUID = 2L;
    private static final Records rc = new Records();

    private JEditorPane recordsPane;
    private JTextArea sendTextArea;
    private final PrintStream ps;
    private final User toUser;

    public void addRecords(Message msg) throws Exception {
        switch (msg.type) {
            case "text":
                recordsPane.setText(rc.parseText(msg));
                break;
            case "img":
                recordsPane.setText(rc.parseImg(msg, this));
                break;
            case "file":
                recordsPane.setText(rc.parseFile(msg));
                break;
        }
    }

    HyperlinkListener hyperlinkListener = e -> {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                String x = e.getDescription();
                if (x.startsWith("user://")) {
                    String toUser = x.replace("user://", "");
                    sendTextArea.setText(sendTextArea.getText() + " @" + toUser);
                } else if (x.startsWith("img://")) {
                    LinkUtils.ImageHandler(x);
                } else if (x.startsWith("file://")) {
                    LinkUtils.FileHandler(x);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    };


    private User getUser() {
        return new User(Config.getInstance().getUserName());
    }

    public void sendText() {
        String text = sendTextArea.getText();
        if (text.equals("")) {
            return;
        }
        Message msg = new Message(getUser(), text);
        msg.toUser = this.toUser;
        String jsonString = JSON.toJSONString(msg);
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
            msg.toUser = this.toUser;
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

    public void initialize() {
        setTitle(Config.getInstance().getUserName() + " 在与 " + toUser.userName + " 私聊");
        setBounds(100, 100, 700, 500);
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.max(d.width / 2, getSize().width);
        int height = Math.max(d.height / 2, getSize().height);
        width = Math.min(width, d.width * 2 / 3);
        height = Math.min(height, d.height * 2 / 3);
        setSize(width, height);
        setLocation(d.width / 2 - width / 2, new Random().nextInt(height) + 1);
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

    public PrivateChatFrame(PrintStream ps, User user) {
        this.ps = ps;
        this.toUser = user;
        initialize();
    }
}
