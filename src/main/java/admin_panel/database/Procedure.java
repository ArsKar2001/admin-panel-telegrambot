package admin_panel.database;

import org.apache.log4j.Logger;

import java.sql.CallableStatement;
import java.sql.SQLException;

public class Procedure extends DB {
    private static final Logger LOG = Logger.getLogger(Procedure.class);

    /**
     * Проверка на существование такого ChatId в базе.
     * @param chatId идетификатор чата
     * @return
     */
    public boolean isExists(Long chatId) {
        CallableStatement statement;
        try {
            statement = this.getConnection().prepareCall("{call isCheckChatId(?)}");
            statement.setLong("chatId", chatId);
            return statement.execute();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
        return false;
    }
}
