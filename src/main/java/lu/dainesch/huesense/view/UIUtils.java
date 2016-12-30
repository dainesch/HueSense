package lu.dainesch.huesense.view;

import javafx.collections.ListChangeListener;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public class UIUtils {

    private UIUtils() {
    }

    public static void setIcon(Dialog diag) {
        Stage stage = (Stage) diag.getDialogPane().getScene().getWindow();
        setIcon(stage);
    }

    public static Image getImage(String path) {
        return new Image( path);
    }

    public static void setIcon(Stage stage) {
        stage.getIcons().add(getImage("icon.png"));
    }

    public static <S> void addAutoScroll(final TableView<S> view) {
        if (view == null) {
            throw new NullPointerException();
        }

        view.getItems().addListener((ListChangeListener<S>) (c -> {
            c.next();
            final int size = view.getItems().size();
            if (size > 0) {
                view.scrollTo(size - 1);
            }
        }));
    }

    public static <S> void addAutoScroll(final ListView<S> view) {
        if (view == null) {
            throw new NullPointerException();
        }

        view.getItems().addListener((ListChangeListener<S>) (c -> {
            c.next();
            final int size = view.getItems().size();
            if (size > 0) {
                view.scrollTo(size - 1);
            }
        }));
    }

    public static void setTextFieldNumbersOnly(TextField field, int min, int max) {
        field.addEventFilter(KeyEvent.KEY_TYPED, (e) -> {

            if (e.getCharacter().matches("[0-9]")) {
                String text = field.getText();
                if (e.getCode() == KeyCode.BACK_SPACE && text.length() > 0) {
                    text = text.substring(0, text.length() - 2);
                } else {
                    text = text + e.getCharacter();
                }
                try {
                    long value = Long.parseLong(text);
                    if (value < min) {
                        e.consume();
                        field.setText(String.valueOf(min));
                    } else if (value > max) {
                        e.consume();
                        field.setText(String.valueOf(max));
                    }
                } catch (NumberFormatException ex) {
                    e.consume();
                }
            } else {
                e.consume();
            }
        });
    }

}
