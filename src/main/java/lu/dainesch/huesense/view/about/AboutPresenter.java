package lu.dainesch.huesense.view.about;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import lu.dainesch.huesense.view.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AboutPresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AboutPresenter.class);

    @FXML
    private BorderPane borderPane;
    @FXML
    private ImageView logoImg;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        logoImg.setImage(UIUtils.getImage("logo.png"));

        Dialog diag = new Dialog();
        diag.getDialogPane().setContent(borderPane);
        diag.getDialogPane().setBackground(Background.EMPTY);
        diag.setResizable(true);

        UIUtils.setIcon(diag);

        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);

        diag.getDialogPane().getButtonTypes().addAll(closeButton);
        diag.setTitle("About HueSense");

        Optional<ButtonType> opt = diag.showAndWait();
        if (opt.isPresent() && opt.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            // nothing
        }

    }

    @FXML
    void openLink(ActionEvent event) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop d = Desktop.getDesktop();
                d.browse(new URI("https://github.com/dainesch/HueSense"));
            } catch (IOException | URISyntaxException ex) {
                LOG.error("Error opening url", ex);
            }
        }
    }

}
