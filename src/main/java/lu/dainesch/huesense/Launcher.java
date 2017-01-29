package lu.dainesch.huesense;

import com.airhacks.afterburner.injection.Injector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lu.dainesch.huesense.hue.DBManager;
import lu.dainesch.huesense.hue.DataStore;
import lu.dainesch.huesense.hue.HueComm;
import lu.dainesch.huesense.net.LanComm;
import lu.dainesch.huesense.net.MailService;
import lu.dainesch.huesense.view.UIUtils;
import lu.dainesch.huesense.view.connection.ConnectingView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(Launcher.class);

    private final HueSenseConfig config;
    private final DBManager dbMan;
    private final DataStore store;
    private final HueComm hue;
    private final LanComm lan;
    private final MailService mailServ;

    public Launcher() {
        config = new HueSenseConfig();
        dbMan = new DBManager();
        store = new DataStore(config, dbMan);
        hue = new HueComm(config, store);
        lan = new LanComm(config, store, dbMan);
        mailServ = new MailService(config);
    }

    @Override
    public void start(Stage stage) throws Exception {

        Platform.setImplicitExit(false);

        Injector.setConfigurationSource(config::getInjectionValue);
        Injector.setLogger(s -> LOG.info(s));
        Injector.setModelOrService(Logger.class, LOG);
        Injector.setModelOrService(HueSenseConfig.class, config);
        Injector.setModelOrService(DBManager.class, dbMan);
        Injector.setModelOrService(DataStore.class, store);
        Injector.setModelOrService(HueComm.class, hue);
        Injector.setModelOrService(LanComm.class, lan);
        Injector.setModelOrService(MailService.class, mailServ);

        ConnectingView connView = new ConnectingView();
        Scene scene = new Scene(connView.getView());
        scene.setFill(Color.WHITE);
        stage.setTitle("HueSense");
        final String uri = Thread.currentThread().getContextClassLoader().getResource("global.css").toExternalForm();
        scene.getStylesheets().add(uri);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);

        stage.centerOnScreen();
        UIUtils.setIcon(stage);

        stage.show();

        hue.startConnecting();

    }

    @Override
    public void stop() throws Exception {
        Injector.forgetAll();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
