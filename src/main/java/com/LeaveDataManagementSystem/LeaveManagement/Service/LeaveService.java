package com.LeaveDataManagementSystem.LeaveManagement.Service;

import com.LeaveDataManagementSystem.LeaveManagement.Model.*;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.LeaveRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.LeaveRequest;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.LeaveApprovalRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LeaveService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LeaveEntitlementService leaveEntitlementService;


    // Helper method to send initial notification
    private void sendInitialNotification(Leave leave, LeaveRequest leaveRequest) {
        if (leaveRequest.hasActingOfficer()) {
            notificationService.notifyActingOfficer(leave);
        } else if (leaveRequest.hasSupervisingOfficer()) {
            notificationService.notifySupervisingOfficer(leave);
        } else {
            notificationService.notifyApprovalOfficer(leave);
        }
    }



    // Helper method to determine next status after supervising officer approval
    private LeaveStatus getNextStatusAfterSupervisingApproval(Leave leave) {
        if (leave.getApprovalOfficerEmail() != null &&
                !leave.getApprovalOfficerEmail().trim().isEmpty()) {
            return LeaveStatus.PENDING_APPROVAL_OFFICER;
        } else {
            return LeaveStatus.APPROVED;
        }
    }


        // Helper method to determine next status after acting officer approval
    private LeaveStatus getNextStatusAfterActingApproval(Leave leave) {
        if (leave.getSupervisingOfficerEmail() != null &&
                !leave.getSupervisingOfficerEmail().trim().isEmpty()) {
            return LeaveStatus.PENDING_SUPERVISING_OFFICER;
        } else if (leave.getApprovalOfficerEmail() != null &&
                !leave.getApprovalOfficerEmail().trim().isEmpty()) {
            return LeaveStatus.PENDING_APPROVAL_OFFICER;
        } else {
            return LeaveStatus.APPROVED;
        }
    }



    // Replace your existing processActingOfficerAction method with this:
    public String processActingOfficerAction(String leaveId, String email, LeaveApprovalRequest request) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (!leave.getActingOfficerEmail().equalsIgnoreCase(email))
            return "You are not authorized to review this leave request";

        if (!leave.getStatus().equals(LeaveStatus.PENDING_ACTING_OFFICER))
            return "Leave request has already been processed";

        LocalDateTime originalCreatedAt = leave.getCreatedAt();

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            // FIXED: Special validation for maternity leave
            String entitlementValidation;
            if ("MATERNITY".equals(leave.getLeaveType())) {
                entitlementValidation = validateMaternityLeaveForApproval(leave);
            } else {
                entitlementValidation = leaveEntitlementService.validateLeaveRequest(
                        leave.getEmployeeEmail(), leave.getLeaveType(),
                        leave.getStartDate(), leave.getEndDate());
            }

            if (!"VALID".equals(entitlementValidation)) {
                return "Cannot approve: " + entitlementValidation;
            }

            leave.setActingOfficerStatus(ActingOfficerStatus.APPROVED);
            leave.setActingOfficerApprovedAt(LocalDateTime.now());
            leave.setActingOfficerComments(request.getComments());

            // Determine next status based on workflow
            LeaveStatus nextStatus = getNextStatusAfterActingApproval(leave);
            leave.setStatus(nextStatus);

            if (originalCreatedAt != null) {
                leave.setCreatedAt(originalCreatedAt);
            }

            leaveRepository.save(leave);

            // Send appropriate notification based on next status
            if (nextStatus == LeaveStatus.PENDING_SUPERVISING_OFFICER) {
                notificationService.notifySupervisingOfficer(leave);
                notificationService.notifyEmployee(leave, "APPROVED", "Acting Officer");
                return "Leave approved and forwarded to supervising officer";
            } else if (nextStatus == LeaveStatus.PENDING_APPROVAL_OFFICER) {
                notificationService.notifyApprovalOfficer(leave);
                notificationService.notifyEmployee(leave, "APPROVED", "Acting Officer");
                return "Leave approved and forwarded to approval officer";
            } else if (nextStatus == LeaveStatus.APPROVED) {
                // FIXED: Handle entitlement updates differently for maternity leave
                if ("MATERNITY".equals(leave.getLeaveType())) {
                    logger.info("Maternity leave fully approved. End date will be set by admin before entitlement update.");
                } else {
                    leaveEntitlementService.updateEntitlementOnLeaveApproval(
                            leave.getEmployeeEmail(), leave.getLeaveType(),
                            leave.getStartDate(), leave.getEndDate(),
                            leave.isShortLeave(), leave.isHalfDay());
                }
                notificationService.notifyEmployee(leave, "APPROVED", "Acting Officer");
                return "Leave approved successfully";
            }

        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            leave.setActingOfficerStatus(ActingOfficerStatus.REJECTED);
            leave.setStatus(LeaveStatus.REJECTED_BY_ACTING_OFFICER);
            leave.setActingOfficerComments(request.getComments());

            if (originalCreatedAt != null) {
                leave.setCreatedAt(originalCreatedAt);
            }

            leaveRepository.save(leave);
            notificationService.notifyEmployee(leave, "REJECTED", "Acting Officer");
            return "Leave rejected";
        }

        return "Invalid action. Use APPROVE or REJECT";
    }

    // Replace your existing processSupervisingOfficerAction method with this:
    public String processSupervisingOfficerAction(String leaveId, String email, LeaveApprovalRequest request) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (!leave.getSupervisingOfficerEmail().equalsIgnoreCase(email))
            return "You are not authorized to review this leave request";

        if (!leave.getStatus().equals(LeaveStatus.PENDING_SUPERVISING_OFFICER))
            return "Leave request is not ready for your review";

        LocalDateTime originalCreatedAt = leave.getCreatedAt();
        LocalDateTime originalActingApprovedAt = leave.getActingOfficerApprovedAt();

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            // FIXED: Special validation for maternity leave
            String entitlementValidation;
            if ("MATERNITY".equals(leave.getLeaveType())) {
                entitlementValidation = validateMaternityLeaveForApproval(leave);
            } else {
                entitlementValidation = leaveEntitlementService.validateLeaveRequest(
                        leave.getEmployeeEmail(), leave.getLeaveType(),
                        leave.getStartDate(), leave.getEndDate());
            }

            if (!"VALID".equals(entitlementValidation)) {
                return "Cannot approve: " + entitlementValidation;
            }

            leave.setSupervisingOfficerStatus(SupervisingOfficerStatus.APPROVED);
            leave.setSupervisingOfficerApprovedAt(LocalDateTime.now());
            leave.setSupervisingOfficerComments(request.getComments());

            // Determine next status
            LeaveStatus nextStatus = getNextStatusAfterSupervisingApproval(leave);
            leave.setStatus(nextStatus);

            if (originalCreatedAt != null) {
                leave.setCreatedAt(originalCreatedAt);
            }
            if (originalActingApprovedAt != null) {
                leave.setActingOfficerApprovedAt(originalActingApprovedAt);
            }

            leaveRepository.save(leave);

            if (nextStatus == LeaveStatus.PENDING_APPROVAL_OFFICER) {
                notificationService.notifyApprovalOfficer(leave);
                notificationService.notifyEmployee(leave, "APPROVED", "Supervising Officer");
                return "Leave approved and forwarded to approval officer";
            } else if (nextStatus == LeaveStatus.APPROVED) {
                // FIXED: Handle entitlement updates differently for maternity leave
                if ("MATERNITY".equals(leave.getLeaveType())) {
                    logger.info("Maternity leave fully approved. End date will be set by admin before entitlement update.");
                } else {
                    leaveEntitlementService.updateEntitlementOnLeaveApproval(
                            leave.getEmployeeEmail(), leave.getLeaveType(),
                            leave.getStartDate(), leave.getEndDate(),
                            leave.isShortLeave(), leave.isHalfDay());
                }
                notificationService.notifyEmployee(leave, "APPROVED", "Supervising Officer");
                return "Leave approved successfully";
            }

        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            leave.setSupervisingOfficerStatus(SupervisingOfficerStatus.REJECTED);
            leave.setStatus(LeaveStatus.REJECTED_BY_SUPERVISING_OFFICER);
            leave.setSupervisingOfficerComments(request.getComments());

            if (originalCreatedAt != null) {
                leave.setCreatedAt(originalCreatedAt);
            }
            if (originalActingApprovedAt != null) {
                leave.setActingOfficerApprovedAt(originalActingApprovedAt);
            }

            leaveRepository.save(leave);
            notificationService.notifyEmployee(leave, "REJECTED", "Supervising Officer");
            return "Leave rejected";
        }

        return "Invalid action. Use APPROVE or REJECT";
    }

    // Replace your existing processApprovalOfficerAction method with this:
    public String processApprovalOfficerAction(String leaveId, String email, LeaveApprovalRequest request) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (!leave.getApprovalOfficerEmail().equalsIgnoreCase(email))
            return "You are not authorized to review this leave request";

        if (!leave.getStatus().equals(LeaveStatus.PENDING_APPROVAL_OFFICER))
            return "Leave request is not ready for your review";

        LocalDateTime originalCreatedAt = leave.getCreatedAt();
        LocalDateTime originalActingApprovedAt = leave.getActingOfficerApprovedAt();
        LocalDateTime originalSupervisingApprovedAt = leave.getSupervisingOfficerApprovedAt();

        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            // FIXED: Special validation for maternity leave
            String entitlementValidation;
            if ("MATERNITY".equals(leave.getLeaveType())) {
                // For maternity leave, validate without end date (since it's not set yet)
                entitlementValidation = validateMaternityLeaveForApproval(leave);
            } else {
                // For regular leaves, validate normally
                entitlementValidation = leaveEntitlementService.validateLeaveRequest(
                        leave.getEmployeeEmail(), leave.getLeaveType(),
                        leave.getStartDate(), leave.getEndDate());
            }

            if (!"VALID".equals(entitlementValidation)) {
                return "Cannot approve: " + entitlementValidation;
            }

            leave.setApprovalOfficerStatus(ApprovalOfficerStatus.APPROVED);
            leave.setStatus(LeaveStatus.APPROVED);
            leave.setApprovalOfficerApprovedAt(LocalDateTime.now());
            leave.setApprovalOfficerComments(request.getComments());

            if (originalCreatedAt != null) {
                leave.setCreatedAt(originalCreatedAt);
            }
            if (originalActingApprovedAt != null) {
                leave.setActingOfficerApprovedAt(originalActingApprovedAt);
            }
            if (originalSupervisingApprovedAt != null) {
                leave.setSupervisingOfficerApprovedAt(originalSupervisingApprovedAt);
            }

            leaveRepository.save(leave);

            // FIXED: Handle entitlement updates differently for maternity leave
            if ("MATERNITY".equals(leave.getLeaveType())) {
                // For maternity leave, don't update entitlements until admin sets end date
                logger.info("Maternity leave approved. End date will be set by admin before entitlement update.");
            } else {
                // For regular leaves, update entitlements immediately
                leaveEntitlementService.updateEntitlementOnLeaveApproval(
                        leave.getEmployeeEmail(),
                        leave.getLeaveType(),
                        leave.getStartDate(),
                        leave.getEndDate(),
                        leave.isShortLeave(),
                        leave.isHalfDay()
                );
            }

            notificationService.notifyEmployee(leave, "APPROVED", "Approval Officer");
            return "Leave approved successfully";

        } else if ("REJECT".equalsIgnoreCase(request.getAction())) {
            leave.setApprovalOfficerStatus(ApprovalOfficerStatus.REJECTED);
            leave.setStatus(LeaveStatus.REJECTED_BY_APPROVAL_OFFICER);
            leave.setApprovalOfficerComments(request.getComments());

            if (originalCreatedAt != null) {
                leave.setCreatedAt(originalCreatedAt);
            }
            if (originalActingApprovedAt != null) {
                leave.setActingOfficerApprovedAt(originalActingApprovedAt);
            }
            if (originalSupervisingApprovedAt != null) {
                leave.setSupervisingOfficerApprovedAt(originalSupervisingApprovedAt);
            }

            leaveRepository.save(leave);
            notificationService.notifyEmployee(leave, "REJECTED", "Approval Officer");
            return "Leave rejected";
        }

        return "Invalid action. Use APPROVE or REJECT";
    }

    // ... [Keep all other existing methods unchanged] ...

    // ------------------- EMPLOYEE LEAVES -------------------
    public List<Leave> getEmployeeLeaves(String employeeEmail) {
        return leaveRepository.findByEmployeeEmailOrderByCreatedAtDesc(employeeEmail);
    }

    // ------------------- PENDING LEAVES -------------------
    public List<Leave> getPendingLeavesForActingOfficer(String email) {
        return leaveRepository.findByActingOfficerEmailAndStatusOrderByCreatedAtAsc(
                email, LeaveStatus.PENDING_ACTING_OFFICER
        );
    }

    public List<Leave> getPendingLeavesForApprovalOfficer(String email) {
        return leaveRepository.findByApprovalOfficerEmailAndStatusOrderByCreatedAtAsc(
                email, LeaveStatus.PENDING_APPROVAL_OFFICER
        );
    }

    public List<Leave> getPendingLeavesForSupervisingOfficer(String email) {
        return leaveRepository.findBySupervisingOfficerEmailAndStatusOrderByCreatedAtAsc(
                email, LeaveStatus.PENDING_SUPERVISING_OFFICER
        );
    }

    // ------------------- DASHBOARD COUNTS -------------------
    public long getPendingCountForActingOfficer(String email) {
        return leaveRepository.countByActingOfficerEmailAndStatus(email, LeaveStatus.PENDING_ACTING_OFFICER);
    }

    public long getPendingCountForApprovalOfficer(String email) {
        return leaveRepository.countByApprovalOfficerEmailAndStatus(email, LeaveStatus.PENDING_APPROVAL_OFFICER);
    }

    public long getPendingCountForSupervisingOfficer(String email) {
        return leaveRepository.countBySupervisingOfficerEmailAndStatus(
                email, LeaveStatus.PENDING_SUPERVISING_OFFICER);
    }

    // ------------------- CANCEL LEAVE REQUEST -------------------
    public String cancelLeaveRequest(String leaveId, String employeeEmail, String cancellationReason) {
        try {
            Leave leave = leaveRepository.findById(leaveId)
                    .orElseThrow(() -> new RuntimeException("Leave request not found"));

            if (!leave.getEmployeeEmail().equalsIgnoreCase(employeeEmail)) {
                return "You are not authorized to cancel this leave request";
            }

            if (!canCancelLeave(leave)) {
                if (leave.isCancelled()) {
                    return "Leave request has already been cancelled";
                }
                if (leave.getStatus().toString().contains("REJECTED")) {
                    return "Cannot cancel a rejected leave request";
                }
                if (!leave.getStartDate().isAfter(LocalDate.now())) {
                    return "Cannot cancel a leave request that has already started or is in the past";
                }
                return "This leave request cannot be cancelled";
            }

            boolean wasApproved = leave.getStatus() == LeaveStatus.APPROVED;

            LocalDateTime originalCreatedAt = leave.getCreatedAt();
            LocalDateTime originalActingApprovedAt = leave.getActingOfficerApprovedAt();
            LocalDateTime originalSupervisingApprovedAt = leave.getSupervisingOfficerApprovedAt();

            leave.setCancelled(true);
            leave.setStatus(LeaveStatus.CANCELLED_BY_EMPLOYEE);
            leave.setCancelledAt(LocalDateTime.now());
            leave.setCancelledBy(employeeEmail);
            leave.setCancellationReason(cancellationReason);

            if (originalCreatedAt != null) {
                leave.setCreatedAt(originalCreatedAt);
            }
            if (originalActingApprovedAt != null) {
                leave.setActingOfficerApprovedAt(originalActingApprovedAt);
            }
            if (originalSupervisingApprovedAt != null) {
                leave.setSupervisingOfficerApprovedAt(originalSupervisingApprovedAt);
            }

            leaveRepository.save(leave);

            if (wasApproved) {
                logger.info("Reverting entitlements for cancelled approved leave: {}", leaveId);
                leaveEntitlementService.revertEntitlementOnLeaveRejection(
                        leave.getEmployeeEmail(),
                        leave.getLeaveType(),
                        leave.getStartDate(),
                        leave.getEndDate(),
                        leave.isShortLeave(),
                        leave.isHalfDay()
                );
                logger.info("Entitlements reverted successfully for leave: {}", leaveId);
            } else {
                logger.info("Leave was not approved, no entitlement reversion needed for leave: {}", leaveId);
            }

            try {
                notificationService.notifyLeaveCancellation(leave, employeeEmail);
            } catch (Exception e) {
                logger.warn("Failed to send cancellation notification: {}", e.getMessage());
            }

            return "Leave request cancelled successfully";

        } catch (Exception e) {
            logger.error("Error cancelling leave request: {}", e.getMessage(), e);
            return "Failed to cancel leave request: " + e.getMessage();
        }
    }

    // ------------------- CAN CANCEL LEAVE (HELPER METHOD) -------------------
    private boolean canCancelLeave(Leave leave) {
        if (leave.isCancelled()) {
            return false;
        }

        if (leave.getStatus() == LeaveStatus.REJECTED_BY_ACTING_OFFICER ||
                leave.getStatus() == LeaveStatus.REJECTED_BY_SUPERVISING_OFFICER ||
                leave.getStatus() == LeaveStatus.REJECTED_BY_APPROVAL_OFFICER) {
            return false;
        }

        LocalDate today = LocalDate.now();
        return !leave.getStartDate().isBefore(today);
    }

    // ------------------- PUBLIC CAN CANCEL LEAVE METHOD -------------------
    public boolean canCancelLeave(String leaveId, String employeeEmail) {
        try {
            Leave leave = leaveRepository.findById(leaveId).orElse(null);
            if (leave == null) return false;

            if (!leave.getEmployeeEmail().equalsIgnoreCase(employeeEmail)) return false;

            return canCancelLeave(leave);
        } catch (Exception e) {
            logger.error("Error checking if leave can be cancelled: {}", e.getMessage());
            return false;
        }
    }

    // ------------------- GET CANCELLABLE LEAVES -------------------
    public List<Leave> getCancellableLeaves(String employeeEmail) {
        LocalDate currentDate = LocalDate.now();

        return leaveRepository.findByEmployeeEmailOrderByCreatedAtDesc(employeeEmail)
                .stream()
                .filter(leave -> {
                    // Can cancel if:
                    // 1. Not already cancelled
                    // 2. Not rejected
                    // 3. Start date is today or in the future
                    return !leave.isCancelled() &&
                            !leave.getStatus().toString().contains("REJECTED") &&
                            !leave.getStartDate().isBefore(currentDate);
                })
                .toList();
    }

    // ------------------- ENTITLEMENT METHODS -------------------
    public List<LeaveEntitlement> getEmployeeEntitlements(String employeeEmail) {
        return leaveEntitlementService.getEmployeeEntitlements(employeeEmail);
    }

    public Map<String, Object> getEmployeeEntitlementSummary(String employeeEmail) {
        return leaveEntitlementService.getEntitlementSummary(employeeEmail);
    }

    public void recalculateEmployeeEntitlements(String employeeEmail) {
        leaveEntitlementService.recalculateEntitlements(employeeEmail);
    }

    // ------------------- ADMIN ENTITLEMENT METHODS -------------------
    public void adjustEmployeeEntitlement(String employeeEmail, String leaveType,
                                          int year, int newTotalEntitlement) {
        leaveEntitlementService.adjustEntitlement(employeeEmail, leaveType, year, newTotalEntitlement);
    }

    public void initializeEntitlementsForNewEmployee(String employeeEmail) {
        leaveEntitlementService.initializeEntitlementsForEmployee(employeeEmail);
    }

    public String validateLeaveRequest(String employeeEmail, String leaveType,
                                       LocalDate startDate, LocalDate endDate) {
        return leaveEntitlementService.validateLeaveRequest(employeeEmail, leaveType, startDate, endDate);
    }

    // ------------------- OFFICERS FOR EMPLOYEE -------------------
    public Map<String, Object> getOfficersForEmployee(String employeeEmail) {
        User employee = userRepository.findByEmail(employeeEmail);
        if (employee == null || employee.getDepartment() == null)
            return Map.of(
                    "acting", List.of(),
                    "supervising", List.of(),
                    "approval", List.of(),
                    "department", "No Department"
            );

        String dept = employee.getDepartment();
        List<User> departmentEmployees = userRepository.findByDepartment(dept).stream()
                .filter(u -> !u.getEmail().equalsIgnoreCase(employeeEmail))
                .toList();

        return Map.of(
                "acting", departmentEmployees,
                "supervising", departmentEmployees,
                "approval", departmentEmployees,
                "department", dept
        );
    }

    public List<User> getActingOfficersByDepartment(String department) {
        return userRepository.findByDepartment(department);
    }

    public List<User> getApprovalOfficersByDepartment(String department) {
        return userRepository.findByDepartment(department);
    }

    public List<User> getActingOfficersByDepartmentExcluding(String department, String excludeEmail) {
        return userRepository.findByDepartment(department)
                .stream()
                .filter(user -> !user.getEmail().equalsIgnoreCase(excludeEmail))
                .toList();
    }

    public List<User> getApprovalOfficersByDepartmentExcluding(String department, String excludeEmail) {
        List<User> deptOfficers = userRepository.findByDepartment(department);
        List<User> deptHeads = userRepository.findByDepartment("All");

        return Stream.concat(deptOfficers.stream(), deptHeads.stream())
                .filter(user -> !user.getEmail().equalsIgnoreCase(excludeEmail))
                .toList();
    }

    public List<String> getAllDepartmentsWithOfficers() {
        return userRepository.findAll().stream()
                .map(User::getDepartment)
                .filter(dept -> dept != null && !dept.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    // ------------------- COMPREHENSIVE ENTITLEMENT SUMMARY -------------------
    public Map<String, Object> getComprehensiveEmployeeEntitlementSummary(String email) {
        List<LeaveEntitlement> entitlements = leaveEntitlementService.getEmployeeEntitlements(email);

        return entitlements.stream().collect(Collectors.toMap(
                LeaveEntitlement::getLeaveType,
                e -> Map.of(
                        "total", e.getTotalEntitlement(),
                        "used", e.getUsedDays(),
                        "remaining", e.getRemainingDays()
                )
        ));
    }

    // ------------------- HALF-DAY VALIDATION -------------------
    public String validateHalfDayLeaveRequest(String email, String leaveType, LocalDate date, String halfDayPeriod) {
        String validation = leaveEntitlementService.validateLeaveRequest(email, "CASUAL", date, date);

        if (!"VALID".equals(validation)) {
            return validation;
        }

        List<Leave> existing = leaveRepository.findByEmployeeEmailAndDateRange(email, date, date);
        boolean alreadyHalfDay = existing.stream()
                .anyMatch(l -> "HALF_DAY".equals(l.getLeaveType()) && l.getStartDate().equals(date));

        if (alreadyHalfDay) {
            return "You already have a half-day leave on this date";
        }

        return "VALID";
    }

    // ------------------- SHORT LEAVE VALIDATION -------------------
    public String validateShortLeaveRequest(String email, LocalDate date) {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        LocalDate endOfMonth = date.withDayOfMonth(date.lengthOfMonth());

        long shortLeavesThisMonth = leaveRepository.findByEmployeeEmailAndDateRange(email, startOfMonth, endOfMonth)
                .stream()
                .filter(l -> "SHORT".equals(l.getLeaveType()))
                .count();

        if (shortLeavesThisMonth >= 3) {
            return "You have already taken the maximum number of short leaves this month";
        }

        return "VALID";
    }


    /// //////////////
    // Add these methods to your existing LeaveService.java

// Helper method to create Leave from LeaveRequest (UPDATED)
    private Leave createLeaveFromRequest(User employee, LeaveRequest leaveRequest,
                                         User actingOfficer, User supervisingOfficer, User approvalOfficer) {
        Leave leave;

        if ("SHORT".equals(leaveRequest.getLeaveType())) {
            leave = new Leave(
                    employee.getEmail(), employee.getName(),
                    leaveRequest.getStartDate(),
                    leaveRequest.getStartTime(), leaveRequest.getEndTime(),
                    leaveRequest.getReason(),
                    actingOfficer != null ? actingOfficer.getEmail() : null,
                    actingOfficer != null ? actingOfficer.getName() : null,
                    supervisingOfficer != null ? supervisingOfficer.getEmail() : null,
                    supervisingOfficer != null ? supervisingOfficer.getName() : null,
                    approvalOfficer.getEmail(), approvalOfficer.getName()
            );
        } else if ("HALF_DAY".equals(leaveRequest.getLeaveType())) {
            leave = new Leave(
                    employee.getEmail(), employee.getName(),
                    "HALF_DAY",
                    leaveRequest.getStartDate(),
                    leaveRequest.getHalfDayPeriod(),
                    leaveRequest.getReason(),
                    actingOfficer != null ? actingOfficer.getEmail() : null,
                    actingOfficer != null ? actingOfficer.getName() : null,
                    supervisingOfficer != null ? supervisingOfficer.getEmail() : null,
                    supervisingOfficer != null ? supervisingOfficer.getName() : null,
                    approvalOfficer.getEmail(), approvalOfficer.getName()
            );
        } else if ("MATERNITY".equals(leaveRequest.getLeaveType())) {
            // NEW: Handle maternity leave creation
            leave = new Leave(
                    employee.getEmail(), employee.getName(),
                    leaveRequest.getStartDate(),
                    leaveRequest.getMaternityLeaveType(),
                    leaveRequest.getReason(),
                    actingOfficer != null ? actingOfficer.getEmail() : null,
                    actingOfficer != null ? actingOfficer.getName() : null,
                    supervisingOfficer != null ? supervisingOfficer.getEmail() : null,
                    supervisingOfficer != null ? supervisingOfficer.getName() : null,
                    approvalOfficer.getEmail(), approvalOfficer.getName()
            );
        } else {
            leave = new Leave(
                    employee.getEmail(), employee.getName(),
                    leaveRequest.getLeaveType(),
                    leaveRequest.getStartDate(), leaveRequest.getEndDate(),
                    leaveRequest.getReason(),
                    actingOfficer != null ? actingOfficer.getEmail() : null,
                    actingOfficer != null ? actingOfficer.getName() : null,
                    supervisingOfficer != null ? supervisingOfficer.getEmail() : null,
                    supervisingOfficer != null ? supervisingOfficer.getName() : null,
                    approvalOfficer.getEmail(), approvalOfficer.getName()
            );
        }

        return leave;
    }

    // Updated validation for maternity leave
    public String submitLeaveRequest(String employeeEmail, LeaveRequest leaveRequest) {
        User employee = userRepository.findByEmail(employeeEmail);
        if (employee == null) return "Employee not found";

        // Validate that at least one officer is selected (approval officer is mandatory)
        if (!leaveRequest.hasApprovalOfficer()) {
            return "Approval officer is mandatory";
        }

        // Validate officers exist if they are selected
        User actingOfficer = null;
        User supervisingOfficer = null;
        User approvalOfficer = null;

        if (leaveRequest.hasActingOfficer()) {
            actingOfficer = userRepository.findByEmail(leaveRequest.getActingOfficerEmail());
            if (actingOfficer == null) return "Acting officer not found";

            if (!employee.getDepartment().equalsIgnoreCase(actingOfficer.getDepartment()))
                return "Acting officer must be from the same department";

            if (employee.getEmail().equalsIgnoreCase(actingOfficer.getEmail()))
                return "You cannot select yourself as acting officer";
        }

        if (leaveRequest.hasSupervisingOfficer()) {
            supervisingOfficer = userRepository.findByEmail(leaveRequest.getSupervisingOfficerEmail());
            if (supervisingOfficer == null) return "Supervising officer not found";

            if (!employee.getDepartment().equalsIgnoreCase(supervisingOfficer.getDepartment()))
                return "Supervising officer must be from the same department";

            if (employee.getEmail().equalsIgnoreCase(supervisingOfficer.getEmail()))
                return "You cannot select yourself as supervising officer";
        }

        // Approval officer is mandatory
        approvalOfficer = userRepository.findByEmail(leaveRequest.getApprovalOfficerEmail());
        if (approvalOfficer == null) return "Approval officer not found";

        if (!employee.getDepartment().equalsIgnoreCase(approvalOfficer.getDepartment())
                && !"All".equalsIgnoreCase(approvalOfficer.getDepartment()))
            return "Approval officer must be from the same department or have 'All' department access";

        if (employee.getEmail().equalsIgnoreCase(approvalOfficer.getEmail()))
            return "You cannot select yourself as approval officer";

        // Cross-validation between selected officers
        if (leaveRequest.hasActingOfficer() && leaveRequest.hasSupervisingOfficer()) {
            if (actingOfficer.getEmail().equalsIgnoreCase(supervisingOfficer.getEmail()))
                return "Acting officer and supervising officer must be different";
        }

        if (leaveRequest.hasActingOfficer()) {
            if (actingOfficer.getEmail().equalsIgnoreCase(approvalOfficer.getEmail()))
                return "Acting officer and approval officer must be different";
        }

        if (leaveRequest.hasSupervisingOfficer()) {
            if (supervisingOfficer.getEmail().equalsIgnoreCase(approvalOfficer.getEmail()))
                return "Supervising officer and approval officer must be different";
        }

        // ENTITLEMENT VALIDATION
        String entitlementValidation;
        if ("SHORT".equals(leaveRequest.getLeaveType())) {
            entitlementValidation = leaveEntitlementService.validateShortLeaveRequest(
                    employeeEmail, leaveRequest.getStartDate());
        } else if ("HALF_DAY".equals(leaveRequest.getLeaveType())) {
            entitlementValidation = leaveEntitlementService.validateLeaveRequest(
                    employeeEmail, "HALF_DAY",
                    leaveRequest.getStartDate(), leaveRequest.getEndDate(),
                    true, leaveRequest.getHalfDayPeriod());
        } else if ("MATERNITY".equals(leaveRequest.getLeaveType())) {
            // NEW: Special validation for maternity leave
            entitlementValidation = validateMaternityLeaveRequest(employeeEmail, leaveRequest);
        } else {
            entitlementValidation = leaveEntitlementService.validateLeaveRequest(
                    employeeEmail, leaveRequest.getLeaveType(),
                    leaveRequest.getStartDate(), leaveRequest.getEndDate());
        }

        if (!"VALID".equals(entitlementValidation)) {
            return entitlementValidation;
        }

        // Overlapping leave check (skip for maternity and short leaves)
        if (!"SHORT".equals(leaveRequest.getLeaveType()) && !"MATERNITY".equals(leaveRequest.getLeaveType())) {
            List<Leave> overlapping = leaveRepository.findOverlappingLeaves(
                    employeeEmail, leaveRequest.getStartDate(), leaveRequest.getEndDate()
            ).stream().filter(l -> l.getStatus() != LeaveStatus.REJECTED_BY_ACTING_OFFICER &&
                    l.getStatus() != LeaveStatus.REJECTED_BY_SUPERVISING_OFFICER &&
                    l.getStatus() != LeaveStatus.REJECTED_BY_APPROVAL_OFFICER).toList();
            if (!overlapping.isEmpty()) return "You already have overlapping leave requests";
        }

        // Create Leave object based on leave type
        Leave leave = createLeaveFromRequest(employee, leaveRequest, actingOfficer, supervisingOfficer, approvalOfficer);

        // Set initial workflow status
        leave.setStatus(leaveRequest.getInitialWorkflowStatus());

        // Set appropriate officer statuses
        if (leaveRequest.hasActingOfficer()) {
            leave.setActingOfficerStatus(ActingOfficerStatus.PENDING);
        } else {
            leave.setActingOfficerStatus(ActingOfficerStatus.NOT_REQUIRED);
        }

        if (leaveRequest.hasSupervisingOfficer()) {
            leave.setSupervisingOfficerStatus(SupervisingOfficerStatus.PENDING);
        } else {
            leave.setSupervisingOfficerStatus(SupervisingOfficerStatus.NOT_REQUIRED);
        }

        leave.setApprovalOfficerStatus(ApprovalOfficerStatus.PENDING);

        leaveRepository.save(leave);

        // Send notification to the appropriate first officer in workflow
        sendInitialNotification(leave, leaveRequest);

        return "Leave request submitted successfully";
    }



// Replace your existing validateMaternityLeaveRequest method in LeaveService with this updated version:

public String validateMaternityLeaveRequest(String employeeEmail, LeaveRequest leaveRequest) {
    // Basic validations for maternity leave
    if (leaveRequest.getMaternityLeaveType() == null || leaveRequest.getMaternityLeaveType().trim().isEmpty()) {
        return "Maternity leave type is required";
    }

    // Validate maternity leave type
    if (!Arrays.asList("FULL_PAY", "HALF_PAY", "NO_PAY").contains(leaveRequest.getMaternityLeaveType())) {
        return "Invalid maternity leave type. Must be FULL_PAY, HALF_PAY, or NO_PAY";
    }

    // UPDATED: More flexible validation for existing maternity leaves
    List<Leave> existingMaternityLeaves = leaveRepository.findByEmployeeEmailOrderByCreatedAtDesc(employeeEmail)
            .stream()
            .filter(leave -> "MATERNITY".equals(leave.getLeaveType()) &&
                    !leave.isCancelled() &&
                    leave.getStatus() != LeaveStatus.REJECTED_BY_ACTING_OFFICER &&
                    leave.getStatus() != LeaveStatus.REJECTED_BY_SUPERVISING_OFFICER &&
                    leave.getStatus() != LeaveStatus.REJECTED_BY_APPROVAL_OFFICER)
            .collect(Collectors.toList());

    if (!existingMaternityLeaves.isEmpty()) {
        // Check for overlapping or conflicting maternity leaves
        LocalDate newLeaveStartDate = leaveRequest.getStartDate();

        for (Leave existingLeave : existingMaternityLeaves) {
            // Case 1: Check for pending maternity leaves (not yet approved)
            if (existingLeave.getStatus() == LeaveStatus.PENDING_ACTING_OFFICER ||
                    existingLeave.getStatus() == LeaveStatus.PENDING_SUPERVISING_OFFICER ||
                    existingLeave.getStatus() == LeaveStatus.PENDING_APPROVAL_OFFICER) {
                return "You already have a pending maternity leave request. Please wait for it to be processed or cancel it first.";
            }

            // Case 2: Check for approved maternity leaves
            if (existingLeave.getStatus() == LeaveStatus.APPROVED) {
                // If the existing leave has an end date set, check for date conflicts
                if (existingLeave.getEndDate() != null) {
                    // New leave should start after the existing leave ends
                    if (newLeaveStartDate.isBefore(existingLeave.getEndDate().plusDays(1))) {
                        return String.format(
                                "You have an existing maternity leave from %s to %s (%s). " +
                                        "New maternity leave must start after %s.",
                                existingLeave.getStartDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                existingLeave.getEndDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                                formatMaternityLeaveType(existingLeave.getMaternityLeaveType()),
                                existingLeave.getEndDate().plusDays(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                        );
                    }
                    // Allow the new request if it starts after the existing leave ends
                } else {
                    // Existing leave is approved but end date not set yet - allow continuation requests
                    // Check if this is a continuation/extension request
                    if (isContinuationRequest(existingLeave, leaveRequest)) {
                        // Allow continuation but with a warning message
                        logger.info("Allowing maternity leave continuation request for employee: {}", employeeEmail);
                    } else {
                        return "You have an existing approved maternity leave without an end date set. " +
                                "Please contact admin to set the end date first, or ensure your new request is a valid continuation.";
                    }
                }
            }
        }
    }

    // Additional business logic validations can be added here
    // For example: check if employee is eligible for maternity leave based on tenure, etc.

    return "VALID";
}

    // Helper method to determine if this is a valid continuation request
    private boolean isContinuationRequest(Leave existingLeave, LeaveRequest newRequest) {
        // Consider it a continuation if:
        // 1. The payment types are different (e.g., FULL_PAY -> HALF_PAY -> NO_PAY)
        // 2. The new leave starts on or after the existing leave's start date
        // 3. It's a logical progression (FULL_PAY -> HALF_PAY or HALF_PAY -> NO_PAY)

        String existingType = existingLeave.getMaternityLeaveType();
        String newType = newRequest.getMaternityLeaveType();

        // Don't allow same type continuation without end date set
        if (existingType.equals(newType)) {
            return false;
        }

        // Allow logical progression: FULL_PAY -> HALF_PAY -> NO_PAY
        if ("FULL_PAY".equals(existingType) && ("HALF_PAY".equals(newType) || "NO_PAY".equals(newType))) {
            return true;
        }

        if ("HALF_PAY".equals(existingType) && "NO_PAY".equals(newType)) {
            return true;
        }

        // Check date relationship - new leave should be after or continuous with existing
        LocalDate newStartDate = newRequest.getStartDate();
        LocalDate existingStartDate = existingLeave.getStartDate();

        // Allow if new leave starts after existing leave started
        return !newStartDate.isBefore(existingStartDate);
    }

    // Helper method to format maternity leave type for display
    private String formatMaternityLeaveType(String maternityLeaveType) {
        if (maternityLeaveType == null) return "Full Pay";
        switch (maternityLeaveType.toUpperCase()) {
            case "FULL_PAY": return "Full Pay";
            case "HALF_PAY": return "Half Pay";
            case "NO_PAY": return "No Pay";
            default: return maternityLeaveType.replace("_", " ");
        }
    }

// Replace your existing setMaternityLeaveEndDate method with this updated version
public String setMaternityLeaveEndDate(String leaveId, String adminEmail, LocalDate endDate, String adminComments) {
    try {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        // Validate it's a maternity leave
        if (!leave.isMaternityLeave()) {
            return "This is not a maternity leave request";
        }

        // Validate the leave is approved
        if (leave.getStatus() != LeaveStatus.APPROVED) {
            return "Maternity leave must be approved before setting end date";
        }

        // Validate end date is not set already
        if (leave.isMaternityEndDateSet()) {
            return "End date has already been set for this maternity leave";
        }

        // Validate end date is after start date
        if (endDate.isBefore(leave.getStartDate()) || endDate.isEqual(leave.getStartDate())) {
            return "End date must be after the start date";
        }

        // Set the end date and mark as set
        LocalDateTime originalCreatedAt = leave.getCreatedAt();
        LocalDateTime originalActingApprovedAt = leave.getActingOfficerApprovedAt();
        LocalDateTime originalSupervisingApprovedAt = leave.getSupervisingOfficerApprovedAt();
        LocalDateTime originalApprovalApprovedAt = leave.getApprovalOfficerApprovedAt();

        leave.setEndDate(endDate);
        leave.setMaternityEndDateSet(true);

        // Enhanced additional details with admin comments
        StringBuilder additionalDetails = new StringBuilder();
        additionalDetails.append("End date set by admin: ").append(adminEmail);
        additionalDetails.append(" on ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")));
        if (adminComments != null && !adminComments.trim().isEmpty()) {
            additionalDetails.append(" - Comments: ").append(adminComments);
        }
        leave.setMaternityAdditionalDetails(additionalDetails.toString());

        // Preserve original timestamps
        if (originalCreatedAt != null) {
            leave.setCreatedAt(originalCreatedAt);
        }
        if (originalActingApprovedAt != null) {
            leave.setActingOfficerApprovedAt(originalActingApprovedAt);
        }
        if (originalSupervisingApprovedAt != null) {
            leave.setSupervisingOfficerApprovedAt(originalSupervisingApprovedAt);
        }
        if (originalApprovalApprovedAt != null) {
            leave.setApprovalOfficerApprovedAt(originalApprovalApprovedAt);
        }

        leaveRepository.save(leave);

        // Now update entitlements with the actual leave duration
        leaveEntitlementService.updateEntitlementOnLeaveApproval(
                leave.getEmployeeEmail(),
                leave.getLeaveType(),
                leave.getStartDate(),
                leave.getEndDate(),
                false, // not short leave
                false  // not half day
        );

        // FIXED: Send notification to employee about end date being set
        try {
            notificationService.notifyMaternityLeaveEndDateSet(leave, adminEmail);
            logger.info("Maternity leave end date notification sent to employee: {}", leave.getEmployeeEmail());
        } catch (Exception e) {
            logger.warn("Failed to send maternity leave end date notification: {}", e.getMessage(), e);
        }

        long totalDays = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        return "Maternity leave end date set successfully. Duration: " + totalDays + " days. Employee has been notified via email.";

    } catch (Exception e) {
        logger.error("Error setting maternity leave end date: {}", e.getMessage(), e);
        return "Failed to set maternity leave end date: " + e.getMessage();
    }
}

    // NEW: Get maternity leaves needing end date (for admin dashboard)
    public List<Leave> getMaternityLeavesNeedingEndDate() {
        return leaveRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(leave -> leave.isMaternityLeave() &&
                        leave.getStatus() == LeaveStatus.APPROVED &&
                        !leave.isMaternityEndDateSet())
                .collect(Collectors.toList());
    }




    // Add this method to your LeaveService class
// ADDED: Helper method for maternity leave validation during approval
    private String validateMaternityLeaveForApproval(Leave leave) {
        // Basic validation for maternity leave approval
        if (leave.getMaternityLeaveType() == null || leave.getMaternityLeaveType().trim().isEmpty()) {
            return "Maternity leave type is not set";
        }

        // Validate maternity leave type
        if (!Arrays.asList("FULL_PAY", "HALF_PAY", "NO_PAY").contains(leave.getMaternityLeaveType())) {
            return "Invalid maternity leave type";
        }

        // Check if employee already has another active maternity leave
        List<Leave> existingMaternityLeaves = leaveRepository.findByEmployeeEmailOrderByCreatedAtDesc(leave.getEmployeeEmail())
                .stream()
                .filter(existingLeave -> "MATERNITY".equals(existingLeave.getLeaveType()) &&
                        !existingLeave.getId().equals(leave.getId()) && // exclude current leave
                        !existingLeave.isCancelled() &&
                        (existingLeave.getStatus() == LeaveStatus.APPROVED ||
                                existingLeave.getStatus() == LeaveStatus.PENDING_ACTING_OFFICER ||
                                existingLeave.getStatus() == LeaveStatus.PENDING_SUPERVISING_OFFICER ||
                                existingLeave.getStatus() == LeaveStatus.PENDING_APPROVAL_OFFICER))
                .collect(Collectors.toList());

        if (!existingMaternityLeaves.isEmpty()) {
            return "Employee already has an active maternity leave request";
        }

        return "VALID";
    }


}