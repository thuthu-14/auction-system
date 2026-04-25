// src/main/java/common/AppConfig.java
package common;

/**
 * AppConfig - Cấu hình ứng dụng
 * Design Pattern: Singleton
 */
public class AppConfig {

    private static String serverHost = Constants.SERVER_HOST;
    private static int serverPort = Constants.SERVER_PORT;
    private boolean isDebugMode = false;
    private static AppConfig instance;

    private AppConfig() {}

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public boolean isDebugMode() {
        return isDebugMode;
    }

    public void setServerHost(String host) {
        serverHost = host;
    }

    public void setServerPort(int port) {
        serverPort = port;
    }

    public void setDebugMode(boolean debug) {
        isDebugMode = debug;
    }

    public String getServerURL() {
        return "http://" + serverHost + ":" + serverPort;
    }
}

