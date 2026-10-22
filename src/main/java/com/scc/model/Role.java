package com.scc.model;

/**
 * System roles from the approved ERD (users.role: Admin / Registrar / Student).
 * The label is the exact value stored in the users.role column.
 */
public enum Role {
    ADMIN("Admin"),
    REGISTRAR("Registrar"),
    STUDENT("Student");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Role fromString(String value) {
        for (Role r : values()) {
            if (r.label.equalsIgnoreCase(value)) {
                return r;
            }
        }
        return null;
    }
}
