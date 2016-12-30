package lu.dainesch.huesense.hue.data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.util.Duration;
import javax.json.JsonObject;
import lu.dainesch.huesense.Constants;
import lu.dainesch.huesense.HueSenseConfig;

public abstract class Sensor<A> implements Serializable, Comparable<Sensor<?>> {

    public static final long TIME_TO_KEEP = TimeUnit.DAYS.toMillis(7);

    protected final HueSenseConfig config;
    protected final int id;
    protected final SensorType type;

    protected String name;
    protected Date lastUpdate;
    protected A currentValue;
    protected Boolean on;
    protected Boolean reachable;
    protected int battery = 100;

    protected final NavigableSet<SensorValue<A>> values = Collections.synchronizedNavigableSet(new TreeSet<>());
    protected final IntegerProperty updateCount = new SimpleIntegerProperty(0);
    protected final BooleanProperty quickView = new SimpleBooleanProperty(false);

    public Sensor(int id, SensorType type, HueSenseConfig config) {
        this.id = id;
        this.type = type;
        this.config = config;

        quickView.set(config.getBoolean(Constants.QV_SENSOR + id));
        quickView.addListener((observable, oldValue, newValue) -> {
            config.putBoolean(Constants.QV_SENSOR + id, newValue);
        });

    }

    public abstract void updateSensor(JsonObject obj) throws UpdateException;

    public abstract void updateView(SensorValue<A> val);

    public abstract String getValueAsString(A value);

    public String getCurrentValueAsString() {
        A val = getCurrentValue();
        if (val != null) {
            return getValueAsString(val);
        }
        return "";
    }

    public synchronized void updateValue(SensorValue<A> val) {
        lastUpdate = val.getTime();
        currentValue = val.getValue();
        if (values.add(val)) {
            Platform.runLater(() -> updateView(val));
        }
        Platform.runLater(() -> updateCount.set(updateCount.get() + 1));
        long timeout = System.currentTimeMillis() - TIME_TO_KEEP;
        values.removeIf(v -> v.getTime().getTime() < timeout);
    }

    public Set<SensorValue<A>> getValuesInRange(Date start, Date end) {
        if (end == null) {
            return values.tailSet(new SensorValue(start, null));
        } else {
            return values.subSet(new SensorValue(start, null), new SensorValue(end, null));
        }
    }

    public int getId() {
        return id;
    }

    public SensorType getType() {
        return type;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized Date getLastUpdate() {
        return lastUpdate;
    }

    public synchronized void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public synchronized A getCurrentValue() {
        return currentValue;
    }

    public synchronized void setCurrentValue(A currentValue) {
        this.currentValue = currentValue;
    }

    public synchronized Boolean getOn() {
        return on;
    }

    public synchronized void setOn(Boolean on) {
        this.on = on;
    }

    public synchronized Boolean getReachable() {
        return reachable;
    }

    public synchronized void setReachable(Boolean reachable) {
        this.reachable = reachable;
    }

    public synchronized int getBattery() {
        return battery;
    }

    public synchronized void setBattery(int battery) {
        this.battery = battery;
    }

    public IntegerProperty getUpdateCount() {
        return updateCount;
    }

    public BooleanProperty getQuickView() {
        return quickView;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Sensor<?> other = (Sensor<?>) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Sensor<?> o) {
        return id - o.id;
    }

    public static class SensorValue<A> implements Comparable<SensorValue<A>> {

        private final Date time;
        private final A value;

        public SensorValue(Date time, A value) {
            this.time = time;
            this.value = value;
        }

        public Date getTime() {
            return time;
        }

        public A getValue() {
            return value;
        }

        @Override
        public int compareTo(SensorValue<A> o) {
            return time.compareTo(o.time);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 73 * hash + Objects.hashCode(this.time);
            hash = 73 * hash + Objects.hashCode(this.value);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SensorValue<?> other = (SensorValue<?>) obj;
            if (!Objects.equals(this.time, other.time)) {
                return false;
            }
            if (!Objects.equals(this.value, other.value)) {
                return false;
            }
            return true;
        }

    }

}
