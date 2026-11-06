package com.scc;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.CourseDao;
import com.scc.dao.DatabaseInitializer;
import com.scc.dao.EnrollmentDao;
import com.scc.dao.StudentDao;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;
import com.scc.util.PasswordUtil;

import java.util.Scanner;

/** Single entry point. Contains no menu, login, or SQL logic. */
public class Main {

    public static void main(String[] args) {
        // 1. Initialize database
        if (!DatabaseInitializer.initialize()) {
            System.err.println("Cannot start application without a database.");
            return;
        }

        // 2. Instantiate DAOs & utilities (one shared instance of each)
        ActivityLogDao activityLogDao = new ActivityLogDao();
        UserDao userDao = new UserDao(activityLogDao);
        StudentDao studentDao = new StudentDao(activityLogDao);
        CourseDao courseDao = new CourseDao(activityLogDao);
        EnrollmentDao enrollmentDao = new EnrollmentDao(activityLogDao);
        PasswordUtil passwordUtil = new PasswordUtil();

        // 3. Delegate to Authentication
        try (Scanner scanner = new Scanner(System.in)) {
            Authentication authentication = new Authentication(userDao, activityLogDao, studentDao, courseDao,
                    enrollmentDao, passwordUtil, scanner);
            authentication.start();
        }
    }
}
