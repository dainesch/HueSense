package lu.dainesch.huesense.net;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import lu.dainesch.huesense.HueSenseConfig;
import lu.dainesch.huesense.net.data.MailSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);
    
    private final HueSenseConfig config;
    private MailSettings currentSettings;

    public MailService(HueSenseConfig config) {
        this.config = config;
    }
    
    public boolean testSettings(MailSettings settings) {
        if (!settings.isValid()) {
            return false;
        }
        try {
            sendMail("HueSense test mail", "This is only a test message", settings);
            return true;
        } catch (MessagingException ex) {
            LOG.error("Error testing mail", ex);
        }
        return false;
    }

    private void sendMail(String subject, String body, MailSettings settings) throws MessagingException {

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", settings.getSmtpServer());
        props.put("mail.smtp.port", settings.getSmtpPort() + "");

        if (settings.getMode() == MailSettings.Mode.SSL) {
            props.put("mail.smtp.socketFactory.port", settings.getSmtpPort() + "");
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
        } else if (settings.getMode() == MailSettings.Mode.TLS) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        props.put("mail.smtp.auth", settings.isUseAuth() + "");

        Session session;
        if (settings.isUseAuth()) {
            session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(settings.getUser(), settings.getPass());
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(settings.getFrom()));
        message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(settings.getTo()));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);

    }

    public MailSettings getCurrentSettings() {
        return currentSettings;
    }

    public void setCurrentSettings(MailSettings currentSettings) {
        this.currentSettings = currentSettings;
    }
    
    

}
