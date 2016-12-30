package lu.dainesch.huesense.hue.data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.XYChart;
import javafx.util.Duration;
import javax.json.JsonObject;
import lu.dainesch.huesense.Constants;
import lu.dainesch.huesense.HueSenseConfig;

public class LightSensor extends Sensor<LightSensor.LightLevel> {

    public static final String LUX_SUFFIX = " lux";

    private final XYChart.Series<Date, Number> data;
    private final ObjectProperty<GraphInterval> graphInterval;

    public LightSensor(int id, HueSenseConfig config) {
        super(id, SensorType.LIGHT, config);
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

            JsonObject config = obj.getJsonObject("config");
            setOn(config.getBoolean("on"));
            setBattery(config.getInt("battery"));
            setReachable(config.getBoolean("reachable"));

        } catch (ParseException | NullPointerException | ClassCastException ex) {
            throw new UpdateException("Error updating temp sensor", ex);
        }
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
