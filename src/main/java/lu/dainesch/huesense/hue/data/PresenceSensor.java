package lu.dainesch.huesense.hue.data;

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
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;
import javax.json.JsonObject;
import lu.dainesch.huesense.Constants;
import lu.dainesch.huesense.HueSenseConfig;
import lu.dainesch.huesense.hue.DBManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PresenceSensor extends Sensor<Boolean> {

    private static final String INSERT_CUR_VAL = "Insert into PRESENCE_DATA (SD_ID, PRESENCE) values (?, ?)";
    private static final String SELECT_RANGE = "Select p.SD_ID, p.PRESENCE, d.CREATED from PRESENCE_DATA p "
            + "JOIN SENSOR_DATA d on d.SD_ID=p.SD_ID "
            + "where d.SENSOR_ID = ? and d.CREATED > ? and d.CREATED <= ? "
            + "order by d.created asc ";

    private static final String DETECT = "Motion detected";
    private static final String NOTHING = "No motion";
    private static final Logger LOG = LoggerFactory.getLogger(PresenceSensor.class);

    private final ObservableList<PresenceEntry> data;
    private final ObjectProperty<GraphInterval> graphInterval;
    private PresenceEntry lastEntry = null;
    private boolean currentDetect = false;

    public PresenceSensor(int id, String uniqueID, HueSenseConfig config, DBManager dbMan) {
        super(id, uniqueID, SensorType.PRESENCE, config, dbMan);
        data = FXCollections.observableArrayList();
        graphInterval = new SimpleObjectProperty<>(GraphInterval.INTERVALS.get(1));
    }

    @Override
    public void initSensor() {
        // load data
        setGraphInterval(GraphInterval.INTERVALS.get(1));
    }

    @Override
    public void updateSensor(JsonObject obj) throws UpdateException {
        try {
            setName(obj.getString("name"));
            JsonObject state = obj.getJsonObject("state");

            Boolean pres = state.getBoolean("presence");

            SimpleDateFormat sdf = new SimpleDateFormat(Constants.HUEDATEFORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date time = sdf.parse(state.getString("lastupdated"));
            SensorValue<Boolean> val = new SensorValue<>(time, pres);
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
                stmt.setBoolean(2, currentValue);
                stmt.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new UpdateException("Error updating presence data", ex);
        }
    }

    @Override
    public Set<SensorValue<Boolean>> getValuesInRange(Date start, Date end) {
        NavigableSet<SensorValue<Boolean>> ret = new TreeSet<>();

        try (Connection conn = dbMan.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(SELECT_RANGE)) {
                stmt.setLong(1, dbId);
                stmt.setTimestamp(2, new Timestamp(start.getTime()));
                long endTime = end == null ? System.currentTimeMillis() : end.getTime();
                stmt.setTimestamp(3, new Timestamp(endTime));
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    SensorValue<Boolean> val = new SensorValue<>(rs.getTimestamp("CREATED"), rs.getBoolean("PRESENCE"));
                    ret.add(val);
                }
            }
        } catch (SQLException ex) {
            LOG.error("Error querying light values", ex);
        }
        return ret;
    }

    @Override
    public void updateView(SensorValue<Boolean> val) {
        Duration maxAge = graphInterval.get().getDuration();
        while (!data.isEmpty() && (System.currentTimeMillis() - data.get(data.size() - 1).getStart().getTime() > maxAge.toMillis())) {
            // remove old values
            data.remove(data.size() - 1);
        }
        if (val == null || System.currentTimeMillis() - val.getTime().getTime() > maxAge.toMillis()) {
            // do not add old data
            return;
        }

        boolean detect = val.getValue();

        if (lastEntry == null) {
            if (detect) {
                // first entry
                lastEntry = new PresenceEntry(val.getTime());
                data.add(0, lastEntry);
                currentDetect = true;
            } else {
                // nothing
                currentDetect = false;
            }
            return;
        }

        if (detect) {
            if (!currentDetect) {
                // new detection
                lastEntry = new PresenceEntry(val.getTime());
                data.add(0, lastEntry);
            }
            currentDetect = true;
        } else {
            if (currentDetect && lastEntry != null) {
                // end detection
                lastEntry.setEnd(val.getTime());
            }
            currentDetect = false;
        }

    }

    public ObservableList<PresenceEntry> getData() {
        return data;
    }

    public void setGraphInterval(GraphInterval val) {

        Date start = new Date(System.currentTimeMillis() - (long) val.getDuration().toMillis());
        data.removeAll(data);   // clear does not work
        graphInterval.set(val);
        getValuesInRange(start, null).forEach((s) -> {
            updateView(s);
        });

    }

    @Override
    public String getValueAsString(Boolean value) {
        if (value) {
            return DETECT;
        }
        return NOTHING;
    }

    public static class PresenceEntry {

        private final SimpleStringProperty date = new SimpleStringProperty();
        private final SimpleStringProperty startTime = new SimpleStringProperty();
        private final SimpleStringProperty endTime = new SimpleStringProperty(null);
        private final SimpleStringProperty duration = new SimpleStringProperty(null);

        private final Date start;

        public PresenceEntry(Date start) {
            this.start = start;
            date.set(new SimpleDateFormat(Constants.DATEFORMAT).format(start));
            startTime.set(new SimpleDateFormat(Constants.TIMEFORMAT).format(start));

        }

        public void setEnd(Date end) {
            endTime.set(new SimpleDateFormat(Constants.TIMEFORMAT).format(end));
            Duration d = Duration.millis(end.getTime() - start.getTime());
            duration.set(((int) d.toSeconds()) + " seconds");
        }

        public Date getStart() {
            return start;
        }

        public String getDate() {
            return date.get();
        }

        public void setDate(String date) {
            this.date.set(date);
        }

        public String getStartTime() {
            return startTime.get();
        }

        public void setStartTime(String startTime) {
            this.startTime.set(startTime);
        }

        public String getEndTime() {
            return endTime.get();
        }

        public void setEndTime(String endTime) {
            this.endTime.set(endTime);
        }

        public String getDuration() {
            return duration.get();
        }

        public void setDuration(String duration) {
            this.duration.set(duration);
        }

        public SimpleStringProperty dateProperty() {
            return date;
        }

        public SimpleStringProperty startTimeProperty() {
            return startTime;
        }

        public SimpleStringProperty endTimeProperty() {
            return endTime;
        }

        public SimpleStringProperty durationProperty() {
            return duration;
        }

    }

}
