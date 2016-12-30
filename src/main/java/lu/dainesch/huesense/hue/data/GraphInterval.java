package lu.dainesch.huesense.hue.data;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

public class GraphInterval {

    public static final ObservableList<GraphInterval> INTERVALS = FXCollections.observableArrayList(
            new GraphInterval("1 Hour", Duration.hours(1)),
            new GraphInterval("12 Hours", Duration.hours(12)),
            new GraphInterval("1 Day", Duration.hours(24)),
            new GraphInterval("7 Days", Duration.hours(7 * 24))
    );

    private final String name;
    private final Duration duration;

    public GraphInterval(String name, Duration duration) {
        this.name = name;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return name;
    }

    public Duration getDuration() {
        return duration;
    }

}
