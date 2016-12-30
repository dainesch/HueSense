package lu.dainesch.huesense.view.connection;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javax.inject.Inject;
import lu.dainesch.huesense.hue.HueComm;
import lu.dainesch.huesense.view.UIUtils;
import lu.dainesch.huesense.view.mainscreen.MainView;

public class ConnectingPresenter implements Initializable {

    @FXML
    private Label statusLabel;
    @FXML
    private Group push;
    @FXML
    private ImageView logo;
    @FXML
    private Label ipLabel;

    @Inject
    private HueComm hue;

    private BooleanProperty isWaitPush;
    private BooleanProperty isSearching;
    private BooleanProperty isDone;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        logo.setImage(UIUtils.getImage("logo.png"));
        
        isWaitPush = new SimpleBooleanProperty();
        isSearching = new SimpleBooleanProperty();
        isDone = new SimpleBooleanProperty();
        isSearching.bind(hue.getFound().not());
        isWaitPush.bind(hue.getFound().and(hue.getConnected()).and(hue.getAuthenticated().not()));
        isDone.bind(hue.getFound().and(hue.getConnected()).and(hue.getAuthenticated()));
        ipLabel.textProperty().bind(hue.getBridgeIp());

        push.setVisible(false);

        isSearching.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                statusLabel.setText("Searching Bridge...");
                logo.setVisible(true);
                push.setVisible(false);
            }
        });
        isWaitPush.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                statusLabel.setText("Press button on bridge!");
                logo.setVisible(false);
                push.setVisible(true);
            }
        });
        isDone.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                statusLabel.setText("Loading...");
                
                MainView main = new MainView();
                main.getView();
                
                Stage stage = (Stage) statusLabel.getScene().getWindow();
                stage.close();
            }
        });
        
   
    }

}
