


package com.LeaveDataManagementSystem.LeaveManagement.DTO;

import com.LeaveDataManagementSystem.LeaveManagement.Model.Leave;
import com.LeaveDataManagementSystem.LeaveManagement.Model.LeaveStatus;
import com.LeaveDataManagementSystem.LeaveManagement.Model.ActingOfficerStatus;
import com.LeaveDataManagementSystem.LeaveManagement.Model.ApprovalOfficerStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LeaveResponse {
    private String id;
    private String employeeEmail;
    private String employeeName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private int numberOfDays; // Alias for totalDays for frontend compatibility
    private String reason;
    private String actingOfficerEmail;
    private String actingOfficerName;
    private String approvalOfficerEmail;
    private String approvalOfficerName;
    private String status;
    private String actingOfficerStatus;
    private String approvalOfficerStatus;
    private String actingOfficerComments;
    private String approvalOfficerComments;
    private String createdAt;
    private String submittedDate; // Alias for createdAt for frontend compatibility
    private String actingOfficerApprovedAt;
    private String approvalOfficerApprovedAt;
    private LocalTime shortLeaveStartTime;
    private LocalTime shortLeaveEndTime;


    private String supervisingOfficerEmail;
    private String supervisingOfficerName;
    private String supervisingOfficerStatus;
    private String supervisingOfficerComments;
    private String supervisingOfficerApprovedAt;


    private String maternityLeaveType;
    private boolean isMaternityLeave;
    private String maternityLeaveDuration;

    // Constructor from Leave entity
    public LeaveResponse(Leave leave) {
        this.id = leave.getId();
        this.employeeEmail = leave.getEmployeeEmail();
        this.employeeName = leave.getEmployeeName();
        this.leaveType = leave.getLeaveType();
        this.startDate = leave.getStartDate();
        this.endDate = leave.getEndDate();

        this.totalDays = (int) leave.getTotalDays();
        this.numberOfDays = (int) leave.getTotalDays();

        this.reason = leave.getReason();
        this.actingOfficerEmail = leave.getActingOfficerEmail();
        this.actingOfficerName = leave.getActingOfficerName();
        this.approvalOfficerEmail = leave.getApprovalOfficerEmail();
        this.approvalOfficerName = leave.getApprovalOfficerName();

        this.status = leave.getStatus() != null ? leave.getStatus().toString() : null;
        this.actingOfficerStatus = leave.getActingOfficerStatus() != null ? leave.getActingOfficerStatus().toString() : null;
        this.approvalOfficerStatus = leave.getApprovalOfficerStatus() != null ? leave.getApprovalOfficerStatus().toString() : null;
        this.supervisingOfficerStatus = leave.getSupervisingOfficerStatus() != null ?leave.getSupervisingOfficerStatus().toString() : null;

        this.actingOfficerComments = leave.getActingOfficerComments();
        this.approvalOfficerComments = leave.getApprovalOfficerComments();
        this.supervisingOfficerComments = leave.getSupervisingOfficerComments();


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.createdAt = leave.getCreatedAt() != null ? leave.getCreatedAt().format(formatter) : null;
        this.submittedDate = this.createdAt;

        this.actingOfficerApprovedAt = leave.getActingOfficerApprovedAt() != null ? leave.getActingOfficerApprovedAt().format(formatter) : null;
        this.approvalOfficerApprovedAt = leave.getApprovalOfficerApprovedAt() != null ? leave.getApprovalOfficerApprovedAt().format(formatter) : null;
        this.supervisingOfficerApprovedAt = leave.getSupervisingOfficerApprovedAt() != null ?leave.getSupervisingOfficerApprovedAt().format(formatter) : null;

        // ✅ Add short leave times
        this.shortLeaveStartTime = leave.getShortLeaveStartTime();
        this.shortLeaveEndTime = leave.getShortLeaveEndTime();

        // NEW: Map supervising officer fields
        this.supervisingOfficerEmail = leave.getSupervisingOfficerEmail();
        this.supervisingOfficerName = leave.getSupervisingOfficerName();


// Add maternity leave specific fields
        this.maternityLeaveType = leave.getMaternityLeaveType();
        this.isMaternityLeave = leave.isMaternityLeave();

        // Optional: include maternity duration description
        if (leave.isMaternityLeave()) {
            this.maternityLeaveDuration = leave.getMaternityLeaveDuration();
        }



    }


    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalTime getShortLeaveStartTime() { return shortLeaveStartTime; }
    public void setShortLeaveStartTime(LocalTime shortLeaveStartTime) { this.shortLeaveStartTime = shortLeaveStartTime; }

    public LocalTime getShortLeaveEndTime() { return shortLeaveEndTime; }
    public void setShortLeaveEndTime(LocalTime shortLeaveEndTime) { this.shortLeaveEndTime = shortLeaveEndTime; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public int getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(int numberOfDays) { this.numberOfDays = numberOfDays; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getActingOfficerEmail() { return actingOfficerEmail; }
    public void setActingOfficerEmail(String actingOfficerEmail) { this.actingOfficerEmail = actingOfficerEmail; }

    public String getActingOfficerName() { return actingOfficerName; }
    public void setActingOfficerName(String actingOfficerName) { this.actingOfficerName = actingOfficerName; }

    public String getApprovalOfficerEmail() { return approvalOfficerEmail; }
    public void setApprovalOfficerEmail(String approvalOfficerEmail) { this.approvalOfficerEmail = approvalOfficerEmail; }

    public String getApprovalOfficerName() { return approvalOfficerName; }
    public void setApprovalOfficerName(String approvalOfficerName) { this.approvalOfficerName = approvalOfficerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getActingOfficerStatus() { return actingOfficerStatus; }
    public void setActingOfficerStatus(String actingOfficerStatus) { this.actingOfficerStatus = actingOfficerStatus; }

    public String getApprovalOfficerStatus() { return approvalOfficerStatus; }
    public void setApprovalOfficerStatus(String approvalOfficerStatus) { this.approvalOfficerStatus = approvalOfficerStatus; }

    public String getActingOfficerComments() { return actingOfficerComments; }
    public void setActingOfficerComments(String actingOfficerComments) { this.actingOfficerComments = actingOfficerComments; }

    public String getApprovalOfficerComments() { return approvalOfficerComments; }
    public void setApprovalOfficerComments(String approvalOfficerComments) { this.approvalOfficerComments = approvalOfficerComments; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(String submittedDate) { this.submittedDate = submittedDate; }

    public String getActingOfficerApprovedAt() { return actingOfficerApprovedAt; }
    public void setActingOfficerApprovedAt(String actingOfficerApprovedAt) { this.actingOfficerApprovedAt = actingOfficerApprovedAt; }

    public String getApprovalOfficerApprovedAt() { return approvalOfficerApprovedAt; }
    public void setApprovalOfficerApprovedAt(String approvalOfficerApprovedAt) { this.approvalOfficerApprovedAt = approvalOfficerApprovedAt; }

    public String getSupervisingOfficerEmail() { return supervisingOfficerEmail; }
    public void setSupervisingOfficerEmail(String supervisingOfficerEmail) { this.supervisingOfficerEmail = supervisingOfficerEmail; }

    public String getSupervisingOfficerName() { return supervisingOfficerName; }
    public void setSupervisingOfficerName(String supervisingOfficerName) { this.supervisingOfficerName = supervisingOfficerName; }

    public String getSupervisingOfficerStatus() { return supervisingOfficerStatus; }
    public void setSupervisingOfficerStatus(String supervisingOfficerStatus) { this.supervisingOfficerStatus = supervisingOfficerStatus; }

    public String getSupervisingOfficerComments() { return supervisingOfficerComments; }
    public void setSupervisingOfficerComments(String supervisingOfficerComments) { this.supervisingOfficerComments = supervisingOfficerComments; }

    public String getSupervisingOfficerApprovedAt() { return supervisingOfficerApprovedAt; }
    public void setSupervisingOfficerApprovedAt(String supervisingOfficerApprovedAt) { this.supervisingOfficerApprovedAt = supervisingOfficerApprovedAt; }


    public String getMaternityLeaveType() {
        return maternityLeaveType;
    }

    public void setMaternityLeaveType(String maternityLeaveType) {
        this.maternityLeaveType = maternityLeaveType;
    }

    public boolean isMaternityLeave() {
        return isMaternityLeave;
    }

    public void setMaternityLeave(boolean maternityLeave) {
        isMaternityLeave = maternityLeave;
    }

    public String getMaternityLeaveDuration() {
        return maternityLeaveDuration;
    }

    public void setMaternityLeaveDuration(String maternityLeaveDuration) {
        this.maternityLeaveDuration = maternityLeaveDuration;
    }
}