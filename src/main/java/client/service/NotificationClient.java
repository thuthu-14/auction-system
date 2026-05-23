package client.service;

import client.network.ClientSocket;
import common.Message;
import server.model.Notification;
import server.model.User;

public interface NotificationClient {
    Message fetchNotifications(ClientSocket socket, User user) throws Exception;

    Message markAllRead(ClientSocket socket, User user, String scope) throws Exception;

    Message markAllRead(ClientSocket socket, User user, NotificationClientService.NotificationScope scope) throws Exception;

    Message fetchAuctionDetail(ClientSocket socket, String auctionId) throws Exception;

    boolean isVisible(Notification notification, NotificationClientService.NotificationScope scope);

    NotificationClientService.NotificationPresentation presentation(Notification notification);

    NotificationClientService.NotificationAction actionFor(Notification notification, NotificationClientService.NotificationScope scope);

    String containerStyle(Notification notification);
}
