package lu.dainesch.huesense.view.mainscreen;

import lu.dainesch.huesense.view.mainscreen.list.SensorListCell;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javax.inject.Inject;
import lu.dainesch.huesense.Constants;
import lu.dainesch.huesense.HueSenseConfig;
import lu.dainesch.huesense.hue.DataStore;
import lu.dainesch.huesense.hue.HueComm;
import lu.dainesch.huesense.hue.data.LightSensor;
import lu.dainesch.huesense.hue.data.PingSensor;
import lu.dainesch.huesense.hue.data.PresenceSensor;
import lu.dainesch.huesense.hue.data.Sensor;
import lu.dainesch.huesense.hue.data.TempSensor;
import lu.dainesch.huesense.view.UIUtils;
import lu.dainesch.huesense.view.about.AboutView;
import lu.dainesch.huesense.view.alarm.AlarmConfigView;
import lu.dainesch.huesense.view.mainscreen.sensors.LightSensorPresenter;
import lu.dainesch.huesense.view.mainscreen.sensors.LightSensorView;
import lu.dainesch.huesense.view.mainscreen.sensors.PingSensorPresenter;
import lu.dainesch.huesense.view.mainscreen.sensors.PingSensorView;
import lu.dainesch.huesense.view.mainscreen.sensors.PresenceSensorPresenter;
import lu.dainesch.huesense.view.mainscreen.sensors.PresenceSensorView;
import lu.dainesch.huesense.view.mainscreen.sensors.TempSensorPresenter;
import lu.dainesch.huesense.view.mainscreen.sensors.TempSensorView;
import lu.dainesch.huesense.view.quickview.QuickPresenter;
import lu.dainesch.huesense.view.quickview.QuickView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainPresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(MainPresenter.class);

    @FXML
    private BorderPane mainPane;
    @FXML
    private Label updLabel;
    @FXML
    private ListView<Sensor<?>> sensorList;
    @FXML
    private ChoiceBox<PollInterval> updCB;
    @FXML
    private ToggleButton hideBut;
    @FXML
    private ToggleButton quickBut;
    @FXML
    private ColorPicker colorPicker;
    @FXML
    private ImageView logoImg;
    @FXML
    private ToggleButton alarmBut;
    @FXML
    private Button alarmSettBut;
    @FXML
    private Button aboutBut;

    @Inject
    private HueComm hue;
    @Inject
    private DataStore store;
    @Inject
    private HueSenseConfig config;

    private Stage stage;
    private TrayView trayView;
    private QuickPresenter quickPres;
    private SimpleDateFormat sdf;
    private Timeline pollTimeLine;
    private Map<Integer, Node> sensorViews;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        trayView = new TrayView();
        sensorViews = new HashMap<>();
        updLabel.setText("Never");
        logoImg.setImage(UIUtils.getImage("logo.png"));
        sdf = new SimpleDateFormat(Constants.TIMEFORMAT);

        sensorList.setCellFactory((param) -> {
            return new SensorListCell();
        });
        sensorList.setItems(store.getSensors());
        sensorList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            showSensor(newValue);
        });
        store.getSensors().addListener((ListChangeListener.Change<? extends Sensor<?>> c) -> {
            if (sensorList.getSelectionModel().getSelectedItem() == null) {
                sensorList.getSelectionModel().selectFirst();
            }
        });

        hideBut.setGraphic(new ImageView(UIUtils.getImage("hide.png")));
        quickBut.setGraphic(new ImageView(UIUtils.getImage("quickview.png")));
        alarmBut.setGraphic(new ImageView(UIUtils.getImage("alarm.png")));
        alarmSettBut.setGraphic(new ImageView(UIUtils.getImage("alarm_sett.png")));
        aboutBut.setGraphic(new ImageView(UIUtils.getImage("about.png")));
        hideBut.setSelected(config.getBoolean(Constants.HIDE_ON_CLOSE));
        quickBut.setSelected(config.getBoolean(Constants.SHOW_QV));
        onQuickBut(null);

        hideBut.setTooltip(new Tooltip("Minimizes the application to the tray instead of closing it"));
        quickBut.setTooltip(new Tooltip("Display a minimal view with current sensor data"));

        colorPicker.setTooltip(new Tooltip("Color of the QuickView text"));

        updCB.setItems(PollInterval.INTERVALS);
        updCB.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            setPollInterval(newValue);
        });
        updCB.getSelectionModel().select(1);
        onRefresh(null);
        updCB.setTooltip(new Tooltip("Update interval of the sensor data"));

        stage = new Stage();
        stage.setTitle("HueSense");
        stage.setOnCloseRequest((ev) -> {
            if (hideBut.isSelected() && trayView.isSupported()) {
                trayView.showTrayIcon();
                ev.consume();
                stage.hide();
            } else {
                Platform.exit();
                System.exit(0);
            }
        });
        trayView.setOpenListener((observable, oldValue, newValue) -> {
            trayView.hideTrayIcon();
            stage.show();
        });
        trayView.setExitListener((observable, oldValue, newValue) -> {
            trayView.hideTrayIcon();
            Platform.exit();
            System.exit(0);
        });

        Scene scene = new Scene(mainPane);
        scene.setFill(Color.WHITE);
        final String uri = Thread.currentThread().getContextClassLoader().getResource("global.css").toExternalForm();
        scene.getStylesheets().add(uri);
        stage.setScene(scene);
        stage.centerOnScreen();
        UIUtils.setIcon(stage);
        stage.show();

    }

    private void showSensor(Sensor<?> sensor) {
        if (sensor==null) {
            return;
        }
        Node o = sensorViews.get(sensor.getId());
        if (o == null) {
            switch (sensor.getType()) {
                case LIGHT:
                    LightSensorView lview = new LightSensorView();
                    o = lview.getView();
                    ((LightSensorPresenter) lview.getPresenter()).setSensor((LightSensor) sensor);
                    sensorViews.put(sensor.getId(), o);
                    break;
                case PRESENCE:
                    PresenceSensorView pview = new PresenceSensorView();
                    o = pview.getView();
                    ((PresenceSensorPresenter) pview.getPresenter()).setSensor((PresenceSensor) sensor);
                    sensorViews.put(sensor.getId(), o);
                    break;
                case TEMPERATURE:
                    TempSensorView tview = new TempSensorView();
                    o = tview.getView();
                    ((TempSensorPresenter) tview.getPresenter()).setSensor((TempSensor) sensor);
                    sensorViews.put(sensor.getId(), o);
                    break;
                case PING:
                    PingSensorView dview = new PingSensorView();
                    o = dview.getView();
                    ((PingSensorPresenter) dview.getPresenter()).setSensor((PingSensor) sensor);
                    sensorViews.put(sensor.getId(), o);
                    break;
                default:
                    LOG.error("Unknown sensor type: " + sensor.getType());
                    return;
            }
        }
        if (o != null) {
            mainPane.setCenter(o);
        }
    }

    private void setPollInterval(PollInterval in) {
        if (pollTimeLine != null) {
            pollTimeLine.stop();
        }
        pollTimeLine = new Timeline(new KeyFrame(
                in.getDuration(),
                ae -> onRefresh(null)));
        pollTimeLine.setCycleCount(Animation.INDEFINITE);
        pollTimeLine.play();
    }

    @FXML
    void onRefresh(ActionEvent event) {
        hue.updateSensors();
        updLabel.setText(sdf.format(new Date()));
    }

    @FXML
    void onHideBut(ActionEvent event) {
        config.putBoolean(Constants.HIDE_ON_CLOSE, hideBut.isSelected());
    }

    @FXML
    void onQuickBut(ActionEvent event) {
        config.putBoolean(Constants.SHOW_QV, quickBut.isSelected());
        if (quickBut.isSelected()) {
            if (quickPres == null) {
                QuickView qv = new QuickView();
                quickPres = (QuickPresenter) qv.getPresenter();
                colorPicker.valueProperty().set(quickPres.getColor().get());
                quickPres.getColor().bind(colorPicker.valueProperty());
            }

        }
        if (quickPres != null) {
            quickPres.setVisible(quickBut.isSelected());
        }
    }

    @FXML
    void onAbout(ActionEvent event) {
        AboutView about = new AboutView();
        about.getView();
    }

    @FXML
    void onAlarm(ActionEvent event) {

    }

    @FXML
    void onAlarmSett(ActionEvent event) {
        AlarmConfigView acv = new AlarmConfigView();
        acv.getView();
    }

}
