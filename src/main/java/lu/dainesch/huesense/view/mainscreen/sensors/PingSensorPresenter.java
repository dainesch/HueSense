package lu.dainesch.huesense.view.mainscreen.sensors;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import lu.dainesch.huesense.Constants;
import lu.dainesch.huesense.hue.data.GraphInterval;
import lu.dainesch.huesense.hue.data.PingSensor;
import lu.dainesch.huesense.view.DateAxis;

public class PingSensorPresenter implements Initializable {

    @FXML
    private Label updateLabel;
    @FXML
    private Label pingLabel;
    @FXML
    private BorderPane contentPane;
    @FXML
    private ChoiceBox<GraphInterval> intervalCB;

    @FXML
    private CheckBox qvCB;

    private LineChart<Date, Number> lightChart;
    private PingSensor sensor;
    private SimpleDateFormat sdf;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sdf = new SimpleDateFormat(Constants.TIMEFORMAT);

        NumberAxis numberAxis = new NumberAxis();
        DateAxis dateAxis = new DateAxis();
        lightChart = new LineChart<>(dateAxis, numberAxis);
        contentPane.setCenter(lightChart);

        intervalCB.setItems(GraphInterval.INTERVALS);
        intervalCB.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (sensor != null) {
                sensor.setGraphInterval(newValue);
            }
        });
        intervalCB.getSelectionModel().select(1);

    }

    public void setSensor(PingSensor sensor) {
        this.sensor = sensor;

        lightChart.setData(sensor.getData());
        updateData();

        sensor.getUpdateCount().addListener((observable, oldValue, newValue) -> {
            updateData();
        });
        qvCB.setSelected(sensor.getQuickView().get());
    }

    public void updateData() {
        if (sensor == null || sensor.getLastUpdate() == null) {
            return;
        }
        pingLabel.setText(sensor.getCurrentValueAsString());
        updateLabel.setText(sdf.format(sensor.getLastUpdate()));
    }

    @FXML
    void onQuickView(ActionEvent event) {
        sensor.getQuickView().set(!sensor.getQuickView().get());

    }

}
