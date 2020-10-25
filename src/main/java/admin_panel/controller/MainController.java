package admin_panel.controller;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import admin_panel.converter.ConvertPDFToCSV;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

public class MainController extends BaseController {

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Font x1;

    @FXML
    private Color x2;

    @FXML
    private Font x3;

    @FXML
    private Color x4;

    @FXML
    private Button btnConvert;

    @FXML
    void initialize() {
        btnConvert.setOnAction(this::clickOpenFileToConvert);
    }

    private void clickOpenFileToConvert(ActionEvent actionEvent) {
        File file = showOpenDialogSelectingFile((Node) actionEvent.getSource());
        if (file != null) {
            ConvertPDFToCSV convertPDFToCSV = new ConvertPDFToCSV(file);
            convertPDFToCSV.convert();
        }
    }
}

