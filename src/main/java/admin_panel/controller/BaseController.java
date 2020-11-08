package admin_panel.controller;

import javafx.scene.Node;
import javafx.stage.FileChooser;

import java.io.File;

public class BaseController {
    public File showOpenDialogSelectingFile(Node node) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("src\\main\\resources\\files\\pdf"));
        fileChooser.setTitle("Импорт данных");
        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("DOCX files", "*.docx");
        fileChooser.getExtensionFilters().add(extFilter);
        return fileChooser.showOpenDialog(node.getScene().getWindow());
    }
}
