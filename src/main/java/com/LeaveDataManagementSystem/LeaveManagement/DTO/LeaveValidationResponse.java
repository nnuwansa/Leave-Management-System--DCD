package com.LeaveDataManagementSystem.LeaveManagement.DTO;

public class LeaveValidationResponse {
    private boolean valid;
    private String message;
    private int requestedDays;
    private int availableDays;
    private String leaveType;

    public LeaveValidationResponse() {
    }

    public LeaveValidationResponse(boolean valid, String message, int requestedDays,
                                   int availableDays, String leaveType) {
        this.valid = valid;
        this.message = message;
        this.requestedDays = requestedDays;
        this.availableDays = availableDays;
        this.leaveType = leaveType;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRequestedDays() {
        return requestedDays;
    }

    public void setRequestedDays(int requestedDays) {
        this.requestedDays = requestedDays;
    }

    public int getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(int availableDays) {
        this.availableDays = availableDays;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }
}