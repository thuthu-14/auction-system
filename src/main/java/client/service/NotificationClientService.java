package client.service;

import client.network.ClientSocket;
import common.Message;
import common.MessageType;
import server.model.Notification;
import server.model.User;

import java.util.Map;
import java.util.Set;

public class NotificationClientService {
    public enum NotificationScope {
        BIDDER,
        SELLER
    }

    public enum NotificationAction {
        WALLET,
        AUCTION_DETAIL,
        AUCTION_HISTORY,
        PROFILE,
        SELLER_MANAGE_AUCTIONS,
        SELLER_DASHBOARD
    }

    public record NotificationPresentation(
            String icon,
            String iconBoxStyle,
            String iconColor,
            String titleColor,
            String buttonStyle,
            String unreadContainerStyle,
            NotificationAction bidderAction,
            NotificationAction sellerAction,
            Set<NotificationScope> scopes
    ) {
    }

    private static final String DEFAULT_UNREAD_CONTAINER_STYLE =
            "-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #3182ce; -fx-border-radius: 15; -fx-border-width: 2; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(49,130,206,0.14), 15, 0, 0, 5);";
    private static final String READ_CONTAINER_STYLE =
            "-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #f3f4f6; -fx-border-radius: 15; -fx-border-width: 1; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.03), 10, 0, 0, 2);";
    private static final String DEFAULT_ICON_BOX_STYLE = "-fx-background-color: #e0f2fe; -fx-background-radius: 30;";
    private static final String DEFAULT_BUTTON_STYLE = "-fx-background-color: #f3f4f6; -fx-text-fill: #4a5568; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20 8 20; -fx-border-color: #cbd5e1; -fx-border-radius: 20;";

    private static final NotificationPresentation DEFAULT_PRESENTATION = new NotificationPresentation(
            "i",
            DEFAULT_ICON_BOX_STYLE,
            "#075985",
            "#1a1a1a",
            DEFAULT_BUTTON_STYLE,
            DEFAULT_UNREAD_CONTAINER_STYLE,
            NotificationAction.PROFILE,
            NotificationAction.SELLER_DASHBOARD,
            Set.of(NotificationScope.BIDDER, NotificationScope.SELLER)
    );

    private static final Map<String, NotificationPresentation> PRESENTATIONS = Map.of(
            "WIN", new NotificationPresentation(
                    "T",
                    "-fx-background-color: #fef08a; -fx-background-radius: 30;",
                    "#854d0e",
                    "#b45309",
                    "-fx-background-color: #facc15; -fx-text-fill: #1a1a1a; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20 8 20;",
                    "-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #fde047; -fx-border-radius: 15; -fx-border-width: 2; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(253,224,71,0.2), 15, 0, 0, 5);",
                    NotificationAction.WALLET,
                    NotificationAction.SELLER_DASHBOARD,
                    Set.of(NotificationScope.BIDDER)
            ),
            "OUTBID", new NotificationPresentation(
                    "!",
                    "-fx-background-color: #fee2e2; -fx-background-radius: 30;",
                    "#991b1b",
                    "#1a1a1a",
                    "-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20 8 20;",
                    "-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #ef4444; -fx-border-radius: 15; -fx-border-width: 2; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(239,68,68,0.14), 15, 0, 0, 5);",
                    NotificationAction.AUCTION_DETAIL,
                    NotificationAction.SELLER_DASHBOARD,
                    Set.of(NotificationScope.BIDDER)
            ),
            "SYSTEM", new NotificationPresentation("i", DEFAULT_ICON_BOX_STYLE, "#075985", "#1a1a1a", DEFAULT_BUTTON_STYLE, DEFAULT_UNREAD_CONTAINER_STYLE, NotificationAction.PROFILE, NotificationAction.SELLER_DASHBOARD, Set.of(NotificationScope.BIDDER)),
            "PAYMENT", new NotificationPresentation("i", "-fx-background-color: #dbeafe; -fx-background-radius: 30;", "#1d4ed8", "#1a1a1a", "-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20 8 20;", DEFAULT_UNREAD_CONTAINER_STYLE, NotificationAction.WALLET, NotificationAction.WALLET, Set.of(NotificationScope.BIDDER, NotificationScope.SELLER)),
            "SELLER_BID", new NotificationPresentation("S", "-fx-background-color: #dcfce7; -fx-background-radius: 30;", "#166534", "#166534", "-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20 8 20;", "-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #22c55e; -fx-border-radius: 15; -fx-border-width: 2; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.18), 15, 0, 0, 5);", NotificationAction.PROFILE, NotificationAction.SELLER_MANAGE_AUCTIONS, Set.of(NotificationScope.SELLER)),
            "AUCTION_FINISHED", new NotificationPresentation("F", "-fx-background-color: #fef3c7; -fx-background-radius: 30;", "#92400e", "#1a1a1a", DEFAULT_BUTTON_STYLE, DEFAULT_UNREAD_CONTAINER_STYLE, NotificationAction.PROFILE, NotificationAction.SELLER_MANAGE_AUCTIONS, Set.of(NotificationScope.SELLER)),
            "SALE", new NotificationPresentation("S", "-fx-background-color: #fef3c7; -fx-background-radius: 30;", "#92400e", "#166534", "-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20 8 20;", "-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #22c55e; -fx-border-radius: 15; -fx-border-width: 2; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(34,197,94,0.18), 15, 0, 0, 5);", NotificationAction.PROFILE, NotificationAction.SELLER_MANAGE_AUCTIONS, Set.of(NotificationScope.SELLER))
    );

    public Message fetchNotifications(ClientSocket socket, User user) throws Exception {
        ensureContext(socket, user);
        return socket.sendAndReceive(new Message(MessageType.GET_NOTIFICATIONS, null, user.getUserId()));
    }

    public Message markAllRead(ClientSocket socket, User user, String scope) throws Exception {
        ensureContext(socket, user);
        return socket.sendAndReceive(new Message(MessageType.MARK_NOTIFICATIONS_READ, scope, user.getUserId()));
    }

    public Message markAllRead(ClientSocket socket, User user, NotificationScope scope) throws Exception {
        return markAllRead(socket, user, scope.name());
    }

    public Message fetchAuctionDetail(ClientSocket socket, String auctionId) throws Exception {
        if (socket == null || !socket.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
        return socket.sendAndReceive(new Message(MessageType.GET_AUCTION_DETAIL, auctionId, "CLIENT"));
    }

    private void ensureContext(ClientSocket socket, User user) throws Exception {
        if (socket == null || !socket.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }
    }

    public boolean isVisible(Notification notification, NotificationScope scope) {
        return presentation(notification).scopes().contains(scope);
    }

    public NotificationPresentation presentation(Notification notification) {
        return PRESENTATIONS.getOrDefault(typeOf(notification), DEFAULT_PRESENTATION);
    }

    public NotificationAction actionFor(Notification notification, NotificationScope scope) {
        NotificationPresentation presentation = presentation(notification);
        return scope == NotificationScope.SELLER ? presentation.sellerAction() : presentation.bidderAction();
    }

    public String containerStyle(Notification notification) {
        if (notification != null && !notification.isRead()) {
            return presentation(notification).unreadContainerStyle();
        }
        return READ_CONTAINER_STYLE;
    }

    private String typeOf(Notification notification) {
        return notification == null || notification.getType() == null
                ? ""
                : notification.getType().toUpperCase();
    }
}
