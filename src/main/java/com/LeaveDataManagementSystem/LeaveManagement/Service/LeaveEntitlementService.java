package com.LeaveDataManagementSystem.LeaveManagement.Service;

import com.LeaveDataManagementSystem.LeaveManagement.Model.*;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.LeaveEntitlementRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.LeaveRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.ShortLeaveEntitlementRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaveEntitlementService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveEntitlementService.class);

    @Autowired
    private LeaveEntitlementRepository leaveEntitlementRepository;

    @Autowired
    private ShortLeaveEntitlementRepository shortLeaveEntitlementRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UserRepository userRepository;

    // Standard leave entitlements (-1 means unlimited for DUTY)
    private static final Map<String, Integer> STANDARD_ENTITLEMENTS = Map.of(
            "CASUAL", 21,
            "SICK", 24,
            "DUTY", -1  // Unlimited duty leave
    );

    // Desired order of leave types
    private static final List<String> LEAVE_ORDER =
            Arrays.asList("CASUAL", "SICK", "DUTY");

    // ------------------- VALIDATE LEAVE REQUEST (UPDATED FOR UNLIMITED DUTY) -------------------
    public String validateLeaveRequest(String employeeEmail, String leaveType,
                                       LocalDate startDate, LocalDate endDate,
                                       boolean isHalfDay, String halfDayPeriod) {

        int currentYear = LocalDate.now().getYear();

        // Handle short leave types (both "SHORT" and "SHORT_LEAVE")
        if ("SHORT".equals(leaveType) || "SHORT_LEAVE".equals(leaveType)) {
            return validateShortLeaveRequest(employeeEmail, startDate);
        }

        // For half-day leaves, always use CASUAL leave type for validation
        String actualLeaveType = "HALF_DAY".equals(leaveType) ? "CASUAL" : leaveType;

        // Calculate days based on whether it's half day or not
        double requestedDays;
        if (isHalfDay || "HALF_DAY".equals(leaveType)) {
            requestedDays = 0.5; // Half day = 0.5 days
        } else {
            requestedDays = calculateDays(startDate, endDate);
        }

        // Initialize entitlements if not exists
        initializeEntitlementsForEmployee(employeeEmail);

        // Get entitlement for the actual leave type (CASUAL for half-days)
        Optional<LeaveEntitlement> entitlementOpt = leaveEntitlementRepository
                .findByEmployeeEmailAndLeaveTypeAndYear(employeeEmail, actualLeaveType, currentYear);

        if (entitlementOpt.isEmpty()) {
            return "Leave entitlement not found for leave type: " + actualLeaveType;
        }

        LeaveEntitlement entitlement = entitlementOpt.get();

        // For DUTY leave (unlimited), always allow the request
        if ("DUTY".equals(actualLeaveType) && entitlement.isUnlimited()) {
            return "VALID";
        }

        // For half-day leaves, check effective remaining days (including accumulated half days)
        if ("HALF_DAY".equals(leaveType)) {
            if (!entitlement.canTakeHalfDay()) {
                return String.format(
                        "Insufficient CASUAL leave balance for half-day. Available: %.1f days",
                        entitlement.getEffectiveRemainingDays()
                );
            }
        } else {
            // Check if sufficient leave is available for regular leaves
            if (!entitlement.hasSufficientLeave(requestedDays)) {
                if (entitlement.isUnlimited()) {
                    return "VALID"; // Unlimited leave is always available
                } else {
                    return String.format(
                            "Insufficient %s leave balance. Requested: %.1f days, Available: %.1f days",
                            actualLeaveType.replace("_", " "), requestedDays, entitlement.getRemainingDays()
                    );
                }
            }
        }

        return "VALID";
    }

    // Overloaded method for regular leaves (backward compatibility)
    public String validateLeaveRequest(String employeeEmail, String leaveType,
                                       LocalDate startDate, LocalDate endDate) {
        boolean isHalfDay = "HALF_DAY".equals(leaveType);
        return validateLeaveRequest(employeeEmail, leaveType, startDate, endDate, isHalfDay, null);
    }

    // ------------------- VALIDATE SHORT LEAVE REQUEST (UNCHANGED) -------------------
    public String validateShortLeaveRequest(String employeeEmail, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        // Initialize short leave entitlement for the month if not exists
        initializeShortLeaveEntitlementForMonth(employeeEmail, year, month);

        // Get short leave entitlement for the specific month
        Optional<ShortLeaveEntitlement> shortLeaveEntitlementOpt =
                shortLeaveEntitlementRepository.findByEmployeeEmailAndYearAndMonth(employeeEmail, year, month);

        if (shortLeaveEntitlementOpt.isEmpty()) {
            return "Short leave entitlement not found for the month";
        }

        ShortLeaveEntitlement shortLeaveEntitlement = shortLeaveEntitlementOpt.get();

        if (!shortLeaveEntitlement.hasShortLeaveAvailable()) {
            return String.format(
                    "You have already taken the maximum number of short leaves (%d) this month. Remaining: %d",
                    shortLeaveEntitlement.getTotalShortLeaves(),
                    shortLeaveEntitlement.getRemainingShortLeaves()
            );
        }

        return "VALID";
    }

    // ------------------- INITIALIZE SHORT LEAVE ENTITLEMENTS -------------------
    public void initializeShortLeaveEntitlementForMonth(String employeeEmail, int year, int month) {
        if (!shortLeaveEntitlementRepository.existsByEmployeeEmailAndYearAndMonth(employeeEmail, year, month)) {
            ShortLeaveEntitlement shortLeaveEntitlement = new ShortLeaveEntitlement(employeeEmail, year, month);
            shortLeaveEntitlementRepository.save(shortLeaveEntitlement);
        }
    }

    // ------------------- UPDATE ENTITLEMENTS (SUPPORTS UNLIMITED DUTY) -------------------
    public void updateEntitlementOnLeaveApproval(String employeeEmail, String leaveType,
                                                 LocalDate startDate, LocalDate endDate,
                                                 boolean isShortLeave, boolean isHalfDay) {

        int currentYear = LocalDate.now().getYear();

        // Handle short leaves (both "SHORT" and "SHORT_LEAVE")
        if (isShortLeave || "SHORT".equals(leaveType) || "SHORT_LEAVE".equals(leaveType)) {
            updateShortLeaveEntitlementOnApproval(employeeEmail, startDate);
            return; // No regular entitlement deduction for short leaves
        }

        // For half-day leaves, use CASUAL leave type
        String actualLeaveType = ("HALF_DAY".equals(leaveType) || isHalfDay) ? "CASUAL" : leaveType;

        // Calculate leave days
        double leaveDays;
        if ("HALF_DAY".equals(leaveType) || isHalfDay) {
            leaveDays = 0.5; // Half day = 0.5 days
        } else {
            leaveDays = calculateDays(startDate, endDate);
        }

        Optional<LeaveEntitlement> entitlementOpt = leaveEntitlementRepository
                .findByEmployeeEmailAndLeaveTypeAndYear(employeeEmail, actualLeaveType, currentYear);

        if (entitlementOpt.isPresent()) {
            LeaveEntitlement entitlement = entitlementOpt.get();

            if ("HALF_DAY".equals(leaveType) || isHalfDay) {
                // For half days, use the special half day tracking
                entitlement.addHalfDay();
            } else {
                // For regular leaves, update used days normally (works for both limited and unlimited)
                entitlement.updateUsedDays(leaveDays);
            }

            leaveEntitlementRepository.save(entitlement);

            logger.info("Updated entitlement for {} - Type: {}, Used Days: {}, Remaining: {}, Is Unlimited: {}",
                    employeeEmail, actualLeaveType, entitlement.getUsedDays(),
                    entitlement.getRemainingDaysDisplay(), entitlement.isUnlimited());
        }
    }

    // ------------------- UPDATE SHORT LEAVE ENTITLEMENT -------------------
    private void updateShortLeaveEntitlementOnApproval(String employeeEmail, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        // Initialize if not exists
        initializeShortLeaveEntitlementForMonth(employeeEmail, year, month);

        Optional<ShortLeaveEntitlement> shortLeaveEntitlementOpt =
                shortLeaveEntitlementRepository.findByEmployeeEmailAndYearAndMonth(employeeEmail, year, month);

        if (shortLeaveEntitlementOpt.isPresent()) {
            ShortLeaveEntitlement shortLeaveEntitlement = shortLeaveEntitlementOpt.get();
            shortLeaveEntitlement.useShortLeave();
            shortLeaveEntitlementRepository.save(shortLeaveEntitlement);
        }
    }

    // Overloaded method for backward compatibility
    public void updateEntitlementOnLeaveApproval(String employeeEmail, String leaveType,
                                                 LocalDate startDate, LocalDate endDate) {
        boolean isHalfDay = "HALF_DAY".equals(leaveType);
        boolean isShortLeave = "SHORT".equals(leaveType) || "SHORT_LEAVE".equals(leaveType);
        updateEntitlementOnLeaveApproval(employeeEmail, leaveType, startDate, endDate, isShortLeave, isHalfDay);
    }

    // ------------------- REVERT ENTITLEMENTS (SUPPORTS UNLIMITED DUTY) -------------------
    public void revertEntitlementOnLeaveRejection(String employeeEmail, String leaveType,
                                                  LocalDate startDate, LocalDate endDate,
                                                  boolean isShortLeave, boolean isHalfDay) {

        logger.info("Starting entitlement reversion for employee: {}, leaveType: {}, startDate: {}, endDate: {}, isShortLeave: {}, isHalfDay: {}",
                employeeEmail, leaveType, startDate, endDate, isShortLeave, isHalfDay);

        int currentYear = LocalDate.now().getYear();

        // Handle short leaves (both "SHORT" and "SHORT_LEAVE")
        if (isShortLeave || "SHORT".equals(leaveType) || "SHORT_LEAVE".equals(leaveType)) {
            logger.info("Reverting short leave entitlement for employee: {}", employeeEmail);
            revertShortLeaveEntitlementOnRejection(employeeEmail, startDate);
            return;
        }

        // For half-day leaves, use CASUAL leave type
        String actualLeaveType = ("HALF_DAY".equals(leaveType) || isHalfDay) ? "CASUAL" : leaveType;

        logger.info("Actual leave type for reversion: {} (original: {})", actualLeaveType, leaveType);

        // Calculate leave days
        double leaveDays;
        if ("HALF_DAY".equals(leaveType) || isHalfDay) {
            leaveDays = 0.5;
        } else {
            leaveDays = calculateDays(startDate, endDate);
        }

        logger.info("Leave days to revert: {}", leaveDays);

        Optional<LeaveEntitlement> entitlementOpt = leaveEntitlementRepository
                .findByEmployeeEmailAndLeaveTypeAndYear(employeeEmail, actualLeaveType, currentYear);

        if (entitlementOpt.isPresent()) {
            LeaveEntitlement entitlement = entitlementOpt.get();

            logger.info("Found entitlement - Before reversion: usedDays={}, remainingDays={}, accumulatedHalfDays={}, isUnlimited={}",
                    entitlement.getUsedDays(), entitlement.getRemainingDaysDisplay(), entitlement.getAccumulatedHalfDays(), entitlement.isUnlimited());

            if ("HALF_DAY".equals(leaveType) || isHalfDay) {
                // For half days, remove half day
                entitlement.removeHalfDay();
                logger.info("Reverted half day. After reversion: usedDays={}, accumulatedHalfDays={}",
                        entitlement.getUsedDays(), entitlement.getAccumulatedHalfDays());
            } else {
                // For regular leaves, revert the used days (works for both limited and unlimited)
                double oldUsedDays = entitlement.getUsedDays();
                entitlement.setUsedDays(Math.max(0, entitlement.getUsedDays() - leaveDays));

                // Update remaining days only for limited entitlements
                if (!entitlement.isUnlimited()) {
                    entitlement.setRemainingDays(entitlement.getTotalEntitlement() - entitlement.getUsedDays());
                }

                logger.info("Reverted regular leave. Used days: {} -> {}, Remaining days: {}, Is Unlimited: {}",
                        oldUsedDays, entitlement.getUsedDays(), entitlement.getRemainingDaysDisplay(), entitlement.isUnlimited());
            }

            leaveEntitlementRepository.save(entitlement);

            logger.info("Entitlement saved successfully - Final state: usedDays={}, remainingDays={}, accumulatedHalfDays={}, isUnlimited={}",
                    entitlement.getUsedDays(), entitlement.getRemainingDaysDisplay(), entitlement.getAccumulatedHalfDays(), entitlement.isUnlimited());
        } else {
            logger.warn("No entitlement found for employee: {}, leaveType: {}, year: {}",
                    employeeEmail, actualLeaveType, currentYear);
        }
    }

    // Overloaded method for backward compatibility
    public void revertEntitlementOnLeaveRejection(String employeeEmail, String leaveType,
                                                  LocalDate startDate, LocalDate endDate) {
        boolean isHalfDay = "HALF_DAY".equals(leaveType);
        boolean isShortLeave = "SHORT".equals(leaveType) || "SHORT_LEAVE".equals(leaveType);
        revertEntitlementOnLeaveRejection(employeeEmail, leaveType, startDate, endDate, isShortLeave, isHalfDay);
    }

    // ------------------- REVERT SHORT LEAVE ENTITLEMENT -------------------
    private void revertShortLeaveEntitlementOnRejection(String employeeEmail, LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();

        logger.info("Reverting short leave entitlement for employee: {}, year: {}, month: {}",
                employeeEmail, year, month);

        Optional<ShortLeaveEntitlement> shortLeaveEntitlementOpt =
                shortLeaveEntitlementRepository.findByEmployeeEmailAndYearAndMonth(employeeEmail, year, month);

        if (shortLeaveEntitlementOpt.isPresent()) {
            ShortLeaveEntitlement shortLeaveEntitlement = shortLeaveEntitlementOpt.get();

            logger.info("Found short leave entitlement - Before reversion: used={}, remaining={}",
                    shortLeaveEntitlement.getUsedShortLeaves(), shortLeaveEntitlement.getRemainingShortLeaves());

            shortLeaveEntitlement.revertShortLeave();
            shortLeaveEntitlementRepository.save(shortLeaveEntitlement);

            logger.info("Short leave entitlement reverted - After reversion: used={}, remaining={}",
                    shortLeaveEntitlement.getUsedShortLeaves(), shortLeaveEntitlement.getRemainingShortLeaves());
        } else {
            logger.warn("No short leave entitlement found for employee: {}, year: {}, month: {}",
                    employeeEmail, year, month);
        }
    }

    // ------------------- GET SHORT LEAVE ENTITLEMENTS -------------------
    public List<ShortLeaveEntitlement> getEmployeeShortLeaveEntitlements(String employeeEmail) {
        return shortLeaveEntitlementRepository.findByEmployeeEmailOrderByYearDescMonthDesc(employeeEmail);
    }

    public ShortLeaveEntitlement getEmployeeShortLeaveEntitlement(String employeeEmail, int year, int month) {
        initializeShortLeaveEntitlementForMonth(employeeEmail, year, month);
        return shortLeaveEntitlementRepository.findByEmployeeEmailAndYearAndMonth(employeeEmail, year, month)
                .orElse(null);
    }

    // ------------------- INITIALIZE ENTITLEMENTS -------------------
    public void initializeEntitlementsForEmployee(String employeeEmail) {
        int currentYear = LocalDate.now().getYear();

        // Initialize regular leave entitlements
        for (Map.Entry<String, Integer> entry : STANDARD_ENTITLEMENTS.entrySet()) {
            String leaveType = entry.getKey();
            int entitlement = entry.getValue();

            if (!leaveEntitlementRepository.existsByEmployeeEmailAndLeaveTypeAndYear(
                    employeeEmail, leaveType, currentYear)) {

                LeaveEntitlement newEntitlement = new LeaveEntitlement(
                        employeeEmail, leaveType, entitlement, currentYear);
                leaveEntitlementRepository.save(newEntitlement);

                logger.info("Initialized {} entitlement for {}: {} days (unlimited: {})",
                        leaveType, employeeEmail, entitlement == -1 ? "Unlimited" : entitlement, entitlement == -1);
            }
        }

        // Initialize short leave entitlement for current month
        int currentMonth = LocalDate.now().getMonthValue();
        initializeShortLeaveEntitlementForMonth(employeeEmail, currentYear, currentMonth);
    }

    public List<LeaveEntitlement> getEmployeeEntitlements(String employeeEmail) {
        int currentYear = LocalDate.now().getYear();

        // Initialize if not exists
        initializeEntitlementsForEmployee(employeeEmail);

        List<LeaveEntitlement> entitlements =
                leaveEntitlementRepository.findByEmployeeEmailAndYear(employeeEmail, currentYear);

        // Sort entitlements in desired order
        entitlements.sort(Comparator.comparingInt(
                e -> LEAVE_ORDER.indexOf(e.getLeaveType()))
        );

        return entitlements;
    }

    public List<LeaveEntitlement> getEmployeeEntitlementsByYear(String employeeEmail, int year) {
        List<LeaveEntitlement> entitlements =
                leaveEntitlementRepository.findByEmployeeEmailAndYear(employeeEmail, year);

        // Sort entitlements in desired order
        entitlements.sort(Comparator.comparingInt(
                e -> LEAVE_ORDER.indexOf(e.getLeaveType()))
        );

        return entitlements;
    }

    public void initializeEntitlementsForNewYear(int year) {
        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            // Initialize regular leave entitlements
            for (Map.Entry<String, Integer> entry : STANDARD_ENTITLEMENTS.entrySet()) {
                String leaveType = entry.getKey();
                int entitlement = entry.getValue();

                if (!leaveEntitlementRepository.existsByEmployeeEmailAndLeaveTypeAndYear(
                        user.getEmail(), leaveType, year)) {

                    LeaveEntitlement newEntitlement = new LeaveEntitlement(
                            user.getEmail(), leaveType, entitlement, year);
                    leaveEntitlementRepository.save(newEntitlement);
                }
            }

            // Initialize short leave entitlements for all months of the year
            for (int month = 1; month <= 12; month++) {
                initializeShortLeaveEntitlementForMonth(user.getEmail(), year, month);
            }
        }
    }

    private int calculateDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public void adjustEntitlement(String employeeEmail, String leaveType,
                                  int year, int newTotalEntitlement) {

        Optional<LeaveEntitlement> entitlementOpt = leaveEntitlementRepository
                .findByEmployeeEmailAndLeaveTypeAndYear(employeeEmail, leaveType, year);

        if (entitlementOpt.isPresent()) {
            LeaveEntitlement entitlement = entitlementOpt.get();
            entitlement.setTotalEntitlement(newTotalEntitlement);

            // Handle remaining days based on unlimited status
            if (newTotalEntitlement == -1) {
                entitlement.setRemainingDays(-1.0); // Set as unlimited
            } else {
                entitlement.setRemainingDays(newTotalEntitlement - entitlement.getUsedDays());
            }

            leaveEntitlementRepository.save(entitlement);
            logger.info("Adjusted entitlement for {} - Type: {}, New Total: {}, Is Unlimited: {}",
                    employeeEmail, leaveType, newTotalEntitlement == -1 ? "Unlimited" : newTotalEntitlement, newTotalEntitlement == -1);
        } else {
            // Create new entitlement with custom value
            LeaveEntitlement newEntitlement = new LeaveEntitlement(
                    employeeEmail, leaveType, newTotalEntitlement, year);
            leaveEntitlementRepository.save(newEntitlement);
        }
    }

    public Map<String, Object> getEntitlementSummary(String employeeEmail) {
        int currentYear = LocalDate.now().getYear();
        List<LeaveEntitlement> entitlements = getEmployeeEntitlements(employeeEmail);

        Map<String, Object> summary = new HashMap<>();
        summary.put("year", currentYear);
        summary.put("employeeEmail", employeeEmail);
        summary.put("entitlements", entitlements);

        // Calculate totals (excluding unlimited entitlements from totals)
        double totalUsed = entitlements.stream()
                .filter(e -> !e.isUnlimited())
                .mapToDouble(e -> e.getUsedDays() + (e.getAccumulatedHalfDays() * 0.5))
                .sum();

        double totalRemaining = entitlements.stream()
                .filter(e -> !e.isUnlimited())
                .mapToDouble(LeaveEntitlement::getEffectiveRemainingDays)
                .sum();

        // Get DUTY leave usage separately
        Optional<LeaveEntitlement> dutyLeave = entitlements.stream()
                .filter(e -> "DUTY".equals(e.getLeaveType()) && e.isUnlimited())
                .findFirst();

        if (dutyLeave.isPresent()) {
            summary.put("dutyLeaveUsed", dutyLeave.get().getUsedDays());
            summary.put("dutyLeaveUnlimited", true);
        }

        summary.put("totalUsed", totalUsed);
        summary.put("totalRemaining", totalRemaining);

        // Add short leave entitlements for current month
        int currentMonth = LocalDate.now().getMonthValue();
        ShortLeaveEntitlement currentMonthShortLeave = getEmployeeShortLeaveEntitlement(
                employeeEmail, currentYear, currentMonth);

        if (currentMonthShortLeave != null) {
            summary.put("shortLeaveThisMonth", Map.of(
                    "total", currentMonthShortLeave.getTotalShortLeaves(),
                    "used", currentMonthShortLeave.getUsedShortLeaves(),
                    "remaining", currentMonthShortLeave.getRemainingShortLeaves()
            ));
        }

        return summary;
    }

    public Map<String, Object> getComprehensiveEntitlementSummary(String employeeEmail) {
        Map<String, Object> summary = getEntitlementSummary(employeeEmail);

        // Add all short leave entitlements for the year
        List<ShortLeaveEntitlement> shortLeaveEntitlements = getEmployeeShortLeaveEntitlements(employeeEmail);
        summary.put("shortLeaveEntitlements", shortLeaveEntitlements);

        return summary;
    }

    // Add this method to force refresh entitlements after cancellation
    public void forceRefreshEntitlements(String employeeEmail) {
        logger.info("Force refreshing entitlements for employee: {}", employeeEmail);

        // This will recalculate all entitlements based on approved leaves
        recalculateEntitlements(employeeEmail);

        logger.info("Force refresh completed for employee: {}", employeeEmail);
    }

    // Enhanced recalculateEntitlements method with better logging and unlimited duty support
    public void recalculateEntitlements(String employeeEmail) {
        int currentYear = LocalDate.now().getYear();

        logger.info("Starting entitlement recalculation for employee: {}, year: {}", employeeEmail, currentYear);

        // Initialize entitlements
        initializeEntitlementsForEmployee(employeeEmail);

        // Get all leaves for current year (including cancelled ones)
        List<Leave> allLeaves = leaveRepository.findByEmployeeEmailOrderByCreatedAtDesc(employeeEmail)
                .stream()
                .filter(leave -> leave.getStartDate().getYear() == currentYear)
                .toList();

        // Filter only approved leaves for entitlement calculation
        List<Leave> approvedLeaves = allLeaves.stream()
                .filter(leave -> leave.getStatus() == LeaveStatus.APPROVED && !leave.isCancelled())
                .toList();

        logger.info("Found {} total leaves, {} approved leaves for employee: {}",
                allLeaves.size(), approvedLeaves.size(), employeeEmail);

        // Reset all entitlements to zero used days and accumulated half days
        List<LeaveEntitlement> entitlements = leaveEntitlementRepository
                .findByEmployeeEmailAndYear(employeeEmail, currentYear);

        for (LeaveEntitlement entitlement : entitlements) {
            logger.info("Resetting entitlement: {} - was usedDays={}, accumulatedHalfDays={}, isUnlimited={}",
                    entitlement.getLeaveType(), entitlement.getUsedDays(), entitlement.getAccumulatedHalfDays(), entitlement.isUnlimited());

            entitlement.setUsedDays(0);
            entitlement.setAccumulatedHalfDays(0);

            // Reset remaining days based on unlimited status
            if (entitlement.isUnlimited()) {
                entitlement.setRemainingDays(-1.0);
            } else {
                entitlement.setRemainingDays(entitlement.getTotalEntitlement());
            }

            leaveEntitlementRepository.save(entitlement);
        }

        // Reset short leave entitlements for the year
        List<ShortLeaveEntitlement> shortLeaveEntitlements =
                shortLeaveEntitlementRepository.findByEmployeeEmailAndYear(employeeEmail, currentYear);
        for (ShortLeaveEntitlement shortEntitlement : shortLeaveEntitlements) {
            logger.info("Resetting short leave entitlement for month {}: used={}",
                    shortEntitlement.getMonth(), shortEntitlement.getUsedShortLeaves());

            shortEntitlement.setUsedShortLeaves(0);
            shortEntitlement.setRemainingShortLeaves(shortEntitlement.getTotalShortLeaves());
            shortLeaveEntitlementRepository.save(shortEntitlement);
        }

        // Recalculate used days from approved leaves only
        for (Leave leave : approvedLeaves) {
            logger.info("Processing approved leave: id={}, type={}, startDate={}, endDate={}, isShortLeave={}, isHalfDay={}",
                    leave.getId(), leave.getLeaveType(), leave.getStartDate(), leave.getEndDate(),
                    leave.isShortLeave(), leave.isHalfDay());

            // Handle short leaves
            if (leave.isShortLeave() || "SHORT".equals(leave.getLeaveType()) || "SHORT_LEAVE".equals(leave.getLeaveType())) {
                updateShortLeaveEntitlementOnApproval(employeeEmail, leave.getStartDate());
                continue;
            }

            // Get the actual leave type for entitlement deduction
            String actualLeaveType;
            if (leave.isHalfDay() || "HALF_DAY".equals(leave.getLeaveType())) {
                actualLeaveType = "CASUAL"; // Half days use CASUAL leave
            } else {
                actualLeaveType = leave.getLeaveType();
            }

            Optional<LeaveEntitlement> entitlementOpt = leaveEntitlementRepository
                    .findByEmployeeEmailAndLeaveTypeAndYear(
                            employeeEmail, actualLeaveType, currentYear);

            if (entitlementOpt.isPresent()) {
                LeaveEntitlement entitlement = entitlementOpt.get();

                if (leave.isHalfDay() || "HALF_DAY".equals(leave.getLeaveType())) {
                    // Add half day
                    entitlement.addHalfDay();
                    logger.info("Added half day to {} entitlement. Now: usedDays={}, accumulatedHalfDays={}",
                            actualLeaveType, entitlement.getUsedDays(), entitlement.getAccumulatedHalfDays());
                } else {
                    // Add regular leave days (works for both limited and unlimited)
                    double leaveDays = calculateDays(leave.getStartDate(), leave.getEndDate());
                    entitlement.updateUsedDays(leaveDays);
                    logger.info("Added {} days to {} entitlement. Now: usedDays={}, remainingDays={}, isUnlimited={}",
                            leaveDays, actualLeaveType, entitlement.getUsedDays(), entitlement.getRemainingDaysDisplay(), entitlement.isUnlimited());
                }

                leaveEntitlementRepository.save(entitlement);
            } else {
                logger.warn("No entitlement found for leave type: {} for employee: {}", actualLeaveType, employeeEmail);
            }
        }

        logger.info("Entitlement recalculation completed for employee: {}", employeeEmail);
    }

    // ------------------- GET DUTY LEAVE STATISTICS -------------------
    /**
     * Get DUTY leave statistics for an employee for a specific year
     */
    public Map<String, Object> getDutyLeaveStatistics(String employeeEmail, int year) {
        Optional<LeaveEntitlement> dutyEntitlementOpt = leaveEntitlementRepository
                .findByEmployeeEmailAndLeaveTypeAndYear(employeeEmail, "DUTY", year);

        Map<String, Object> dutyStats = new HashMap<>();
        dutyStats.put("year", year);
        dutyStats.put("employeeEmail", employeeEmail);
        dutyStats.put("leaveType", "DUTY");
        dutyStats.put("isUnlimited", true);

        if (dutyEntitlementOpt.isPresent()) {
            LeaveEntitlement dutyEntitlement = dutyEntitlementOpt.get();
            dutyStats.put("totalDutyLeaveTaken", dutyEntitlement.getUsedDays());
            dutyStats.put("accumulatedHalfDays", dutyEntitlement.getAccumulatedHalfDays());
            dutyStats.put("effectiveDaysUsed", dutyEntitlement.getUsedDays() + (dutyEntitlement.getAccumulatedHalfDays() * 0.5));
        } else {
            dutyStats.put("totalDutyLeaveTaken", 0.0);
            dutyStats.put("accumulatedHalfDays", 0);
            dutyStats.put("effectiveDaysUsed", 0.0);
        }

        // Get all DUTY leaves for the year
        List<Leave> dutyLeaves = leaveRepository.findByEmployeeEmailOrderByCreatedAtDesc(employeeEmail)
                .stream()
                .filter(leave -> "DUTY".equals(leave.getLeaveType()) &&
                        leave.getStartDate().getYear() == year &&
                        leave.getStatus() == LeaveStatus.APPROVED &&
                        !leave.isCancelled())
                .collect(Collectors.toList());

        dutyStats.put("totalDutyLeaveRequests", dutyLeaves.size());
        dutyStats.put("dutyLeaves", dutyLeaves.stream().map(leave -> {
            Map<String, Object> leaveInfo = new HashMap<>();
            leaveInfo.put("id", leave.getId());
            leaveInfo.put("startDate", leave.getStartDate());
            leaveInfo.put("endDate", leave.getEndDate());
            leaveInfo.put("days", leave.getTotalDays());
            leaveInfo.put("reason", leave.getReason());
            leaveInfo.put("approvedAt", leave.getApprovalOfficerApprovedAt());
            return leaveInfo;
        }).collect(Collectors.toList()));

        return dutyStats;
    }

    /**
     * Get DUTY leave statistics for current year
     */
    public Map<String, Object> getDutyLeaveStatistics(String employeeEmail) {
        return getDutyLeaveStatistics(employeeEmail, LocalDate.now().getYear());
    }


    /**
     * Get monthly short leave breakdown for an employee for the current year
     */
    /**
     * Get monthly short leave breakdown for an employee for the current year (SAFE VERSION)
     */
    public Map<String, Object> getEmployeeShortLeaveMonthlyBreakdown(String employeeEmail) {
        try {
            int currentYear = LocalDate.now().getYear();
            Map<String, Object> monthlyData = new HashMap<>();

            String[] monthNames = {"January", "February", "March", "April", "May", "June",
                    "July", "August", "September", "October", "November", "December"};

            for (int month = 1; month <= 12; month++) {
                try {
                    // Initialize short leave entitlement for the month if not exists
                    initializeShortLeaveEntitlementForMonth(employeeEmail, currentYear, month);

                    // Get short leave entitlement for the month
                    Optional<ShortLeaveEntitlement> shortLeaveOpt =
                            shortLeaveEntitlementRepository.findByEmployeeEmailAndYearAndMonth(employeeEmail, currentYear, month);

                    Map<String, Integer> monthData = new HashMap<>();
                    if (shortLeaveOpt.isPresent()) {
                        ShortLeaveEntitlement shortLeave = shortLeaveOpt.get();
                        monthData.put("used", shortLeave.getUsedShortLeaves());
                        monthData.put("total", shortLeave.getTotalShortLeaves());
                        monthData.put("remaining", shortLeave.getRemainingShortLeaves());
                    } else {
                        // Default values if not found
                        monthData.put("used", 0);
                        monthData.put("total", 2);
                        monthData.put("remaining", 2);
                    }

                    monthlyData.put(monthNames[month - 1], monthData);

                } catch (Exception monthError) {
                    logger.warn("Error processing month {} for employee {}: {}", month, employeeEmail, monthError.getMessage());
                    // Add default values for this month so it doesn't break the whole process
                    Map<String, Integer> defaultMonthData = new HashMap<>();
                    defaultMonthData.put("used", 0);
                    defaultMonthData.put("total", 2);
                    defaultMonthData.put("remaining", 2);
                    monthlyData.put(monthNames[month - 1], defaultMonthData);
                }
            }

            logger.debug("Monthly short leave data for {}: {}", employeeEmail, monthlyData);
            return monthlyData;

        } catch (Exception e) {
            logger.error("Error getting monthly short leave breakdown for {}: {}", employeeEmail, e.getMessage(), e);
            // Return empty data instead of throwing exception
            return new HashMap<>();
        }
    }



    /**
     * Get monthly short leave breakdown for an employee for the current year
     */
