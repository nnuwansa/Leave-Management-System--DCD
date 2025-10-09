package com.LeaveDataManagementSystem.LeaveManagement.Service;

import com.LeaveDataManagementSystem.LeaveManagement.Model.HistoricalLeaveSummary;
import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.HistoricalLeaveSummaryRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HistoricalLeaveSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalLeaveSummaryService.class);

    @Autowired
    private HistoricalLeaveSummaryRepository historicalLeaveSummaryRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create or update historical leave summary for an employee
     */
    public HistoricalLeaveSummary createOrUpdateHistoricalSummary(String employeeEmail, int year,
                                                                  HistoricalLeaveSummary summaryData, String addedBy) {

        logger.info("Creating/updating historical summary for employee: {}, year: {}", employeeEmail, year);

        Optional<HistoricalLeaveSummary> existingOpt =
                historicalLeaveSummaryRepository.findByEmployeeEmailAndYear(employeeEmail, year);

        HistoricalLeaveSummary summary;
        if (existingOpt.isPresent()) {
            summary = existingOpt.get();
            logger.info("Updating existing historical summary for employee: {}, year: {}", employeeEmail, year);
        } else {
            summary = new HistoricalLeaveSummary(employeeEmail, year);
            logger.info("Creating new historical summary for employee: {}, year: {}", employeeEmail, year);
        }

        // Update the summary with provided data
        summary.setCasualUsed(summaryData.getCasualUsed());
        summary.setCasualTotal(summaryData.getCasualTotal());
        summary.setSickUsed(summaryData.getSickUsed());
        summary.setSickTotal(summaryData.getSickTotal());
        summary.setDutyUsed(summaryData.getDutyUsed());
        summary.setShortLeaveMonthlyDetails(summaryData.getShortLeaveMonthlyDetails());
        summary.setNotes(summaryData.getNotes());
        summary.setAddedBy(addedBy);

        HistoricalLeaveSummary saved = historicalLeaveSummaryRepository.save(summary);
        logger.info("Historical summary saved successfully for employee: {}, year: {}", employeeEmail, year);

        return saved;
    }

    /**
     * Bulk create/update historical summaries for multiple employees
     */
    public List<HistoricalLeaveSummary> bulkCreateHistoricalSummaries(
            List<Map<String, Object>> employeesSummaryData, String addedBy) {

        List<HistoricalLeaveSummary> results = new ArrayList<>();

        for (Map<String, Object> employeeData : employeesSummaryData) {
            try {
                String employeeEmail = (String) employeeData.get("employeeEmail");
                int year = (Integer) employeeData.get("year");

                HistoricalLeaveSummary summaryData = mapToHistoricalSummary(employeeData);
                HistoricalLeaveSummary result = createOrUpdateHistoricalSummary(
                        employeeEmail, year, summaryData, addedBy);
                results.add(result);

            } catch (Exception e) {
                logger.error("Error processing employee data: {}", employeeData, e);
                // Continue with other employees even if one fails
            }
        }

        logger.info("Bulk historical summaries created/updated: {} out of {} employees",
                results.size(), employeesSummaryData.size());

        return results;
    }

    /**
     * Get historical summary for specific employee and year
     */
    public Optional<HistoricalLeaveSummary> getHistoricalSummary(String employeeEmail, int year) {
        return historicalLeaveSummaryRepository.findByEmployeeEmailAndYear(employeeEmail, year);
    }

    /**
     * Get all historical summaries for an employee
     */
    public List<HistoricalLeaveSummary> getEmployeeHistoricalSummaries(String employeeEmail) {
        return historicalLeaveSummaryRepository.findByEmployeeEmailOrderByYearDesc(employeeEmail);
    }

    /**
     * Get all historical summaries for a specific year
     */
    public List<HistoricalLeaveSummary> getHistoricalSummariesByYear(int year) {
        return historicalLeaveSummaryRepository.findByYearOrderByEmployeeEmail(year);
    }

    /**
     * Get all years that have historical data
     */
    public List<Integer> getAvailableHistoricalYears() {
        return historicalLeaveSummaryRepository.getYearSummaries().stream()
                .map(summary -> (Integer) summary.get("_id"))
                .collect(Collectors.toList());
    }

    /**
     * Delete historical summary for specific employee and year
     */
    public boolean deleteHistoricalSummary(String employeeEmail, int year) {
        Optional<HistoricalLeaveSummary> existingOpt =
                historicalLeaveSummaryRepository.findByEmployeeEmailAndYear(employeeEmail, year);

        if (existingOpt.isPresent()) {
            historicalLeaveSummaryRepository.delete(existingOpt.get());
            logger.info("Deleted historical summary for employee: {}, year: {}", employeeEmail, year);
            return true;
        }

        logger.warn("No historical summary found to delete for employee: {}, year: {}", employeeEmail, year);
        return false;
    }

    /**
     * Get comprehensive historical summary with employee details
     */
    public Map<String, Object> getHistoricalSummaryWithEmployeeDetails(String employeeEmail, int year) {
        Optional<HistoricalLeaveSummary> summaryOpt = getHistoricalSummary(employeeEmail, year);

        if (summaryOpt.isEmpty()) {
            return null;
        }

        HistoricalLeaveSummary summary = summaryOpt.get();
        // Fixed: Handle the case where findByEmail returns User directly, not Optional<User>
        User user = userRepository.findByEmail(employeeEmail);

        Map<String, Object> result = new HashMap<>();
        result.put("historicalSummary", summary);

        if (user != null) {
            Map<String, Object> employeeDetails = new HashMap<>();
            employeeDetails.put("email", user.getEmail());
            employeeDetails.put("name", user.getName());
            employeeDetails.put("fullName", user.getFullName());
            employeeDetails.put("department", user.getDepartment());
            employeeDetails.put("designation", user.getDesignation());

            result.put("employeeDetails", employeeDetails);
        }

        return result;
    }

    /**
     * Get all historical summaries for a year with employee details
     */
    public List<Map<String, Object>> getHistoricalSummariesWithEmployeeDetailsByYear(int year) {
        List<HistoricalLeaveSummary> summaries = getHistoricalSummariesByYear(year);
        List<Map<String, Object>> results = new ArrayList<>();

        for (HistoricalLeaveSummary summary : summaries) {
            Map<String, Object> employeeData = getHistoricalSummaryWithEmployeeDetails(
                    summary.getEmployeeEmail(), year);
            if (employeeData != null) {
                results.add(employeeData);
            }
        }

        return results;
    }

    /**
     * Check if historical data exists for a specific year
     */
    public boolean hasHistoricalDataForYear(int year) {
        return !historicalLeaveSummaryRepository.findByYear(year).isEmpty();
    }

    /**
     * Get year summary statistics
     */
    public Map<String, Object> getYearSummaryStatistics() {
        List<Map<String, Object>> yearSummaries = historicalLeaveSummaryRepository.getYearSummaries();

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalYears", yearSummaries.size());
        statistics.put("yearBreakdown", yearSummaries);

        if (!yearSummaries.isEmpty()) {
            int totalEmployeesWithHistoricalData = yearSummaries.stream()
                    .mapToInt(summary -> (Integer) summary.get("count"))
                    .sum();
            statistics.put("totalHistoricalRecords", totalEmployeesWithHistoricalData);
        }

        return statistics;
    }

    /**
     * Helper method to map request data to HistoricalLeaveSummary object
     */
    private HistoricalLeaveSummary mapToHistoricalSummary(Map<String, Object> data) {
        HistoricalLeaveSummary summary = new HistoricalLeaveSummary();

        // Map basic leave data
        summary.setCasualUsed(getDoubleValue(data, "casualUsed", 0.0));
        summary.setCasualTotal(getIntValue(data, "casualTotal", 21));
        summary.setSickUsed(getDoubleValue(data, "sickUsed", 0.0));
        summary.setSickTotal(getIntValue(data, "sickTotal", 24));
        summary.setDutyUsed(getDoubleValue(data, "dutyUsed", 0.0));
        summary.setNotes((String) data.get("notes"));

        // Map short leave monthly details
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> monthlyDetails =
                (Map<String, Map<String, Integer>>) data.get("shortLeaveMonthlyDetails");

        if (monthlyDetails != null) {
            summary.setShortLeaveMonthlyDetails(monthlyDetails);
        }

        return summary;
    }

    private double getDoubleValue(Map<String, Object> data, String key, double defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }

    private int getIntValue(Map<String, Object> data, String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
}