package com.LeaveDataManagementSystem.LeaveManagement.Model;

public enum LeaveStatus {
    PENDING_ACTING_OFFICER,
    PENDING_SUPERVISING_OFFICER,
    PENDING_APPROVAL_OFFICER,
    APPROVED,
    REJECTED_BY_ACTING_OFFICER,
    REJECTED_BY_SUPERVISING_OFFICER,
    REJECTED_BY_APPROVAL_OFFICER,
    CANCELLED_BY_EMPLOYEE,  // NEW
    CANCELLED_ADMIN         // NEW (for admin cancellations)
}