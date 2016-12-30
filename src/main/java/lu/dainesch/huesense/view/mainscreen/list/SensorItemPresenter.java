package lu.dainesch.huesense.view.mainscreen.list;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lu.dainesch.huesense.hue.data.Sensor;
import lu.dainesch.huesense.view.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SensorItemPresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(SensorItemPresenter.class);

    @FXML
    private ImageView iconView;
    @FXML
    private Label nameLabel;
    @FXML
    private Label valueLabel;

    private Sensor<?> sensor;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public void setSensor(Sensor<?> sensor) {
        this.sensor = sensor;
        switch (sensor.getType()) {
            case LIGHT:
                iconView.setImage(UIUtils.getImage("light.png"));
                break;
            case PRESENCE:
                iconView.setImage(UIUtils.getImage("motion.png"));
                break;
            case TEMPERATURE:
                iconView.setImage(UIUtils.getImage("temp.png"));
                break;
            case PING:
                iconView.setImage(UIUtils.getImage("ping.png"));
                break;
            default:
                LOG.error("Unknown sensor type: " + sensor.getType());
        }
        sensor.getUpdateCount().addListener((observable, oldValue, newValue) -> {
            update();
        });
        update();
    }

    public void update() {
        if (sensor == null) {
            return;
        }
        nameLabel.setText(sensor.getName());
        valueLabel.setText(sensor.getCurrentValueAsString());

    }

}
