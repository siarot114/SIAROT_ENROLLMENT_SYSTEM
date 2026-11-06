package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** All SQL for the activity_logs audit trail lives here. Every query is parameterized. */
public class ActivityLogDao {

    private static final String COLUMNS = "id, user_id, action_type, description, timestamp";

    /**
     * Records one audit entry. A logging failure must never break the action being
     * logged, so errors are reported to stderr and false is returned instead of thrown.
     */
    public boolean logAction(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action_type, description) VALUES (?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, actionType);
            statement.setString(3, description);
            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Audit log error: " + e.getMessage());
            return false;
        }
    }

    /** Every log entry, newest first. id DESC breaks ties between entries in the same second. */
    public List<ActivityLog> getAllLogs() {
        String sql = "SELECT " + COLUMNS + " FROM activity_logs ORDER BY timestamp DESC, id DESC";
        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                logs.add(mapLog(result));
            }

        } catch (SQLException e) {
            System.err.println("Error loading audit logs: " + e.getMessage());
        }

        return logs;
    }

    /** Log entries created by one user, newest first. */
    public List<ActivityLog> getLogsByUserId(int userId) {
        String sql = "SELECT " + COLUMNS + " FROM activity_logs WHERE user_id = ? "
                + "ORDER BY timestamp DESC, id DESC";
        List<ActivityLog> logs = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    logs.add(mapLog(result));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error loading audit logs: " + e.getMessage());
        }

        return logs;
    }

    /** Deletes every log entry. @return the number of rows removed, or -1 on error. */
    public int clearAllLogs() {
        String sql = "DELETE FROM activity_logs";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            return statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error clearing audit logs: " + e.getMessage());
            return -1;
        }
    }

    private ActivityLog mapLog(ResultSet result) throws SQLException {
        return new ActivityLog(
                result.getInt("id"),
                result.getInt("user_id"),
                result.getString("action_type"),
                result.getString("description"),
                result.getString("timestamp")
        );
    }
}
