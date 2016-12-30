package lu.dainesch.huesense.hue;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.json.Json;
import javax.json.JsonObject;
import lu.dainesch.huesense.HueSenseConfig;
import lu.dainesch.huesense.hue.data.LightSensor;
import lu.dainesch.huesense.hue.data.PresenceSensor;
import lu.dainesch.huesense.hue.data.Sensor;
import lu.dainesch.huesense.hue.data.SensorType;
import lu.dainesch.huesense.hue.data.TempSensor;
import lu.dainesch.huesense.hue.data.UpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataStore {

    private static final Logger LOG = LoggerFactory.getLogger(DataStore.class);

    private final HueSenseConfig config;

    private final Map<Integer, Sensor<?>> sensorMap = Collections.synchronizedMap(new HashMap<>());
    private final ObservableList<Sensor<?>> sensors = FXCollections.observableArrayList();

    public DataStore(HueSenseConfig config) {
        this.config = config;
    }

    public void updateSensorData(String data) {
        JsonObject list = Json.createReader(new StringReader(data)).readObject();
        for (String sId : list.keySet()) {
            Integer id = Integer.parseInt(sId);

            JsonObject sensObj = list.getJsonObject(sId);
            String sType = sensObj.getString("type");
            SensorType type = SensorType.getByType(sType);

            if (type != null) {
                // only supported types

                Sensor<?> sensor = sensorMap.get(id);
                if (sensor == null) {
                    switch (type) {
                        case LIGHT:
                            sensor = new LightSensor(id, config);
                            break;
                        case PRESENCE:
                            sensor = new PresenceSensor(id, config);
                            break;
                        case TEMPERATURE:
                            sensor = new TempSensor(id, config);
                            break;
                        default:
                            LOG.error("Unknown sensor type: " + type);
                            continue;
                    }
                    addSensor(sensor);

                }

                if (sensor != null) {   // not needed, but warning ...
                    try {
                        sensor.updateSensor(sensObj);
                    } catch (UpdateException ex) {
                        LOG.error("Error reading sensor data", ex);
                    }
                }
            }

        }
    }

    public void addSensor(Sensor<?> sensor) {
        sensorMap.put(sensor.getId(), sensor);
        Platform.runLater(() -> {
            sensors.add(sensor);
            FXCollections.sort(sensors);
        });
    }

    public ObservableList<Sensor<?>> getSensors() {
        return sensors;
    }

    public Sensor<?> getSensor(int id) {
        return sensorMap.get(id);
    }

}
