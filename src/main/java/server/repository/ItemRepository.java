package server.repository;

import server.model.Item;

public interface ItemRepository {
    void saveItem(Item item) throws Exception;
}
