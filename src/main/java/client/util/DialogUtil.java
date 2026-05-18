package client.util;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.DialogPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public final class DialogUtil {
    private static final String ALERT_STYLE_CLASS = "my-custom-alert";

    private DialogUtil() {
    }

    public static void showAlert(Alert.AlertType type, String title, String content) {
        showAlert(type, title, null, content, null);
    }

    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        showAlert(type, title, header, content, null);
    }

    public static void showAlert(Alert.AlertType type, String title, String header, String content, Node ownerNode) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header == null || header.isBlank() ? title : header);
        alert.setContentText(content);
        prepareDialog(alert, ownerNode);
        alert.showAndWait();
    }

    public static void prepareDialog(Dialog<?> dialog, Node ownerNode) {
        Window owner = resolveOwner(ownerNode);
        if (owner != null && dialog.getOwner() == null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        styleDialog(dialog);
        Region overlay = installOverlay(ownerNode, owner);
        EventHandler<DialogEvent> existingShownHandler = dialog.getOnShown();
        dialog.setOnShown(event -> {
            if (existingShownHandler != null) {
                existingShownHandler.handle(event);
            }
            Platform.runLater(() -> {
                if (dialog.getDialogPane().getScene() != null) {
                    dialog.getDialogPane().getScene().setFill(Color.TRANSPARENT);
                }
                configureEnterToClose(dialog);
                centerDialog(dialog, owner);
            });
        });

        EventHandler<DialogEvent> existingHiddenHandler = dialog.getOnHidden();
        dialog.setOnHidden(event -> {
            removeOverlay(overlay);
            if (existingHiddenHandler != null) {
                existingHiddenHandler.handle(event);
            }
        });
    }

    private static void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStyleClass().contains(ALERT_STYLE_CLASS)) {
            pane.getStyleClass().add(ALERT_STYLE_CLASS);
        }

        String stylesheet = DialogUtil.class.getResource("/CSS/style.css").toExternalForm();
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }

        try {
            dialog.initStyle(StageStyle.TRANSPARENT);
        } catch (IllegalStateException ignored) {
            // Dialog may already have a style if a caller configured it before prepareDialog.
        }

        if (dialog instanceof Alert alert) {
            alert.setGraphic(null);
            alert.setContentText(wrapAlertText(alert.getContentText(), 42));
            applyAlertTypeClass(alert);
            styleAlertButtons(alert);
        }
    }

    private static String wrapAlertText(String text, int maxLineLength) {
        if (text == null || text.isBlank() || text.contains("\n")) {
            return text;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder wrapped = new StringBuilder();
        int lineLength = 0;

        for (String word : words) {
            if (lineLength > 0 && lineLength + 1 + word.length() > maxLineLength) {
                wrapped.append('\n');
                lineLength = 0;
            } else if (lineLength > 0) {
                wrapped.append(' ');
                lineLength++;
            }
            wrapped.append(word);
            lineLength += word.length();
        }

        return wrapped.toString();
    }

    private static void applyAlertTypeClass(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        pane.getStyleClass().removeAll(
                "alert-info",
                "alert-warning",
                "alert-error",
                "alert-confirmation"
        );

        String typeClass = switch (alert.getAlertType()) {
            case INFORMATION -> "alert-info";
            case WARNING -> "alert-warning";
            case ERROR -> "alert-error";
            case CONFIRMATION -> "alert-confirmation";
            default -> "alert-info";
        };
        pane.getStyleClass().add(typeClass);
    }

    private static void styleAlertButtons(Alert alert) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType buttonType : pane.getButtonTypes()) {
            Node node = pane.lookupButton(buttonType);
            if (!(node instanceof Button button)) {
                continue;
            }
            ButtonBar.ButtonData buttonData = buttonType.getButtonData();
            button.getStyleClass().removeAll("alert-primary-button", "alert-secondary-button");
            if (buttonData == ButtonBar.ButtonData.OK_DONE
                    || buttonData == ButtonBar.ButtonData.YES
                    || buttonData == ButtonBar.ButtonData.APPLY) {
                button.getStyleClass().add("alert-primary-button");
                button.setDefaultButton(true);
            } else {
                button.getStyleClass().add("alert-secondary-button");
            }
        }
    }

    private static void configureEnterToClose(Dialog<?> dialog) {
        if (dialog.getDialogPane().getScene() == null) {
            return;
        }
        dialog.getDialogPane().getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ENTER) {
                return;
            }
            Button primaryButton = findPrimaryButton(dialog.getDialogPane());
            if (primaryButton != null) {
                primaryButton.fire();
                event.consume();
            }
        });
    }

    private static Button findPrimaryButton(DialogPane pane) {
        for (ButtonType buttonType : pane.getButtonTypes()) {
            Node node = pane.lookupButton(buttonType);
            if (node instanceof Button button && button.getStyleClass().contains("alert-primary-button")) {
                return button;
            }
        }
        return null;
    }

    private static Region installOverlay(Node ownerNode, Window owner) {
        Pane host = resolveOverlayHost(ownerNode, owner);
        if (host == null) {
            return null;
        }

        Region overlay = new Region();
        overlay.getStyleClass().add("app-dialog-overlay");
        overlay.setManaged(false);
        overlay.resizeRelocate(0, 0, host.getWidth(), host.getHeight());
        host.widthProperty().addListener((observable, oldValue, newValue) ->
                overlay.resizeRelocate(0, 0, newValue.doubleValue(), host.getHeight()));
        host.heightProperty().addListener((observable, oldValue, newValue) ->
                overlay.resizeRelocate(0, 0, host.getWidth(), newValue.doubleValue()));
        host.getChildren().add(overlay);
        overlay.toFront();
        return overlay;
    }

    private static Pane resolveOverlayHost(Node ownerNode, Window owner) {
        Node node = ownerNode;
        if (node == null && owner != null && owner.getScene() != null) {
            node = owner.getScene().getRoot();
        }

        while (node != null) {
            if (node instanceof StackPane || node instanceof AnchorPane) {
                return (Pane) node;
            }
            Parent parent = node.getParent();
            if (parent == null && node.getScene() != null) {
                parent = node.getScene().getRoot();
                if (parent == node) {
                    break;
                }
            }
            node = parent;
        }
        return null;
    }

    private static void removeOverlay(Region overlay) {
        if (overlay != null && overlay.getParent() instanceof Pane pane) {
            pane.getChildren().remove(overlay);
        }
    }

    public static void centerDialog(Dialog<?> dialog, Node ownerNode) {
        centerDialog(dialog, resolveOwner(ownerNode));
    }

    private static Window resolveOwner(Node ownerNode) {
        if (ownerNode != null && ownerNode.getScene() != null) {
            Window window = ownerNode.getScene().getWindow();
            if (window != null && window.isShowing()) {
                return window;
            }
        }

        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(Window::isFocused)
                .findFirst()
                .orElseGet(() -> Window.getWindows().stream()
                        .filter(Window::isShowing)
                        .findFirst()
                        .orElse(null));
    }

    private static void centerDialog(Dialog<?> dialog, Window owner) {
        if (dialog.getDialogPane().getScene() == null) {
            return;
        }

        Window dialogWindow = dialog.getDialogPane().getScene().getWindow();
        if (dialogWindow == null) {
            return;
        }

        Rectangle2D bounds = Screen.getScreensForRectangle(
                        dialogWindow.getX(),
                        dialogWindow.getY(),
                        Math.max(dialogWindow.getWidth(), 1),
                        Math.max(dialogWindow.getHeight(), 1))
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary())
                .getVisualBounds();

        double x;
        double y;
        if (owner != null && owner.isShowing()) {
            x = owner.getX() + (owner.getWidth() - dialogWindow.getWidth()) / 2;
            y = owner.getY() + (owner.getHeight() - dialogWindow.getHeight()) / 2;
        } else {
            x = bounds.getMinX() + (bounds.getWidth() - dialogWindow.getWidth()) / 2;
            y = bounds.getMinY() + (bounds.getHeight() - dialogWindow.getHeight()) / 2;
        }

        dialogWindow.setX(clamp(x, bounds.getMinX(), bounds.getMaxX() - dialogWindow.getWidth()));
        dialogWindow.setY(clamp(y, bounds.getMinY(), bounds.getMaxY() - dialogWindow.getHeight()));
    }

    private static double clamp(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
