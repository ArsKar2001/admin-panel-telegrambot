package admin_panel.database;

import admin_panel.Config;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    private static final Logger LOG = Logger.getLogger(DB.class);
    private static final short PAUSE_MS = 10000;
    @Getter
    public Connection connection;
    public static boolean isConnected = false;

    public void connectingDB() {
        JSONObject dbConfig = Config.getDBConfig();

        String dbHost = (String) dbConfig.get("host");
        String dbName = (String) dbConfig.get("name");
        String dbPort = (String) dbConfig.get("port");
        String dbPass = (String) dbConfig.get("password");
        String dbLogin = (String) dbConfig.get("login");

        String url = "jdbc:mysql://"+dbHost+":"+dbPort+"/"+dbName+"?useSSL=false";
        LOG.info("[STARTED] DB Thread. DB url: "+url);
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(url, dbLogin, dbPass);
            isConnected = true;
            LOG.debug("The connection is established...");
        } catch (ClassNotFoundException | SQLException e) {
            LOG.warn(e.getMessage()+" Repeat after "+PAUSE_MS / 1000 +" sec.");
            isConnected = false;
            try {
                Thread.sleep(PAUSE_MS);
            } catch (InterruptedException interruptedException) {
                LOG.error(interruptedException.getMessage(), interruptedException);
            }
            connectingDB();
        }
    }
}
