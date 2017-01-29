package lu.dainesch.huesense.hue;

import java.sql.Connection;
import java.sql.SQLException;
import lu.dainesch.huesense.Constants;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBManager {

    private static final Logger LOG = LoggerFactory.getLogger(DBManager.class);

    private final BasicDataSource ds;
    private final Flyway flyway;

    public DBManager() {
        ds = new BasicDataSource();
        ds.setDriver(new EmbeddedDriver());
        ds.setUrl(Constants.JDBC);
        
        flyway = new Flyway();
        flyway.setDataSource(ds);
        //flyway.clean();
        flyway.migrate();

        // just to be sure, try to close
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    LOG.info("Closing DB connection...");
                    ds.close();
                    LOG.info("DB closed");
                } catch (SQLException ex) {
                    LOG.error("Error closing DB cconnection", ex);
                }
            }
        });
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public void close() throws SQLException {
        ds.close();
    }
    
    public void cleanDB() {
        flyway.clean();
        flyway.migrate();
    }

}
