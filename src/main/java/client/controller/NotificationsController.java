package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.NotificationClient;
import client.service.NotificationClientService;
import client.service.NotificationClientService.NotificationAction;
import client.service.NotificationClientService.NotificationScope;
import client.ui.NotificationViewFactory;
import common.Message;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.Notification;
import server.model.User;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationsController implements Initializable {

    private final NotificationClient notificationService;
    private final NotificationViewFactory viewFactory;

    public NotificationsController() {
        this(new NotificationClientService());
    }

    NotificationsController(NotificationClient notificationService) {
        this.notificationService = notificationService;
        this.viewFactory = new NotificationViewFactory(notificationService);
    }

    @FXML private Button markAllReadBtn;
    @FXML private VBox notificationsContainer;

    private User currentUser;
    private ClientSocket clientSocket;
    private HomeScreenController homeController;
    private Timeline timeRefreshTimer;
    private final List<Runnable> timeLabelUpdaters = new ArrayList<>();

    public void setHomeController(HomeScreenController homeController) {
        this.homeController = homeController;
    }

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        loadNotifications();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (markAllReadBtn != null) {
            markAllReadBtn.setOnAction(event -> markAllRead());
        }
        startTimeRefreshTimer();
    }

    public void loadNotifications() {
        if (notificationsContainer == null) return;

        showLoadingState();

        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = resolveSocket();
                User user = resolveUser();

                if (socket == null || user == null) {
                    Platform.runLater(() -> showEmptyState("Khong co du lieu nguoi dung hoac ket noi server."));
                    return;
                }

                Message response = notificationService.fetchNotifications(socket, user);

                Platform.runLater(() -> {
                    if (response != null && response.getData() instanceof List<?> rawList) {
                        renderNotifications(rawList);
                    } else {
                        showEmptyState("Chua co thong bao nao.");
                    }
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error",
                        new RuntimeException("Loi load bidder notifications: " + e.getMessage()));
                Platform.runLater(() -> showEmptyState("Khong tai duoc thong bao."));
            }
        });
    }

    private void renderNotifications(List<?> rawList) {
        notificationsContainer.getChildren().clear();
        timeLabelUpdaters.clear();

        Notification profileReminder = createProfileReminderIfNeeded();
        if (profileReminder != null) {
            notificationsContainer.getChildren().add(createNotificationRow(profileReminder));
        }

        for (Object item : rawList) {
            if (item instanceof Notification notification
                    && notificationService.isVisible(notification, NotificationScope.BIDDER)) {
                notificationsContainer.getChildren().add(createNotificationRow(notification));
            }
        }

        if (notificationsContainer.getChildren().isEmpty()) {
            showEmptyState("Chua co thong bao nao.");
        }
    }

    private Notification createProfileReminderIfNeeded() {
        User user = resolveUser();
        if (ProfileController.hasCompleteBidProfile(user)) {
            return null;
        }
        String userId = user != null ? user.getUserId() : "";
        return new Notification(
                userId,
                "PROFILE_UPDATE",
                "Cap nhat thong tin tai khoan",
                "Ban can cap nhat so dien thoai va dia chi truoc khi tham gia dau gia.",
                "Vua xong",
                "Cap nhat",
                "PROFILE"
        );
    }

    private javafx.scene.Node createNotificationRow(Notification data) {
        return viewFactory.createNotificationRow(data, this::handleNotificationAction, timeLabelUpdaters::add);
    }

    private void markAllRead() {
        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = resolveSocket();
                User user = resolveUser();
                if (socket == null || user == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Loi", "Mat ket noi toi server."));
                    return;
                }

                Message response = notificationService.markAllRead(socket, user, NotificationScope.BIDDER);
                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        loadNotifications();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Loi", "Khong the danh dau da doc.");
                    }
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error",
                        new RuntimeException("Loi mark bidder notifications read: " + e.getMessage()));
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Loi", "Khong the danh dau da doc."));
            }
        });
    }

    private void handleNotificationAction(Notification data) {
        NotificationAction action = notificationService.actionFor(data, NotificationScope.BIDDER);
        if (homeController == null) {
            return;
        }

        switch (action) {
            case WALLET -> homeController.loadWalletView();
            case AUCTION_DETAIL -> openReferencedAuction(data.getReferenceId());
            case AUCTION_HISTORY -> homeController.loadAuctionHistoryView();
            case PROFILE -> homeController.loadProfileView();
            default -> homeController.loadProfileView();
        }
    }

    private void openReferencedAuction(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            if (homeController != null) {
                homeController.loadAuctionHistoryView();
            }
            return;
        }

        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = resolveSocket();
                if (socket == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Loi", "Mat ket noi toi server."));
                    return;
                }

                Message response = notificationService.fetchAuctionDetail(socket, auctionId);
                Platform.runLater(() -> {
                    if (response != null && response.getData() instanceof Auction auction && homeController != null) {
                        homeController.loadAuctionDetailView(auction);
                    } else if (homeController != null) {
                        homeController.loadAuctionHistoryView();
                    }
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error",
                        new RuntimeException("Loi mo phien tu notification: " + e.getMessage()));
                Platform.runLater(() -> {
                    if (homeController != null) {
                        homeController.loadAuctionHistoryView();
                    }
                });
            }
        });
    }

    private void showLoadingState() {
        notificationsContainer.getChildren().setAll(viewFactory.createStateLabel("Dang tai thong bao..."));
    }

    private void showEmptyState(String text) {
        notificationsContainer.getChildren().setAll(viewFactory.createStateLabel(text));
        timeLabelUpdaters.clear();
    }

    private ClientSocket resolveSocket() {
        if (clientSocket != null) return clientSocket;
        if (NavigationManager.getInstance().getClientSocket() != null) {
            return NavigationManager.getInstance().getClientSocket();
        }
        return ConnectionManager.getInstance().getClientSocket();
    }

    private User resolveUser() {
        if (currentUser != null) return currentUser;
        return NavigationManager.getInstance().getCurrentUser();
    }

    private void startTimeRefreshTimer() {
        if (timeRefreshTimer != null) {
            timeRefreshTimer.stop();
        }

        timeRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(30), event -> {
            for (Runnable updater : timeLabelUpdaters) {
                updater.run();
            }
        }));
        timeRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        timeRefreshTimer.play();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        client.util.DialogUtil.showAlert(type, title, null, content);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
