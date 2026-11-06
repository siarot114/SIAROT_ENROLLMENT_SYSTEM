package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.Role;
import com.scc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Admin menu: users, approvals, role/status updates, and the system audit log viewer. No SQL here. */
public class AdminMenu {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, ActivityLogDao activityLogDao, Scanner scanner) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
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
            System.out.println("4. Update User Role & Status");
            System.out.println("5. View System Audit Logs");
            System.out.println("6. Logout");
            System.out.println("7. Exit Application");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser(admin);
                case "4" -> updateUser(admin);
                case "5" -> auditLogMenu(admin);
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

    private void viewAllUsers() {
        System.out.println("\n=== ALL SYSTEM USERS ===");
        printUserTable(userDao.getAllUsers());
    }

    private void viewPendingUsers() {
        System.out.println("\n=== PENDING REGISTRATIONS ===");
        printUserTable(userDao.getPendingUsers());
    }

    private void approveUser(User admin) {
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

            if (userDao.approveUser(admin.userId(), userId)) {
                System.out.println("Success! Account '" + target.username()
                        + "' (ID " + userId + ") has been APPROVED.");
            } else {
                System.out.println("Approval failed. Please try again.");
            }
            return;
        }
    }

    // ------------------------------------------------------------------
    // Update user role & status
    // ------------------------------------------------------------------

    private void updateUser(User currentAdmin) {
        System.out.println("\n=== UPDATE USER ROLE & STATUS ===");

        List<User> users = userDao.getAllUsers();
        printUserTable(users);

        if (users.isEmpty()) {
            return;
        }

        // Step 1: target user (0 cancels)
        User target;
        while (true) {
            int targetUserId = readIntInput("Enter User ID to update (0 to cancel): ");

            if (targetUserId == 0) {
                System.out.println("Update cancelled.");
                return;
            }

            if (targetUserId == currentAdmin.userId()) {
                System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                continue;
            }

            target = findById(users, targetUserId);
            if (target == null) {
                System.out.println("User ID " + targetUserId + " does not exist.");
                continue;
            }
            break;
        }

        System.out.println("Selected: " + target.username()
                + " (current role: " + target.role() + ", status: " + target.status() + ")");

        // Step 2: new role (0 cancels)
        String newRole = readValidRole();
        if (newRole == null) {
            System.out.println("Update cancelled.");
            return;
        }

        // Step 3: new status (0 cancels)
        String newStatus = readValidStatus();
        if (newStatus == null) {
            System.out.println("Update cancelled.");
            return;
        }

        if (userDao.updateUserRoleAndStatus(currentAdmin.userId(), target.userId(), newRole, newStatus)) {
            System.out.println("Success! User '" + target.username() + "' (ID " + target.userId()
                    + ") is now " + newRole + " / " + newStatus + ".");
        } else {
            System.out.println("Update failed. Please try again.");
        }
    }

    /** Keeps asking until the input is a whole number. Never throws on bad input. */
    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number (digits only).");
            }
        }
    }

    /** @return a valid role in upper case, or null if the admin entered 0 to cancel. */
    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role (" + roleList() + ") or 0 to cancel: ");
            String input = scanner.nextLine().trim();

            if (input.equals("0")) {
                return null;
            }

            Role role = Role.fromString(input);
            if (role != null) {
                return role.name();
            }
            System.out.println("Invalid role. Valid roles are: " + roleList() + ".");
        }
    }

    private String roleList() {
        StringBuilder list = new StringBuilder();
        for (Role role : Role.values()) {
            if (list.length() > 0) {
                list.append(", ");
            }
            list.append(role.name());
        }
        return list.toString();
    }

    /** @return APPROVED or PENDING, or null if the admin entered 0 to cancel. */
    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status (APPROVED, PENDING) or 0 to cancel: ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.equals("0")) {
                return null;
            }

            if (input.equals("APPROVED") || input.equals("PENDING")) {
                return input;
            }
            System.out.println("Invalid status. Valid statuses are: APPROVED, PENDING.");
        }
    }

    // ------------------------------------------------------------------
    // System audit logs
    // ------------------------------------------------------------------

    private static final int DESCRIPTION_WIDTH = 35;
    private static final String LOG_FORMAT = "%-5s | %-8s | %-18s | %-" + DESCRIPTION_WIDTH + "s | %-20s%n";

    private void auditLogMenu(User admin) {
        while (true) {
            System.out.println("\n=== SYSTEM AUDIT LOGS ===");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear All Logs");
            System.out.println("0. Back");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> printLogTable(activityLogDao.getAllLogs());
                case "2" -> filterLogsByUser();
                case "3" -> clearLogs(admin);
                case "0" -> {
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void filterLogsByUser() {
        int userId = readIntInput("Enter User ID to filter (0 to cancel): ");

        if (userId == 0) {
            System.out.println("Filter cancelled.");
            return;
        }

        List<ActivityLog> logs = activityLogDao.getLogsByUserId(userId);
        if (logs.isEmpty()) {
            System.out.println("No log entries found for User ID " + userId + ".");
            return;
        }
        printLogTable(logs);
    }

    /** Deletes all history only after an explicit YES, then records that it happened. */
    private void clearLogs(User admin) {
        System.out.println("WARNING: This permanently deletes ALL audit log entries.");
        System.out.print("Type YES to confirm, anything else cancels: ");

        if (!scanner.nextLine().trim().equals("YES")) {
            System.out.println("Clear cancelled. No logs were deleted.");
            return;
        }

        int removed = activityLogDao.clearAllLogs();
        if (removed < 0) {
            System.out.println("Clearing logs failed. Please try again.");
            return;
        }

        // The wipe itself must stay auditable, so it becomes the first entry of the fresh log.
        activityLogDao.logAction(admin.userId(), "LOGS_CLEARED",
                "Admin " + admin.userId() + " cleared " + removed + " log entries.");
        System.out.println("Cleared " + removed + " log entries.");
    }

    /** Aligned console table. Long descriptions wrap onto extra lines instead of breaking the columns. */
    private void printLogTable(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("No log entries found.");
            return;
        }

        String border = "-".repeat(5 + 8 + 18 + DESCRIPTION_WIDTH + 20 + 4 * 3);

        System.out.println(border);
        System.out.printf(LOG_FORMAT, "ID", "USER ID", "ACTION TYPE", "DESCRIPTION", "TIMESTAMP");
        System.out.println(border);
        for (ActivityLog log : logs) {
            List<String> lines = wrap(log.description(), DESCRIPTION_WIDTH);
            System.out.printf(LOG_FORMAT, log.id(), log.userId(), log.actionType(),
                    lines.get(0), log.timestamp());
            for (int i = 1; i < lines.size(); i++) {
                System.out.printf(LOG_FORMAT, "", "", "", lines.get(i), "");
            }
        }
        System.out.println(border);
        System.out.println("Total: " + logs.size());
    }

    /** Splits text into lines of at most width characters, breaking at spaces where possible. */
    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();

        while (remaining.length() > width) {
            int cut = remaining.lastIndexOf(' ', width);
            if (cut <= 0) {
                cut = width; // one very long word: hard split
            }
            lines.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
        }
        lines.add(remaining);
        return lines;
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
