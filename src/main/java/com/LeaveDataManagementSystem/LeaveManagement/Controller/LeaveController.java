package com.LeaveDataManagementSystem.LeaveManagement.Controller;

import com.LeaveDataManagementSystem.LeaveManagement.Model.*;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.LeaveRequest;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.LeaveApprovalRequest;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.LeaveResponse;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.LeaveRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Service.LeaveEntitlementService;
import com.LeaveDataManagementSystem.LeaveManagement.Service.LeaveService;
import com.LeaveDataManagementSystem.LeaveManagement.Config.JwtUtil;
import com.LeaveDataManagementSystem.LeaveManagement.Service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/leaves")
public class LeaveController {

    private static final Logger logger = LoggerFactory.getLogger(LeaveController.class);

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveEntitlementService leaveEntitlementService;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    // ---------------- Submit Leave ----------------
    @PostMapping("/submit")
    public ResponseEntity<?> submitLeaveRequest(
            @RequestHeader("Authorization") String token,
            @RequestBody LeaveRequest leaveRequest) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            String result = leaveService.submitLeaveRequest(email, leaveRequest);
            return result.contains("successfully")
                    ? ResponseEntity.ok("✅ " + result)
                    : ResponseEntity.badRequest().body("❌ " + result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Invalid token or request data");
        }
    }

    // ---------------- Submit Half Day Leave ----------------
    @PostMapping("/submit-half-day")
    public ResponseEntity<?> submitHalfDayLeave(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            String leaveType = (String) request.get("leaveType");
            LocalDate date = LocalDate.parse((String) request.get("date"));
            String halfDayPeriod = (String) request.get("halfDayPeriod");
            String reason = (String) request.get("reason");
            String actingOfficerEmail = (String) request.get("actingOfficerEmail");
            String supervisingOfficerEmail = (String) request.get("supervisingOfficerEmail");
            String approvalOfficerEmail = (String) request.get("approvalOfficerEmail");

            LeaveRequest leaveRequest = new LeaveRequest(leaveType, date, halfDayPeriod, reason,
                    actingOfficerEmail, supervisingOfficerEmail, approvalOfficerEmail);

            String result = leaveService.submitLeaveRequest(email, leaveRequest);
            return result.contains("successfully")
                    ? ResponseEntity.ok("✅ " + result)
                    : ResponseEntity.badRequest().body("❌ " + result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Invalid request data");
        }
    }

    // ---------------- Submit Short Leave ----------------
    @PostMapping("/submit-short-leave")
    public ResponseEntity<?> submitShortLeave(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            LocalDate date = LocalDate.parse((String) request.get("date"));
            LocalTime startTime = LocalTime.parse((String) request.get("startTime"));
            LocalTime endTime = LocalTime.parse((String) request.get("endTime"));
            String reason = (String) request.get("reason");
            String actingOfficerEmail = (String) request.get("actingOfficerEmail");
            String supervisingOfficerEmail = (String) request.get("supervisingOfficerEmail");
            String approvalOfficerEmail = (String) request.get("approvalOfficerEmail");

            LeaveRequest leaveRequest = new LeaveRequest(date, startTime, endTime, reason,
                    actingOfficerEmail, supervisingOfficerEmail, approvalOfficerEmail);

            String result = leaveService.submitLeaveRequest(email, leaveRequest);
            return result.contains("successfully")
                    ? ResponseEntity.ok("✅ " + result)
                    : ResponseEntity.badRequest().body("❌ " + result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Invalid request data");
        }
    }

    // ---------------- Get My Leaves ----------------
    @GetMapping("/my-leaves")
    public ResponseEntity<?> getMyLeaves(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            List<Leave> leaves = leaveService.getEmployeeLeaves(email);
            return ResponseEntity.ok(leaves.stream().map(LeaveResponse::new).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Invalid token");
        }
    }

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

    // ---------------- Get My Short Leave Entitlements ----------------
    @GetMapping("/my-short-leave-entitlements")
    public ResponseEntity<?> getMyShortLeaveEntitlements(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            List<ShortLeaveEntitlement> shortLeaveEntitlements =
                    leaveEntitlementService.getEmployeeShortLeaveEntitlements(email);
            return ResponseEntity.ok(shortLeaveEntitlements);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch short leave entitlements");
        }
    }

    // ---------------- Get Entitlement Summary ----------------
    @GetMapping("/entitlement-summary")
    public ResponseEntity<?> getEntitlementSummary(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Map<String, Object> summary = leaveEntitlementService.getEntitlementSummary(email);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch entitlement summary");
        }
    }

    // ---------------- Get Comprehensive Entitlement Summary ----------------
    @GetMapping("/comprehensive-entitlement-summary")
    public ResponseEntity<?> getComprehensiveEntitlementSummary(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Map<String, Object> summary = leaveEntitlementService.getComprehensiveEntitlementSummary(email);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch comprehensive entitlement summary");
        }
    }

    // ---------------- Validate Leave Request ----------------
    @PostMapping("/validate")
    public ResponseEntity<?> validateLeaveRequest(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            String leaveType = (String) request.get("leaveType");
            LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
            LocalDate endDate = LocalDate.parse((String) request.get("endDate"));

            Boolean isHalfDay = (Boolean) request.get("isHalfDay");
            String halfDayPeriod = (String) request.get("halfDayPeriod");

            String validation;

            if ("SHORT".equals(leaveType) || "SHORT_LEAVE".equals(leaveType)) {
                validation = leaveEntitlementService.validateShortLeaveRequest(email, startDate);
            } else if ("HALF_DAY".equals(leaveType) || (isHalfDay != null && isHalfDay)) {
                validation = leaveEntitlementService.validateLeaveRequest(email, leaveType, startDate, endDate, true, halfDayPeriod);
            } else {
                validation = leaveEntitlementService.validateLeaveRequest(email, leaveType, startDate, endDate, false, null);
            }

            if ("VALID".equals(validation)) {
                return ResponseEntity.ok(Map.of("valid", true, "message", "Leave request is valid"));
            } else {
                return ResponseEntity.ok(Map.of("valid", false, "message", validation));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("❌ Failed to validate leave request: " + e.getMessage());
        }
    }

    // ---------------- Validate Regular Leave Request ----------------
    @PostMapping("/validate-entitlement")
    public ResponseEntity<?> validateLeaveEntitlement(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            String leaveType = (String) request.get("leaveType");
            LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
            LocalDate endDate = LocalDate.parse((String) request.get("endDate"));

            String validation = leaveEntitlementService.validateLeaveRequest(email, leaveType, startDate, endDate);

            if ("VALID".equals(validation)) {
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "message", "Leave request is valid",
                        "requestedDays", calculateDays(startDate, endDate)
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "valid", false,
                        "message", validation,
                        "requestedDays", calculateDays(startDate, endDate)
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to validate leave request");
        }
    }

    // ---------------- Validate Half Day Leave Request ----------------
    @PostMapping("/validate-half-day")
    public ResponseEntity<?> validateHalfDayLeave(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            LocalDate date = LocalDate.parse((String) request.get("date"));
            String halfDayPeriod = (String) request.get("halfDayPeriod");

            String validation = leaveEntitlementService.validateLeaveRequest(email, "CASUAL", date, date, true, halfDayPeriod);

            if ("VALID".equals(validation)) {
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "message", "Half day leave request is valid (will deduct 0.5 from CASUAL leave)",
                        "requestedDays", 0.5
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "valid", false,
                        "message", validation,
                        "requestedDays", 0.5
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to validate half day leave request: " + e.getMessage());
        }
    }

    // ---------------- Validate Short Leave Request ----------------
    @PostMapping("/validate-short-leave")
    public ResponseEntity<?> validateShortLeave(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            LocalDate date = LocalDate.parse((String) request.get("date"));

            String validation = leaveEntitlementService.validateShortLeaveRequest(email, date);

            if ("VALID".equals(validation)) {
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "message", "Short leave request is valid (does not deduct from leave balance)",
                        "requestedDays", 0
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "valid", false,
                        "message", validation,
                        "requestedDays", 0
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to validate short leave request: " + e.getMessage());
        }
    }

    // Helper method to calculate days
    private int calculateDays(LocalDate startDate, LocalDate endDate) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    // ---------------- Dashboard Counts ----------------
    @GetMapping("/dashboard/counts")
    public ResponseEntity<?> getDashboardCounts(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            return ResponseEntity.ok(new DashboardCounts(
                    leaveService.getPendingCountForActingOfficer(email),
                    leaveService.getPendingCountForSupervisingOfficer(email),
                    leaveService.getPendingCountForApprovalOfficer(email)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Invalid token");
        }
    }

    // ---------------- Pending Leaves ----------------
    @GetMapping("/pending/acting")
    public ResponseEntity<?> getPendingForActingOfficer(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            return ResponseEntity.ok(
                    leaveService.getPendingLeavesForActingOfficer(email).stream().map(LeaveResponse::new).toList()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Invalid token");
        }
    }

    @GetMapping("/pending/supervising")
    public ResponseEntity<?> getPendingSupervisingLeaves(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            List<Leave> pendingLeaves = leaveService.getPendingLeavesForSupervisingOfficer(email);
            return ResponseEntity.ok(
                    pendingLeaves.stream().map(LeaveResponse::new).collect(Collectors.toList())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Invalid token");
        }
    }

    @GetMapping("/pending/approval")
    public ResponseEntity<?> getPendingForApprovalOfficer(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            return ResponseEntity.ok(
                    leaveService.getPendingLeavesForApprovalOfficer(email).stream().map(LeaveResponse::new).toList()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Invalid token");
        }
    }

    // ---------------- Acting Officer Action ----------------
    @PostMapping("/{leaveId}/acting-action")
    public ResponseEntity<?> actingOfficerAction(
            @PathVariable String leaveId,
            @RequestHeader("Authorization") String token,
            @RequestBody LeaveApprovalRequest approvalRequest) {
        try {
            logger.info("Acting officer action requested for leave: {}", leaveId);
            logger.info("Action: {}", approvalRequest.getAction());

            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            logger.info("Extracted email: {}", email);

            String result = leaveService.processActingOfficerAction(leaveId, email, approvalRequest);
            logger.info("Service result: {}", result);

            return result.contains("approved") || result.contains("rejected")
                    ? ResponseEntity.ok("✅ " + result)
                    : ResponseEntity.badRequest().body("❌ " + result);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.error("JWT token expired: {}", e.getMessage());
            return ResponseEntity.status(401).body("❌ Token expired. Please login again.");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            logger.error("Malformed JWT token: {}", e.getMessage());
            return ResponseEntity.status(401).body("❌ Invalid token format");
        } catch (Exception e) {
            logger.error("Error in actingOfficerAction: ", e);
            return ResponseEntity.badRequest().body("❌ " + e.getMessage());
        }
    }

    @PostMapping("/{leaveId}/supervising-action")
    public ResponseEntity<?> processSupervisingOfficerAction(
            @PathVariable String leaveId,
            @RequestHeader("Authorization") String token,
            @RequestBody LeaveApprovalRequest approvalRequest) {
        try {
            logger.info("Supervising officer action requested for leave: {}", leaveId);

            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            logger.info("Extracted email: {}", email);

            String result = leaveService.processSupervisingOfficerAction(leaveId, email, approvalRequest);
            logger.info("Service result: {}", result);

            return result.contains("approved") || result.contains("rejected")
                    ? ResponseEntity.ok("✅ " + result)
                    : ResponseEntity.badRequest().body("❌ " + result);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.error("JWT token expired: {}", e.getMessage());
            return ResponseEntity.status(401).body("❌ Token expired. Please login again.");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            logger.error("Malformed JWT token: {}", e.getMessage());
            return ResponseEntity.status(401).body("❌ Invalid token format");
        } catch (Exception e) {
            logger.error("Error in processSupervisingOfficerAction: ", e);
            return ResponseEntity.badRequest().body("❌ " + e.getMessage());
        }
    }

    @PostMapping("/{leaveId}/approval-action")
    public ResponseEntity<?> approvalOfficerAction(
            @PathVariable String leaveId,
            @RequestHeader("Authorization") String token,
            @RequestBody LeaveApprovalRequest approvalRequest) {
        try {
            logger.info("Approval officer action requested for leave: {}", leaveId);

            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            logger.info("Extracted email: {}", email);

            String result = leaveService.processApprovalOfficerAction(leaveId, email, approvalRequest);
            logger.info("Service result: {}", result);

            return result.contains("approved") || result.contains("rejected")
                    ? ResponseEntity.ok("✅ " + result)
                    : ResponseEntity.badRequest().body("❌ " + result);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.error("JWT token expired: {}", e.getMessage());
            return ResponseEntity.status(401).body("❌ Token expired. Please login again.");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            logger.error("Malformed JWT token: {}", e.getMessage());
            return ResponseEntity.status(401).body("❌ Invalid token format");
        } catch (Exception e) {
            logger.error("Error in approvalOfficerAction: ", e);
            return ResponseEntity.badRequest().body("❌ " + e.getMessage());
        }
    }

    // ---------------- Recalculate Entitlements ----------------
    @PostMapping("/recalculate-entitlements")
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

    // ---------------- Initialize Entitlements for Employee (Admin) ----------------
    @PostMapping("/admin/initialize-entitlements/{email}")
    public ResponseEntity<?> initializeEntitlementsForEmployee(
            @PathVariable String email,
            @RequestHeader("Authorization") String token) {
        try {
            leaveEntitlementService.initializeEntitlementsForEmployee(email);
            return ResponseEntity.ok("✅ Entitlements initialized successfully for " + email);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to initialize entitlements");
        }
    }

    // ---------------- Adjust Employee Entitlement (Admin) ----------------
    @PutMapping("/admin/adjust-entitlement")
    public ResponseEntity<?> adjustEmployeeEntitlement(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String employeeEmail = (String) request.get("employeeEmail");
            String leaveType = (String) request.get("leaveType");
            int year = (Integer) request.get("year");
            int newTotalEntitlement = (Integer) request.get("newTotalEntitlement");

            leaveEntitlementService.adjustEntitlement(employeeEmail, leaveType, year, newTotalEntitlement);
            return ResponseEntity.ok("✅ Entitlement adjusted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to adjust entitlement");
        }
    }

    // ---------------- Get Employee Entitlements (Admin) ----------------
    @GetMapping("/admin/entitlements/{email}")
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
    @GetMapping("/admin/entitlement-summary/{email}")
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

    // ---------------- Get Employee Comprehensive Entitlement Summary (Admin) ----------------
    @GetMapping("/admin/comprehensive-entitlement-summary/{email}")
    public ResponseEntity<?> getEmployeeComprehensiveEntitlementSummary(
            @PathVariable String email,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> summary = leaveEntitlementService.getComprehensiveEntitlementSummary(email);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch comprehensive employee entitlement summary");
        }
    }

    // ---------------- Cancel Leave Endpoint ----------------
    @PostMapping("/{leaveId}/cancel")
    public ResponseEntity<?> cancelLeaveRequest(
            @PathVariable String leaveId,
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            String cancellationReason = (String) request.get("reason");

            logger.info("Cancellation request received for leave: {} by employee: {}", leaveId, email);

            if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("❌ Cancellation reason is required");
            }

            String result = leaveService.cancelLeaveRequest(leaveId, email, cancellationReason);

            if (result.contains("successfully")) {
                try {
                    logger.info("Forcing entitlement recalculation after leave cancellation for employee: {}", email);
                    leaveEntitlementService.forceRefreshEntitlements(email);
                    logger.info("Entitlement recalculation completed after leave cancellation");
                } catch (Exception e) {
                    logger.error("Error during entitlement recalculation after cancellation: {}", e.getMessage(), e);
                }

                return ResponseEntity.ok("✅ " + result);
            } else {
                return ResponseEntity.badRequest().body("❌ " + result);
            }

        } catch (Exception e) {
            logger.error("Error in cancel leave request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("❌ Failed to cancel leave request: " + e.getMessage());
        }
    }

    // ---------------- Check if Leave Can Be Cancelled ----------------
    @GetMapping("/{leaveId}/can-cancel")
    public ResponseEntity<?> canCancelLeave(
            @PathVariable String leaveId,
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            boolean canCancel = leaveService.canCancelLeave(leaveId, email);

            return ResponseEntity.ok(Map.of("canCancel", canCancel));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to check cancellation status");
        }
    }

    // ---------------- Get Cancellable Leaves ----------------
    @GetMapping("/cancellable")
    public ResponseEntity<?> getCancellableLeaves(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            List<Leave> cancellableLeaves = leaveService.getCancellableLeaves(email);

            return ResponseEntity.ok(cancellableLeaves.stream().map(LeaveResponse::new).toList());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch cancellable leaves");
        }
    }

    // Add a debug endpoint to check entitlement status (for testing only)
    @GetMapping("/debug/entitlements")
    public ResponseEntity<?> getEntitlementsDebug(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            List<LeaveEntitlement> entitlements = leaveEntitlementService.getEmployeeEntitlements(email);

            List<Map<String, Object>> recentLeaves = leaveRepository.findByEmployeeEmailOrderByCreatedAtDesc(email)
                    .stream()
                    .limit(10)
                    .map(leave -> {
                        Map<String, Object> leaveInfo = new HashMap<>();
                        leaveInfo.put("id", leave.getId());
                        leaveInfo.put("leaveType", leave.getLeaveType());
                        leaveInfo.put("startDate", leave.getStartDate());
                        leaveInfo.put("endDate", leave.getEndDate());
                        leaveInfo.put("status", leave.getStatus());
                        leaveInfo.put("isCancelled", leave.isCancelled());
                        leaveInfo.put("isHalfDay", leave.isHalfDay());
                        leaveInfo.put("isShortLeave", leave.isShortLeave());
                        leaveInfo.put("cancelledAt", leave.getCancelledAt());
                        leaveInfo.put("createdAt", leave.getCreatedAt());
                        return leaveInfo;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("employeeEmail", email);
            debugInfo.put("currentYear", LocalDate.now().getYear());
            debugInfo.put("entitlements", entitlements);
            debugInfo.put("recentLeaves", recentLeaves);
            debugInfo.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            logger.error("Error in debug entitlements: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("❌ Failed to get debug info: " + e.getMessage());
        }
    }

    // Add endpoint to force recalculation (for testing/admin use)
    @PostMapping("/debug/recalculate-entitlements")
    public ResponseEntity<?> forceRecalculateEntitlements(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            logger.info("Manual entitlement recalculation triggered for employee: {}", email);
            leaveEntitlementService.forceRefreshEntitlements(email);

            return ResponseEntity.ok("✅ Entitlements recalculated successfully for " + email);

        } catch (Exception e) {
            logger.error("Error in force recalculate: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("❌ Failed to recalculate entitlements: " + e.getMessage());
        }
    }

    // ---------------- Get All Leaves for Admin ----------------
    @GetMapping("/all-leaves")
    public ResponseEntity<?> getAllLeavesForAdmin(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            User currentUser = userRepository.findByEmail(email);
            if (currentUser == null) {
                return ResponseEntity.status(401).body("User not found");
            }

            if (!currentUser.getRoles().contains("ADMIN")) {
                return ResponseEntity.status(403).body("Access denied. Admin role required.");
            }

            List<Leave> allLeaves = leaveRepository.findAllByOrderByCreatedAtDesc();

            List<Map<String, Object>> enhancedLeaves = allLeaves.stream().map(leave -> {
                User employee = userRepository.findByEmail(leave.getEmployeeEmail());
                Map<String, Object> leaveData = new HashMap<>();

                leaveData.put("id", leave.getId());
                leaveData.put("employeeEmail", leave.getEmployeeEmail());
                leaveData.put("employeeName", leave.getEmployeeName());
                leaveData.put("leaveType", leave.getLeaveType());
                leaveData.put("startDate", leave.getStartDate());
                leaveData.put("endDate", leave.getEndDate());
                leaveData.put("status", leave.getStatus());
                leaveData.put("reason", leave.getReason());
                leaveData.put("createdAt", leave.getCreatedAt());
                leaveData.put("isHalfDay", leave.isHalfDay());
                leaveData.put("isShortLeave", leave.isShortLeave());
                leaveData.put("isCancelled", leave.isCancelled());

                if (employee != null) {
                    leaveData.put("department", employee.getDepartment());
                    leaveData.put("employeeDesignation", employee.getDesignation());
                    leaveData.put("employeeFullName", employee.getFullName());
                } else {
                    leaveData.put("department", "Unknown");
                    leaveData.put("employeeDesignation", "Unknown");
                    leaveData.put("employeeFullName", leave.getEmployeeName());
                }

                // Officer information
                leaveData.put("actingOfficerName", leave.getActingOfficerName());
                leaveData.put("actingOfficerEmail", leave.getActingOfficerEmail());
                leaveData.put("actingOfficerComments", leave.getActingOfficerComments());
                leaveData.put("actingOfficerApprovedAt", leave.getActingOfficerApprovedAt());
                leaveData.put("actingOfficerStatus", leave.getActingOfficerStatus());

                leaveData.put("supervisingOfficerName", leave.getSupervisingOfficerName());
                leaveData.put("supervisingOfficerEmail", leave.getSupervisingOfficerEmail());
                leaveData.put("supervisingOfficerComments", leave.getSupervisingOfficerComments());
                leaveData.put("supervisingOfficerApprovedAt", leave.getSupervisingOfficerApprovedAt());
                leaveData.put("supervisingOfficerStatus", leave.getSupervisingOfficerStatus());

                leaveData.put("approvalOfficerName", leave.getApprovalOfficerName());
                leaveData.put("approvalOfficerEmail", leave.getApprovalOfficerEmail());
                leaveData.put("approvalOfficerComments", leave.getApprovalOfficerComments());
                leaveData.put("approvalOfficerApprovedAt", leave.getApprovalOfficerApprovedAt());
                leaveData.put("approvalOfficerStatus", leave.getApprovalOfficerStatus());

                // Cancellation information
                leaveData.put("cancelledAt", leave.getCancelledAt());
                leaveData.put("cancelledBy", leave.getCancelledBy());
                leaveData.put("cancellationReason", leave.getCancellationReason());

                // Special leave type details
                if (leave.isHalfDay()) {
                    leaveData.put("halfDayPeriod", leave.getHalfDayPeriod());
                }
                if (leave.isShortLeave()) {
                    leaveData.put("startTime", leave.getShortLeaveStartTime());
                    leaveData.put("endTime", leave.getShortLeaveEndTime());
                }

                // Calculate leave duration
                if (leave.isShortLeave()) {
                    leaveData.put("leaveDuration", "Short Leave");
                } else if (leave.isHalfDay()) {
                    leaveData.put("leaveDuration", "0.5 days");
                } else if (leave.getStartDate() != null && leave.getEndDate() != null) {
                    long daysBetween = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
                    leaveData.put("leaveDuration", daysBetween + " day" + (daysBetween != 1 ? "s" : ""));
                } else {
                    leaveData.put("leaveDuration", "1 day");
                }

                return leaveData;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(enhancedLeaves);

        } catch (Exception e) {
            logger.error("Error fetching all leaves for admin: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to fetch leaves: " + e.getMessage());
        }
    }

    // ---------------- Validate Maternity Leave Request ----------------
    @PostMapping("/validate-maternity")
    public ResponseEntity<?> validateMaternityLeave(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            LocalDate startDate = LocalDate.parse((String) request.get("startDate"));
            String maternityLeaveType = (String) request.get("maternityLeaveType");

            // Create a temporary LeaveRequest for validation
            LeaveRequest leaveRequest = new LeaveRequest();
            leaveRequest.setLeaveType("MATERNITY");
            leaveRequest.setStartDate(startDate);
            leaveRequest.setMaternityLeaveType(maternityLeaveType);

            String validation = leaveService.validateMaternityLeaveRequest(email, leaveRequest);

            if ("VALID".equals(validation)) {
                String durationInfo = "";
                switch (maternityLeaveType) {
                    case "FULL_PAY":
                        durationInfo = "Full Pay - 84 Days";
                        break;
                    case "HALF_PAY":
                        durationInfo = "Half Pay - 84 Days";
                        break;
                    case "NO_PAY":
                        durationInfo = "No Pay - 84 Days";
                        break;
                    default:
                        durationInfo = "84 Days";
                }

                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "message", "Maternity leave request is valid (" + durationInfo + "). End date will be set by admin after approval.",
                        "expectedDuration", durationInfo
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "valid", false,
                        "message", validation
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to validate maternity leave request: " + e.getMessage());
        }
    }


    // ---------------- Get Maternity Leave Details (Admin) ----------------
    @GetMapping("/admin/maternity/{leaveId}/details")
    public ResponseEntity<?> getMaternityLeaveDetails(
            @PathVariable String leaveId,
            @RequestHeader("Authorization") String token) {
        try {
            String adminEmail = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            User admin = userRepository.findByEmail(adminEmail);
            if (admin == null || !admin.getRoles().contains("ADMIN")) {
                return ResponseEntity.status(403).body("❌ Access denied. Admin role required.");
            }

            Leave leave = leaveRepository.findById(leaveId).orElse(null);
            if (leave == null) {
                return ResponseEntity.badRequest().body("❌ Leave request not found");
            }

            if (!leave.isMaternityLeave()) {
                return ResponseEntity.badRequest().body("❌ This is not a maternity leave request");
            }

            User employee = userRepository.findByEmail(leave.getEmployeeEmail());

            Map<String, Object> details = new HashMap<>();
            details.put("leaveId", leave.getId());
            details.put("employeeEmail", leave.getEmployeeEmail());
            details.put("employeeName", leave.getEmployeeName());
            details.put("employeeFullName", employee != null ? employee.getFullName() : leave.getEmployeeName());
            details.put("department", employee != null ? employee.getDepartment() : "Unknown");
            details.put("designation", employee != null ? employee.getDesignation() : "Unknown");
            details.put("startDate", leave.getStartDate());
            details.put("endDate", leave.getEndDate());
            details.put("maternityLeaveType", leave.getMaternityLeaveType());
            details.put("maternityLeaveDuration", leave.getMaternityLeaveDuration());
            details.put("isMaternityEndDateSet", leave.isMaternityEndDateSet());
            details.put("reason", leave.getReason());
            details.put("status", leave.getStatus());
            details.put("createdAt", leave.getCreatedAt());
            details.put("approvalOfficerApprovedAt", leave.getApprovalOfficerApprovedAt());
            details.put("maternityAdditionalDetails", leave.getMaternityAdditionalDetails());

            if (leave.getEndDate() != null) {
                long totalDays = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
                details.put("totalDays", totalDays);
            }

            return ResponseEntity.ok(details);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch maternity leave details: " + e.getMessage());
        }
    }

    // ---------------- Get All Maternity Leaves (Admin) ----------------
    @GetMapping("/admin/maternity/all")
    public ResponseEntity<?> getAllMaternityLeaves(@RequestHeader("Authorization") String token) {
        try {
            String adminEmail = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            User admin = userRepository.findByEmail(adminEmail);
            if (admin == null || !admin.getRoles().contains("ADMIN")) {
                return ResponseEntity.status(403).body("❌ Access denied. Admin role required.");
            }

            List<Leave> allMaternityLeaves = leaveRepository.findAllByOrderByCreatedAtDesc()
                    .stream()
                    .filter(Leave::isMaternityLeave)
                    .collect(Collectors.toList());

            List<Map<String, Object>> enhancedLeaves = allMaternityLeaves.stream().map(leave -> {
                User employee = userRepository.findByEmail(leave.getEmployeeEmail());
                Map<String, Object> leaveData = new HashMap<>();

                leaveData.put("id", leave.getId());
                leaveData.put("employeeEmail", leave.getEmployeeEmail());
                leaveData.put("employeeName", leave.getEmployeeName());
                leaveData.put("startDate", leave.getStartDate());
                leaveData.put("endDate", leave.getEndDate());
                leaveData.put("maternityLeaveType", leave.getMaternityLeaveType());
                leaveData.put("maternityLeaveDuration", leave.getMaternityLeaveDuration());
                leaveData.put("isMaternityEndDateSet", leave.isMaternityEndDateSet());
                leaveData.put("status", leave.getStatus());
                leaveData.put("reason", leave.getReason());
                leaveData.put("createdAt", leave.getCreatedAt());
                leaveData.put("approvalOfficerApprovedAt", leave.getApprovalOfficerApprovedAt());
                leaveData.put("isCancelled", leave.isCancelled());


                if (employee != null) {
                    leaveData.put("department", employee.getDepartment());
                    leaveData.put("employeeDesignation", employee.getDesignation());
                    leaveData.put("employeeFullName", employee.getFullName());
                }

                if (leave.getEndDate() != null) {
                    long totalDays = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
                    leaveData.put("totalDays", totalDays);
                } else {
                    leaveData.put("totalDays", "Not set");
                }

                return leaveData;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(enhancedLeaves);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch maternity leaves: " + e.getMessage());
        }
    }



    private boolean isAdminUser(String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            // Special case for hardcoded admin
            if ("admin@example.com".equals(email)) {
                return true;
            }

            // Get user from database
            User user = userRepository.findByEmail(email);
            if (user == null) return false;

            // Check if user has ADMIN role
            Set<String> roles = user.getRoles();
            if (roles == null) return false;

            // Check for ADMIN role (case insensitive)
            return roles.stream().anyMatch(role ->
                    "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role)
            );

        } catch (Exception e) {
            logger.error("Error validating admin user: {}", e.getMessage());
            return false;
        }
    }
    // Update your maternity endpoints like this:
    @GetMapping("/admin/maternity/pending-end-date")
    public ResponseEntity<?> getMaternityLeavesNeedingEndDate(@RequestHeader("Authorization") String token) {
        try {
            // Use the helper method instead of inline validation
            if (!isAdminUser(token)) {
                return ResponseEntity.status(403).body("❌ Access denied. Admin role required.");
            }

            List<Leave> maternityLeaves = leaveService.getMaternityLeavesNeedingEndDate();

            List<Map<String, Object>> enhancedLeaves = maternityLeaves.stream().map(leave -> {
                User employee = userRepository.findByEmail(leave.getEmployeeEmail());
                Map<String, Object> leaveData = new HashMap<>();

                leaveData.put("id", leave.getId());
                leaveData.put("employeeEmail", leave.getEmployeeEmail());
                leaveData.put("employeeName", leave.getEmployeeName());
                leaveData.put("startDate", leave.getStartDate());
                leaveData.put("maternityLeaveType", leave.getMaternityLeaveType());
                leaveData.put("reason", leave.getReason());
                leaveData.put("createdAt", leave.getCreatedAt());
                leaveData.put("approvalOfficerApprovedAt", leave.getApprovalOfficerApprovedAt());
                leaveData.put("maternityLeaveDuration", leave.getMaternityLeaveDuration());

                if (employee != null) {
                    leaveData.put("department", employee.getDepartment());
                    leaveData.put("employeeDesignation", employee.getDesignation());
                    leaveData.put("employeeFullName", employee.getFullName());
                } else {
                    leaveData.put("department", "Unknown");
                    leaveData.put("employeeDesignation", "Unknown");
                    leaveData.put("employeeFullName", leave.getEmployeeName());
                }

                if (leave.getApprovalOfficerApprovedAt() != null) {
                    long daysSinceApproval = ChronoUnit.DAYS.between(
                            leave.getApprovalOfficerApprovedAt().toLocalDate(),
                            LocalDate.now()
                    );
                    leaveData.put("daysSinceApproval", daysSinceApproval);
                }

                return leaveData;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(enhancedLeaves);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch maternity leaves: " + e.getMessage());
        }
    }

    @PostMapping("/admin/maternity/{leaveId}/set-end-date")
    public ResponseEntity<?> setMaternityLeaveEndDate(
            @PathVariable String leaveId,
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Object> request) {
        try {
            // Use the helper method instead of inline validation
            if (!isAdminUser(token)) {
                return ResponseEntity.status(403).body("❌ Access denied. Admin role required.");
            }

            String adminEmail = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            LocalDate endDate = LocalDate.parse((String) request.get("endDate"));
            String adminComments = (String) request.get("comments");

            String result = leaveService.setMaternityLeaveEndDate(leaveId, adminEmail, endDate, adminComments);

            return result.contains("successfully")
                    ? ResponseEntity.ok("✅ " + result)
                    : ResponseEntity.badRequest().body("❌ " + result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to set maternity leave end date: " + e.getMessage());
        }
    }



@GetMapping("/history/acting")
public ResponseEntity<?> getActingOfficerHistory(
        @RequestHeader("Authorization") String token) {
    try {
        String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

        // Get all leaves where current user was acting officer and action was taken
        List<Leave> actingHistory = leaveRepository.findByActingOfficerEmailOrderByCreatedAtDesc(email)
                .stream()
                .filter(leave -> leave.getActingOfficerStatus() != null &&
                        leave.getActingOfficerStatus() != ActingOfficerStatus.PENDING)
                .collect(Collectors.toList());

        // Convert to response DTOs
        List<LeaveResponse> response = actingHistory.stream()
                .map(LeaveResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        logger.error("Error fetching acting officer history: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body("❌ Failed to fetch history");
    }
}

    @GetMapping("/history/supervising")
    public ResponseEntity<?> getSupervisingOfficerHistory(
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            // Get all leaves where current user was supervising officer and action was taken
            List<Leave> supervisingHistory = leaveRepository.findBySupervisingOfficerEmailOrderByCreatedAtDesc(email)
                    .stream()
                    .filter(leave -> leave.getSupervisingOfficerStatus() != null &&
                            leave.getSupervisingOfficerStatus() != SupervisingOfficerStatus.PENDING)
                    .collect(Collectors.toList());

            // Convert to response DTOs
            List<LeaveResponse> response = supervisingHistory.stream()
                    .map(LeaveResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching supervising officer history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("❌ Failed to fetch history");
        }
    }

    @GetMapping("/history/approval")
    public ResponseEntity<?> getApprovalOfficerHistory(
            @RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));

            // Get all leaves where current user was approval officer and action was taken
            List<Leave> approvalHistory = leaveRepository.findByApprovalOfficerEmailOrderByCreatedAtDesc(email)
                    .stream()
                    .filter(leave -> leave.getApprovalOfficerStatus() != null &&
                            leave.getApprovalOfficerStatus() != ApprovalOfficerStatus.PENDING)
                    .collect(Collectors.toList());

            // Convert to response DTOs
            List<LeaveResponse> response = approvalHistory.stream()
                    .map(LeaveResponse::new)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching approval officer history: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("❌ Failed to fetch history");
        }
    }



    // ---------------- DashboardCounts Helper Class ----------------
    public static class DashboardCounts {
        private long pendingAsActingOfficer;
        private long pendingAsSupervisingOfficer;
        private long pendingAsApprovalOfficer;

        public DashboardCounts(long pendingAsActingOfficer, long pendingAsSupervisingOfficer, long pendingAsApprovalOfficer) {
            this.pendingAsActingOfficer = pendingAsActingOfficer;
            this.pendingAsSupervisingOfficer = pendingAsSupervisingOfficer;
            this.pendingAsApprovalOfficer = pendingAsApprovalOfficer;
        }

        public long getPendingAsActingOfficer() {
            return pendingAsActingOfficer;
        }

        public long getPendingAsSupervisingOfficer() {
            return pendingAsSupervisingOfficer;
        }

        public long getPendingAsApprovalOfficer() {
            return pendingAsApprovalOfficer;
        }
    }
}