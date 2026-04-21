package server.storage;

import server.model.User;
import util.JsonUtil;
import util.SerializationUtil;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;

public class UserDAO {

    public static List<User> getAllUsers() throws IOException, ClassNotFoundException {
        JsonUtil.createFileIfNotExists(DataManager.JSON_USERS);
        List<User> users = JsonUtil.loadListFromJson(DataManager.JSON_USERS, User.class);
        return users != null ? users : new ArrayList<>();
    }

    public static User getUserById(String userId) throws IOException, ClassNotFoundException {
        List<User> users = getAllUsers();
        for (User user : users) {
            if (user.getUserId().equals(userId)) {
                return user;
            }
        }
        return null;
    }

    public static User getUserByUsername(String username) throws IOException, ClassNotFoundException {
        List<User> users = getAllUsers();
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public static User getUserByEmail(String email) throws IOException, ClassNotFoundException {
        List<User> users = getAllUsers();
        for (User user : users) {
            if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }
        return null;
    }


    public static void saveUser(User user) throws IOException, ClassNotFoundException {
        List<User> users = getAllUsers();

        users.removeIf(u -> u.getUserId().equals(user.getUserId()));

        users.add(user);

        JsonUtil.saveToJson(DataManager.JSON_USERS, users);
        SerializationUtil.serialize(DataManager.DAT_USERS, users);

        LoggerUtil.info("User saved: " + user.getUsername());
    }

    public static void registerUser(User user) throws IOException, ClassNotFoundException {
        List<User> users = getAllUsers();
        users.add(user);

        JsonUtil.saveToJson(DataManager.JSON_USERS, users);
        SerializationUtil.serialize(DataManager.DAT_USERS, users);

        LoggerUtil.info("User registered: " + user.getUsername());
    }

    public static void deleteUser(String userId) throws IOException, ClassNotFoundException {
        List<User> users = getAllUsers();
        users.removeIf(u -> u.getUserId().equals(userId));

        JsonUtil.saveToJson(DataManager.JSON_USERS, users);
        SerializationUtil.serialize(DataManager.DAT_USERS, users);

        LoggerUtil.info("User deleted: " + userId);
    }
}
