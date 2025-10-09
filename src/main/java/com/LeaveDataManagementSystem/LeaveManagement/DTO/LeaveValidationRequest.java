package com.LeaveDataManagementSystem.LeaveManagement.DTO;


public class LeaveValidationRequest {
    private String leaveType;
    private String startDate;
    private String endDate;

    public LeaveValidationRequest() {}

    public LeaveValidationRequest(String leaveType, String startDate, String endDate) {
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