//    public Map<String, Object> getEmployeeShortLeaveMonthlyBreakdown(String employeeEmail) {
//        int currentYear = LocalDate.now().getYear();
//        Map<String, Object> monthlyData = new HashMap<>();
//
//        String[] monthNames = {"January", "February", "March", "April", "May", "June",
//                "July", "August", "September", "October", "November", "December"};
//
//        for (int month = 1; month <= 12; month++) {
//            // Initialize short leave entitlement for the month if not exists
//            initializeShortLeaveEntitlementForMonth(employeeEmail, currentYear, month);
//
//            // Get short leave entitlement for the month
//            Optional<ShortLeaveEntitlement> shortLeaveOpt =
//                    shortLeaveEntitlementRepository.findByEmployeeEmailAndYearAndMonth(employeeEmail, currentYear, month);
//
//            if (shortLeaveOpt.isPresent()) {
//                ShortLeaveEntitlement shortLeave = shortLeaveOpt.get();
//                Map<String, Integer> monthData = new HashMap<>();
//                monthData.put("used", shortLeave.getUsedShortLeaves());
//                monthData.put("total", shortLeave.getTotalShortLeaves());
//                monthData.put("remaining", shortLeave.getRemainingShortLeaves());
//
//                monthlyData.put(monthNames[month - 1], monthData);
//            } else {
//                // Default values if not found
//                Map<String, Integer> monthData = new HashMap<>();
//                monthData.put("used", 0);
//                monthData.put("total", 2);
//                monthData.put("remaining", 2);
//
//                monthlyData.put(monthNames[month - 1], monthData);
//            }
//        }
//
//        return monthlyData;
//    }
}