package Client;

import Model.Message;
import Model.User;
import Utils.FileUtils;
import com.alibaba.fastjson.JSON;

import javax.swing.*;
import java.io.File;
import java.io.PrintStream;

abstract public class ChatFrame extends JFrame {
    protected final Records rc;
    protected PrintStream ps;

    protected ChatFrame() {
        this.rc = new Records();
    }

    public void setPrintStream(PrintStream ps) {
        this.ps = ps;
    }

    abstract public void initialize();

    abstract public void addRecords(Message msg) throws Exception;

    public void sendMsg(Message msg) {
        if (msg == null) return;
        String jsonString = JSON.toJSONString(msg);
        ps.println(jsonString);
    }

    public Message getSendFileMsg(File file, String type) {
        String dataB64 = FileUtils.getGZippedFileB64(file);
        if (dataB64 == null) return null;
        return new Message(Config.getInstance().getUser(), dataB64, type, file.getName());
    }

    abstract public void sendText();

    abstract public void sendFile();

    abstract public void sendImg();
}
