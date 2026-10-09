
package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {

    public boolean register(String username, String password, String roles) {

        if (!roles.equals("TEACHER") && !roles.equals("STUDENT")) {
            System.out.println("Invalid role.");
            return false;
        }

        String sql = """
                INSERT INTO users (username, password, roles)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, roles);

            statement.executeUpdate();

            System.out.println("Registration successful!");
            System.out.println("Account status: PENDING");
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE")) {
                System.out.println("Username already exists.");
            } else {
                System.out.println("Registration error: " + e.getMessage());
            }
            return false;
        }
    }

    public User login(String username, String password) {

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE username = ? AND password = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return new User(
                            result.getInt("id"),
                            result.getString("username"),
                            result.getString("password"),
                            result.getString("roles"),
                            result.getString("status")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }

        return null;
    }
}
