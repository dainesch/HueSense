package lu.dainesch.huesense.hue;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
    private static final String STMT_CREATE_ENTRY = "Insert into SENSOR_DATA (SENSOR_ID, CREATED, IS_ON, IS_REACHABLE, BATTERY_LEVEL)"
            + "values (?, ?, ?, ?, ?)";

    private final HueSenseConfig config;
    private final DBManager dbMan;

    private final Map<Integer, Sensor<?>> sensorMap = Collections.synchronizedMap(new HashMap<>());
    private final ObservableList<Sensor<?>> sensors = FXCollections.observableArrayList();

    public DataStore(HueSenseConfig config, DBManager dbMan) {
        this.config = config;
        this.dbMan = dbMan;
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
                boolean isNew = false;
                if (sensor == null) {
                    // create it
                    String uniqueID = sensObj.getString("uniqueid");
                    sensor = createSensor(id, uniqueID, type);
                    isNew = true;
                }

                if (sensor != null) {
                    try {
                        sensor.updateSensor(sensObj);
                        if (isNew) {
                            addSensor(sensor);
                        }

                        // database save
                        Long entryId = createSensorDataEntry(sensor);
                        if (entryId != null) {
                            sensor.saveCurrentValueInDB(entryId);
                        }
                    } catch (UpdateException ex) {
                        LOG.error("Error updating sensor data", ex);
                    }
                }

            }

        }
    }

    public void addSensor(Sensor<?> sensor) throws UpdateException {
        addOrUpdateSensorDB(sensor);

        sensorMap.put(sensor.getId(), sensor);
        Platform.runLater(() -> {
            sensors.add(sensor);
            FXCollections.sort(sensors);
            sensor.initSensor();
        });
    }

    private Sensor<?> createSensor(Integer id, String uniqueID, SensorType type) {

        switch (type) {
            case LIGHT:
                return new LightSensor(id, uniqueID, config, dbMan);

            case PRESENCE:
                return new PresenceSensor(id, uniqueID, config, dbMan);

            case TEMPERATURE:
                return new TempSensor(id, uniqueID, config, dbMan);

            default:
                LOG.error("Unknown sensor type: " + type);

        }
        return null;
    }

    private void addOrUpdateSensorDB(Sensor<?> sensor) throws UpdateException {
        String name = null;
        try (Connection conn = dbMan.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(Sensor.STMT_SEL_UID)) {
                stmt.setString(1, sensor.getUniqueID());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    name = rs.getString("SENSOR_NAME");
                    if (sensor.getDbId() == null) {
                        sensor.setDbId(rs.getLong("S_ID"));
                    }
                }
            }

            if (sensor.getDbId() == null) {
                // create new sensor
                try (PreparedStatement stmt = conn.prepareStatement(Sensor.STMT_CREATE, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, sensor.getName());
                    stmt.setString(2, sensor.getType().toString());
                    stmt.setString(3, sensor.getUniqueID());
                    stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    stmt.executeUpdate();
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        sensor.setDbId(rs.getLong(1));
                    }
                }
            } else if (!sensor.getName().equals(name)) {
                // update name
                try (PreparedStatement stmt = conn.prepareStatement(Sensor.STMT_UPD_NAME)) {
                    stmt.setString(1, sensor.getName());
                    stmt.setLong(2, sensor.getDbId());
                    stmt.executeUpdate();
                }
            }

        } catch (SQLException ex) {
            throw new UpdateException("Error updating Sensor DB values", ex);

        }
    }

    private Long createSensorDataEntry(Sensor<?> sensor) {
        if (sensor == null || sensor.getDbId() == null) {
            return null;
        }
        try (Connection conn = dbMan.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(STMT_CREATE_ENTRY, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, sensor.getDbId());
                stmt.setTimestamp(2, new Timestamp(sensor.getLastUpdate().getTime()));
                stmt.setBoolean(3, sensor.getOn());
                stmt.setBoolean(4, sensor.getReachable());
                stmt.setInt(5, sensor.getBattery());
                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException ex) {
            LOG.error("Error Creating data entry", ex);
        }
        return null;
    }

    public ObservableList<Sensor<?>> getSensors() {
        return sensors;
    }

    public Sensor<?> getSensor(int id) {
        return sensorMap.get(id);
    }

}
