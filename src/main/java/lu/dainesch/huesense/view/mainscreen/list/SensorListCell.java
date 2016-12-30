package lu.dainesch.huesense.view.mainscreen.list;

import java.util.HashMap;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import lu.dainesch.huesense.hue.data.Sensor;

public class SensorListCell extends ListCell<Sensor<?>> {

    private final static Map<Integer, Node> ITEMVIEWS = new HashMap<>();

    @Override
    protected void updateItem(Sensor<?> item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
        } else {
            Node view = ITEMVIEWS.get(item.getId());
            if (view == null) {
                SensorItemView iv = new SensorItemView();
                ((SensorItemPresenter) iv.getPresenter()).setSensor(item);
                view = iv.getView();
                ITEMVIEWS.put(item.getId(), view);
            }
            setGraphic(view);
        }
    }
}
