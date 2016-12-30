package lu.dainesch.huesense.hue;

import lu.dainesch.huesense.net.UPNPUtil;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import lu.dainesch.huesense.Constants;
import lu.dainesch.huesense.HueSenseConfig;
import lu.dainesch.huesense.hue.data.Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HueComm {

    private static final Logger LOG = LoggerFactory.getLogger(HueComm.class);

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";

    private static final String PROTOCOL = "http://";
    private static final String DESCRIPTION = "/description.xml";
    private static final String API = "/api";
    private static final String SENSORS = "/sensors";
    private static final String IDENTBODY = "{\"devicetype\":\"huesense#app\"}";

    private final StringProperty bridgeIp = new SimpleStringProperty();
    private final BooleanProperty found = new SimpleBooleanProperty(false);
    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    private final BooleanProperty authenticated = new SimpleBooleanProperty(false);

    private final ExecutorService execServ;
    private final HueSenseConfig config;
    private final DataStore store;

    private final Bridge bridge;

    public HueComm(HueSenseConfig config, DataStore store) {
        this.config = config;
        this.store = store;
        this.bridge = new Bridge(config.getString(Constants.BRIDGEIP), config.getString(Constants.BRIDGEKEY));
        this.bridgeIp.setValue(bridge.getIp());
        this.execServ = Executors.newFixedThreadPool(1, (Runnable r) -> {
            Thread ret = new Thread(r);
            ret.setDaemon(true);
            ret.setName(HueComm.class.getSimpleName() + " Thread");
            return ret;
        });

    }

    public void updateSensors() {
        if (!authenticated.get()) {
            return;
        }
        execServ.submit(() -> {
            try {
                String data = makeQuery(GET, PROTOCOL + bridge.getIp() + API + "/" + bridge.getKey() + SENSORS, null);
                store.updateSensorData(data);
            } catch (Exception ex) {
                LOG.error("Error updating sensors", ex);
            }
        });
    }

    public void startConnecting() {
        if (authenticated.get()) {
            return;
        }
        execServ.submit(() -> {

            try {
                if (testConnection() && testAuth()) {
                    // done
                    LOG.info("Using saved settings");
                    return;
                }

                bridge.setIp(UPNPUtil.searchBridge());

                if (bridge.getIp() != null && testConnection()) {
                    if (!testAuth()) {
                        waitForAuth();
                    }
                }

            } catch (Exception ex) {
                LOG.error("Error connecting", ex);
            }

        });

    }

    private boolean waitForAuth() throws InterruptedException, IOException {
        for (int i = 0; i < 100; i++) {

            if (getKey() && testAuth()) {
                return true;
            }
            Thread.sleep(3000);
        }
        return false;
    }

    private boolean getKey() throws IOException {
        String resp = makeQuery(POST, PROTOCOL + bridge.getIp() + API, IDENTBODY);
        if (resp != null) {
            JsonReader reader = Json.createReader(new StringReader(resp));
            JsonArray arr = reader.readArray();
            JsonObject obj = arr.getJsonObject(0);
            if (obj.containsKey("success")) {
                JsonObject success = obj.getJsonObject("success");
                bridge.setKey(success.getString("username"));
                LOG.info("Got key from bridge");
                return true;
            }

        }
        return false;
    }

    private boolean testAuth() throws IOException {
        if (bridge.getIp() != null && !bridge.getIp().isEmpty()
                && bridge.getKey() != null && !bridge.getKey().isEmpty()) {

            if (makeQuery(GET, PROTOCOL + bridge.getIp() + API + "/" + bridge.getKey() + SENSORS, null) != null) {
                saveConfig();
                LOG.info("Authenticated on bridge");
                Platform.runLater(() -> {
                    authenticated.set(true);
                });
                return true;
            } else {
                bridge.setKey(null);
            }
        }
        return false;
    }

    private boolean testConnection() throws IOException {
        if (bridge.getIp() != null && !bridge.getIp().isEmpty()) {
            if (makeQuery(GET, PROTOCOL + bridge.getIp() + DESCRIPTION, null) != null) {
                // connected
                LOG.info("Connection established");
                Platform.runLater(() -> {
                    found.set(true);
                    connected.set(true);
                    bridgeIp.set(bridge.getIp());
                });
                return true;
            } else {
                bridge.setIp(null);
                Platform.runLater(() -> {
                    bridgeIp.set(null);
                });
            }

        }
        return false;
    }

    private String makeQuery(String method, String path, String body) throws IOException {

        StringBuilder result = new StringBuilder();
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setReadTimeout(Constants.TIMEOUT.intValue());
        if ((method.equals(POST) || method.endsWith(PUT)) && body != null) {
            conn.setDoOutput(true);
            try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8))) {
                w.write(body);
                w.flush();
            }
        }
        if (conn.getResponseCode() == 200) {
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
            }
            return result.toString();
        }
        return null;

    }

    public void renameSensor(int sensorId, String name) throws IOException {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        JsonObjectBuilder obj = Json.createObjectBuilder();
        obj.add("name", name);

        makeQuery(PUT, PROTOCOL + bridge.getIp() + API + "/" + bridge.getKey() + SENSORS + "/" + sensorId, obj.build().toString());

    }

    public StringProperty getBridgeIp() {
        return bridgeIp;
    }

    public BooleanProperty getFound() {
        return found;
    }

    public BooleanProperty getConnected() {
        return connected;
    }

    public BooleanProperty getAuthenticated() {
        return authenticated;
    }

    public HueSenseConfig getConfig() {
        return config;
    }

    private void saveConfig() {
        config.putString(Constants.BRIDGEIP, bridge.getIp());
        config.putString(Constants.BRIDGEKEY, bridge.getKey());
    }

}
