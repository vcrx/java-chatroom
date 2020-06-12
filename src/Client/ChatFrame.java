package Client;

import Model.Message;
import Client.Utils.FileUtils;
import com.alibaba.fastjson.JSON;

import javax.swing.*;
import java.io.File;
import java.io.PrintStream;

abstract public class ChatFrame extends JFrame {
    protected final Records rc;

    protected ChatFrame() {
        this.rc = new Records();
    }

    abstract public void initialize();

    abstract public void addRecords(Message msg) throws Exception;

    public void sendMsg(Message msg) {
        if (msg == null) return;
        String jsonString = JSON.toJSONString(msg);
        CurrUser.getInstance().send(jsonString);
    }

    public Message getSendFileMsg(File file, String type) {
        String dataB64 = FileUtils.getGZippedFileB64(file);
        if (dataB64 == null) return null;
        return new Message(CurrUser.getInstance().getUser(), dataB64, type, file.getName());
    }

    abstract public void sendText();

    abstract public void sendFile();

    abstract public void sendImg();
}
