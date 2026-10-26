package com.scc.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates every table in the approved ERD if it does not exist yet. */
public class DatabaseInitializer {

    private static final String USERS = """
            CREATE TABLE IF NOT EXISTS users (
                user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username      TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                full_name     TEXT,
                role          TEXT NOT NULL
                              CHECK (UPPER(role) IN ('ADMIN', 'REGISTRAR', 'STUDENT')),
                status        TEXT NOT NULL DEFAULT 'PENDING',
                date_created  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String STUDENTS = """
            CREATE TABLE IF NOT EXISTS students (
                student_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id        INTEGER NOT NULL UNIQUE,
                student_number TEXT NOT NULL UNIQUE,
                first_name     TEXT,
                last_name      TEXT,
                birthdate      TEXT,
                gender         TEXT,
                address        TEXT,
                contact_number TEXT,
                email          TEXT,
                program        TEXT,
                year_level     INTEGER,
                status         TEXT,
                FOREIGN KEY (user_id) REFERENCES users (user_id)
            )
            """;

    private static final String COURSES = """
            CREATE TABLE IF NOT EXISTS courses (
                course_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                course_code  TEXT NOT NULL UNIQUE,
                course_title TEXT,
                units        INTEGER,
                program      TEXT,
                year_level   INTEGER,
                semester     TEXT,
                schedule     TEXT,
                slots        INTEGER,
                status       TEXT
            )
            """;

    private static final String ENROLLMENTS = """
            CREATE TABLE IF NOT EXISTS enrollments (
                enrollment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id    INTEGER NOT NULL,
                course_id     INTEGER NOT NULL,
                school_year   TEXT,
                semester      TEXT,
                date_enrolled TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status        TEXT,
                FOREIGN KEY (student_id) REFERENCES students (student_id),
                FOREIGN KEY (course_id)  REFERENCES courses (course_id)
            )
            """;

    private static final String ACTIVITY_LOGS = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                log_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id        INTEGER,
                action         TEXT,
                table_affected TEXT,
                record_id      INTEGER,
                description    TEXT,
                log_datetime   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users (user_id)
            )
            """;

    public static boolean initialize() {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            // Parent tables first, then the tables that reference them.
            statement.execute(USERS);
            migrateUsersRoleCheck(connection);
            statement.execute(STUDENTS);
            statement.execute(COURSES);
            statement.execute(ENROLLMENTS);
            statement.execute(ACTIVITY_LOGS);

            System.out.println("Database initialized successfully.");
            return true;

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Older databases were created with a case-sensitive role CHECK ('Admin', ...),
     * which rejects the upper-case roles written by
     * UserDao.updateUserRoleAndStatus.
     * SQLite cannot alter a CHECK constraint, so the users table is rebuilt once
     * (SQLite's documented "create new, copy, drop old, rename" procedure).
     */
    private static void migrateUsersRoleCheck(Connection connection) throws SQLException {
        String currentSql = null;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'users'")) {
            if (result.next()) {
                currentSql = result.getString(1);
            }
        }

        if (currentSql == null || currentSql.toUpperCase().contains("UPPER(ROLE)")) {
            return; // nothing to migrate
        }

        String newTable = USERS.replace("CREATE TABLE IF NOT EXISTS users", "CREATE TABLE users_new");

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF"); // must be set outside a transaction
            connection.setAutoCommit(false);
            try {
                statement.execute("DROP TABLE IF EXISTS users_new");
                statement.execute(newTable);
                statement.execute("""
                        INSERT INTO users_new (user_id, username, password_hash, full_name, role, status, date_created)
                        SELECT user_id, username, password_hash, full_name, role, status, date_created FROM users
                        """);
                statement.execute("DROP TABLE users");
                statement.execute("ALTER TABLE users_new RENAME TO users");
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
                statement.execute("PRAGMA foreign_keys = ON");
            }
        }
    }
}
