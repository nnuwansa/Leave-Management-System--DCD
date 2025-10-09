package com.LeaveDataManagementSystem.LeaveManagement.Model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;



@Document(collection = "Leave Entitlements")
public class LeaveEntitlement {
    @Id
    private String id;

    private String employeeEmail;
    private String leaveType;
    private int totalEntitlement; // -1 for unlimited (DUTY leave)
    private double usedDays;
    private double remainingDays; // -1 for unlimited
    private int year;

    // Track accumulated half days
    private int accumulatedHalfDays = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Constructors
    public LeaveEntitlement() {}

    public LeaveEntitlement(String employeeEmail, String leaveType, int totalEntitlement, int year) {
        this.employeeEmail = employeeEmail;
        this.leaveType = leaveType;
        this.totalEntitlement = totalEntitlement;
        this.usedDays = 0.0;
        this.remainingDays = totalEntitlement == -1 ? -1.0 : (double) totalEntitlement;
        this.year = year;
        this.accumulatedHalfDays = 0;
    }

    // Check if this is an unlimited entitlement (like DUTY leave)
    public boolean isUnlimited() {
        return totalEntitlement == -1;
    }

    // Method to check if sufficient leave is available
    public boolean hasSufficientLeave(double requestedDays) {
        if (isUnlimited()) {
            return true; // Unlimited leave always has sufficient balance
        }
        return remainingDays >= requestedDays;
    }

    // Method to update used days (supports half days and unlimited leave)
    public void updateUsedDays(double additionalDays) {
        this.usedDays += additionalDays;
        if (!isUnlimited()) {
            this.remainingDays = this.totalEntitlement - this.usedDays;
        }
        // For unlimited leave, remainingDays stays at -1
    }

    // Method to add half day and convert to full day if needed
    public void addHalfDay() {
        this.accumulatedHalfDays++;

        // If we have 2 half days, convert to 1 full day
        if (this.accumulatedHalfDays >= 2) {
            this.usedDays += 1.0;
            this.accumulatedHalfDays -= 2;
            if (!isUnlimited()) {
                this.remainingDays = this.totalEntitlement - this.usedDays;
            }
        }
    }

    // Method to remove half day (for reversions)
    public void removeHalfDay() {
        if (this.accumulatedHalfDays > 0) {
            this.accumulatedHalfDays--;
        } else if (this.usedDays >= 1.0) {
            // Convert 1 full day back to 1 half day
            this.usedDays -= 1.0;
            this.accumulatedHalfDays = 1;
            if (!isUnlimited()) {
                this.remainingDays = this.totalEntitlement - this.usedDays;
            }
        }
    }

    // Get effective remaining days including half days
    public double getEffectiveRemainingDays() {
        if (isUnlimited()) {
            return Double.MAX_VALUE; // Return a very large number for unlimited
        }
        double effectiveUsed = this.usedDays + (this.accumulatedHalfDays * 0.5);
        return this.totalEntitlement - effectiveUsed;
    }

    // Check if can take half day
    public boolean canTakeHalfDay() {
        if (isUnlimited()) {
            return true;
        }
        return getEffectiveRemainingDays() >= 0.5;
    }

    // Get display string for remaining days
    public String getRemainingDaysDisplay() {
        if (isUnlimited()) {
            return "Unlimited";
        }
        return String.format("%.1f", remainingDays);
    }

    // Get display string for total entitlement
    public String getTotalEntitlementDisplay() {
        if (isUnlimited()) {
            return "Unlimited";
        }
        return String.valueOf(totalEntitlement);
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public int getTotalEntitlement() { return totalEntitlement; }
    public void setTotalEntitlement(int totalEntitlement) {
        this.totalEntitlement = totalEntitlement;
        // Recalculate remaining days
        if (totalEntitlement == -1) {
            this.remainingDays = -1.0; // Unlimited
        } else {
            this.remainingDays = totalEntitlement - this.usedDays;
        }
    }

    public double getUsedDays() { return usedDays; }
    public void setUsedDays(double usedDays) {
        this.usedDays = usedDays;
        if (!isUnlimited()) {
            this.remainingDays = this.totalEntitlement - usedDays;
        }
    }

    public double getRemainingDays() { return remainingDays; }
    public void setRemainingDays(double remainingDays) { this.remainingDays = remainingDays; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getAccumulatedHalfDays() { return accumulatedHalfDays; }
    public void setAccumulatedHalfDays(int accumulatedHalfDays) { this.accumulatedHalfDays = accumulatedHalfDays; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}