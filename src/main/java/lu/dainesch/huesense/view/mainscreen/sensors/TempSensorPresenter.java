package lu.dainesch.huesense.view.mainscreen.sensors;

import java.io.IOException;
import java.math.BigDecimal;
import lu.dainesch.huesense.hue.data.GraphInterval;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javax.inject.Inject;
import lu.dainesch.huesense.Constants;
import lu.dainesch.huesense.hue.HueComm;
import lu.dainesch.huesense.hue.data.TempSensor;
import lu.dainesch.huesense.view.DateAxis;

public class TempSensorPresenter implements Initializable {

    private static final ObservableList<BigDecimal> OFFSET_VALUES = FXCollections.observableArrayList(
            new BigDecimal("-3"), new BigDecimal("-2.5"), new BigDecimal("-2"), new BigDecimal("-1.5"), new BigDecimal("-1"), new BigDecimal("-0.5"),
            new BigDecimal("0"),
            new BigDecimal("0.5"), new BigDecimal("1"), new BigDecimal("1.5"), new BigDecimal("2"), new BigDecimal("2.5"), new BigDecimal("3")
    );

    @FXML
    private RadioButton onRadio;
    @FXML
    private RadioButton reachRadio;
    @FXML
    private ProgressBar battProg;
    @FXML
    private TextField nameField;
    @FXML
    private Label updateLabel;
    @FXML
    private Label tempLabel;
    @FXML
    private BorderPane contentPane;
    @FXML
    private ChoiceBox<GraphInterval> intervalCB;
    @FXML
    private Button changeNameBut;
    @FXML
    private CheckBox qvCB;
    @FXML
    private ChoiceBox<BigDecimal> offsetCB;

    @Inject
    private HueComm hue;

    private LineChart<Date, Number> tempChart;
    private TempSensor sensor;
    private SimpleDateFormat sdf;
    private boolean isNameEdit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sdf = new SimpleDateFormat(Constants.TIMEFORMAT);
        isNameEdit = false;

        NumberAxis numberAxis = new NumberAxis();
        DateAxis dateAxis = new DateAxis();
        tempChart = new LineChart<>(dateAxis, numberAxis);
        tempChart.setLegendVisible(false);
        contentPane.setCenter(tempChart);

        intervalCB.setItems(GraphInterval.INTERVALS);
        intervalCB.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (sensor != null) {
                sensor.setGraphInterval(newValue);
            }
        });
        intervalCB.getSelectionModel().select(1);

        offsetCB.setItems(OFFSET_VALUES);
        offsetCB.getSelectionModel().select(6);

    }

    public void setSensor(TempSensor sensor) {
        this.sensor = sensor;

        tempChart.getData().add(sensor.getData());
        updateData();

        sensor.getUpdateCount().addListener((observable, oldValue, newValue) -> {
            updateData();
        });
        qvCB.setSelected(sensor.getQuickView().get());
        offsetCB.getSelectionModel().select(sensor.getTempOffset().get());
        sensor.getTempOffset().bind(offsetCB.getSelectionModel().selectedItemProperty());
    }

    public void updateData() {
        if (sensor == null) {
            return;
        }
        nameField.setText(sensor.getName());
        tempLabel.setText(sensor.getCurrentValueAsString());
        updateLabel.setText(sdf.format(sensor.getLastUpdate()));
        onRadio.setSelected(sensor.getOn());
        reachRadio.setSelected(sensor.getReachable());
        battProg.setProgress((double) sensor.getBattery() / 100d);
    }

    @FXML
    void onChangeName(ActionEvent event) {
        if (!isNameEdit) {
            nameField.setEditable(true);
            nameField.requestFocus();
            changeNameBut.setText("Save");
            isNameEdit = true;
        } else {
            try {
                // save
                hue.renameSensor(sensor.getId(), nameField.getText());
                hue.updateSensors();
                nameField.setEditable(false);
                changeNameBut.setText("Change");
                isNameEdit = false;
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    @FXML
    void onQuickView(ActionEvent event) {
        sensor.getQuickView().set(!sensor.getQuickView().get());
    }

}
