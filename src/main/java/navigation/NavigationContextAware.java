package navigation;

import client.network.ClientSocket;
import server.model.User;

public interface NavigationContextAware {
    void setUserData(User user, ClientSocket socket);
}
