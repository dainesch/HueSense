package lu.dainesch.huesense;

import java.util.concurrent.TimeUnit;

public class Constants {

    private Constants() {
    }

    public static final Long TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    public static final String BRIDGEIP = "bridge";
    public static final String BRIDGEKEY = "bridgeKey";

    public static final String HUEDATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String TIMEFORMAT = "HH:mm:ss";
    public static final String DATEFORMAT = "dd MMM yyyy";

    public static final String QV_POS_X = "qvPos.x";
    public static final String QV_POS_Y = "qvPos.y";

    public static final String HIDE_ON_CLOSE = "hideOnClose";
    public static final String QV_COLOR = "qvColor";
    public static final String SHOW_QV = "qvShow";

    public static final String QV_SENSOR = "qvState.";
    public static final String TEMP_OFFSET_SENSOR = "temp.offset.";

    public static final String ENV_USER_HOME = System.getProperty("user.home");
    public static final String WORK_DIR = "/.huesense";
    public static final String DB_FILE = "/derby.db";

    public static final String JDBC = "jdbc:derby:" + ENV_USER_HOME + WORK_DIR + DB_FILE + ";create=true";
    
    public static final String CSV_SEP = ",";

}
