
package lu.dainesch.huesense.hue.data;


public enum SensorType {
    
    TEMPERATURE("ZLLTemperature"),
    PRESENCE("ZLLPresence"),
    LIGHT("ZLLLightLevel"),
    PING("Ping")
    ;
    
    private final String type;

    SensorType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
    
    public static SensorType getByType(String type) {
        for (SensorType t:values()) {
            if (t.getType().equalsIgnoreCase(type)) {
                return t;
            }
        }
        return null;
    }
    
    
    
}
