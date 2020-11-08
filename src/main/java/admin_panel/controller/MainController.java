package admin_panel.controller;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import admin_panel.converter.ConvertDocxToCSV;
import admin_panel.entityes.Lesson;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import lombok.var;
import org.apache.log4j.Logger;

public class MainController extends BaseController {
    private static final Logger LOG = Logger.getLogger(MainController.class);

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private Font x1;

    @FXML
    private Color x2;

    @FXML
    private Button btnShowListUsers;

    @FXML
    private Button btnShowListGroup;

    @FXML
    private AnchorPane usersPane;

    @FXML
    private AnchorPane groupsPane;

    @FXML
    private Font x11;

    @FXML
    private Color x21;

    @FXML
    private TableView<Lesson> tbGroups;

    @FXML
    private TableColumn<Lesson, String> clGName;

    @FXML
    private TableColumn<Lesson, String> clGNumber;

    @FXML
    private TableColumn<Lesson, String> clGLesson;

    @FXML
    private TableColumn<Lesson, String> clGTeachers;

    @FXML
    private TableColumn<Lesson, String> clGAudience;

    @FXML
    private Button btnImportGroups;

    @FXML
    private Font x3;

    @FXML
    private Color x4;

    @FXML
    void initialize() {
        btnImportGroups.setOnAction(this::clickOpenFileToConvert);
        btnShowListUsers.setOnAction(this::clickShowPaneUsers);
        btnShowListGroup.setOnAction(this::clickShowPaneGroups);

        clGName.setCellValueFactory(new PropertyValueFactory<>("groupName"));
        clGNumber.setCellValueFactory(new PropertyValueFactory<>("number"));
        clGLesson.setCellValueFactory(new PropertyValueFactory<>("discipline"));
        clGAudience.setCellValueFactory(new PropertyValueFactory<>("audience"));
        clGTeachers.setCellValueFactory(new PropertyValueFactory<>("teacher"));
    }

    private void clickShowPaneGroups(ActionEvent actionEvent) {
        usersPane.setOpacity(0);
        groupsPane.setOpacity(1);
    }

    private void clickShowPaneUsers(ActionEvent actionEvent) {
        usersPane.setOpacity(1);
        groupsPane.setOpacity(0);
    }

    private void clickOpenFileToConvert(ActionEvent actionEvent) {
        File file = showOpenDialogSelectingFile((Node) actionEvent.getSource());
        if (file != null) {
            ConvertDocxToCSV convertDocxToCSV = new ConvertDocxToCSV(file);
            convertDocxToCSV.convert();
            loadGroupsInTable(convertDocxToCSV.getSplitPages());
        }
    }

    private void loadGroupsInTable(List<List<String>> splitPages) {
        List<Lesson> lessonList = new ArrayList<>();
        for (var page : splitPages) {
            for (var line : page) {
                String[] splitLine = line.split(";");
                Lesson newLes;
                try {
                    if(splitLine[0].equals("")) continue;
                    if(splitLine.length == 2) {
                        newLes = new Lesson();
                        newLes.setGroupName(splitLine[0]);
                        newLes.setNumber(splitLine[1]);
                    } else if (splitLine.length == 4) {
                        newLes = new Lesson(splitLine[0], splitLine[1], splitLine[2], splitLine[3], "-");
                    } else {
                        newLes = new Lesson(splitLine[0], splitLine[1], splitLine[2], splitLine[3], splitLine[4]);
                    }
                    lessonList.add(newLes);
                } catch (Exception e) {
                    LOG.warn(e.getMessage()+"\t"+line);
                }
            }
        }
        tbGroups.getItems().addAll(lessonList);
    }
}

