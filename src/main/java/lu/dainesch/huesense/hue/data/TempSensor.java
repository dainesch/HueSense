package lu.dainesch.huesense.hue.data;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.XYChart;
import javafx.util.Duration;
import javax.json.JsonObject;
import lu.dainesch.huesense.Constants;
import lu.dainesch.huesense.HueSenseConfig;
import lu.dainesch.huesense.hue.DBManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempSensor extends Sensor<BigDecimal> {

    private static final String INSERT_CUR_VAL = "Insert into TEMP_DATA (SD_ID, TEMPERATURE) values (?, ?)";
    private static final String SELECT_RANGE = "Select t.SD_ID, t.TEMPERATURE, d.CREATED from TEMP_DATA t "
            + "JOIN SENSOR_DATA d on d.SD_ID=t.SD_ID "
            + "where d.SENSOR_ID = ? and d.CREATED > ? and d.CREATED <= ? "
            + "order by d.created asc ";
    public static final String TEMP_SUFFIX = " °C";
    private static final Logger LOG = LoggerFactory.getLogger(TempSensor.class);

    private final XYChart.Series<Date, Number> data;
    private final ObjectProperty<GraphInterval> graphInterval;
    private final ObjectProperty<BigDecimal> tempOffset;

    public TempSensor(int id, String uniqueID, HueSenseConfig config, DBManager dbMan) {
        super(id, uniqueID, SensorType.TEMPERATURE, config, dbMan);
        data = new XYChart.Series<>();
        data.setName("Temperature");
        graphInterval = new SimpleObjectProperty<>(GraphInterval.INTERVALS.get(1));
        tempOffset = new SimpleObjectProperty<>(BigDecimal.ZERO);
        if (config.getString(Constants.TEMP_OFFSET_SENSOR + id) != null) {
            tempOffset.set(new BigDecimal(config.getString(Constants.TEMP_OFFSET_SENSOR + id)));
        }
        tempOffset.addListener((observable, oldValue, newValue) -> {
            changeTempOffset();
        });
    }

    @Override
    public void updateSensor(JsonObject obj) throws UpdateException {
        try {
            setName(obj.getString("name"));

            JsonObject state = obj.getJsonObject("state");

            int t = state.getInt("temperature");
            BigDecimal temp = new BigDecimal(t).movePointLeft(2);

            SimpleDateFormat sdf = new SimpleDateFormat(Constants.HUEDATEFORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date time = sdf.parse(state.getString("lastupdated"));

            SensorValue<BigDecimal> val = new SensorValue<>(time, temp);
            updateValue(val);

            JsonObject conf = obj.getJsonObject("config");
            setOn(conf.getBoolean("on"));
            setBattery(conf.getInt("battery"));
            setReachable(conf.getBoolean("reachable"));

        } catch (ParseException | NullPointerException | ClassCastException ex) {
            throw new UpdateException("Error updating temp sensor", ex);
        }
    }

    @Override
    public synchronized BigDecimal getCurrentValue() {
        return super.getCurrentValue().add(tempOffset.get());
    }

    @Override
    public void saveCurrentValueInDB(Long dataId) throws UpdateException {
        try (Connection conn = dbMan.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_CUR_VAL)) {
                stmt.setLong(1, dataId);
                stmt.setBigDecimal(2, currentValue);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new UpdateException("Error updating presence data", ex);
        }
    }

    @Override
    public Set<SensorValue<BigDecimal>> getValuesInRange(Date start, Date end) {
        NavigableSet<SensorValue<BigDecimal>> ret = new TreeSet<>();

        try (Connection conn = dbMan.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_RANGE)) {
                stmt.setLong(1, dbId);
                stmt.setTimestamp(2, new Timestamp(start.getTime()));
                long endTime = end == null ? System.currentTimeMillis() : end.getTime();
                stmt.setTimestamp(3, new Timestamp(endTime));
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    SensorValue<BigDecimal> val = new SensorValue<>(rs.getTimestamp("CREATED"), rs.getBigDecimal("TEMPERATURE"));
                    ret.add(val);
                }
            }
        } catch (SQLException ex) {
            LOG.error("Error querying temp values", ex);
        }
        return ret;
    }

    @Override
    public void updateView(SensorValue<BigDecimal> val) {
        Duration maxAge = graphInterval.get().getDuration();
        while (!data.getData().isEmpty() && (System.currentTimeMillis() - data.getData().get(0).getXValue().getTime() > maxAge.toMillis())) {
            // remove old values
            data.getData().remove(0);
        }
        if (val == null || System.currentTimeMillis() - val.getTime().getTime() > maxAge.toMillis()) {
            // do not add old data
            return;
        }

        data.getData().add(new XYChart.Data<>(val.getTime(), val.getValue().add(tempOffset.get())));
    }

    public XYChart.Series<Date, Number> getData() {
        return data;
    }

    @Override
    public String getValueAsString(BigDecimal value) {
        return value.add(tempOffset.get()).toPlainString() + TEMP_SUFFIX;
    }

    public void setGraphInterval(GraphInterval val) {
        if (graphInterval.get().getDuration().lessThan(val.getDuration())) {
            // longer interval
            Date start = new Date(System.currentTimeMillis() - (long) val.getDuration().toMillis());
            data.getData().removeAll(data.getData());   // clear does not work
            graphInterval.set(val);
            getValuesInRange(start, null).forEach((s) -> {
                updateView(s);
            });
        } else {
            graphInterval.set(val);
            updateView(null);
        }
    }

    private void changeTempOffset() {
        // redraw
        Date start = new Date(System.currentTimeMillis() - (long) graphInterval.get().getDuration().toMillis());
        data.getData().removeAll(data.getData());   // clear does not work
        getValuesInRange(start, null).forEach((s) -> {
            updateView(s);
        });
        // trigger change, not elegant, but meh
        updateCount.set(updateCount.get() - 1);
        updateCount.set(updateCount.get() + 1);
        config.putString(Constants.TEMP_OFFSET_SENSOR + id, tempOffset.get().toPlainString());
    }

    public ObjectProperty<BigDecimal> getTempOffset() {
        return tempOffset;
    }

}
