package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

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

    /** Changes a user's status from PENDING to APPROVED. Returns true if a row was updated. */
    public boolean approveUser(int userId) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ? AND status = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "APPROVED");
            statement.setInt(2, userId);
            statement.setString(3, "PENDING");

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Approval error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates a user's role and account status. Both values are upper-cased before
     * they are sent to the database. Returns true if a row was updated.
     */
    public boolean updateUserRoleAndStatus(int userId, String newRole, String newStatus) {
        String sql = "UPDATE users SET role = ?, status = ? WHERE user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, newRole.toUpperCase());
            statement.setString(2, newStatus.toUpperCase());
            statement.setInt(3, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Update error: " + e.getMessage());
            return false;
        }
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
