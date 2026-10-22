package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.Role;
import com.scc.model.User;
import com.scc.util.PasswordUtil;

import java.util.Scanner;

/** Handles registration, login, and routing to the role-based menus. No SQL here. */
public class Authentication {

    private final UserDao userDao;
    private final PasswordUtil passwordUtil;
    private final Scanner scanner;

    private final AdminMenu adminMenu;
    private final RegistrarMenu registrarMenu;
    private final StudentMenu studentMenu;

    public Authentication(UserDao userDao, PasswordUtil passwordUtil, Scanner scanner) {
        this.userDao = userDao;
        this.passwordUtil = passwordUtil;
        this.scanner = scanner;

        // The same UserDao and Scanner are passed to every menu.
        this.adminMenu = new AdminMenu(userDao, scanner);
        this.registrarMenu = new RegistrarMenu(userDao, scanner);
        this.studentMenu = new StudentMenu(userDao, scanner);
    }

    public void start() {
        while (true) {
            System.out.println("\n=== STUDENT ENROLLMENT SYSTEM ===");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> register();
                case "2" -> {
                    boolean exitApp = login();
                    if (exitApp) {
                        System.out.println("Application closed.");
                        return;
                    }
                }
                case "3" -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void register() {
        System.out.println("\n=== REGISTER ACCOUNT ===");

        System.out.print("Enter full name: ");
        String fullName = scanner.nextLine().trim();

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            System.out.println("Full name, username, and password are required.");
            return;
        }

        System.out.println("Select role:");
        System.out.println("1. Admin");
        System.out.println("2. Registrar");
        System.out.println("3. Student");
        System.out.print("Choose role: ");

        Role role = switch (scanner.nextLine().trim()) {
            case "1" -> Role.ADMIN;
            case "2" -> Role.REGISTRAR;
            case "3" -> Role.STUDENT;
            default -> null;
        };

        if (role == null) {
            System.out.println("Invalid role selection.");
            return;
        }

        if (userDao.usernameExists(username)) {
            System.out.println("Username already exists.");
            return;
        }

        boolean saved = userDao.register(username, passwordUtil.hash(password), fullName, role.label());

        if (saved) {
            System.out.println("Registration successful!");
            System.out.println("Account status: PENDING");
        } else {
            System.out.println("Registration failed. Please try again.");
        }
    }

    /** @return true if the user chose to exit the whole application from their menu. */
    private boolean login() {
        System.out.println("\n=== LOGIN ===");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        User user = userDao.login(username, passwordUtil.hash(password));

        if (user == null) {
            System.out.println("Invalid username or password.");
            return false;
        }

        System.out.println("\nLogin successful!");
        return routeToMenu(user);
    }

    private boolean routeToMenu(User user) {
        Role role = Role.fromString(user.role());

        if (role == null) {
            System.out.println("Unknown role: " + user.role());
            return false;
        }

        return switch (role) {
            case ADMIN -> adminMenu.start(user);
            case REGISTRAR -> registrarMenu.start(user);
            case STUDENT -> studentMenu.start(user);
        };
    }
}
