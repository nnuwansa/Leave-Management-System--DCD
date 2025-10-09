package com.LeaveDataManagementSystem.LeaveManagement.Model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "Historical Leave Summaries")
public class HistoricalLeaveSummary {
    @Id
    private String id;

    private String employeeEmail;
    private int year;

    // Leave type summaries
    private double casualUsed = 0.0;
    private int casualTotal = 21;
    private double sickUsed = 0.0;
    private int sickTotal = 24;
    private double dutyUsed = 0.0; // Track duty leave usage even though unlimited

    // Short leave monthly breakdown (Jan-Dec)
    private Map<String, Map<String, Integer>> shortLeaveMonthlyDetails = new HashMap<>();

    // Additional fields for historical data
    private String notes; // Optional notes about the historical data
    private String addedBy; // Email of admin who added this record

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Constructors
    public HistoricalLeaveSummary() {
        initializeShortLeaveMonthlyDetails();
    }

    public HistoricalLeaveSummary(String employeeEmail, int year) {
        this.employeeEmail = employeeEmail;
        this.year = year;
        initializeShortLeaveMonthlyDetails();
    }

    // Initialize short leave monthly details with default values
    private void initializeShortLeaveMonthlyDetails() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        for (String month : months) {
            Map<String, Integer> monthData = new HashMap<>();
            monthData.put("used", 0);
            monthData.put("total", 2);
            shortLeaveMonthlyDetails.put(month, monthData);
        }
    }

    // Calculate total short leave used across all months
    public int getTotalShortLeaveUsed() {
        return shortLeaveMonthlyDetails.values().stream()
                .mapToInt(monthData -> monthData.getOrDefault("used", 0))
                .sum();
    }

    // Calculate total short leave available across all months
    public int getTotalShortLeaveAvailable() {
        return shortLeaveMonthlyDetails.values().stream()
                .mapToInt(monthData -> monthData.getOrDefault("total", 2))
                .sum();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public double getCasualUsed() { return casualUsed; }
    public void setCasualUsed(double casualUsed) { this.casualUsed = casualUsed; }

    public int getCasualTotal() { return casualTotal; }
    public void setCasualTotal(int casualTotal) { this.casualTotal = casualTotal; }

    public double getSickUsed() { return sickUsed; }
    public void setSickUsed(double sickUsed) { this.sickUsed = sickUsed; }

    public int getSickTotal() { return sickTotal; }
    public void setSickTotal(int sickTotal) { this.sickTotal = sickTotal; }

    public double getDutyUsed() { return dutyUsed; }
    public void setDutyUsed(double dutyUsed) { this.dutyUsed = dutyUsed; }

    public Map<String, Map<String, Integer>> getShortLeaveMonthlyDetails() { return shortLeaveMonthlyDetails; }
    public void setShortLeaveMonthlyDetails(Map<String, Map<String, Integer>> shortLeaveMonthlyDetails) {
        this.shortLeaveMonthlyDetails = shortLeaveMonthlyDetails;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAddedBy() { return addedBy; }
    public void setAddedBy(String addedBy) { this.addedBy = addedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}