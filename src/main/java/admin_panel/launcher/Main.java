package admin_panel.launcher;

import admin_panel.database.DB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.io.IOException;

public class Main extends Application {

    private static final Logger LOG = Logger.getLogger(Main.class);

    @Override
    public void start(Stage stage) throws Exception {
        LOG.info("[STARTED] "+toString()+" application.");
        startPrimaryStage(stage);
        DB db = new DB();
        db.connectingDB();
    }

    private void startPrimaryStage(Stage stage) throws IOException {
        Parent parent = FXMLLoader.load(getClass().getResource("/layout/main.fxml"));
        stage.setTitle("Панель администратора");
        stage.setScene(new Scene(parent, 900, 600));
        stage.setMinHeight(600);
        stage.setMinWidth(900);
        stage.getIcons().add(new Image("/img/logo.png"));
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        LOG.info("[STOP] "+toString()+" application.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
