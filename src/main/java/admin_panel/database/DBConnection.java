package admin_panel.database;

import admin_panel.Config;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection implements Runnable {
    private static final Logger LOG = Logger.getLogger(DBConnection.class);
    private static final short PAUSE_MS = 10000;
    private static Connection connection;

    @Getter
    @Setter
    private Long chatId;

    private void getConnection() {
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
            LOG.info("[STARTED] DBConnection. DB url: "+url);
        } catch (ClassNotFoundException | SQLException e) {
            LOG.error(e.getMessage(), e);
            try {
                Thread.sleep(PAUSE_MS);
            } catch (InterruptedException interruptedException) {
                LOG.error(interruptedException.getMessage(), interruptedException);
            }
            getConnection();
        }
    }

    public static boolean isExists(Long chatId) {
        CallableStatement statement;
        try {
            statement = connection.prepareCall("{call isCheckChatId(?)}");
            statement.setLong("chatId", chatId);
            return statement.execute();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void run() {
        getConnection();
    }
}
