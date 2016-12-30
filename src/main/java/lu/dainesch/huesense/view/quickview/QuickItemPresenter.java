package lu.dainesch.huesense.view.quickview;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableObjectValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import lu.dainesch.huesense.hue.data.Sensor;

public class QuickItemPresenter implements Initializable {

    @FXML
    private VBox vbox;
    @FXML
    private Label valueLabel;
    @FXML
    private Label nameLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        vbox.setBackground(null);
    }

    public void setItem(Sensor<?> sensor, ObservableObjectValue<Color> color) {
        color.addListener((observable, oldValue, newValue) -> {
            valueLabel.setTextFill(newValue);
            nameLabel.setTextFill(newValue);
        });
        sensor.getUpdateCount().addListener((observable, oldValue, newValue) -> {
            valueLabel.setText(sensor.getCurrentValueAsString());
            nameLabel.setText(sensor.getName());
        });
        valueLabel.setText(sensor.getCurrentValueAsString());
        nameLabel.setText(sensor.getName());
        valueLabel.setTextFill(color.get());
        nameLabel.setTextFill(color.get());
    }

}
