package server.service;

import server.model.RegularUser;
import server.model.User;
import server.repository.JsonUserRepository;
import server.repository.UserRepository;
import util.LoggerUtil;

import java.io.IOException;
import java.util.List;

public class UserService {
    private static final UserService DEFAULT = new UserService(new JsonUserRepository());

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getById(String userId) throws Exception {
        return userRepository.getUserById(userId);
    }

    public User getByUsername(String username) throws Exception {
        return userRepository.getUserByUsername(username);
    }

    public List<User> getAll() throws Exception {
        return userRepository.getAllUsers();
    }

    public void setUserStatus(String userId, boolean active) throws Exception {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new IOException("User not found");
        }
        user.setActive(active);
        userRepository.saveUser(user);
        LoggerUtil.info("User status updated: " + user.getUsername() + " active=" + active);
    }

    public void addFundsToUser(String userId, double amount) throws Exception {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new IOException("User not found");
        }
        if (amount <= 0) {
            throw new IOException("Amount must be greater than 0");
        }
        user.addFunds(amount);
        userRepository.saveUser(user);
        LoggerUtil.info("Funds added: " + amount + " to user " + user.getUsername());
    }

    public void upgradeUserToSeller(String userId, String shopName, String shopPhone,
                                    String shopAddress, String shopEmail) throws Exception {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new IOException("User not found");
        }
        if (!(user instanceof RegularUser regularUser)) {
            throw new IOException("Only RegularUser can be upgraded to Seller");
        }
        if (regularUser.isSeller()) {
            throw new IOException("User is already a Seller");
        }
        regularUser.upgradeSeller(shopName, shopPhone, shopAddress, shopEmail);
        userRepository.saveUser(regularUser);
        LoggerUtil.info("User upgraded to Seller: " + user.getUsername() + " - Shop: " + shopName);
    }

    public boolean isSeller(String userId) throws Exception {
        User user = userRepository.getUserById(userId);
        return user instanceof RegularUser regularUser && regularUser.isSeller();
    }

    public static User getUserById(String userId) throws IOException, ClassNotFoundException {
        return call(() -> DEFAULT.getById(userId));
    }

    public static User getUserByUsername(String username) throws IOException, ClassNotFoundException {
        return call(() -> DEFAULT.getByUsername(username));
    }

    public static List<User> getAllUsers() throws IOException, ClassNotFoundException {
        return call(() -> DEFAULT.getAll());
    }

    public static void addFunds(String userId, double amount) throws IOException, ClassNotFoundException {
        run(() -> DEFAULT.addFundsToUser(userId, amount));
    }

    public static void banUser(String userId) throws IOException, ClassNotFoundException {
        updateUserStatus(userId, false);
    }

    public static void updateUserStatus(String userId, boolean active) throws IOException, ClassNotFoundException {
        run(() -> DEFAULT.setUserStatus(userId, active));
    }

    public static void unbanUser(String userId) throws IOException, ClassNotFoundException {
        updateUserStatus(userId, true);
    }

    public static void upgradeSeller(String userId, String shopName, String shopPhone,
                                     String shopAddress, String shopEmail)
            throws IOException, ClassNotFoundException {
        run(() -> DEFAULT.upgradeUserToSeller(userId, shopName, shopPhone, shopAddress, shopEmail));
    }

    public static boolean isUserSeller(String userId) throws IOException, ClassNotFoundException {
        return call(() -> DEFAULT.isSeller(userId));
    }

    private static <T> T call(ServiceCall<T> call) throws IOException, ClassNotFoundException {
        try {
            return call.execute();
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static void run(ServiceRun run) throws IOException, ClassNotFoundException {
        call(() -> {
            run.execute();
            return null;
        });
    }

    @FunctionalInterface
    private interface ServiceCall<T> {
        T execute() throws Exception;
    }

    @FunctionalInterface
    private interface ServiceRun {
        void execute() throws Exception;
    }
}
