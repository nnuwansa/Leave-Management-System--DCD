
package com.LeaveDataManagementSystem.LeaveManagement.DTO;

import com.LeaveDataManagementSystem.LeaveManagement.Model.LeaveStatus;

import java.time.LocalDate;
import java.time.LocalTime;

//
//public class LeaveRequest {
//    private String leaveType;
//    private LocalDate startDate;
//    private LocalDate endDate;
//    private String reason;
//    private String actingOfficerEmail;
//    private String supervisingOfficerEmail;
//    private String approvalOfficerEmail;
//
//    // New fields for half-day and short leave
//    private String halfDayPeriod; // "MORNING" or "AFTERNOON"
//    private LocalTime startTime;   // For short leaves
//    private LocalTime endTime;     // For short leaves
//
//    // Default constructor
//    public LeaveRequest() {}
//
//    // Regular leave constructor
//    public LeaveRequest(String leaveType, LocalDate startDate, LocalDate endDate,
//                        String reason, String actingOfficerEmail, String supervisingOfficerEmail,
//                        String approvalOfficerEmail) {
//        this.leaveType = leaveType;
//        this.startDate = startDate;
//        this.endDate = endDate;
//        this.reason = reason;
//        this.actingOfficerEmail = actingOfficerEmail;
//        this.supervisingOfficerEmail = supervisingOfficerEmail;
//        this.approvalOfficerEmail = approvalOfficerEmail;
//    }
//
//    // Half-day leave constructor
//    public LeaveRequest(String leaveType, LocalDate date, String halfDayPeriod,
//                        String reason, String actingOfficerEmail, String supervisingOfficerEmail,
//                        String approvalOfficerEmail) {
//        this.leaveType = leaveType;
//        this.startDate = date;
//        this.endDate = date; // Same day for half day
//        this.halfDayPeriod = halfDayPeriod;
//        this.reason = reason;
//        this.actingOfficerEmail = actingOfficerEmail;
//        this.supervisingOfficerEmail = supervisingOfficerEmail;
//        this.approvalOfficerEmail = approvalOfficerEmail;
//    }
//
//    // Short leave constructor
//    public LeaveRequest(LocalDate date, LocalTime startTime, LocalTime endTime,
//                        String reason, String actingOfficerEmail, String supervisingOfficerEmail,
//                        String approvalOfficerEmail) {
//        this.leaveType = "SHORT";
//        this.startDate = date;
//        this.endDate = date; // Same day for short leave
//        this.startTime = startTime;
//        this.endTime = endTime;
//        this.reason = reason;
//        this.actingOfficerEmail = actingOfficerEmail;
//        this.supervisingOfficerEmail = supervisingOfficerEmail;
//        this.approvalOfficerEmail = approvalOfficerEmail;
//    }
//
//    // Helper methods to check if officers are selected
//    public boolean hasActingOfficer() {
//        return actingOfficerEmail != null &&
//                !actingOfficerEmail.trim().isEmpty() &&
//                !"NONE".equalsIgnoreCase(actingOfficerEmail);
//    }
//
//    public boolean hasSupervisingOfficer() {
//        return supervisingOfficerEmail != null &&
//                !supervisingOfficerEmail.trim().isEmpty() &&
//                !"NONE".equalsIgnoreCase(supervisingOfficerEmail);
//    }
//
//    public boolean hasApprovalOfficer() {
//        return approvalOfficerEmail != null &&
//                !approvalOfficerEmail.trim().isEmpty() &&
//                !"NONE".equalsIgnoreCase(approvalOfficerEmail);
//    }
//
//    // Method to determine the initial workflow status
//    public LeaveStatus getInitialWorkflowStatus() {
//        if (hasActingOfficer()) {
//            return LeaveStatus.PENDING_ACTING_OFFICER;
//        } else if (hasSupervisingOfficer()) {
//            return LeaveStatus.PENDING_SUPERVISING_OFFICER;
//        } else if (hasApprovalOfficer()) {
//            return LeaveStatus.PENDING_APPROVAL_OFFICER;
//        } else {
//            // If no officers are selected, auto-approve or handle as needed
//            return LeaveStatus.APPROVED; // or throw an exception
//        }
//    }
//
//    // Method to get the next workflow status after approval
//    public LeaveStatus getNextWorkflowStatus(LeaveStatus currentStatus) {
//        switch (currentStatus) {
//            case PENDING_ACTING_OFFICER:
//                if (hasSupervisingOfficer()) {
//                    return LeaveStatus.PENDING_SUPERVISING_OFFICER;
//                } else if (hasApprovalOfficer()) {
//                    return LeaveStatus.PENDING_APPROVAL_OFFICER;
//                } else {
//                    return LeaveStatus.APPROVED;
//                }
//
//            case PENDING_SUPERVISING_OFFICER:
//                if (hasApprovalOfficer()) {
//                    return LeaveStatus.PENDING_APPROVAL_OFFICER;
//                } else {
//                    return LeaveStatus.APPROVED;
//                }
//
//            case PENDING_APPROVAL_OFFICER:
//                return LeaveStatus.APPROVED;
//
//            default:
//                return LeaveStatus.APPROVED;
//        }
//    }
//
//    // Getters and Setters
//    public String getLeaveType() { return leaveType; }
//    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
//
//    public LocalDate getStartDate() { return startDate; }
//    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
//
//    public LocalDate getEndDate() { return endDate; }
//    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
//
//    public String getReason() { return reason; }
//    public void setReason(String reason) { this.reason = reason; }
//
//    public String getActingOfficerEmail() { return actingOfficerEmail; }
//    public void setActingOfficerEmail(String actingOfficerEmail) {
//        // Convert "NONE" to null for consistency
//        this.actingOfficerEmail = "NONE".equalsIgnoreCase(actingOfficerEmail) ? null : actingOfficerEmail;
//    }
//
//    public String getSupervisingOfficerEmail() { return supervisingOfficerEmail; }
//    public void setSupervisingOfficerEmail(String supervisingOfficerEmail) {
//        // Convert "NONE" to null for consistency
//        this.supervisingOfficerEmail = "NONE".equalsIgnoreCase(supervisingOfficerEmail) ? null : supervisingOfficerEmail;
//    }
//
//    public String getApprovalOfficerEmail() { return approvalOfficerEmail; }
//    public void setApprovalOfficerEmail(String approvalOfficerEmail) { this.approvalOfficerEmail = approvalOfficerEmail; }
//
//    public String getHalfDayPeriod() { return halfDayPeriod; }
//    public void setHalfDayPeriod(String halfDayPeriod) { this.halfDayPeriod = halfDayPeriod; }
//
//    public LocalTime getStartTime() { return startTime; }
//    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
//
//    public LocalTime getEndTime() { return endTime; }
//    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
//}


