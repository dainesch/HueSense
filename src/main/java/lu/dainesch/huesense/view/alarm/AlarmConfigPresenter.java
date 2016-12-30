package lu.dainesch.huesense.view.alarm;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javax.inject.Inject;
import lu.dainesch.huesense.hue.DataStore;
import lu.dainesch.huesense.hue.data.Sensor;
import lu.dainesch.huesense.hue.data.SensorType;
import lu.dainesch.huesense.net.LanComm;
import lu.dainesch.huesense.net.MailService;
import lu.dainesch.huesense.net.data.MailSettings;
import lu.dainesch.huesense.net.data.NetworkDevice;
import lu.dainesch.huesense.view.UIUtils;

public class AlarmConfigPresenter implements Initializable {

    @FXML
    private BorderPane borderPane;
    @FXML
    private TableView<SelectableTableEntry<Sensor<?>>> sensorTable;
    @FXML
    private TableView<SelectableTableEntry<NetworkDevice>> netTable;
    @FXML
    private TextField addHostField;
    @FXML
    private TextField userField;
    @FXML
    private TextField smtpServerField;
    @FXML
    private TextField smtpPortField;
    @FXML
    private PasswordField passField;
    @FXML
    private CheckBox useAuthCB;
    @FXML
    private ChoiceBox<MailSettings.Mode> modeCB;
    @FXML
    private TextField fromField;
    @FXML
    private TextField toField;
    @FXML
    private Label mailTestLabel;
    @FXML
    private ProgressBar progBar;

    @Inject
    private DataStore store;
    @Inject
    private MailService mailServ;
    @Inject
    private LanComm lan;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        mailTestLabel.setText("");
        UIUtils.setTextFieldNumbersOnly(smtpPortField, 1, 65535);

        TableColumn sensorCol = new TableColumn("Sensor");
        TableColumn selectedSensorCol = new TableColumn("");
        selectedSensorCol.setMaxWidth(50);

        sensorCol.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );
        selectedSensorCol.setCellValueFactory(
                new SelectableTableEntry.STECellFactory()
        );
        sensorTable.getColumns().addAll(sensorCol, selectedSensorCol);
        sensorTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        store.getSensors().stream().filter(s -> s.getType() == SensorType.PRESENCE).forEach(s -> {
            sensorTable.getItems().add(new SelectableTableEntry<>(s.getId(), s.getName(), s));
        });

        TableColumn deviceCol = new TableColumn("Device");
        TableColumn<SelectableTableEntry<NetworkDevice>, String> reachCol = new TableColumn("Reachable");
        TableColumn selectedDeviceCol = new TableColumn("");
        reachCol.setMaxWidth(50);
        selectedDeviceCol.setMaxWidth(50);

        deviceCol.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );
        reachCol.setCellValueFactory((TableColumn.CellDataFeatures<SelectableTableEntry<NetworkDevice>, String> param) -> {
            NetworkDevice dev = param.getValue().getElement();
            StringProperty prop = new SimpleStringProperty("No");
            if (dev.getReachable().get()) {
                prop.set("Yes");
            }
            dev.getReachable().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    prop.set("Yes");
                } else {
                    prop.set("No");
                }
            });
            return prop;
        });
        selectedDeviceCol.setCellValueFactory(
                new SelectableTableEntry.STECellFactory()
        );
        netTable.getColumns().addAll(deviceCol, reachCol, selectedDeviceCol);
        netTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        lan.getDevices().addListener((ListChangeListener.Change<? extends NetworkDevice> c) -> {
            while (c.next()) {
                c.getAddedSubList().forEach((d) -> {
                    netTable.getItems().add(new SelectableTableEntry<>(d.toString().hashCode(), d.toString(), d));
                });
                c.getRemoved().forEach(d -> {
                    netTable.getItems().remove(new SelectableTableEntry<>(d.toString().hashCode(), d.toString(), d));
                });
            }
        });

        modeCB.getItems().addAll(Arrays.asList(MailSettings.Mode.values()));
        modeCB.getSelectionModel().select(MailSettings.Mode.TLS);

        lan.getScanning().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                progBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            } else {
                progBar.setProgress(0);
            }
        });

        Dialog diag = new Dialog();
        diag.getDialogPane().setContent(borderPane);
        diag.getDialogPane().setBackground(Background.EMPTY);
        diag.setResizable(true);

        UIUtils.setIcon(diag);

        ButtonType okButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        diag.getDialogPane().getButtonTypes().addAll(cancel, okButton);
        diag.setTitle("Configure Alarm");

        lan.startScan();

        Optional<ButtonType> opt = diag.showAndWait();
        if (opt.isPresent() && opt.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            onSave();
        }

    }

    @FXML
    void onAddHost(ActionEvent event) {
        String host = addHostField.getText();
        if (host != null && !host.trim().isEmpty()) {
            NetworkDevice dev = new NetworkDevice(host, null);
            netTable.getItems().add(new SelectableTableEntry<>(dev.toString().hashCode(), host, dev));
            lan.checkIfReachable(dev);
        }
    }

    @FXML
    void onMailTest(ActionEvent event) {
        MailSettings sett = new MailSettings();
        sett.setFrom(fromField.getText());
        sett.setMode(modeCB.getSelectionModel().getSelectedItem());
        sett.setPass(passField.getText());
        sett.setSmtpPort(Integer.parseInt(smtpPortField.getText()));
        sett.setSmtpServer(smtpServerField.getText());
        sett.setTo(toField.getText());
        sett.setUseAuth(useAuthCB.isSelected());
        sett.setUser(userField.getText());

        if (mailServ.testSettings(sett)) {
            mailTestLabel.setTextFill(Color.GREEN);
            mailTestLabel.setText("Success!");
        } else {
            mailTestLabel.setTextFill(Color.RED);
            mailTestLabel.setText("Test failed");
        }
    }

    @FXML
    void onRescan(ActionEvent event) {
        lan.startScan();
    }

    @FXML
    void onRetestPresence(ActionEvent event) {
        netTable.getItems().forEach(e -> lan.checkIfReachable(e.getElement()));
    }

    private void onSave() {
        
        List<NetworkDevice> selectedDev = netTable.getItems().stream()
                .filter( s -> s.getSelected()).map(s -> s.getElement()).collect(Collectors.toList());
        
        lan.setPingDevices(selectedDev);

    }

}
