package client.service;

import client.network.MessageTransport;
import common.Message;
import server.model.User;

import java.util.List;
import java.util.Map;

public interface AddAuctionProductClient {
    List<String> categoryLabels();

    AddAuctionProductClientService.CategoryFormConfig findCategoryForm(String category);

    Map<String, Object> buildPayload(MessageTransport transport, User user, String name, String description,
                                     String category, List<String> imagePaths,
                                     AddAuctionProductClientService.CategoryFieldReader fields) throws Exception;

    Message publish(MessageTransport transport, User user, Map<String, Object> payload) throws Exception;
}
