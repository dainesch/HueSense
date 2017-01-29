package lu.dainesch.huesense.net;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lu.dainesch.huesense.HueSenseConfig;
import lu.dainesch.huesense.hue.DBManager;
import lu.dainesch.huesense.hue.DataStore;
import lu.dainesch.huesense.hue.data.PingSensor;
import lu.dainesch.huesense.net.data.NetworkDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanComm {

    private static final Logger LOG = LoggerFactory.getLogger(LanComm.class);

    private static final int TIMEOUT_SCAN = 150;
    private static final int TIMEOUT_PING = 500;

    private final List<NetworkDevice> pingDevices;
    private final HueSenseConfig config;
    private final ExecutorService exec;
    private final ScheduledExecutorService scheduleExec;
    private final PingSensor sensor;

    private final ObservableList<NetworkDevice> devices = FXCollections.observableArrayList();
    private final BooleanProperty scanning = new SimpleBooleanProperty();

    public LanComm(HueSenseConfig config, DataStore store, DBManager dbMan) {
        this.config = config;

        this.sensor = new PingSensor(Integer.MAX_VALUE,"PingSensor", config, dbMan);
        this.sensor.setName("Reachable devices");

        this.exec = Executors.newCachedThreadPool((Runnable r) -> {
            Thread ret = new Thread(r);
            ret.setDaemon(true);
            ret.setName(NetworkUtil.class.getSimpleName() + " Thread");
            return ret;
        });
        this.scheduleExec = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
            Thread ret = new Thread(r);
            ret.setDaemon(true);
            ret.setName(NetworkUtil.class.getSimpleName() + " Ping Thread");
            return ret;
        });
        this.pingDevices = Collections.synchronizedList(new ArrayList<>());
        //TODO finish impl
        /*store.addSensor(sensor);
        this.scheduleExec.scheduleWithFixedDelay(() -> {
            updatePingSensor();
        }, 10, 1, TimeUnit.SECONDS);*/
    }

    private void updatePingSensor() {
        Map<String, Integer> values = new HashMap<>();
        synchronized (pingDevices) {
            for (NetworkDevice dev : pingDevices) {
                try {
                    values.put(dev.toString(), pingDevice(dev));
                } catch (IOException ex) {
                    LOG.error("Error pinging " + dev);
                }
            }
        }
        if (!values.isEmpty()) {
            sensor.updateSensor(values);
        }
    }

    private int pingDevice(NetworkDevice dev) throws IOException {

        InetAddress adr;
        if (dev.getHostname() != null) {
            adr = InetAddress.getByName(dev.getHostname());
        } else {
            adr = InetAddress.getByName(dev.getIp());
        }
        long start = System.currentTimeMillis();
        if (adr.isReachable(TIMEOUT_PING)) {
            long time = System.currentTimeMillis() - start;
            if (time == 0) {
                // special case, make sure that 0 is not returned when reachable
                return 1;
            }
            return (int) time;
        }
        return 0;

    }

    public void checkIfReachable(NetworkDevice dev) {
        if (scanning.get()) {
            return;
        }

        exec.submit(() -> {
            try {
                if (pingDevice(dev) > 0) {
                    Platform.runLater(() -> dev.getReachable().set(true));
                } else {
                    Platform.runLater(() -> dev.getReachable().set(false));
                }
            } catch (IOException ex) {
                Platform.runLater(() -> dev.getReachable().set(false));
            }

        });

    }

    public void startScan() {
        if (scanning.get()) {
            return;
        }
        scanning.set(true);
        devices.removeAll(devices); // clear does not trigger
        exec.submit(() -> {
            List<NetworkDevice> found = getLocalDevices();
            if (found != null) {
                Platform.runLater(() -> {
                    devices.addAll(found);
                    scanning.set(false);
                });
            } else {
                Platform.runLater(() -> scanning.set(false));
            }
        });

    }

    private List<NetworkDevice> getLocalDevices() {

        try {
            List<Future<NetworkDevice>> pings = new ArrayList<>();

            for (InetAddress local : NetworkUtil.getLocalInetAddresses()) {
                String host = local.getHostAddress();
                String subnet = host.substring(0, host.lastIndexOf('.'));

                for (int i = 1; i < 255; i++) {
                    final int device = i;
                    pings.add(exec.submit(() -> {
                        String ip = subnet + "." + device;
                        try {
                            InetAddress adr = InetAddress.getByName(ip);
                            if (adr.isReachable(TIMEOUT_SCAN)) {
                                String hostName = adr.getHostName();
                                NetworkDevice dev;
                                if (hostName != null && !hostName.equalsIgnoreCase(ip)) {
                                    dev = new NetworkDevice(ip, hostName);
                                } else {
                                    dev = new NetworkDevice(ip, null);
                                }
                                dev.getReachable().set(true);
                                return dev;
                            }
                        } catch (IOException ex) {
                            LOG.info("Host not found: " + ip);
                        }
                        return null;
                    }));
                }
            }
            return pings.stream().filter(f -> {
                try {
                    return f.get() != null;
                } catch (InterruptedException | ExecutionException ex) {
                    // ignore
                }
                return false;
            }).map(f -> {
                try {
                    return f.get();
                } catch (InterruptedException | ExecutionException ex) {
                    // never the case
                }
                return null;
            }).collect(Collectors.toList());
        } catch (IOException ex) {
            LOG.error("Error scanning for devices", ex);
            return null;
        }

    }

    public BooleanProperty getScanning() {
        return scanning;
    }

    public ObservableList<NetworkDevice> getDevices() {
        return devices;
    }

    public void setPingDevices(List<NetworkDevice> devices) {
        pingDevices.clear();
        pingDevices.addAll(devices);
    }

}
