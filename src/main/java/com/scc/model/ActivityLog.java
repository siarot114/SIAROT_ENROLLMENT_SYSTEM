package com.scc.model;

/** One row of the activity_logs audit trail. */
public record ActivityLog(
        int id,
        int userId,
        String actionType,
        String description,
        String timestamp
) {
}
