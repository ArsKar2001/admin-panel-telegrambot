package admin_panel;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;


public class Config {
    private static final String PATH_TO_DB_CONFIG = "src/main/resources/config/db_config.json";
    private static final String PATH_TO_BOT_CONFIG = "src/main/resources/config/bot_config.json";

    private static JSONObject getJSON() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(Config.PATH_TO_DB_CONFIG)) {
            return (JSONObject) jsonParser.parse(reader);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject getDBConfig() {
        JSONObject json = getJSON();
        assert json != null;
        return (JSONObject) json.get("config");
    }
}
