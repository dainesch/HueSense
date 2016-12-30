package lu.dainesch.huesense.view.quickview;

import java.awt.Point;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import lu.dainesch.huesense.Constants;
import lu.dainesch.huesense.HueSenseConfig;
import lu.dainesch.huesense.hue.DataStore;
import lu.dainesch.huesense.hue.data.Sensor;

public class QuickPresenter implements Initializable {

    @FXML
    private VBox vBox;
    @FXML
    private Rectangle moveRect;

    @Inject
    private DataStore store;
    @Inject
    private HueSenseConfig config;

    private InvisibleFrame frame;
    private double xOffset = 0;
    private double yOffset = 0;

    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.WHITE);
    private final Map<Integer, Node> sensors = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        frame = new InvisibleFrame();
        vBox.setBackground(null);
        Scene scene = new Scene(vBox);
        scene.setFill(Color.TRANSPARENT);
        frame.setScene(scene);

        store.getSensors().forEach((s) -> addSensor(s));

        store.getSensors().addListener((ListChangeListener.Change<? extends Sensor<?>> c) -> {
            while (c.next()) {
                c.getAddedSubList().forEach((s) -> {
                    addSensor(s);
                });
                c.getRemoved().forEach((s) -> {
                    vBox.getChildren().remove(sensors.get(s.getId()));
                    sensors.remove(s.getId());
                });
            }
        });

        color.addListener((observable, oldValue, newValue) -> {
            moveRect.setFill(newValue);
            JsonObjectBuilder o = Json.createObjectBuilder();
            o.add("r", newValue.getRed());
            o.add("g", newValue.getGreen());
            o.add("b", newValue.getBlue());
            o.add("a", newValue.getOpacity());
            config.putString(Constants.QV_COLOR, o.build().toString());

        });
        String col = config.getString(Constants.QV_COLOR);
        if (col != null && !col.isEmpty()) {
            JsonObject o = Json.createReader(new StringReader(col)).readObject();
            color.set(new Color(o.getJsonNumber("r").doubleValue(),
                    o.getJsonNumber("g").doubleValue(),
                    o.getJsonNumber("b").doubleValue(),
                    o.getJsonNumber("a").doubleValue()));
        }

        if (config.getInt(Constants.QV_POS_X) > 0 && config.getInt(Constants.QV_POS_Y) > 0) {
            frame.setLocation(config.getInt(Constants.QV_POS_X), config.getInt(Constants.QV_POS_Y));
        }

    }

    private void addSensor(Sensor<?> sensor) {
        if (!sensors.containsKey(sensor.getId())) {
            QuickItemView qiv = new QuickItemView();
            QuickItemPresenter qip = (QuickItemPresenter) qiv.getPresenter();
            qip.setItem(sensor, color);
            Node v = qiv.getView();
            sensors.put(sensor.getId(), v);
            if (sensor.getQuickView().get()) {
                vBox.getChildren().add(v);
            }

        }
        sensor.getQuickView().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                vBox.getChildren().add(sensors.get(sensor.getId()));
            } else {
                vBox.getChildren().remove(sensors.get(sensor.getId()));
            }
        });

    }

    public void setVisible(boolean visible) {
        frame.setVisible(visible);
    }

    @FXML
    void moveMouseDragged(MouseEvent event) {
        frame.setLocation((int) (event.getScreenX() - xOffset), (int) (event.getScreenY() - yOffset));

    }

    @FXML
    void moveMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    void moveMouseReleased(MouseEvent event) {
        Point p = frame.getLocation();
        config.putInt(Constants.QV_POS_X, p.x);
        config.putInt(Constants.QV_POS_Y, p.y);
    }

    public ObjectProperty<Color> getColor() {
        return color;
    }

    private static final class InvisibleFrame extends JFrame {

        private final JFXPanel fxPanel;

        public InvisibleFrame() {
            setType(Type.UTILITY);
            setUndecorated(true);
            setAlwaysOnTop(true);

            setBackground(new java.awt.Color(0, 0, 0, 0));
            setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

            setLocationRelativeTo(null);
            setSize(200, 1000);

            fxPanel = new JFXPanel();
            getContentPane().add(fxPanel);

        }

        public void setScene(Scene scene) {
            fxPanel.setScene(scene);
        }

        @Override
        public void setLocation(int x, int y) {
            SwingUtilities.invokeLater(() -> {
                super.setLocation(x, y);
            });

        }

        @Override
        public void setVisible(boolean visible) {
            SwingUtilities.invokeLater(() -> {
                super.setVisible(visible);
            });
        }

    }

}
