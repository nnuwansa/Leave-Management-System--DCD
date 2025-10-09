package com.LeaveDataManagementSystem.LeaveManagement.Model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "Short Leave Entitlements")
public class ShortLeaveEntitlement {
    @Id
    private String id;

    private String employeeEmail;
    private int year;
    private int month; // 1-12
    private int totalShortLeaves = 2; // 2 short leaves per month
    private int usedShortLeaves = 0;
    private int remainingShortLeaves = 2;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Constructors
    public ShortLeaveEntitlement() {}

    public ShortLeaveEntitlement(String employeeEmail, int year, int month) {
        this.employeeEmail = employeeEmail;
        this.year = year;
        this.month = month;
        this.totalShortLeaves = 2;
        this.usedShortLeaves = 0;
        this.remainingShortLeaves = 2;
    }

    // Method to check if short leave is available
    public boolean hasShortLeaveAvailable() {
        return remainingShortLeaves > 0;
    }

    // Method to use one short leave
    public void useShortLeave() {
        if (hasShortLeaveAvailable()) {
            this.usedShortLeaves++;
            this.remainingShortLeaves = this.totalShortLeaves - this.usedShortLeaves;
        }
    }

    // Method to revert short leave usage
    public void revertShortLeave() {
        if (this.usedShortLeaves > 0) {
            this.usedShortLeaves--;
            this.remainingShortLeaves = this.totalShortLeaves - this.usedShortLeaves;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getTotalShortLeaves() { return totalShortLeaves; }
    public void setTotalShortLeaves(int totalShortLeaves) { this.totalShortLeaves = totalShortLeaves; }

    public int getUsedShortLeaves() { return usedShortLeaves; }
    public void setUsedShortLeaves(int usedShortLeaves) {
        this.usedShortLeaves = usedShortLeaves;
        this.remainingShortLeaves = this.totalShortLeaves - usedShortLeaves;
    }

    public int getRemainingShortLeaves() { return remainingShortLeaves; }
    public void setRemainingShortLeaves(int remainingShortLeaves) { this.remainingShortLeaves = remainingShortLeaves; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}