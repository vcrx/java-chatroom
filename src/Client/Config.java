package Client;

import Model.User;

public class Config {
    private static final Config instance = new Config();

    private final String AppName = "Õ¯…œ≥Â¿À¡ƒÃÏ “";
    private final String SERVER_HOST = "localhost";
    private final int SERVER_PORT = 1002;
    private String userName;

    private Config() {
    }

    public static Config getInstance() {
        return instance;
    }

    public String getUserName() {
        return userName;
    }

    public User getUser() {
        return new User(getUserName());
    }

    public int getServerPort() {
        return SERVER_PORT;
    }

    public String getAppName() {
        return AppName;
    }

    public String getServerHost() {
        return SERVER_HOST;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
