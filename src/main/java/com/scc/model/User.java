package com.scc.model;

public record User(
        int userId,
        String username,
        String passwordHash,
        String fullName,
        String role,
        String status,
        String dateCreated
) {
}
