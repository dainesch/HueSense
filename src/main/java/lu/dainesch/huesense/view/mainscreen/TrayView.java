package lu.dainesch.huesense.view.mainscreen;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javafx.application.Platform;

import javafx.beans.value.ChangeListener;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TrayView {

    private static final Logger LOG = LoggerFactory.getLogger(TrayView.class);

    private final SystemTray tray;
    private TrayIcon trayIcon = null;
    private ChangeListener<Void> exitListener;
    private ChangeListener<Void> openListener;

    public TrayView() {
        if (SystemTray.isSupported()) {
            this.tray = SystemTray.getSystemTray();
            createTrayIcon();
        } else {
            this.tray = null;
        }

    }

    private void createTrayIcon() {

        try {
            PopupMenu popup = new PopupMenu();
            Image image = ImageIO.read(Thread.currentThread().getContextClassLoader().getResource("icon.png"));
            Dimension size = tray.getTrayIconSize();
            image = image.getScaledInstance(size.width, size.height, Image.SCALE_SMOOTH);
            trayIcon = new TrayIcon(image, "HueSense", popup);

            MenuItem openItem = new MenuItem("Open");
            openItem.addActionListener((ActionEvent e) -> {
                Platform.runLater(() -> {
                    if (openListener != null) {
                        openListener.changed(null, null, null);
                    }
                });
            });
            popup.add(openItem);
            popup.addSeparator();

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener((ActionEvent e) -> {
                Platform.runLater(() -> {
                    if (exitListener != null) {
                        exitListener.changed(null, null, null);
                    }
                });
            });
            popup.add(exitItem);

            trayIcon.addActionListener((ActionEvent e) -> {
                Platform.runLater(() -> {
                    if (openListener != null) {
                        openListener.changed(null, null, null);
                    }
                });
            });
        } catch (IOException ex) {
            LOG.error("Error loading try icon", ex);
        }

    }

    public void showTrayIcon() {
        if (tray == null) {
            return;
        }
        try {
            tray.add(trayIcon);
        } catch (AWTException ex) {
            LOG.error("Error adding tray icon", ex);
        }
    }

    public void hideTrayIcon() {
        if (tray == null) {
            return;
        }
        tray.remove(trayIcon);

    }

    public void setExitListener(ChangeListener<Void> exitListener) {
        this.exitListener = exitListener;
    }

    public void setOpenListener(ChangeListener<Void> openListener) {
        this.openListener = openListener;
    }

    public boolean isSupported() {
        return tray != null;
    }

}
