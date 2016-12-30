package lu.dainesch.huesense;

import java.util.prefs.Preferences;

public class HueSenseConfig {

    private final Preferences preferences;

    public HueSenseConfig() {
        preferences = Preferences.userNodeForPackage(HueSenseConfig.class);
    }

    public Object getInjectionValue(Object key) {
        return preferences.get((String) key, null);
    }

    public void putDouble(String key, double val) {
        preferences.putDouble(key, val);
    }

    public double getDouble(String key) {
        return preferences.getDouble(key, -1);
    }

    public void putInt(String key, int value) {
        preferences.putInt(key, value);
    }

    public int getInt(String key) {
        return preferences.getInt(key, -1);
    }

    public void putBoolean(String key, boolean val) {
        preferences.putBoolean(key, val);
    }

    public boolean getBoolean(String key) {
        return preferences.getBoolean(key, false);
    }

    public void putString(String key, String val) {
        preferences.put(key, val);
    }

    public String getString(String key) {
        return preferences.get(key, null);
    }

}
