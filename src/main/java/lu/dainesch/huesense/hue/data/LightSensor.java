package lu.dainesch.huesense.hue.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

public class LightSensor extends Sensor<LightSensor.LightLevel> {

    private static final String INSERT_CUR_VAL = "Insert into LIGHT_DATA (SD_ID, LIGHT_LEVEL, IS_DARK, IS_DAYLIGHT) values (?, ?, ?, ?)";
    private static final String SELECT_RANGE = "Select l.SD_ID, l.LIGHT_LEVEL, l.IS_DARK, l.IS_DAYLIGHT, d.CREATED from LIGHT_DATA l "
            + "JOIN SENSOR_DATA d on d.SD_ID=l.SD_ID "
            + "where d.SENSOR_ID = ? and d.CREATED > ? and d.CREATED <= ? "
            + "order by d.created asc ";
    public static final String LUX_SUFFIX = " lux";
    private static final Logger LOG = LoggerFactory.getLogger(LightSensor.class);

    private final XYChart.Series<Date, Number> data;
    private final ObjectProperty<GraphInterval> graphInterval;

    public LightSensor(int id, String uniqueID, HueSenseConfig config, DBManager dbMan) {
        super(id, uniqueID, SensorType.LIGHT, config, dbMan);
        data = new XYChart.Series<>();
        data.setName("LightLevel");
        graphInterval = new SimpleObjectProperty<>(GraphInterval.INTERVALS.get(1));
    }

    @Override
    public void updateSensor(JsonObject obj) throws UpdateException {
        try {
            setName(obj.getString("name"));

            JsonObject state = obj.getJsonObject("state");

            SimpleDateFormat sdf = new SimpleDateFormat(Constants.HUEDATEFORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date time = sdf.parse(state.getString("lastupdated"));

            LightLevel level = new LightLevel(state.getInt("lightlevel"), state.getBoolean("dark"), state.getBoolean("daylight"));
            SensorValue<LightLevel> val = new SensorValue<>(time, level);
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
    public void saveCurrentValueInDB(Long dataId) throws UpdateException {
        try (Connection conn = dbMan.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_CUR_VAL)) {
                stmt.setLong(1, dataId);
                stmt.setInt(2, currentValue.getLevel());
                stmt.setBoolean(3, currentValue.isDark());
                stmt.setBoolean(4, currentValue.isDayLight());
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new UpdateException("Error updating presence data", ex);
        }
    }

    @Override
    public Set<SensorValue<LightLevel>> getValuesInRange(Date start, Date end) {
        NavigableSet<SensorValue<LightLevel>> ret = new TreeSet<>();

        try (Connection conn = dbMan.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_RANGE)) {
                stmt.setLong(1, dbId);
                stmt.setTimestamp(2, new Timestamp(start.getTime()));
                long endTime = end == null ? System.currentTimeMillis() : end.getTime();
                stmt.setTimestamp(3, new Timestamp(endTime));
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    LightLevel level = new LightLevel(rs.getInt("LIGHT_LEVEL"), rs.getBoolean("IS_DARK"), rs.getBoolean("IS_DAYLIGHT"));
                    SensorValue<LightLevel> val = new SensorValue<>(rs.getTimestamp("CREATED"), level);
                    ret.add(val);
                }
            }
        } catch (SQLException ex) {
            LOG.error("Error querying light values", ex);
        }
        return ret;
    }

    @Override
    public void updateView(SensorValue<LightLevel> val) {
        Duration maxAge = graphInterval.get().getDuration();
        while (!data.getData().isEmpty() && (System.currentTimeMillis() - data.getData().get(0).getXValue().getTime() > maxAge.toMillis())) {
            // remove old values
            data.getData().remove(0);
        }
        if (val == null || System.currentTimeMillis() - val.getTime().getTime() > maxAge.toMillis()) {
            // do not add old data
            return;
        }
        data.getData().add(new XYChart.Data<>(val.getTime(), toLux(val.getValue().getLevel())));
    }

    public XYChart.Series<Date, Number> getData() {
        return data;
    }

    @Override
    public String getValueAsString(LightLevel value) {
        return toLux(value.level) + LUX_SUFFIX;
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

    private static BigDecimal toLux(int input) {
        // linear interpolation from table in api docs
        double val = (double) input;
        double ret = 0;
        if (val > 1 && val <= 3000) {
            ret = val / 2999d + 2998d / 2999d;
        } else if (val > 3000 && val <= 10000) {
            ret = val / 875d - 10d / 7d;
        } else if (val > 10000 && val <= 17000) {
            ret = val / 175d - 330d / 7d;
        } else if (val > 17000 && val <= 22000) {
            ret = val / 50d - 290d;
        } else if (val > 22000 && val <= 25500) {
            ret = 2d * val / 35d - 7750d / 7d;
        } else if (val > 25500 && val <= 28500) {
            ret = 7d * val / 60d - 2625d;
        } else if (val > 28500 && val <= 33000) {
            ret = 13d * val / 45d - 22600d / 3d;
        } else if (val > 33000 && val <= 40000) {
            ret = 8d * val / 7d - 250000d / 7d;
        } else if (val > 40000) {
            ret = 10d * val - 390000d;
        }
        return new BigDecimal(ret).setScale(2, RoundingMode.HALF_EVEN);
    }

    public static class LightLevel implements Comparable<LightLevel> {

        private final int level;
        private final boolean dark;
        private final boolean dayLight;

        public LightLevel(int level, boolean dark, boolean dayLight) {
            this.level = level;
            this.dark = dark;
            this.dayLight = dayLight;
        }

        public int getLevel() {
            return level;
        }

        public boolean isDark() {
            return dark;
        }

        public boolean isDayLight() {
            return dayLight;
        }

        @Override
        public int compareTo(LightLevel o) {
            return level - o.level;
        }

    }

}
