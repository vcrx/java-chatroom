package Utils;

import Client.ImageFrame;
import Utils.ByteUtils;
import Utils.FileUtils;

import java.awt.*;
import java.io.File;

public class LinkUtils {
    public static void FileHandler(String fileLink) {
        String content = fileLink.substring(7);
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
            FileUtils.saveContent(fileSrc, file);
        }
    }

    public static void ImageHandler(String imageLink) {
        String imgPath = imageLink.replace("img://", "");
        EventQueue.invokeLater(() -> {
            try {
                ImageFrame imgFrame = new ImageFrame(imgPath);
                imgFrame.setVisible(true);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
    }
}
