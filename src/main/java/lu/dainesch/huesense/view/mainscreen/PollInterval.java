package lu.dainesch.huesense.view.mainscreen;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

class PollInterval {

    public static final ObservableList<PollInterval> INTERVALS = FXCollections.observableArrayList(
            new PollInterval("10 seconds", Duration.seconds(10)),
            new PollInterval("30 seconds", Duration.seconds(30)),
            new PollInterval("1 minute", Duration.minutes(1)),
            new PollInterval("5 minutes", Duration.minutes(5)),
            new PollInterval("10 minutes", Duration.minutes(10))
    );

    private final String name;
    private final Duration duration;

    public PollInterval(String name, Duration duration) {
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
