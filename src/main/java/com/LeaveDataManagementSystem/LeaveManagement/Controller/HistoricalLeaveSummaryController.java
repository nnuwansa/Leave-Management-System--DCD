package com.LeaveDataManagementSystem.LeaveManagement.Controller;

import com.LeaveDataManagementSystem.LeaveManagement.Config.JwtUtil;
import com.LeaveDataManagementSystem.LeaveManagement.Model.HistoricalLeaveSummary;
import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Service.HistoricalLeaveSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/historical-leave-summaries")
public class HistoricalLeaveSummaryController {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalLeaveSummaryController.class);

    @Autowired
    private HistoricalLeaveSummaryService historicalLeaveSummaryService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    // ---------------- Create/Update Historical Summary for Single Employee ----------------
    @PostMapping("/employee")
    public ResponseEntity<?> createEmployeeHistoricalSummary(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String adminEmail = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            String employeeEmail = (String) request.get("employeeEmail");
            int year = (Integer) request.get("year");

            // Create HistoricalLeaveSummary object from request data
            HistoricalLeaveSummary summaryData = mapRequestToHistoricalSummary(request);

            HistoricalLeaveSummary result = historicalLeaveSummaryService.createOrUpdateHistoricalSummary(
                    employeeEmail, year, summaryData, adminEmail);

            logger.info("Historical summary created/updated by admin {} for employee {} (year {})",
                    adminEmail, employeeEmail, year);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Historical leave summary saved successfully",
                    "data", result
            ));

        } catch (Exception e) {
            logger.error("Error creating historical summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to create historical summary: " + e.getMessage()
            ));
        }
    }

    // ---------------- Bulk Create/Update Historical Summaries ----------------
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkCreateHistoricalSummaries(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String adminEmail = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> employeesSummaryData =
                    (List<Map<String, Object>>) request.get("employees");

            if (employeesSummaryData == null || employeesSummaryData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "No employee data provided"
                ));
            }

            List<HistoricalLeaveSummary> results = historicalLeaveSummaryService.bulkCreateHistoricalSummaries(
                    employeesSummaryData, adminEmail);

            logger.info("Bulk historical summaries created by admin {}: {} records processed",
                    adminEmail, results.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format("Successfully processed %d historical summaries", results.size()),
                    "processedCount", results.size(),
                    "totalRequested", employeesSummaryData.size(),
                    "data", results
            ));

        } catch (Exception e) {
            logger.error("Error in bulk creating historical summaries: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to bulk create historical summaries: " + e.getMessage()
            ));
        }
    }

    // ---------------- Get Historical Summary for Specific Employee and Year ----------------
    @GetMapping("/employee/{email}/year/{year}")
    public ResponseEntity<?> getEmployeeHistoricalSummary(
            @PathVariable String email,
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = historicalLeaveSummaryService.getHistoricalSummaryWithEmployeeDetails(email, year);

            if (result == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No historical data found for employee " + email + " in year " + year
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result
            ));

        } catch (Exception e) {
            logger.error("Error fetching historical summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch historical summary: " + e.getMessage()
            ));
        }
    }

    // ---------------- Get All Historical Summaries for a Specific Year ----------------
    @GetMapping("/year/{year}")
    public ResponseEntity<?> getHistoricalSummariesByYear(
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            List<Map<String, Object>> results = historicalLeaveSummaryService.getHistoricalSummariesWithEmployeeDetailsByYear(year);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "year", year,
                    "employeeCount", results.size(),
                    "data", results
            ));

        } catch (Exception e) {
            logger.error("Error fetching historical summaries for year {}: {}", year, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch historical summaries for year " + year + ": " + e.getMessage()
            ));
        }
    }

    // ---------------- Get All Available Historical Years ----------------
    @GetMapping("/available-years")
    public ResponseEntity<?> getAvailableHistoricalYears(@RequestHeader("Authorization") String token) {
        try {
            List<Integer> years = historicalLeaveSummaryService.getAvailableHistoricalYears();
            Map<String, Object> statistics = historicalLeaveSummaryService.getYearSummaryStatistics();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "availableYears", years,
                    "statistics", statistics
            ));

        } catch (Exception e) {
            logger.error("Error fetching available historical years: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch available historical years: " + e.getMessage()
            ));
        }
    }

    // ---------------- Get All Historical Summaries for a Specific Employee ----------------
    // ---------------- Get All Historical Summaries for a Specific Employee ----------------
    @GetMapping("/employee/{email}")
    public ResponseEntity<?> getEmployeeHistoricalSummaries(
            @PathVariable String email,
            @RequestHeader("Authorization") String token) {
        try {
            List<HistoricalLeaveSummary> summaries = historicalLeaveSummaryService.getEmployeeHistoricalSummaries(email);

            // Get employee details - Fixed: userRepository.findByEmail returns User directly, not Optional<User>
            User user = userRepository.findByEmail(email);
            Map<String, Object> employeeDetails = new HashMap<>();

            if (user != null) {
                employeeDetails.put("email", user.getEmail());
                employeeDetails.put("name", user.getName());
                employeeDetails.put("fullName", user.getFullName());
                employeeDetails.put("department", user.getDepartment());
                employeeDetails.put("designation", user.getDesignation());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "employeeDetails", employeeDetails,
                    "historicalSummaries", summaries,
                    "yearCount", summaries.size()
            ));

        } catch (Exception e) {
            logger.error("Error fetching employee historical summaries: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to fetch employee historical summaries: " + e.getMessage()
            ));
        }
    }

    // ---------------- Delete Historical Summary ----------------
    @DeleteMapping("/employee/{email}/year/{year}")
    public ResponseEntity<?> deleteHistoricalSummary(
            @PathVariable String email,
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            String adminEmail = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            boolean deleted = historicalLeaveSummaryService.deleteHistoricalSummary(email, year);

            if (deleted) {
                logger.info("Historical summary deleted by admin {} for employee {} (year {})",
                        adminEmail, email, year);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Historical summary deleted successfully"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No historical summary found to delete"
                ));
            }

        } catch (Exception e) {
            logger.error("Error deleting historical summary: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to delete historical summary: " + e.getMessage()
            ));
        }
    }

    // ---------------- Check if Historical Data Exists for Year ----------------
    @GetMapping("/year/{year}/exists")
    public ResponseEntity<?> checkHistoricalDataExists(
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            boolean exists = historicalLeaveSummaryService.hasHistoricalDataForYear(year);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "year", year,
                    "hasHistoricalData", exists
            ));

        } catch (Exception e) {
            logger.error("Error checking historical data existence: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to check historical data existence: " + e.getMessage()
            ));
        }
    }

    // ---------------- Helper Methods ----------------

    private HistoricalLeaveSummary mapRequestToHistoricalSummary(Map<String, Object> request) {
        HistoricalLeaveSummary summary = new HistoricalLeaveSummary();

        // Map basic leave data
        summary.setCasualUsed(getDoubleValue(request, "casualUsed", 0.0));
        summary.setCasualTotal(getIntValue(request, "casualTotal", 21));
        summary.setSickUsed(getDoubleValue(request, "sickUsed", 0.0));
        summary.setSickTotal(getIntValue(request, "sickTotal", 24));
        summary.setDutyUsed(getDoubleValue(request, "dutyUsed", 0.0));
        summary.setNotes((String) request.get("notes"));

        // Map short leave monthly details
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> monthlyDetails =
                (Map<String, Map<String, Integer>>) request.get("shortLeaveMonthlyDetails");

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