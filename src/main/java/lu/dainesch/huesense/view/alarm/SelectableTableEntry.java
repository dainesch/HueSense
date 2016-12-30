package lu.dainesch.huesense.view.alarm;

import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class SelectableTableEntry<A> {

    private final A element;
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty();

    public SelectableTableEntry(int id, String name, A element) {
        this.id.set(id);
        this.name.set(name);
        this.element = element;
    }

    public A getElement() {
        return element;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public boolean getSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.id.get());
        hash = 43 * hash + Objects.hashCode(this.name.get());
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
        final SelectableTableEntry<?> other = (SelectableTableEntry<?>) obj;
        if (!Objects.equals(this.id.get(), other.id.get())) {
            return false;
        }
        if (!Objects.equals(this.name.get(), other.name.get())) {
            return false;
        }
        return true;
    }
    
    

    public static class STECellFactory implements Callback<TableColumn.CellDataFeatures<SelectableTableEntry<?>, CheckBox>, ObservableValue<CheckBox>> {

        @Override
        public ObservableValue<CheckBox> call(
                TableColumn.CellDataFeatures<SelectableTableEntry<?>, CheckBox> arg0) {
            SelectableTableEntry<?> entry = arg0.getValue();

            CheckBox checkBox = new CheckBox();
            checkBox.selectedProperty().bindBidirectional(entry.selectedProperty());
            return new SimpleObjectProperty<>(checkBox);

        }

    }

}
