package server.repository;

import server.model.User;

import java.util.List;

public interface UserRepository {
    User getUserById(String userId) throws Exception;

    User getUserByUsername(String username) throws Exception;

    User getUserByEmail(String email) throws Exception;

    List<User> getAllUsers() throws Exception;

    void saveUser(User user) throws Exception;

    void registerUser(User user) throws Exception;
}
