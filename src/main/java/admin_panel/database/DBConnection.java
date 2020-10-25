package admin_panel.database;

import admin_panel.Config;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection implements Runnable {
    private static final Logger LOG = Logger.getLogger(DBConnection.class);
    private static final short PAUSE_MS = 10000;
    @Getter
    protected static Connection connection;
    public static boolean isConnected = false;

    private void startConnection() {
        JSONObject dbConfig = Config.getDBConfig();

        String dbHost = (String) dbConfig.get("host");
        String dbName = (String) dbConfig.get("name");
        String dbPort = (String) dbConfig.get("port");
        String dbPass = (String) dbConfig.get("password");
        String dbLogin = (String) dbConfig.get("login");

        String url = "jdbc:mysql://"+dbHost+":"+dbPort+"/"+dbName+"?useSSL=false";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(url, dbLogin, dbPass);
            isConnected = true;
            LOG.info("[STARTED] DBConnection. DB url: "+url);
        } catch (ClassNotFoundException | SQLException e) {
            LOG.warn(e.getMessage()+" Repeat after "+PAUSE_MS / 1000 +" sec.");
            isConnected = false;
            try {
                Thread.sleep(PAUSE_MS);
            } catch (InterruptedException interruptedException) {
                LOG.error(interruptedException.getMessage(), interruptedException);
            }
            startConnection();
        }
    }

    @Override
    public void run() {
        startConnection();
    }
}
