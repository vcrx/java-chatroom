package Client;

import Model.Message;
import com.alibaba.fastjson.JSON;

import javax.swing.*;

abstract public class ChatFrame extends JFrame {
    protected final Records rc;

    protected ChatFrame() {
        this.rc = new Records();
    }

    abstract public void initialize();

    abstract public void addRecords(Message msg) throws Exception;

    public void sendMsg(Message msg) {
        if (msg == null) return;
        CurrUser.getInstance().send(JSON.toJSONString(msg));
    }


    abstract public void sendText();

    abstract public void sendFile();

    abstract public void sendImg();
}
