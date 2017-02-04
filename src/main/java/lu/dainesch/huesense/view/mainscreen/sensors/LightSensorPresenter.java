package lu.dainesch.huesense.view.mainscreen.sensors;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
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
import lu.dainesch.huesense.hue.data.GraphInterval;
import lu.dainesch.huesense.hue.data.LightSensor;
import lu.dainesch.huesense.view.DateAxis;

public class LightSensorPresenter implements Initializable {

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
    private Label lightLabel;
    @FXML
    private BorderPane contentPane;
    @FXML
    private ChoiceBox<GraphInterval> intervalCB;
    @FXML
    private Button changeNameBut;
    @FXML
    private CheckBox qvCB;

    @Inject
    private HueComm hue;

    private LineChart<Date, Number> lightChart;
    private LightSensor sensor;
    private SimpleDateFormat sdf;
    private boolean isNameEdit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sdf = new SimpleDateFormat(Constants.TIMEFORMAT);

        NumberAxis numberAxis = new NumberAxis();
        DateAxis dateAxis = new DateAxis();
        lightChart = new LineChart<>(dateAxis, numberAxis);
        lightChart.setLegendVisible(false);
        lightChart.setCreateSymbols(false);
        contentPane.setCenter(lightChart);

        intervalCB.setItems(GraphInterval.INTERVALS);
        intervalCB.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (sensor != null) {
                sensor.setGraphInterval(newValue);
            }
        });
        intervalCB.getSelectionModel().select(1);

    }

    public void setSensor(LightSensor sensor) {
        this.sensor = sensor;

        lightChart.getData().add(sensor.getData());
        updateData();

        sensor.getUpdateCount().addListener((observable, oldValue, newValue) -> {
            updateData();
        });
        qvCB.setSelected(sensor.getQuickView().get());
    }

    public void updateData() {
        if (sensor == null) {
            return;
        }
        nameField.setText(sensor.getName());
        lightLabel.setText(sensor.getCurrentValueAsString());
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
