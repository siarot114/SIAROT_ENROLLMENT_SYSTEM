package com.scc.dao;

import com.scc.model.Course;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CourseDao {

    private final ActivityLogDao activityLogDao;

    public CourseDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    /** Adds a course (status OPEN) and logs COURSE_CREATED. */
    public boolean addCourse(int actorId, String courseCode, String courseTitle, int units) {
        String sql = "INSERT INTO courses (course_code, course_title, units, status) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, courseCode);
            statement.setString(2, courseTitle);
            statement.setInt(3, units);
            statement.setString(4, "OPEN");
            statement.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Course error: " + e.getMessage());
            return false;
        }

        activityLogDao.logAction(actorId, "COURSE_CREATED",
                "User " + actorId + " created course " + courseCode + " - " + courseTitle
                        + " (" + units + " units)");
        return true;
    }

    public List<Course> getAllCourses() {
        String sql = "SELECT course_id, course_code, course_title, units FROM courses ORDER BY course_id";
        List<Course> courses = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet r = statement.executeQuery()) {

            while (r.next()) {
                courses.add(new Course(r.getInt("course_id"), r.getString("course_code"),
                        r.getString("course_title"), r.getInt("units")));
            }

        } catch (SQLException e) {
            System.err.println("Error loading courses: " + e.getMessage());
        }

        return courses;
    }
}
