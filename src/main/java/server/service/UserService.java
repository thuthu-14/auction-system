package server.service;

import server.model.RegularUser;
import server.model.User;
import server.repository.SqlUserRepository;
import server.repository.UserRepository;
import util.LoggerUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {
    private static final UserService DEFAULT = new UserService(new SqlUserRepository());
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

    public List<User> getAllForAdmin(User currentUser) throws Exception {
        requireAdmin(currentUser);
        return getAll();
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

    public void setUserStatusForAdmin(User currentUser, Object data) throws Exception {
        requireAdmin(currentUser);
        SetUserStatusRequest request = SetUserStatusRequest.from(data);
        setUserStatus(request.userId(), request.active());
    }

    public void banUserAsAdmin(User currentUser, Object data) throws Exception {
        requireAdmin(currentUser);
        String userId = RequestPayloadUtil.text(data);
        if (userId == null) {
            throw new IOException("User not found");
        }
        setUserStatus(userId, false);
    }

    public RegularUser upgradeUserToSellerWithPassword(String userId,
                                                       String password,
                                                       String shopName,
                                                       String shopPhone,
                                                       String shopAddress,
                                                       String shopEmail) throws Exception {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new IOException("Không tìm thấy tài khoản!");
        }
        if (!(user instanceof RegularUser regularUser)) {
            throw new IOException("Only RegularUser can be upgraded to Seller");
        }
        if (regularUser.isSeller()) {
            throw new IOException("User is already a Seller");
        }
        if (password == null || password.isBlank() || !password.equals(user.getPassword())) {
            throw new IOException("Mật khẩu không đúng!");
        }

        regularUser.upgradeSeller(shopName, shopPhone, shopAddress, shopEmail);
        userRepository.saveUser(regularUser);
        LoggerUtil.info("User upgraded to Seller: " + user.getUsername() + " - Shop: " + shopName);
        return regularUser;
    }

    public RegularUser upgradeUserToSellerWithPassword(User currentUser, Object data) throws Exception {
        if (!(currentUser instanceof RegularUser)) {
            throw new IOException("Bạn phải đăng nhập");
        }
        UpgradeSellerRequest request = UpgradeSellerRequest.from(data);
        return upgradeUserToSellerWithPassword(
                currentUser.getUserId(),
                request.password(),
                request.shopName(),
                request.phone(),
                request.address(),
                request.email()
        );
    }

    public User updateProfile(User currentUser, Object data) throws Exception {
        if (!(currentUser instanceof RegularUser)) {
            throw new IOException("Bạn phải đăng nhập bằng tài khoản người dùng");
        }
        User user = userRepository.getUserById(currentUser.getUserId());
        if (!(user instanceof RegularUser regularUser)) {
            throw new IOException("Không tìm thấy tài khoản");
        }

        UpdateProfileRequest request = UpdateProfileRequest.from(data);
        if (request.name().isBlank() || request.phone().isBlank() || request.address().isBlank()) {
            throw new IOException("Vui lòng cập nhật đầy đủ họ tên, số điện thoại và địa chỉ");
        }
        if (!request.phone().matches("\\d{10,11}")) {
            throw new IOException("Số điện thoại phải gồm 10-11 chữ số");
        }

        regularUser.setUsername(request.name());
        regularUser.setShopName(request.name());
        regularUser.setShopPhone(request.phone());
        regularUser.setShopAddress(request.address());
        regularUser.setShopEmail(nonBlank(regularUser.getShopEmail(), regularUser.getEmail()));
        if (!request.password().isBlank()) {
            regularUser.setPassword(request.password());
        }
        userRepository.saveUser(regularUser);
        LoggerUtil.info("User profile updated: " + regularUser.getUsername());
        return regularUser;
    }

    public Map<String, String> getSellerContact(String sellerId) throws Exception {
        String resolvedSellerId = RequestPayloadUtil.text(sellerId);
        if (resolvedSellerId == null) {
            throw new IOException("Không tìm thấy người bán");
        }

        User seller = userRepository.getUserById(resolvedSellerId);
        if (seller == null) {
            throw new IOException("Không tìm thấy người bán");
        }

        Map<String, String> contact = new HashMap<>();
        contact.put("name", seller.getUsername());
        contact.put("email", seller.getEmail());
        contact.put("phone", "Chưa cập nhật");
        contact.put("address", "Chưa cập nhật");

        if (seller instanceof RegularUser regularSeller) {
            contact.put("name", nonBlank(regularSeller.getShopName(), seller.getUsername()));
            contact.put("email", nonBlank(regularSeller.getShopEmail(), seller.getEmail()));
            contact.put("phone", nonBlank(regularSeller.getShopPhone(), "Chưa cập nhật"));
            contact.put("address", nonBlank(regularSeller.getShopAddress(), "Chưa cập nhật"));
        }
        return contact;
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

    public static List<User> getAllUsersForAdmin(User currentUser) throws IOException, ClassNotFoundException {
        return call(() -> DEFAULT.getAllForAdmin(currentUser));
    }

    public static void addFunds(String userId, double amount) throws IOException, ClassNotFoundException {
        run(() -> DEFAULT.addFundsToUser(userId, amount));
    }

    public static void banUser(String userId) throws IOException, ClassNotFoundException {
        updateUserStatus(userId, false);
    }

    public static void banUserForAdmin(User currentUser, Object data) throws IOException, ClassNotFoundException {
        run(() -> DEFAULT.banUserAsAdmin(currentUser, data));
    }

    public static void updateUserStatus(String userId, boolean active) throws IOException, ClassNotFoundException {
        run(() -> DEFAULT.setUserStatus(userId, active));
    }

    public static void updateUserStatusForAdmin(User currentUser, Object data)
            throws IOException, ClassNotFoundException {
        run(() -> DEFAULT.setUserStatusForAdmin(currentUser, data));
    }

    public static void unbanUser(String userId) throws IOException, ClassNotFoundException {
        updateUserStatus(userId, true);
    }

    public static void upgradeSeller(String userId, String shopName, String shopPhone,
                                     String shopAddress, String shopEmail)
            throws IOException, ClassNotFoundException {
        run(() -> DEFAULT.upgradeUserToSeller(userId, shopName, shopPhone, shopAddress, shopEmail));
    }

    public static RegularUser upgradeSellerWithPassword(String userId,
                                                        String password,
                                                        String shopName,
                                                        String shopPhone,
                                                        String shopAddress,
                                                        String shopEmail)
            throws IOException, ClassNotFoundException {
        return call(() -> DEFAULT.upgradeUserToSellerWithPassword(
                userId, password, shopName, shopPhone, shopAddress, shopEmail
        ));
    }

    public static RegularUser upgradeSellerWithPassword(User currentUser, Object data)
            throws IOException, ClassNotFoundException {
        return call(() -> DEFAULT.upgradeUserToSellerWithPassword(currentUser, data));
    }

    public static User updateUserProfile(User currentUser, Object data)
            throws IOException, ClassNotFoundException {
        return call(() -> DEFAULT.updateProfile(currentUser, data));
    }

    public static Map<String, String> getSellerContactById(String sellerId)
            throws IOException, ClassNotFoundException {
        return call(() -> DEFAULT.getSellerContact(sellerId));
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

    private void requireAdmin(User user) throws IOException {
        if (!(user instanceof server.model.Admin)) {
            throw new IOException("Chi Admin co quyen thuc hien thao tac nay");
        }
    }

    private String nonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
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
