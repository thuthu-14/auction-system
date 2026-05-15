package server.service;

import server.exception.AuthenticationException;
import server.model.RegularUser;
import server.model.User;
import server.repository.SqlUserRepository;
import server.repository.UserRepository;
import util.LoggerUtil;
import util.ValidationUtil;

import java.io.IOException;

public class AuthService {
    private static final AuthService DEFAULT = new AuthService(new SqlUserRepository());

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User loginUser(String email, String password)
            throws AuthenticationException, IOException, ClassNotFoundException {

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Email và mật khẩu không được rỗng!");
        }

        User user = loadUserByEmail(email);

        if (user == null) {
            throw new AuthenticationException("Tài khoản không tồn tại!");
        }

        if (!user.getPassword().equals(password)) {
            throw new AuthenticationException("Mật khẩu không đúng!");
        }

        if (!user.isActive()) {
            throw new AuthenticationException("Tài khoản bị khóa!");
        }

        return user;
    }

    public User registerUser(String username, String password, String email)
            throws IOException, ClassNotFoundException, AuthenticationException {

        if (!ValidationUtil.isValidUsername(username)) {
            throw new AuthenticationException("Tên đăng nhập không hợp lệ (3-50 ký tự, chỉ a-z, 0-9, _)");
        }

        if (!ValidationUtil.isValidPassword(password)) {
            throw new AuthenticationException("Mật khẩu phải có ít nhất 6 ký tự");
        }

        if (!ValidationUtil.isValidEmail(email)) {
            throw new AuthenticationException("Email không hợp lệ");
        }

        User existingByEmail = loadUserByEmail(email);
        if (existingByEmail != null) {
            LoggerUtil.error("Registration failed: Email already exists - " + email);
            throw new AuthenticationException("Email đã tồn tại!");
        }

        if (username.equalsIgnoreCase("admin")) {
            LoggerUtil.error("Registration blocked: Attempted to create admin account - " + username);
            throw new AuthenticationException("Không thể tạo tài khoản admin!");
        }

        User existingUser = loadUserByUsername(username);
        if (existingUser != null) {
            LoggerUtil.error("Registration failed: Username already exists - " + username);
            throw new AuthenticationException("Tên đăng nhập đã tồn tại!");
        }

        String userId = "U" + System.currentTimeMillis();
        RegularUser newUser = new RegularUser(userId, username, password, email);
        newUser.setWallet(0);
        newUser.setSeller(false);

        saveNewUser(newUser);
        LoggerUtil.info("User registered: " + username + " (Role: BIDDER)");

        return newUser;
    }

    public static User login(String email, String password)
            throws AuthenticationException, IOException, ClassNotFoundException {
        return DEFAULT.loginUser(email, password);
    }

    public static User register(String username, String password, String email)
            throws IOException, ClassNotFoundException, AuthenticationException {
        return DEFAULT.registerUser(username, password, email);
    }

    private User loadUserByEmail(String email) throws IOException, ClassNotFoundException {
        try {
            return userRepository.getUserByEmail(email);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private User loadUserByUsername(String username) throws IOException, ClassNotFoundException {
        try {
            return userRepository.getUserByUsername(username);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void saveNewUser(User user) throws IOException, ClassNotFoundException {
        try {
            userRepository.registerUser(user);
        } catch (IOException | ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
