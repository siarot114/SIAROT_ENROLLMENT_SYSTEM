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
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id     INTEGER NOT NULL,
                action_type TEXT NOT NULL,
                description TEXT NOT NULL,
                timestamp   TIMESTAMP DEFAULT (datetime('now', 'localtime')),
                FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
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
            migrateLegacyActivityLogs(connection);
            statement.execute(ACTIVITY_LOGS);

            System.out.println("Database initialized successfully.");
            return true;

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Earlier versions created activity_logs with different columns
     * (log_id, action, table_affected, record_id, log_datetime). CREATE TABLE IF NOT EXISTS
     * would silently keep that old table, so it is detected here and replaced.
     * An empty legacy table is dropped; one that holds rows is renamed to
     * activity_logs_legacy so no history is lost.
     */
    private static void migrateLegacyActivityLogs(Connection connection) throws SQLException {
        boolean exists = false;
        boolean current = false;

        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(activity_logs)")) {
            while (columns.next()) {
                exists = true;
                if ("action_type".equalsIgnoreCase(columns.getString("name"))) {
                    current = true;
                }
            }
        }

        if (!exists || current) {
            return;
        }

        int rows = 0;
        try (Statement statement = connection.createStatement();
             ResultSet count = statement.executeQuery("SELECT COUNT(*) FROM activity_logs")) {
            if (count.next()) {
                rows = count.getInt(1);
            }
        }

        try (Statement statement = connection.createStatement()) {
            if (rows == 0) {
                statement.execute("DROP TABLE activity_logs");
            } else {
                statement.execute("DROP TABLE IF EXISTS activity_logs_legacy");
                statement.execute("ALTER TABLE activity_logs RENAME TO activity_logs_legacy");
            }
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
