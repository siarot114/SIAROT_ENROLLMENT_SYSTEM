package com.scc.model;

public record Student(
        int studentId,
        int userId,
        String studentNumber,
        String firstName,
        String lastName,
        String program,
        int yearLevel
) {
    public String fullName() {
        return firstName + " " + lastName;
    }
}
