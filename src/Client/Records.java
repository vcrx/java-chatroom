package Client;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import Utils.ByteUtils;
import Utils.FileUtils;
import Utils.TimeUtils;
import Common.Message;
import Common.User;
import com.alibaba.fastjson.JSON;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import javax.swing.*;

public class Records {
    static String baseHtml = "<html><head></head>"
            + "<body style='font-size:14px;word-wrap:break-word;white-space:normal;'>" + "</body></html>";
    static Document content = Jsoup.parse(baseHtml);
    static Elements body = content.getElementsByTag("body");

    private static Element genUserA(User user) {
        Element a = new Element("a");
        a.attr("style", "font-weight:bold");
        a.attr("href", "user://" + JSON.toJSONString(user));
        a.append(user.userName);
        return a;
    }

    private static String genHeader(Message msg) {
        Element span = new Element("span");
        span.attr("style", "font-weight:bold; color: blue");
        span.append("[" + TimeUtils.parseTimeStamp(msg.timeStamp) + "]");
        return span.toString() + genUserA(msg.user).toString();
    }

    public static String parseText(Message msg) {
        String build = genHeader(msg) +
                "：<br /><p style='font-size:16px;margin-top:3px;'>" + msg.msg.replace("\n", "<br/>") + "</p><br />";
        body.append(build);
        return content.toString();
    }

    public static String parseJoinOrLeft(Message msg) {
        String type = msg.msg.equals("left") ? "离开" : "加入";

        String build = genHeader(msg) + "<span style='font-weight:bold; color: red'>" + type + "了聊天室。</span><br />";
        body.append(build);
        return content.toString();
    }

    public static String parseOnlineUsers(Message msg) {
        String html = baseHtml;
        Document c = Jsoup.parse(html);
        Elements b = c.getElementsByTag("body");
        b.attr("style", "color:#192e4d;font-size:10px;word-wrap:break-word;white-space:normal;");
        Element ul = new Element("ul");
        b.append("在线用户：");
        ul.attr("style", "font-size:13px;padding:0;margin:14");
        for (User user : msg.users) {
            Element li = new Element("li");
            Element a = genUserA(user);
            li.appendChild(a);
            ul.appendChild(li);
        }
        b.append(ul.toString());
        return c.toString();
    }


    public static String parseImg(Message msg, JFrame frame) throws Exception {
        Path tempDir = FileUtils.getTempDirectory();
        if (tempDir == null) {
            throw new Exception("tempDir is null");
        }
        byte[] imgSrc = ByteUtils.decodeBase64StringToByte(msg.msg);
        imgSrc = ByteUtils.unGZip(imgSrc);
        URI path = Files.write(Paths.get(tempDir + "/" + msg.filename), imgSrc).toAbsolutePath().toUri();
        System.out.println(frame.getSize().width);
        Element a = new Element("a");
        a.attr("href", "img://" + path);
        Element img = new Element("img");
        img.attr("src", String.valueOf(path));
        img.attr("alt", "无法显示图片");
        img.attr("style", "height:auto;");
        img.attr("width", String.valueOf((frame.getSize().width / 2)));
        a.appendChild(img);

        String build = genHeader(msg) +
                "：<br /><div style='font-size:16px;margin-top:3px;'>" +
                a +
                "</div><br />";
        body.append(build);
        return content.toString();
    }

    public static String parseFile(Message msg) {
        Element a = new Element("a");
        a.attr("style", "font-weight:bold");
        a.attr("href", "file://" + msg.filename + ";" + msg.msg);
        a.append(msg.filename);
        String build = genHeader(msg) +
                "：<br /><div style='font-size:20px;margin-top:3px;'>" +
                a.toString() +
                "</div><br />";
        body.append(build);
        return content.toString();
    }
}
