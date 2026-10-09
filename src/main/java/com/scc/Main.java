
package com.scc;

import com.scc.dao.DatabaseConnection;
import com.scc.ui.AuthMenu;

public class Main {

    public static void main(String[] args) {
        DatabaseConnection.initializeDatabase();

        AuthMenu menu = new AuthMenu();
        menu.start();
    }
}
