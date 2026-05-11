package server.repository;

import server.model.User;
import server.storage.UserDAO;

import java.util.List;

public class JsonUserRepository implements UserRepository {
    @Override
    public User getUserById(String userId) throws Exception {
        return UserDAO.getUserById(userId);
    }

    @Override
    public User getUserByUsername(String username) throws Exception {
        return UserDAO.getUserByUsername(username);
    }

    @Override
    public User getUserByEmail(String email) throws Exception {
        return UserDAO.getUserByEmail(email);
    }

    @Override
    public List<User> getAllUsers() throws Exception {
        return UserDAO.getAllUsers();
    }

    @Override
    public void saveUser(User user) throws Exception {
        UserDAO.saveUser(user);
    }

    @Override
    public void registerUser(User user) throws Exception {
        UserDAO.registerUser(user);
    }
}
