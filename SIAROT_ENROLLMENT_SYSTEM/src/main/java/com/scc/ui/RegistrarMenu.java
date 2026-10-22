package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

/** Placeholder REGISTRAR menu. Role features will be added in later deliverables. */
public class RegistrarMenu {

    private final UserDao userDao;
    private final Scanner scanner;

    public RegistrarMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    /** @return true if the user chose to exit the application, false for logout. */
    public boolean start(User user) {
        while (true) {
            System.out.println("\n=== REGISTRAR MENU ===");
            System.out.println("Welcome, " + user.username() + "!");
            System.out.println("Status: " + user.status());
            System.out.println("1. Logout");
            System.out.println("2. Exit Application");
            System.out.print("Choose an option: ");

            switch (scanner.nextLine().trim()) {
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