public class LeaveRequest {
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String actingOfficerEmail;
    private String supervisingOfficerEmail;
    private String approvalOfficerEmail;

    // New fields for half-day and short leave
    private String halfDayPeriod; // "MORNING" or "AFTERNOON"
    private LocalTime startTime;   // For short leaves
    private LocalTime endTime;     // For short leaves

    // NEW: Maternity leave type
    private String maternityLeaveType; // "FULL_PAY", "HALF_PAY", "NO_PAY"

    // Default constructor
    public LeaveRequest() {}

    // Regular leave constructor
    public LeaveRequest(String leaveType, LocalDate startDate, LocalDate endDate,
                        String reason, String actingOfficerEmail, String supervisingOfficerEmail,
                        String approvalOfficerEmail) {
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.actingOfficerEmail = actingOfficerEmail;
        this.supervisingOfficerEmail = supervisingOfficerEmail;
        this.approvalOfficerEmail = approvalOfficerEmail;
    }

    // Half-day leave constructor
    public LeaveRequest(String leaveType, LocalDate date, String halfDayPeriod,
                        String reason, String actingOfficerEmail, String supervisingOfficerEmail,
                        String approvalOfficerEmail) {
        this.leaveType = leaveType;
        this.startDate = date;
        this.endDate = date; // Same day for half day
        this.halfDayPeriod = halfDayPeriod;
        this.reason = reason;
        this.actingOfficerEmail = actingOfficerEmail;
        this.supervisingOfficerEmail = supervisingOfficerEmail;
        this.approvalOfficerEmail = approvalOfficerEmail;
    }

    // Short leave constructor
    public LeaveRequest(LocalDate date, LocalTime startTime, LocalTime endTime,
                        String reason, String actingOfficerEmail, String supervisingOfficerEmail,
                        String approvalOfficerEmail) {
        this.leaveType = "SHORT";
        this.startDate = date;
        this.endDate = date; // Same day for short leave
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.actingOfficerEmail = actingOfficerEmail;
        this.supervisingOfficerEmail = supervisingOfficerEmail;
        this.approvalOfficerEmail = approvalOfficerEmail;
    }

