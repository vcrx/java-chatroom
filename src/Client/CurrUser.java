package Client;

import Model.User;
import cn.hutool.core.io.IoUtil;

import java.io.PrintStream;

public class CurrUser {
    private static final CurrUser instance = new CurrUser();
    private PrintStream ps;

    private String userName;

    private CurrUser() {
    }

    public PrintStream getPs() {
        return this.ps;
    }

    public void setPs(PrintStream ps) {
        this.ps = ps;
    }

    public static CurrUser getInstance() {
        return instance;
    }

    public String getUserName() {
        return userName;
    }

    public User getUser() {
        return new User(getUserName());
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void send(String text) {
        ps.println(text);
    }

    public void close() {
        IoUtil.close(ps);
    }
}
