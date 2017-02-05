package lu.dainesch.huesense.view.export;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javax.inject.Inject;
import lu.dainesch.huesense.hue.DataStore;
import lu.dainesch.huesense.hue.data.Sensor;
import lu.dainesch.huesense.view.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportPresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ExportPresenter.class);

    @FXML
    private BorderPane borderPane;
    @FXML
    private ListView<Sensor<?>> sensorList;
    @FXML
    private DatePicker startPicker;
    @FXML
    private DatePicker endPicker;
    @FXML
    private CheckBox nowCheck;
    @FXML
    private TextField dateInput;
    @FXML
    private Label dateLabel;

    @Inject
    private DataStore store;

    private final BooleanProperty canExport = new SimpleBooleanProperty(false);
    private final BooleanProperty validFormat = new SimpleBooleanProperty(false);
    private final BooleanProperty sensorSelected = new SimpleBooleanProperty(false);
    private Dialog diag;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        diag = new Dialog();

        diag.getDialogPane().setContent(borderPane);
        diag.getDialogPane().setBackground(Background.EMPTY);
        diag.setResizable(true);

        UIUtils.setIcon(diag);

        diag.setTitle("Export senor data to CSV");

        // date
        endPicker.disableProperty().bind(nowCheck.selectedProperty());
        dateInput.textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(newValue);
                dateLabel.setText(sdf.format(new Date()));
                validFormat.set(true);
            } catch (IllegalArgumentException ex) {
                dateLabel.setText("Invalid format!");
                validFormat.set(false);
            }
        });
        // trigger
        String cur = dateInput.getText();
        dateInput.textProperty().set("");
        dateInput.textProperty().set(cur);

        endPicker.setValue(LocalDate.now());
        LocalDate s = LocalDate.now();
        s = s.minus(7, ChronoUnit.DAYS);
        startPicker.setValue(s);

        // Bug: can not bind same data on 2 lists
        sensorList.getItems().addAll(store.getSensors());
        sensorList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            sensorSelected.set(newValue != null);
        });

        canExport.bind(sensorSelected.and(validFormat));

        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType exportButton = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        diag.getDialogPane().getButtonTypes().addAll(closeButton, exportButton);
        diag.getDialogPane().lookupButton(exportButton).disableProperty().bind(canExport.not());

        showDialog();
    }

    private void showDialog() {
        Optional<ButtonType> opt = diag.showAndWait();
        if (opt.isPresent()) {
            if (opt.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                onExport(null);
            }
        }
    }

    void onExport(ActionEvent event) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose save location");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv")
        );
        File file = fileChooser.showSaveDialog(diag.getOwner());
        if (file == null) {
            return;
        }

        Date start = Date.from(startPicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(endPicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (nowCheck.isSelected()) {
            end = new Date();
        }
        Sensor<?> sensor = sensorList.getSelectionModel().getSelectedItem();

        SimpleDateFormat sdf = new SimpleDateFormat(dateInput.getText());

        try {
            sensor.exportCSV(start, end, sdf, file.toPath());
            Alert done = new Alert(AlertType.INFORMATION);
            done.setHeaderText("Export completed");
            done.setTitle("Data export");
            UIUtils.setIcon(done);
            done.showAndWait();
        } catch (IOException ex) {
            LOG.error("Error saving csv", ex);
            Alert error = new Alert(AlertType.ERROR);
            error.setHeaderText("Error while exporting CSV");
            error.setTitle("Data export");
            UIUtils.setIcon(error);
            error.showAndWait();
        }
        showDialog();

    }

}
