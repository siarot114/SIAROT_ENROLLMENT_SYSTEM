package com.scc.ui;

import com.scc.dao.CourseDao;
import com.scc.dao.EnrollmentDao;
import com.scc.dao.StudentDao;
import com.scc.model.Course;
import com.scc.model.Enrollment;
import com.scc.model.Student;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

/**
 * Registrar menu: student records, courses, and enrollment transactions.
 * Every write below is audit-logged inside its DAO. No SQL here.
 */
public class RegistrarMenu {

    private static final List<String> STATUSES = List.of("ENROLLED", "DROPPED", "COMPLETED");

    private final StudentDao studentDao;
    private final CourseDao courseDao;
    private final EnrollmentDao enrollmentDao;
    private final Scanner scanner;

    public RegistrarMenu(StudentDao studentDao, CourseDao courseDao, EnrollmentDao enrollmentDao,
                         Scanner scanner) {
        this.studentDao = studentDao;
        this.courseDao = courseDao;
        this.enrollmentDao = enrollmentDao;
        this.scanner = scanner;
    }

    /** @return true if the user chose to exit the application, false for logout. */
    public boolean start(User registrar) {
        while (true) {
            System.out.println("\n=== REGISTRAR MENU ===");
            System.out.println("Welcome, " + registrar.username() + "!");
            System.out.println("Status: " + registrar.status());
            System.out.println("1. Create Student Record");
            System.out.println("2. Add Course");
            System.out.println("3. Enroll Student in Course");
            System.out.println("4. Update Enrollment Status");
            System.out.println("5. View All Enrollments");
            System.out.println("6. Logout");
            System.out.println("7. Exit Application");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> createStudent(registrar);
                case "2" -> addCourse(registrar);
                case "3" -> enrollStudent(registrar);
                case "4" -> updateEnrollmentStatus(registrar);
                case "5" -> printEnrollments(enrollmentDao.getAllEnrollments());
                case "6" -> {
                    System.out.println("Logged out successfully.");
                    return false;
                }
                case "7" -> {
                    return true;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void createStudent(User registrar) {
        System.out.println("\n=== CREATE STUDENT RECORD ===");

        List<User> eligible = studentDao.getUsersWithoutProfile();
        if (eligible.isEmpty()) {
            System.out.println("No approved student accounts are waiting for a student record.");
            return;
        }

        System.out.println("Approved student accounts without a record:");
        for (User u : eligible) {
            System.out.println("  ID " + u.userId() + " - " + u.username() + " (" + u.fullName() + ")");
        }

        int userId = readInt("Enter User ID (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        if (eligible.stream().noneMatch(u -> u.userId() == userId)) {
            System.out.println("User ID " + userId + " is not in the list above.");
            return;
        }

        String studentNumber = readRequired("Student number: ");
        String firstName = readRequired("First name: ");
        String lastName = readRequired("Last name: ");
        String program = readRequired("Program (e.g. BSIT): ");
        int yearLevel = readInt("Year level (1-6): ");
        if (yearLevel < 1 || yearLevel > 6) {
            System.out.println("Year level must be between 1 and 6.");
            return;
        }

        if (studentDao.createProfile(registrar.userId(), userId, studentNumber, firstName, lastName,
                program, yearLevel)) {
            System.out.println("Success! Student record " + studentNumber + " created.");
        } else {
            System.out.println("Failed. The student number may already exist.");
        }
    }

    private void addCourse(User registrar) {
        System.out.println("\n=== ADD COURSE ===");

        String code = readRequired("Course code (e.g. CS101): ").toUpperCase();
        String title = readRequired("Course title: ");
        int units = readInt("Units: ");
        if (units < 1 || units > 6) {
            System.out.println("Units must be between 1 and 6.");
            return;
        }

        if (courseDao.addCourse(registrar.userId(), code, title, units)) {
            System.out.println("Success! Course " + code + " added.");
        } else {
            System.out.println("Failed. The course code may already exist.");
        }
    }

    private void enrollStudent(User registrar) {
        System.out.println("\n=== ENROLL STUDENT IN COURSE ===");

        List<Student> students = studentDao.getAllStudents();
        List<Course> courses = courseDao.getAllCourses();
        if (students.isEmpty() || courses.isEmpty()) {
            System.out.println("You need at least one student record and one course first.");
            return;
        }

        for (Student s : students) {
            System.out.println("  Student " + s.studentId() + ": " + s.studentNumber() + " - " + s.fullName());
        }
        int studentId = readInt("Enter Student ID (0 to cancel): ");
        if (studentId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        if (students.stream().noneMatch(s -> s.studentId() == studentId)) {
            System.out.println("Student ID " + studentId + " does not exist.");
            return;
        }

        for (Course c : courses) {
            System.out.println("  Course " + c.courseId() + ": " + c.courseCode() + " - " + c.courseTitle());
        }
        int courseId = readInt("Enter Course ID (0 to cancel): ");
        if (courseId == 0) {
            System.out.println("Cancelled.");
            return;
        }
        if (courses.stream().noneMatch(c -> c.courseId() == courseId)) {
            System.out.println("Course ID " + courseId + " does not exist.");
            return;
        }

        String schoolYear = readRequired("School year (e.g. 2026-2027): ");
        String semester = readRequired("Semester (e.g. 1st Sem): ");

        if (enrollmentDao.isAlreadyEnrolled(studentId, courseId, schoolYear, semester)) {
            System.out.println("This student is already enrolled in that course for that term.");
            return;
        }

        if (enrollmentDao.enrollStudent(registrar.userId(), studentId, courseId, schoolYear, semester)) {
            System.out.println("Success! Student enrolled.");
        } else {
            System.out.println("Enrollment failed. Please try again.");
        }
    }

    private void updateEnrollmentStatus(User registrar) {
        System.out.println("\n=== UPDATE ENROLLMENT STATUS ===");

        List<Enrollment> enrollments = enrollmentDao.getAllEnrollments();
        printEnrollments(enrollments);
        if (enrollments.isEmpty()) {
            return;
        }

        int enrollmentId = readInt("Enter Enrollment ID (0 to cancel): ");
        if (enrollmentId == 0) {
            System.out.println("Cancelled.");
            return;
        }

        Enrollment target = enrollments.stream()
                .filter(e -> e.enrollmentId() == enrollmentId).findFirst().orElse(null);
        if (target == null) {
            System.out.println("Enrollment ID " + enrollmentId + " does not exist.");
            return;
        }

        String newStatus;
        while (true) {
            System.out.print("New status (" + String.join(", ", STATUSES) + ") or 0 to cancel: ");
            newStatus = scanner.nextLine().trim().toUpperCase();
            if (newStatus.equals("0")) {
                System.out.println("Cancelled.");
                return;
            }
            if (STATUSES.contains(newStatus)) {
                break;
            }
            System.out.println("Invalid status.");
        }

        if (newStatus.equals(target.status())) {
            System.out.println("Enrollment #" + enrollmentId + " is already " + newStatus + ".");
            return;
        }

        if (enrollmentDao.updateEnrollmentStatus(registrar.userId(), enrollmentId, newStatus)) {
            System.out.println("Success! Enrollment #" + enrollmentId + " is now " + newStatus + ".");
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    private void printEnrollments(List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) {
            System.out.println("No enrollments found.");
            return;
        }

        String format = "%-5s | %-12s | %-20s | %-10s | %-10s | %-8s | %-10s%n";
        System.out.printf(format, "ID", "STUDENT NO.", "NAME", "COURSE", "SCHOOL YR", "SEM", "STATUS");
        for (Enrollment e : enrollments) {
            System.out.printf(format, e.enrollmentId(), e.studentNumber(), e.studentName(),
                    e.courseCode(), e.schoolYear(), e.semester(), e.status());
        }
        System.out.println("Total: " + enrollments.size());
    }

    /** Keeps asking until the input is a whole number. Never throws on bad input. */
    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number (digits only).");
            }
        }
    }

    private String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("This field is required.");
        }
    }
}
