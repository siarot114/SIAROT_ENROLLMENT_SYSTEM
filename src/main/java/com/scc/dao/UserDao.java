package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final ActivityLogDao activityLogDao;

    public UserDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    private static final String COLUMNS =
            "user_id, username, password_hash, full_name, role, status, date_created";

    /** Inserts a new account. Status defaults to PENDING in the table. */
    public boolean register(String username, String passwordHash, String fullName, String role) {
        String sql = """
                INSERT INTO users (username, password_hash, full_name, role)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.setString(3, fullName);
            statement.setString(4, role);
            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }

        } catch (SQLException e) {
            System.err.println("Lookup error: " + e.getMessage());
            return false;
        }
    }

    /** Returns the matching user, or null if the credentials are wrong. */
    public User login(String username, String passwordHash) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ? AND password_hash = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, passwordHash);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapUser(result);
                }
            }

        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
        }

        return null;
    }

    /** All registered accounts, regardless of role or status. */
    public List<User> getAllUsers() {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY user_id";
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                users.add(mapUser(result));
            }

        } catch (SQLException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }

        return users;
    }

    /** All accounts awaiting approval, across every role. */
    public List<User> getPendingUsers() {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE status = ? ORDER BY user_id";
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "PENDING");

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    users.add(mapUser(result));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error loading pending users: " + e.getMessage());
        }

        return users;
    }

    /**
     * Changes a user's status from PENDING to APPROVED and records USER_APPROVED.
     * Returns true if a row was updated.
     */
    public boolean approveUser(int adminId, int userId) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ? AND status = ?";
        boolean updated;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "APPROVED");
            statement.setInt(2, userId);
            statement.setString(3, "PENDING");

            updated = statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Approval error: " + e.getMessage());
            return false;
        }

        if (updated) {
            activityLogDao.logAction(adminId, "USER_APPROVED",
                    "Admin " + adminId + " approved account for User ID " + userId);
        }
        return updated;
    }

    /**
     * Updates a user's role and account status (both upper-cased) and records USER_UPDATED,
     * including the previous values. Returns true if a row was updated.
     */
    public boolean updateUserRoleAndStatus(int adminId, int userId, String newRole, String newStatus) {
        String selectSql = "SELECT role, status FROM users WHERE user_id = ?";
        String updateSql = "UPDATE users SET role = ?, status = ? WHERE user_id = ?";
        String oldRole;
        String oldStatus;

        try (Connection connection = DatabaseConnection.getConnection()) {

            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setInt(1, userId);
                try (ResultSet result = select.executeQuery()) {
                    if (!result.next()) {
                        return false;
                    }
                    oldRole = result.getString("role").toUpperCase();
                    oldStatus = result.getString("status").toUpperCase();
                }
            }

            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, newRole.toUpperCase());
                update.setString(2, newStatus.toUpperCase());
                update.setInt(3, userId);

                if (update.executeUpdate() == 0) {
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("Update error: " + e.getMessage());
            return false;
        }

        activityLogDao.logAction(adminId, "USER_UPDATED",
                "Admin " + adminId + " changed role of User " + userId + " to " + newRole.toUpperCase()
                        + " (was " + oldRole + "), status " + newStatus.toUpperCase()
                        + " (was " + oldStatus + ")");
        return true;
    }

    private User mapUser(ResultSet result) throws SQLException {
        return new User(
                result.getInt("user_id"),
                result.getString("username"),
                result.getString("password_hash"),
                result.getString("full_name"),
                result.getString("role"),
                result.getString("status"),
                result.getString("date_created")
        );
    }
}
