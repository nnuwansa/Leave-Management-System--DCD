package com.LeaveDataManagementSystem.LeaveManagement.Controller;


import com.LeaveDataManagementSystem.LeaveManagement.Model.Leave;

import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.LeaveRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Service.LeaveEntitlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LeaveRepository leaveRepository;
    @Autowired
    private LeaveEntitlementService leaveEntitlementService;

    // ✅ View all users
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    // ✅ View single user by email
    @GetMapping("/users/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        return userRepository.findById(email)
                .map(user -> ResponseEntity.ok((Object) user)) // cast to Object for wildcard
                .orElse(ResponseEntity.status(404).body("❌ User not found"));
    }

    // ✅ Update user
    @PutMapping("/users/{email}")
    public ResponseEntity<?> updateUser(@PathVariable String email, @RequestBody User updatedUser) {
        return userRepository.findById(email).map(user -> {
            user.setName(updatedUser.getName());
            user.setFullName(updatedUser.getFullName());
            user.setDepartment(updatedUser.getDepartment());
            user.setDesignation(updatedUser.getDesignation());
            user.setJoinDate(updatedUser.getJoinDate());
            user.setPhoneNumber(updatedUser.getPhoneNumber());
            user.setAddress(updatedUser.getAddress());
            user.setDateOfBirth(updatedUser.getDateOfBirth());
            user.setGender(updatedUser.getGender());
            user.setMaritalStatus(updatedUser.getMaritalStatus());
            user.setEmploymentType(updatedUser.getEmploymentType());
            user.setNationalId(updatedUser.getNationalId());
            user.setEmergencyContact(updatedUser.getEmergencyContact());
            user.setRoles(updatedUser.getRoles());

            userRepository.save(user);
            return ResponseEntity.ok("✅ User updated successfully");
        }).orElse(ResponseEntity.status(404).body("❌ User not found"));
    }

    // ✅ Delete user
    @DeleteMapping("/users/{email}")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        if (userRepository.existsById(email)) {
            userRepository.deleteById(email);
            return ResponseEntity.ok("🗑️ User deleted successfully");
        } else {
            return ResponseEntity.status(404).body("❌ User not found");
        }
    }




    // ✅ Get all leaves for admin (without validateAdminAccess)
    @GetMapping("/leaves")
    public ResponseEntity<?> getAllLeaves() {
        try {
            List<Leave> allLeaves = leaveRepository.findAllByOrderByCreatedAtDesc();

            // Enhanced response with employee details
            List<Map<String, Object>> enhancedLeaves = allLeaves.stream().map(leave -> {
                User employee = userRepository.findByEmail(leave.getEmployeeEmail());
                Map<String, Object> leaveData = new HashMap<>();

                // Basic leave information
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
                // Add these lines after line 110 (after basic leave information):
                 // ADD MATERNITY-SPECIFIC FIELDS
                leaveData.put("isMaternityLeave", leave.isMaternityLeave());
                leaveData.put("maternityLeaveType", leave.getMaternityLeaveType());
                leaveData.put("maternityLeaveDuration", leave.getMaternityLeaveDuration());
                leaveData.put("isMaternityEndDateSet", leave.isMaternityEndDateSet());
                leaveData.put("maternityAdditionalDetails", leave.getMaternityAdditionalDetails());

                // Employee department information
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
                leaveData.put("actingOfficerStatus", leave.getActingOfficerStatus());
                leaveData.put("supervisingOfficerName", leave.getSupervisingOfficerName());
                leaveData.put("supervisingOfficerStatus", leave.getSupervisingOfficerStatus());
                leaveData.put("approvalOfficerName", leave.getApprovalOfficerName());
                leaveData.put("approvalOfficerStatus", leave.getApprovalOfficerStatus());

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
            return ResponseEntity.status(500).body("❌ " + e.getMessage());
        }
    }

    // ✅ Get employee entitlements summary for admin (without validateAdminAccess)
    @GetMapping("/entitlements/{employeeEmail}")
    public ResponseEntity<?> getEmployeeEntitlements(@PathVariable String employeeEmail) {
        try {
            User employee = userRepository.findByEmail(employeeEmail);
            if (employee == null) {
                return ResponseEntity.status(404).body("❌ Employee not found");
            }

            Map<String, Object> summary = leaveEntitlementService.getComprehensiveEntitlementSummary(employeeEmail);
            summary.put("employeeDetails", Map.of(
                    "email", employee.getEmail(),
                    "name", employee.getName(),
                    "fullName", employee.getFullName(),
                    "department", employee.getDepartment(),
                    "designation", employee.getDesignation()
            ));

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            logger.error("Error fetching employee entitlements: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("❌ " + e.getMessage());
        }
    }



    @GetMapping("/entitlements")
    public ResponseEntity<?> getAllEmployeeEntitlements() {
        try {
            List<User> allUsers = userRepository.findAll();
            logger.info("Total users found: {}", allUsers.size());

            List<Map<String, Object>> entitlementSummaries = new ArrayList<>();
            int processedCount = 0;
            int errorCount = 0;

            for (User user : allUsers) {
                try {
                    Map<String, Object> summary = leaveEntitlementService.getEntitlementSummary(user.getEmail());

                    // Add employee details
                    summary.put("employeeDetails", Map.of(
                            "email", user.getEmail(),
                            "name", user.getName(),
                            "fullName", user.getFullName(),
                            "department", user.getDepartment(),
                            "designation", user.getDesignation()
                    ));

                    // SAFE: Try to get monthly short leave data, but don't fail if it errors
                    try {
                        Map<String, Object> monthlyShortLeaveData = leaveEntitlementService.getEmployeeShortLeaveMonthlyBreakdown(user.getEmail());
                        summary.put("shortLeaveMonthlyDetails", monthlyShortLeaveData);
                    } catch (Exception shortLeaveError) {
                        logger.warn("Error getting short leave monthly data for user {}: {}", user.getEmail(), shortLeaveError.getMessage());
                        // Add empty monthly data so the employee still appears
                        summary.put("shortLeaveMonthlyDetails", new HashMap<>());
                    }

                    entitlementSummaries.add(summary);
                    processedCount++;

                } catch (Exception e) {
                    errorCount++;
                    logger.error("Error getting entitlements for user {}: {}", user.getEmail(), e.getMessage(), e);

                    // IMPORTANT: Still add the employee with minimal data so they appear in the list
                    Map<String, Object> fallbackSummary = new HashMap<>();
                    fallbackSummary.put("employeeDetails", Map.of(
                            "email", user.getEmail(),
                            "name", user.getName(),
                            "fullName", user.getFullName(),
                            "department", user.getDepartment(),
                            "designation", user.getDesignation()
                    ));
                    fallbackSummary.put("entitlements", new ArrayList<>());
                    fallbackSummary.put("shortLeaveMonthlyDetails", new HashMap<>());
                    fallbackSummary.put("error", "Failed to load entitlements");

                    entitlementSummaries.add(fallbackSummary);
                }
            }

            logger.info("Successfully processed: {}, Errors: {}, Total returned: {}",
                    processedCount, errorCount, entitlementSummaries.size());

            return ResponseEntity.ok(entitlementSummaries);

        } catch (Exception e) {
            logger.error("Error fetching all employee entitlements: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("❌ " + e.getMessage());
        }
    }

}


