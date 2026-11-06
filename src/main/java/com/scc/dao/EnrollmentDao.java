package com.scc.dao;

import com.scc.model.Enrollment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Core business process (enrolling a student) and its lifecycle (ENROLLED -> DROPPED / COMPLETED). */
public class EnrollmentDao {

    private static final String SELECT_JOINED = """
            SELECT e.enrollment_id, s.student_number, s.first_name || ' ' || s.last_name AS student_name,
                   c.course_code, e.school_year, e.semester, e.status
            FROM enrollments e
            JOIN students s ON s.student_id = e.student_id
            JOIN courses c ON c.course_id = e.course_id
            """;

    private final ActivityLogDao activityLogDao;

    public EnrollmentDao(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    /** True if the student already has an active (ENROLLED) record for this course and term. */
    public boolean isAlreadyEnrolled(int studentId, int courseId, String schoolYear, String semester) {
        String sql = """
                SELECT 1 FROM enrollments
                WHERE student_id = ? AND course_id = ? AND school_year = ? AND semester = ? AND status = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, studentId);
            statement.setInt(2, courseId);
            statement.setString(3, schoolYear);
            statement.setString(4, semester);
            statement.setString(5, "ENROLLED");

            try (ResultSet r = statement.executeQuery()) {
                return r.next();
            }

        } catch (SQLException e) {
            System.err.println("Lookup error: " + e.getMessage());
            return false;
        }
    }

    /** Enrolls a student in a course and logs ENROLLMENT_CREATED. */
    public boolean enrollStudent(int actorId, int studentId, int courseId, String schoolYear, String semester) {
        String insertSql = """
                INSERT INTO enrollments (student_id, course_id, school_year, semester, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        String infoSql = """
                SELECT s.student_number, s.first_name || ' ' || s.last_name AS student_name, c.course_code
                FROM students s, courses c WHERE s.student_id = ? AND c.course_id = ?
                """;
        String studentNumber;
        String studentName;
        String courseCode;
        int enrollmentId = -1;

        try (Connection connection = DatabaseConnection.getConnection()) {

            try (PreparedStatement info = connection.prepareStatement(infoSql)) {
                info.setInt(1, studentId);
                info.setInt(2, courseId);
                try (ResultSet r = info.executeQuery()) {
                    if (!r.next()) {
                        return false;
                    }
                    studentNumber = r.getString("student_number");
                    studentName = r.getString("student_name");
                    courseCode = r.getString("course_code");
                }
            }

            try (PreparedStatement insert = connection.prepareStatement(insertSql,
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, studentId);
                insert.setInt(2, courseId);
                insert.setString(3, schoolYear);
                insert.setString(4, semester);
                insert.setString(5, "ENROLLED");
                insert.executeUpdate();

                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        enrollmentId = keys.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Enrollment error: " + e.getMessage());
            return false;
        }

        activityLogDao.logAction(actorId, "ENROLLMENT_CREATED",
                "Enrolled " + studentName + " (" + studentNumber + ") in " + courseCode + " - "
                        + schoolYear + " " + semester + " (Enrollment #" + enrollmentId + ")");
        return true;
    }

    /** Changes an enrollment's status and logs ENROLLMENT_UPDATED with the old and new value. */
    public boolean updateEnrollmentStatus(int actorId, int enrollmentId, String newStatus) {
        String selectSql = SELECT_JOINED + " WHERE e.enrollment_id = ?";
        String updateSql = "UPDATE enrollments SET status = ? WHERE enrollment_id = ?";
        String oldStatus;
        String studentNumber;
        String courseCode;

        try (Connection connection = DatabaseConnection.getConnection()) {

            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setInt(1, enrollmentId);
                try (ResultSet r = select.executeQuery()) {
                    if (!r.next()) {
                        return false;
                    }
                    oldStatus = r.getString("status");
                    studentNumber = r.getString("student_number");
                    courseCode = r.getString("course_code");
                }
            }

            try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                update.setString(1, newStatus);
                update.setInt(2, enrollmentId);
                if (update.executeUpdate() == 0) {
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("Status update error: " + e.getMessage());
            return false;
        }

        activityLogDao.logAction(actorId, "ENROLLMENT_UPDATED",
                "Updated Enrollment #" + enrollmentId + " (" + studentNumber + ", " + courseCode
                        + ") status from " + oldStatus + " to " + newStatus);
        return true;
    }

    public List<Enrollment> getAllEnrollments() {
        String sql = SELECT_JOINED + " ORDER BY e.enrollment_id";
        List<Enrollment> enrollments = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet r = statement.executeQuery()) {

            while (r.next()) {
                enrollments.add(new Enrollment(r.getInt("enrollment_id"), r.getString("student_number"),
                        r.getString("student_name"), r.getString("course_code"),
                        r.getString("school_year"), r.getString("semester"), r.getString("status")));
            }

        } catch (SQLException e) {
            System.err.println("Error loading enrollments: " + e.getMessage());
        }

        return enrollments;
    }
}
