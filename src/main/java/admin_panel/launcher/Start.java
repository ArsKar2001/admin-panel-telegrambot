package admin_panel.launcher;

import admin_panel.database.DBConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.io.IOException;

public class Start extends Application {

    private static final Logger LOG = Logger.getLogger(Start.class);
    private static final int PRIORITY_FOR_DB_CONNECT = 1;

    @Override
    public void start(Stage stage) throws Exception {
        LOG.info("[STARTED] "+toString()+" application.");
        setPrimaryStage(stage);
        setDBConnection();
    }

    private void setDBConnection() {
        DBConnection dbConnection = new DBConnection();
        Thread dbConnect = new Thread(dbConnection);
        dbConnect.setDaemon(true);
        dbConnect.setName("DBConnection");
        dbConnect.setPriority(PRIORITY_FOR_DB_CONNECT);
        dbConnect.start();
    }

    private void setPrimaryStage(Stage stage) throws IOException {
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
