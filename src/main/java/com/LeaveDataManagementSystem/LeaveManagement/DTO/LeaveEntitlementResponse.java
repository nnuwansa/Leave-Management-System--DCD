package com.LeaveDataManagementSystem.LeaveManagement.DTO;

import com.LeaveDataManagementSystem.LeaveManagement.Model.LeaveEntitlement;

public class LeaveEntitlementResponse {
    private String leaveType;
    private int totalEntitlement; // -1 for unlimited
    private double usedDays;
    private double remainingDays; // -1 for unlimited
    private int year;
    private String displayName;
    private boolean isUnlimited;
    private int accumulatedHalfDays;
    private double effectiveUsedDays;
    private double effectiveRemainingDays;
    private String totalEntitlementDisplay;
    private String remainingDaysDisplay;

    public LeaveEntitlementResponse() {}

    public LeaveEntitlementResponse(LeaveEntitlement entitlement) {
        this.leaveType = entitlement.getLeaveType();
        this.totalEntitlement = entitlement.getTotalEntitlement();
        this.usedDays = entitlement.getUsedDays();
        this.remainingDays = entitlement.getRemainingDays();
        this.year = entitlement.getYear();
        this.isUnlimited = entitlement.isUnlimited();
        this.accumulatedHalfDays = entitlement.getAccumulatedHalfDays();
        this.displayName = formatLeaveTypeName(entitlement.getLeaveType());

        // Calculate effective values
        this.effectiveUsedDays = entitlement.getUsedDays() + (entitlement.getAccumulatedHalfDays() * 0.5);

        if (entitlement.isUnlimited()) {
            this.effectiveRemainingDays = Double.MAX_VALUE;
            this.totalEntitlementDisplay = "Unlimited";
            this.remainingDaysDisplay = "Unlimited";
        } else {
            this.effectiveRemainingDays = entitlement.getEffectiveRemainingDays();
            this.totalEntitlementDisplay = String.valueOf(this.totalEntitlement);
            this.remainingDaysDisplay = String.format("%.1f", this.effectiveRemainingDays);
        }
    }

    private String formatLeaveTypeName(String leaveType) {
        return switch (leaveType) {
            case "CASUAL" -> "Casual Leave";
            case "SICK" -> "Medical Leave";
            case "DUTY" -> "Duty Leave";
            case "MATERNITY" -> "Maternity Leave";
            case "HALF_DAY" -> "Half Day Leave";
            case "SHORT" -> "Short Leave";
            default -> leaveType.replace("_", " ");
        };
    }

    // Getters and Setters
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public int getTotalEntitlement() { return totalEntitlement; }
    public void setTotalEntitlement(int totalEntitlement) { this.totalEntitlement = totalEntitlement; }

    public double getUsedDays() { return usedDays; }
    public void setUsedDays(double usedDays) { this.usedDays = usedDays; }

    public double getRemainingDays() { return remainingDays; }
    public void setRemainingDays(double remainingDays) { this.remainingDays = remainingDays; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public boolean isUnlimited() { return isUnlimited; }
    public void setUnlimited(boolean unlimited) { isUnlimited = unlimited; }

    public int getAccumulatedHalfDays() { return accumulatedHalfDays; }
    public void setAccumulatedHalfDays(int accumulatedHalfDays) { this.accumulatedHalfDays = accumulatedHalfDays; }

    public double getEffectiveUsedDays() { return effectiveUsedDays; }
    public void setEffectiveUsedDays(double effectiveUsedDays) { this.effectiveUsedDays = effectiveUsedDays; }

    public double getEffectiveRemainingDays() { return effectiveRemainingDays; }
    public void setEffectiveRemainingDays(double effectiveRemainingDays) { this.effectiveRemainingDays = effectiveRemainingDays; }

    public String getTotalEntitlementDisplay() { return totalEntitlementDisplay; }
    public void setTotalEntitlementDisplay(String totalEntitlementDisplay) { this.totalEntitlementDisplay = totalEntitlementDisplay; }

    public String getRemainingDaysDisplay() { return remainingDaysDisplay; }
    public void setRemainingDaysDisplay(String remainingDaysDisplay) { this.remainingDaysDisplay = remainingDaysDisplay; }

    // Helper methods
    public double getUsagePercentage() {
        if (isUnlimited) return 0; // No percentage for unlimited
        return totalEntitlement > 0 ? (effectiveUsedDays / totalEntitlement * 100) : 0;
    }

    public boolean isOverused() {
        if (isUnlimited) return false; // Cannot overuse unlimited leave
        return effectiveUsedDays > totalEntitlement;
    }

    public boolean hasHalfDays() {
        return accumulatedHalfDays > 0;
    }
}