    // NEW: Maternity leave constructor
    public LeaveRequest(LocalDate startDate, String maternityLeaveType, String reason,
                        String actingOfficerEmail, String supervisingOfficerEmail,
                        String approvalOfficerEmail) {
        this.leaveType = "MATERNITY";
        this.startDate = startDate;
        this.endDate = null; // Will be set by admin after approval
        this.maternityLeaveType = maternityLeaveType;
        this.reason = reason;
        this.actingOfficerEmail = actingOfficerEmail;
        this.supervisingOfficerEmail = supervisingOfficerEmail;
        this.approvalOfficerEmail = approvalOfficerEmail;
    }

    // Helper methods to check if officers are selected
    public boolean hasActingOfficer() {
        return actingOfficerEmail != null &&
                !actingOfficerEmail.trim().isEmpty() &&
                !"NONE".equalsIgnoreCase(actingOfficerEmail);
    }

    public boolean hasSupervisingOfficer() {
        return supervisingOfficerEmail != null &&
                !supervisingOfficerEmail.trim().isEmpty() &&
                !"NONE".equalsIgnoreCase(supervisingOfficerEmail);
    }

    public boolean hasApprovalOfficer() {
        return approvalOfficerEmail != null &&
                !approvalOfficerEmail.trim().isEmpty() &&
                !"NONE".equalsIgnoreCase(approvalOfficerEmail);
    }

    // Method to determine the initial workflow status
    public LeaveStatus getInitialWorkflowStatus() {
        if (hasActingOfficer()) {
            return LeaveStatus.PENDING_ACTING_OFFICER;
        } else if (hasSupervisingOfficer()) {
            return LeaveStatus.PENDING_SUPERVISING_OFFICER;
        } else if (hasApprovalOfficer()) {
            return LeaveStatus.PENDING_APPROVAL_OFFICER;
        } else {
            // If no officers are selected, auto-approve or handle as needed
            return LeaveStatus.APPROVED; // or throw an exception
        }
    }

    // Method to get the next workflow status after approval
    public LeaveStatus getNextWorkflowStatus(LeaveStatus currentStatus) {
        switch (currentStatus) {
            case PENDING_ACTING_OFFICER:
                if (hasSupervisingOfficer()) {
                    return LeaveStatus.PENDING_SUPERVISING_OFFICER;
                } else if (hasApprovalOfficer()) {
                    return LeaveStatus.PENDING_APPROVAL_OFFICER;
                } else {
                    return LeaveStatus.APPROVED;
                }

            case PENDING_SUPERVISING_OFFICER:
                if (hasApprovalOfficer()) {
                    return LeaveStatus.PENDING_APPROVAL_OFFICER;
                } else {
                    return LeaveStatus.APPROVED;
                }

            case PENDING_APPROVAL_OFFICER:
                return LeaveStatus.APPROVED;

            default:
                return LeaveStatus.APPROVED;
        }
    }

    // Check if this is a maternity leave request
    public boolean isMaternityLeave() {
        return "MATERNITY".equalsIgnoreCase(this.leaveType);
    }

    // Getters and Setters
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getActingOfficerEmail() { return actingOfficerEmail; }
    public void setActingOfficerEmail(String actingOfficerEmail) {
        // Convert "NONE" to null for consistency
        this.actingOfficerEmail = "NONE".equalsIgnoreCase(actingOfficerEmail) ? null : actingOfficerEmail;
    }

    public String getSupervisingOfficerEmail() { return supervisingOfficerEmail; }
    public void setSupervisingOfficerEmail(String supervisingOfficerEmail) {
        // Convert "NONE" to null for consistency
        this.supervisingOfficerEmail = "NONE".equalsIgnoreCase(supervisingOfficerEmail) ? null : supervisingOfficerEmail;
    }

    public String getApprovalOfficerEmail() { return approvalOfficerEmail; }
    public void setApprovalOfficerEmail(String approvalOfficerEmail) { this.approvalOfficerEmail = approvalOfficerEmail; }

    public String getHalfDayPeriod() { return halfDayPeriod; }
    public void setHalfDayPeriod(String halfDayPeriod) { this.halfDayPeriod = halfDayPeriod; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    // NEW: Maternity leave type getter/setter
    public String getMaternityLeaveType() { return maternityLeaveType; }
    public void setMaternityLeaveType(String maternityLeaveType) { this.maternityLeaveType = maternityLeaveType; }
}