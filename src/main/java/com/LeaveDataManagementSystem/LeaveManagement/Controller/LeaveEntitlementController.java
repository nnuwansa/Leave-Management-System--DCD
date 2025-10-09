package com.LeaveDataManagementSystem.LeaveManagement.Controller;

import com.LeaveDataManagementSystem.LeaveManagement.Config.JwtUtil;
import com.LeaveDataManagementSystem.LeaveManagement.Model.HistoricalLeaveSummary;
import com.LeaveDataManagementSystem.LeaveManagement.Model.LeaveEntitlement;
import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Service.HistoricalLeaveSummaryService;
import com.LeaveDataManagementSystem.LeaveManagement.Service.LeaveEntitlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/entitlements")
public class LeaveEntitlementController {
    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    @Autowired
    private LeaveEntitlementService leaveEntitlementService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private HistoricalLeaveSummaryService historicalLeaveSummaryService;

    // ---------------- Get My Entitlements ----------------
    @GetMapping("/my-entitlements")
    public ResponseEntity<?> getMyEntitlements(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            List<LeaveEntitlement> entitlements = leaveEntitlementService.getEmployeeEntitlements(email);
            return ResponseEntity.ok(entitlements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch entitlements");
        }
    }

    // ---------------- Get Entitlement Summary ----------------
    @GetMapping("/summary")
    public ResponseEntity<?> getMyEntitlementSummary(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Map<String, Object> summary = leaveEntitlementService.getEntitlementSummary(email);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch entitlement summary");
        }
    }

    // ---------------- Get Entitlements by Year ----------------
    @GetMapping("/year/{year}")
    public ResponseEntity<?> getMyEntitlementsByYear(
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            List<LeaveEntitlement> entitlements = leaveEntitlementService.getEmployeeEntitlementsByYear(email, year);
            return ResponseEntity.ok(entitlements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch entitlements for year " + year);
        }
    }

    // ---------------- Get DUTY Leave Statistics ----------------
    @GetMapping("/duty-leave-stats")
    public ResponseEntity<?> getMyDutyLeaveStatistics(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Map<String, Object> dutyStats = leaveEntitlementService.getDutyLeaveStatistics(email);
            return ResponseEntity.ok(dutyStats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch DUTY leave statistics");
        }
    }

    // ---------------- Get DUTY Leave Statistics by Year ----------------
    @GetMapping("/duty-leave-stats/{year}")
    public ResponseEntity<?> getMyDutyLeaveStatisticsByYear(
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Map<String, Object> dutyStats = leaveEntitlementService.getDutyLeaveStatistics(email, year);
            return ResponseEntity.ok(dutyStats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch DUTY leave statistics for year " + year);
        }
    }

    // ---------------- Validate Leave Request  ----------------
    @PostMapping("/validate")
    public ResponseEntity<?> validateLeaveRequest(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            String leaveType = (String) request.get("leaveType");
            LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
            LocalDate endDate = LocalDate.parse((String) request.get("endDate"));

            // Handle half-day flag
            Boolean isHalfDay = (Boolean) request.get("isHalfDay");
            String halfDayPeriod = (String) request.get("halfDayPeriod");

            String validation;

            if (isHalfDay != null && isHalfDay) {
                // For half-day leaves, validate with the actual leave type
                validation = leaveEntitlementService.validateLeaveRequest(
                        email, leaveType, startDate, endDate, true, halfDayPeriod
                );
            } else {
                // For regular leaves (including DUTY)
                validation = leaveEntitlementService.validateLeaveRequest(
                        email, leaveType, startDate, endDate, false, null
                );
            }

            if ("VALID".equals(validation)) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);

                // Special message for DUTY leave
                if ("DUTY".equals(leaveType)) {
                    response.put("message", "DUTY leave request is valid (Unlimited entitlement - no balance deduction)");
                } else {
                    response.put("message", "Leave request is valid");
                }

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of("valid", false, "message", validation));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("❌ Failed to validate leave request: " + e.getMessage());
        }
    }

    // ---------------- Recalculate Entitlements ----------------
    @PostMapping("/recalculate")
    public ResponseEntity<?> recalculateMyEntitlements(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            leaveEntitlementService.recalculateEntitlements(email);
            return ResponseEntity.ok("✅ Entitlements recalculated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to recalculate entitlements");
        }
    }

    // ================ ADMIN ENDPOINTS ================

    // ---------------- Get Employee Entitlements (Admin) ----------------
    @GetMapping("/admin/employee/{email}")
    public ResponseEntity<?> getEmployeeEntitlements(
            @PathVariable String email,
            @RequestHeader("Authorization") String token) {
        try {
            List<LeaveEntitlement> entitlements = leaveEntitlementService.getEmployeeEntitlements(email);
            return ResponseEntity.ok(entitlements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch employee entitlements");
        }
    }

    // ---------------- Get Employee Entitlement Summary (Admin) ----------------
    @GetMapping("/admin/employee/{email}/summary")
    public ResponseEntity<?> getEmployeeEntitlementSummary(
            @PathVariable String email,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> summary = leaveEntitlementService.getEntitlementSummary(email);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch employee entitlement summary");
        }
    }

    // ---------------- Get Employee DUTY Leave Statistics (Admin) ----------------
    @GetMapping("/admin/employee/{email}/duty-leave-stats")
    public ResponseEntity<?> getEmployeeDutyLeaveStatistics(
            @PathVariable String email,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> dutyStats = leaveEntitlementService.getDutyLeaveStatistics(email);
            return ResponseEntity.ok(dutyStats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch employee DUTY leave statistics");
        }
    }

    // ---------------- Get Employee DUTY Leave Statistics by Year (Admin) ----------------
    @GetMapping("/admin/employee/{email}/duty-leave-stats/{year}")
    public ResponseEntity<?> getEmployeeDutyLeaveStatisticsByYear(
            @PathVariable String email,
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> dutyStats = leaveEntitlementService.getDutyLeaveStatistics(email, year);
            return ResponseEntity.ok(dutyStats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch employee DUTY leave statistics for year " + year);
        }
    }

    // ---------------- Adjust Employee Entitlement (Admin) ----------------
    @PutMapping("/admin/adjust")
    public ResponseEntity<?> adjustEmployeeEntitlement(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String employeeEmail = (String) request.get("employeeEmail");
            String leaveType = (String) request.get("leaveType");
            int year = (Integer) request.get("year");
            Object newTotalEntitlementObj = request.get("newTotalEntitlement");

            int newTotalEntitlement;
            if (newTotalEntitlementObj instanceof String && "unlimited".equalsIgnoreCase((String) newTotalEntitlementObj)) {
                newTotalEntitlement = -1; // Set as unlimited
            } else {
                newTotalEntitlement = (Integer) newTotalEntitlementObj;
            }

            leaveEntitlementService.adjustEntitlement(employeeEmail, leaveType, year, newTotalEntitlement);

            String message = newTotalEntitlement == -1
                    ? "✅ Entitlement set to unlimited successfully"
                    : "✅ Entitlement adjusted successfully";
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to adjust entitlement");
        }
    }

    // ---------------- Initialize Entitlements for Employee (Admin) ----------------
    @PostMapping("/admin/initialize/{email}")
    public ResponseEntity<?> initializeEntitlementsForEmployee(
            @PathVariable String email,
            @RequestHeader("Authorization") String token) {
        try {
            leaveEntitlementService.initializeEntitlementsForEmployee(email);
            return ResponseEntity.ok("✅ Entitlements initialized successfully for " + email + " (DUTY leave set as unlimited)");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to initialize entitlements");
        }
    }

    // ---------------- Initialize Entitlements for New Year (Admin) ----------------
    @PostMapping("/admin/initialize-year/{year}")
    public ResponseEntity<?> initializeEntitlementsForYear(
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            leaveEntitlementService.initializeEntitlementsForNewYear(year);
            return ResponseEntity.ok("✅ Entitlements initialized for all employees for year " + year + " (DUTY leave set as unlimited)");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to initialize entitlements for year " + year);
        }
    }

    // ---------------- Recalculate Employee Entitlements (Admin) ----------------
    @PostMapping("/admin/recalculate/{email}")
    public ResponseEntity<?> recalculateEmployeeEntitlements(
            @PathVariable String email,
            @RequestHeader("Authorization") String token) {
        try {
            leaveEntitlementService.recalculateEntitlements(email);
            return ResponseEntity.ok("✅ Entitlements recalculated successfully for " + email);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to recalculate entitlements for " + email);
        }
    }

    // ---------------- Get All Employee DUTY Leave Statistics Report (Admin) ----------------
    @GetMapping("/admin/duty-leave-report/{year}")
    public ResponseEntity<?> getDutyLeaveReport(
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            // This would require a new service method to get all employees' DUTY leave stats
            // For now, return a simple message
            return ResponseEntity.ok("DUTY leave report endpoint - to be implemented based on specific requirements");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to generate DUTY leave report");
        }
    }



    // Add this method to LeaveEntitlementController.java

    @GetMapping("/admin/entitlements")
    public ResponseEntity<?> getAllEmployeeEntitlements(
            @RequestParam(defaultValue = "0") int year, // 0 means current year
            @RequestHeader("Authorization") String token) {
        try {
            int targetYear = year == 0 ? LocalDate.now().getYear() : year;

            // Check if this is current year or historical year
            boolean isCurrentYear = targetYear == LocalDate.now().getYear();

            if (isCurrentYear) {
                // Use existing logic for current year
                List<User> allUsers = userRepository.findAll();
                List<Map<String, Object>> entitlementsWithDetails = new ArrayList<>();

                for (User user : allUsers) {
                    Map<String, Object> employeeData = new HashMap<>();

                    // Get employee details
                    Map<String, Object> employeeDetails = new HashMap<>();
                    employeeDetails.put("email", user.getEmail());
                    employeeDetails.put("name", user.getName());
                    employeeDetails.put("fullName", user.getFullName());
                    employeeDetails.put("department", user.getDepartment());
                    employeeDetails.put("designation", user.getDesignation());

                    // Get regular entitlements
                    List<LeaveEntitlement> entitlements = leaveEntitlementService.getEmployeeEntitlementsByYear(user.getEmail(), targetYear);

                    // Initialize entitlements if empty (for current year)
                    if (entitlements.isEmpty()) {
                        leaveEntitlementService.initializeEntitlementsForEmployee(user.getEmail());
                        entitlements = leaveEntitlementService.getEmployeeEntitlementsByYear(user.getEmail(), targetYear);
                    }

                    // Get short leave monthly breakdown
                    Map<String, Object> shortLeaveMonthlyDetails = leaveEntitlementService.getEmployeeShortLeaveMonthlyBreakdown(user.getEmail());

                    employeeData.put("employeeDetails", employeeDetails);
                    employeeData.put("entitlements", entitlements);
                    employeeData.put("shortLeaveMonthlyDetails", shortLeaveMonthlyDetails);
                    employeeData.put("year", targetYear);
                    employeeData.put("isHistorical", false);

                    entitlementsWithDetails.add(employeeData);
                }

                return ResponseEntity.ok(entitlementsWithDetails);

            } else {
                // Use historical data for previous years
                List<Map<String, Object>> historicalData = historicalLeaveSummaryService.getHistoricalSummariesWithEmployeeDetailsByYear(targetYear);

                // Convert historical data to match the expected format
                List<Map<String, Object>> entitlementsWithDetails = new ArrayList<>();

                for (Map<String, Object> historicalEntry : historicalData) {
                    HistoricalLeaveSummary historicalSummary = (HistoricalLeaveSummary) historicalEntry.get("historicalSummary");
                    Map<String, Object> employeeDetails = (Map<String, Object>) historicalEntry.get("employeeDetails");

                    Map<String, Object> employeeData = new HashMap<>();
                    employeeData.put("employeeDetails", employeeDetails);
                    employeeData.put("year", targetYear);
                    employeeData.put("isHistorical", true);

                    // Convert historical summary to entitlements format
                    List<Map<String, Object>> entitlements = new ArrayList<>();

                    // Casual Leave
                    Map<String, Object> casualEntitlement = new HashMap<>();
                    casualEntitlement.put("leaveType", "CASUAL");
                    casualEntitlement.put("usedDays", historicalSummary.getCasualUsed());
                    casualEntitlement.put("totalEntitlement", historicalSummary.getCasualTotal());
                    casualEntitlement.put("remainingDays", historicalSummary.getCasualTotal() - historicalSummary.getCasualUsed());
                    casualEntitlement.put("accumulatedHalfDays", 0);
                    entitlements.add(casualEntitlement);

                    // Sick Leave
                    Map<String, Object> sickEntitlement = new HashMap<>();
                    sickEntitlement.put("leaveType", "SICK");
                    sickEntitlement.put("usedDays", historicalSummary.getSickUsed());
                    sickEntitlement.put("totalEntitlement", historicalSummary.getSickTotal());
                    sickEntitlement.put("remainingDays", historicalSummary.getSickTotal() - historicalSummary.getSickUsed());
                    sickEntitlement.put("accumulatedHalfDays", 0);
                    entitlements.add(sickEntitlement);

                    // Duty Leave
                    Map<String, Object> dutyEntitlement = new HashMap<>();
                    dutyEntitlement.put("leaveType", "DUTY");
                    dutyEntitlement.put("usedDays", historicalSummary.getDutyUsed());
                    dutyEntitlement.put("totalEntitlement", -1); // Unlimited
                    dutyEntitlement.put("remainingDays", -1.0); // Unlimited
                    dutyEntitlement.put("accumulatedHalfDays", 0);
                    entitlements.add(dutyEntitlement);

                    employeeData.put("entitlements", entitlements);
                    employeeData.put("shortLeaveMonthlyDetails", historicalSummary.getShortLeaveMonthlyDetails());

                    entitlementsWithDetails.add(employeeData);
                }

                // For employees without historical data, include them with zero values
                List<User> allUsers = userRepository.findAll();
                Set<String> historicalEmails = entitlementsWithDetails.stream()
                        .map(data -> (Map<String, Object>) data.get("employeeDetails"))
                        .map(details -> (String) details.get("email"))
                        .collect(Collectors.toSet());

                for (User user : allUsers) {
                    if (!historicalEmails.contains(user.getEmail())) {
                        Map<String, Object> employeeData = new HashMap<>();

                        // Get employee details
                        Map<String, Object> employeeDetails = new HashMap<>();
                        employeeDetails.put("email", user.getEmail());
                        employeeDetails.put("name", user.getName());
                        employeeDetails.put("fullName", user.getFullName());
                        employeeDetails.put("department", user.getDepartment());
                        employeeDetails.put("designation", user.getDesignation());

                        employeeData.put("employeeDetails", employeeDetails);
                        employeeData.put("year", targetYear);
                        employeeData.put("isHistorical", true);

                        // Create zero entitlements
                        List<Map<String, Object>> entitlements = new ArrayList<>();

                        // Casual Leave
                        Map<String, Object> casualEntitlement = new HashMap<>();
                        casualEntitlement.put("leaveType", "CASUAL");
                        casualEntitlement.put("usedDays", 0.0);
                        casualEntitlement.put("totalEntitlement", 21);
                        casualEntitlement.put("remainingDays", 21.0);
                        casualEntitlement.put("accumulatedHalfDays", 0);
                        entitlements.add(casualEntitlement);

                        // Sick Leave
                        Map<String, Object> sickEntitlement = new HashMap<>();
                        sickEntitlement.put("leaveType", "SICK");
                        sickEntitlement.put("usedDays", 0.0);
                        sickEntitlement.put("totalEntitlement", 24);
                        sickEntitlement.put("remainingDays", 24.0);
                        sickEntitlement.put("accumulatedHalfDays", 0);
                        entitlements.add(sickEntitlement);

                        // Duty Leave
                        Map<String, Object> dutyEntitlement = new HashMap<>();
                        dutyEntitlement.put("leaveType", "DUTY");
                        dutyEntitlement.put("usedDays", 0.0);
                        dutyEntitlement.put("totalEntitlement", -1);
                        dutyEntitlement.put("remainingDays", -1.0);
                        dutyEntitlement.put("accumulatedHalfDays", 0);
                        entitlements.add(dutyEntitlement);

                        employeeData.put("entitlements", entitlements);

                        // Create empty short leave monthly details
                        Map<String, Map<String, Integer>> shortLeaveMonthlyDetails = new HashMap<>();
                        String[] months = {"January", "February", "March", "April", "May", "June",
                                "July", "August", "September", "October", "November", "December"};

                        for (String month : months) {
                            Map<String, Integer> monthData = new HashMap<>();
                            monthData.put("used", 0);
                            monthData.put("total", 2);
                            shortLeaveMonthlyDetails.put(month, monthData);
                        }

                        employeeData.put("shortLeaveMonthlyDetails", shortLeaveMonthlyDetails);
                        entitlementsWithDetails.add(employeeData);
                    }
                }

                return ResponseEntity.ok(entitlementsWithDetails);
            }

        } catch (Exception e) {
            logger.error("Failed to fetch employee entitlements for year {}: {}", year, e.getMessage(), e);
            return ResponseEntity.badRequest().body("❌ Failed to fetch employee entitlements for year " + year);
        }
    }





    // ---------------- Get All Employee Entitlements with Monthly Data (Admin) ----------------
