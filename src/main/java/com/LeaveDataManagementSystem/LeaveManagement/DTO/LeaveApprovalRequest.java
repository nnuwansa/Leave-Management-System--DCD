package com.LeaveDataManagementSystem.LeaveManagement.DTO;

public class LeaveApprovalRequest {
    private String action; // "APPROVE" or "REJECT"
    private String comments;


    public LeaveApprovalRequest() {}

    // Parameterized constructor
    public LeaveApprovalRequest(String action, String comments) {
        this.action = action;
        this.comments = comments;
    }

    // Getters and Setters
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "LeaveApprovalRequest{" +
                "action='" + action + '\'' +
                ", comments='" + comments + '\'' +
                '}';
    }
}