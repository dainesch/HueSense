package lu.dainesch.huesense.hue.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.util.Duration;
import javax.json.JsonObject;
import lu.dainesch.huesense.HueSenseConfig;
import lu.dainesch.huesense.hue.DBManager;

public class PingSensor extends Sensor<PingSensor.PingValues> {

    private final ObservableList<XYChart.Series<Date, Number>> data;
    private final ObjectProperty<GraphInterval> graphInterval;
    private final Map<String, XYChart.Series<Date, Number>> seriesMap;
    private boolean lastdetected;

    public PingSensor(int id, String uniqueID, HueSenseConfig config, DBManager dbMan) {
        super(id, uniqueID, SensorType.PING, config, dbMan);
        graphInterval = new SimpleObjectProperty<>(GraphInterval.INTERVALS.get(1));
        data = FXCollections.observableArrayList();
        seriesMap = new HashMap<>();
    }

    public void updateSensor(Map<String, Integer> values) {
        SensorValue<PingValues> val = new SensorValue<>(new Date(), new PingValues(values));
        
        long count = values.entrySet().stream().map(e -> e.getValue()).filter(e -> e > 0).collect(Collectors.counting());
        if (count > 0) {
            updateValue(val);
            lastdetected = true;
        } else if (lastdetected) {
            updateValue(val);
            lastdetected = false;
        }
        
       
    }

    @Override
    public void updateSensor(JsonObject obj) throws UpdateException {

    }

    @Override
    public void updateView(SensorValue<PingValues> val) {
        Duration maxAge = graphInterval.get().getDuration();
        for (XYChart.Series<Date, Number> ser : data) {
            while (!ser.getData().isEmpty() && (System.currentTimeMillis() - ser.getData().get(0).getXValue().getTime() > maxAge.toMillis())) {
                // remove old values
                ser.getData().remove(0);
            }
        }
        if (val == null || System.currentTimeMillis() - val.getTime().getTime() > maxAge.toMillis()) {
            // do not add old data
            return;
        }
        for (Entry<String, Integer> e : val.getValue().getValues().entrySet()) {
            XYChart.Series<Date, Number> ser;
            if (!seriesMap.containsKey(e.getKey())) {
                ser = new XYChart.Series<>();
                ser.setName(e.getKey());
                seriesMap.put(e.getKey(), ser);
                data.add(ser);
            }
            ser = seriesMap.get(e.getKey());
            ser.getData().add(new XYChart.Data<>(val.getTime(), e.getValue()));
        }

    }

    public ObservableList<XYChart.Series<Date, Number>> getData() {
        return data;
    }

    @Override
    public String getValueAsString(PingValues value) {
        long count = value.getValues().entrySet().stream().map(e -> e.getValue()).filter(e -> e > 0).collect(Collectors.counting());
        if (count > 0) {
            return count + " device(s) present";
        }
        return "No device present";
    }

    public void setGraphInterval(GraphInterval val) {
        if (graphInterval.get().getDuration().lessThan(val.getDuration())) {
            // longer interval
            Date start = new Date(System.currentTimeMillis() - (long) val.getDuration().toMillis());
            for (XYChart.Series<Date, Number> ser : data) {
                ser.getData().removeAll(ser.getData());   // clear does not work
            }
            graphInterval.set(val);
            getValuesInRange(start, null).forEach((s) -> {
                updateView(s);
            });
        } else {
            graphInterval.set(val);
            updateView(null);
        }
    }

    public static class PingValues {

        private final Map<String, Integer> values;

        public PingValues(Map<String, Integer> values) {
            this.values = values;
        }

        public void addValue(String device, int ping) {
            values.put(device, ping);
        }

        public Map<String, Integer> getValues() {
            return values;
        }

    }

}
