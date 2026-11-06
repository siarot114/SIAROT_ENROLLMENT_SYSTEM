package com.scc.dao;

import com.scc.model.Student;
import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StudentDao {

    private final ActivityLogDao activityLogDao;

    public StudentDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    /** Approved STUDENT accounts that do not have a students record yet. */
    public List<User> getUsersWithoutProfile() {
        String sql = """
                SELECT user_id, username, password_hash, full_name, role, status, date_created
                FROM users
                WHERE UPPER(role) = ? AND status = ?
                  AND user_id NOT IN (SELECT user_id FROM students)
                ORDER BY user_id
                """;
        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "STUDENT");
            statement.setString(2, "APPROVED");

            try (ResultSet r = statement.executeQuery()) {
                while (r.next()) {
                    users.add(new User(r.getInt("user_id"), r.getString("username"),
                            r.getString("password_hash"), r.getString("full_name"),
                            r.getString("role"), r.getString("status"), r.getString("date_created")));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error loading student accounts: " + e.getMessage());
        }

        return users;
    }

    /** Creates the students record for an approved student account and logs STUDENT_CREATED. */
    public boolean createProfile(int actorId, int userId, String studentNumber,
                                 String firstName, String lastName, String program, int yearLevel) {
        String sql = """
                INSERT INTO students (user_id, student_number, first_name, last_name, program, year_level, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, studentNumber);
            statement.setString(3, firstName);
            statement.setString(4, lastName);
            statement.setString(5, program);
            statement.setInt(6, yearLevel);
            statement.setString(7, "ACTIVE");
            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Student profile error: " + e.getMessage());
            return false;
        }

        activityLogDao.logAction(actorId, "STUDENT_CREATED",
                "User " + actorId + " created student record " + studentNumber + " ("
                        + firstName + " " + lastName + ") for User ID " + userId);
        return true;
    }

    public List<Student> getAllStudents() {
        String sql = """
                SELECT student_id, user_id, student_number, first_name, last_name, program, year_level
                FROM students ORDER BY student_id
                """;
        List<Student> students = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet r = statement.executeQuery()) {

            while (r.next()) {
                students.add(new Student(r.getInt("student_id"), r.getInt("user_id"),
                        r.getString("student_number"), r.getString("first_name"),
                        r.getString("last_name"), r.getString("program"), r.getInt("year_level")));
            }

        } catch (SQLException e) {
            System.err.println("Error loading students: " + e.getMessage());
        }

        return students;
    }
}
