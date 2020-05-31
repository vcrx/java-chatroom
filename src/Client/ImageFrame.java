package Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ImageFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    JPanel contentPane;
    JEditorPane editorPane;
    String imgUri;

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (imgUri != null) {
            System.out.println(imgUri);
            String baseHtml = "<html><head></head>"
                    + "<body style='font-size:14px;word-wrap:break-word;white-space:normal;'>"
                    + "<div style='font-size:16px;margin-top:3px;'><img src='" + this.imgUri
                    + "' alt='无法展示' style='height:auto;' width='" + (int) (this.getSize().width / 1.1) + "' /></div><br />"
                    + "</body></html>";
            editorPane.setText(baseHtml);
        }
    }

    public ImageFrame(String imgUri) {
        this.imgUri = imgUri;
        try {
            // 设置 Win10 UI 样式
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        setBounds(100, 100, 700, 700);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(0, 0));

        editorPane = new JEditorPane();
        JScrollPane scrollPane = new JScrollPane(editorPane);
        contentPane.add(scrollPane, BorderLayout.CENTER);
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
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
        setTitle("查看图片");
    }
}