//    @GetMapping("/admin/entitlements")
//    public ResponseEntity<?> getAllEmployeeEntitlements(@RequestHeader("Authorization") String token) {
//        try {
//            List<User> allUsers = userRepository.findAll();
//            List<Map<String, Object>> entitlementsWithDetails = new ArrayList<>();
//
//            for (User user : allUsers) {
//                Map<String, Object> employeeData = new HashMap<>();
//
//                // Get employee details
//                Map<String, Object> employeeDetails = new HashMap<>();
//                employeeDetails.put("email", user.getEmail());
//                employeeDetails.put("name", user.getName());
//                employeeDetails.put("fullName", user.getFullName());
//                employeeDetails.put("department", user.getDepartment());
//                employeeDetails.put("designation", user.getDesignation());
//
//                // Get regular entitlements
//                List<LeaveEntitlement> entitlements = leaveEntitlementService.getEmployeeEntitlements(user.getEmail());
//
//                // Get short leave monthly breakdown
//                Map<String, Object> shortLeaveMonthlyDetails = leaveEntitlementService.getEmployeeShortLeaveMonthlyBreakdown(user.getEmail());
//
//                employeeData.put("employeeDetails", employeeDetails);
//                employeeData.put("entitlements", entitlements);
//                employeeData.put("shortLeaveMonthlyDetails", shortLeaveMonthlyDetails);
//
//                entitlementsWithDetails.add(employeeData);
//            }
//
//            return ResponseEntity.ok(entitlementsWithDetails);
//        } catch (Exception e) {
//            logger.error("Failed to fetch all employee entitlements: {}", e.getMessage(), e);
//            return ResponseEntity.badRequest().body("❌ Failed to fetch employee entitlements");
//        }
//    }
}