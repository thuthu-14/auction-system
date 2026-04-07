package server.model;

import common.UserRole;
import java.io.Serializable;
import java.util.*;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String userId;
    protected String username;
    protected String password;
    protected String email;
    protected double wallet;
    protected UserRole role;
    protected long createdAt;
    protected boolean active;

    public User(String userId, String username, String password,
                String email, UserRole role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
        this.wallet = 0;
        this.createdAt = System.currentTimeMillis();
        this.active = true;
    }

    public User() {}

    public abstract String getDashboardTitle();
    public abstract List<String> getAvailableActions();

    public synchronized void addFunds(double amount) {
        if (amount > 0) {
            this.wallet += amount;
        }
    }

    public synchronized boolean deductFunds(double amount) {
        if (amount > 0 && this.wallet >= amount) {
            this.wallet -= amount;
            return true;
        }
        return false;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getWallet() {
        return wallet;
    }

    public void setWallet(double wallet) {
        this.wallet = wallet;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "User{" + "userId='" + userId + '\'' + ", username='" + username + '\'' + '}';
    }
}
