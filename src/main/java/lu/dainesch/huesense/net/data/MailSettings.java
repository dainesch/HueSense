package lu.dainesch.huesense.net.data;

public class MailSettings {

    public static enum Mode {
        None,
        TLS,
        SSL
    }

    private String user;
    private String pass;
    private boolean useAuth;
    private String smtpServer;
    private int smtpPort;
    private Mode mode;
    private String from;
    private String to;

    public boolean isValid() {
        if (useAuth && (user == null || user.trim().isEmpty() || pass == null || pass.trim().isEmpty())) {
            return false;
        }
        return mode!=null && smtpPort > 0 && smtpServer != null && !smtpServer.trim().isEmpty() && 
                from != null && !from.trim().isEmpty() && to != null && !to.trim().isEmpty();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public boolean isUseAuth() {
        return useAuth;
    }

    public void setUseAuth(boolean useAuth) {
        this.useAuth = useAuth;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

}
