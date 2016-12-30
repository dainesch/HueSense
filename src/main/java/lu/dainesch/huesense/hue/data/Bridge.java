package lu.dainesch.huesense.hue.data;

public class Bridge {

    private String ip;
    private String key;

    public Bridge(String ip, String key) {
        this.ip = ip;
        this.key = key;
    }

    public synchronized String getIp() {
        return ip;
    }

    public synchronized void setIp(String ip) {
        this.ip = ip;
    }

    public synchronized String getKey() {
        return key;
    }

    public synchronized void setKey(String key) {
        this.key = key;
    }

}
