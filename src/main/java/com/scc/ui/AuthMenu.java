
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class AuthMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final UserDao userDao = new UserDao();

    public void start() {
        while (true) {
            System.out.println("\n=== STUDENT ENROLLMENT SYSTEM ===");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> register();
                case "2" -> {
                    if (login()) {
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

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password are required.");
            return;
        }

        System.out.println("Select role:");
        System.out.println("1. TEACHER");
        System.out.println("2. STUDENT");
        System.out.print("Choose role: ");

        String choice = scanner.nextLine();

        String role = switch (choice) {
            case "1" -> "TEACHER";
            case "2" -> "STUDENT";
            default -> "";
        };

        if (role.isEmpty()) {
            System.out.println("Invalid role selection.");
            return;
        }

        userDao.register(username, password, role);
    }

    private boolean login() {
        System.out.println("\n=== LOGIN ===");

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        User user = userDao.login(username, password);

        if (user == null) {
            System.out.println("Invalid username or password.");
            return false;
        }

        System.out.println("\nLogin successful!");
        System.out.println("Welcome, " + user.username() + "!");
        System.out.println("Role: " + user.roles());
        System.out.println("Status: " + user.status());

        while (true) {
            System.out.println("\n=== USER MENU ===");
            System.out.println("1. Logout");
            System.out.println("2. Exit Application");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> {
                    System.out.println("Logged out successfully.");
                    return false;
                }
                case "2" -> {
                    return true;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }
}
