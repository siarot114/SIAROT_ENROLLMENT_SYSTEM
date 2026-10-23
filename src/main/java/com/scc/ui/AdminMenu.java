package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

/** Admin menu: view users, view pending registrations, approve accounts. No SQL here. */
public class AdminMenu {

    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    /** @return true if the user chose to exit the application, false for logout. */
    public boolean start(User admin) {
        while (true) {
            System.out.println("\n=== ADMIN MENU ===");
            System.out.println("Welcome, " + admin.username() + "!");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Logout");
            System.out.println("5. Exit Application");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "4" -> {
                    System.out.println("Logged out successfully.");
                    return false;
                }
                case "5" -> {
                    return true;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void viewAllUsers() {
        System.out.println("\n=== ALL SYSTEM USERS ===");
        printUserTable(userDao.getAllUsers());
    }

    private void viewPendingUsers() {
        System.out.println("\n=== PENDING REGISTRATIONS ===");
        printUserTable(userDao.getPendingUsers());
    }

    private void approveUser() {
        System.out.println("\n=== APPROVE USER ACCOUNT ===");

        List<User> pending = userDao.getPendingUsers();
        printUserTable(pending);

        if (pending.isEmpty()) {
            return;
        }

        while (true) {
            System.out.print("Enter User ID to approve (0 to cancel): ");
            String input = scanner.nextLine().trim();

            int userId;
            try {
                userId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            if (userId == 0) {
                System.out.println("Approval cancelled.");
                return;
            }

            User target = findById(pending, userId);
            if (target == null) {
                System.out.println("User ID " + userId + " is not in the pending list.");
                continue;
            }

            if (userDao.approveUser(userId)) {
                System.out.println("Success! Account '" + target.username()
                        + "' (ID " + userId + ") has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    private User findById(List<User> users, int userId) {
        for (User user : users) {
            if (user.userId() == userId) {
                return user;
            }
        }
        return null;
    }

    /** Prints any list of users as an aligned table. */
    private void printUserTable(List<User> users) {
        String border = "+-------+-----------------+--------------+------------+";
        String format = "| %-5s | %-15s | %-12s | %-10s |%n";

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        System.out.println(border);
        System.out.printf(format, "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(border);
        for (User user : users) {
            System.out.printf(format, user.userId(), user.username(), user.role(), user.status());
        }
        System.out.println(border);
        System.out.println("Total: " + users.size());
    }
}
