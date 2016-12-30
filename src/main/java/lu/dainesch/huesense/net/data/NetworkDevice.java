package lu.dainesch.huesense.net.data;

import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class NetworkDevice {

    private final String ip;
    private final String hostname;
    private final BooleanProperty reachable = new SimpleBooleanProperty(false);

    public NetworkDevice(String ip, String hostname) {
        this.ip = ip;
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }
    
    public BooleanProperty getReachable() {
        return reachable;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.ip);
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
        final NetworkDevice other = (NetworkDevice) obj;
        if (!Objects.equals(this.ip, other.ip)) {
            return false;
        }
        return true;
    }
    
    

    @Override
    public String toString() {
        if (hostname != null) {
            return hostname + "(" + ip + ")";
        }
        return ip;
    }

}
