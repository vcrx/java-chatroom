package Client;


public class Config {
    private static final Config instance = new Config();

    private final String APP_NAME = "Õ¯…œ≥Â¿À¡ƒÃÏ “";
    private final String SERVER_HOST = "localhost";
    private final int SERVER_PORT = 1002;

    private Config() {
    }

    public static Config getInstance() {
        return instance;
    }

    public int getServerPort() {
        return SERVER_PORT;
    }

    public String getAppName() {
        return APP_NAME;
    }

    public String getServerHost() {
        return SERVER_HOST;
    }
}
