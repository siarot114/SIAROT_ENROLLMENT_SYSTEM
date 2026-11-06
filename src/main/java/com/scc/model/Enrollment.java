package com.scc.model;

public record Enrollment(
        int enrollmentId,
        String studentNumber,
        String studentName,
        String courseCode,
        String schoolYear,
        String semester,
        String status
) {
}
