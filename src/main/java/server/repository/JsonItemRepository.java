package server.repository;

import server.model.Item;
import server.storage.ItemDAO;

public class JsonItemRepository implements ItemRepository {
    @Override
    public void saveItem(Item item) throws Exception {
        ItemDAO.saveItem(item);
    }
}
