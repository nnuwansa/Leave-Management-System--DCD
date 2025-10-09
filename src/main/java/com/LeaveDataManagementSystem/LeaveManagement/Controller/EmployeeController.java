package com.LeaveDataManagementSystem.LeaveManagement.Controller;

import com.LeaveDataManagementSystem.LeaveManagement.Config.JwtUtil;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.ChangePasswordRequest;
import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Service.LeaveService;
import com.LeaveDataManagementSystem.LeaveManagement.Service.PasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LeaveService leaveService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordService passwordService;


    @GetMapping("/me")
    public ResponseEntity<?> getMe(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String email = jwtUtil.extractUsername(token);
            User user = userRepository.findById(email)
                    .orElseThrow(() -> new RuntimeException("User Not Found"));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(400).body("Error Fetching User Data");
        }
    }


    @PutMapping("/change-password/{email}")
    public ResponseEntity<?> changePassword(
            @PathVariable String email,
            @RequestBody ChangePasswordRequest request
    ) {
        try {
            String msg = passwordService.changePassword(email, request);
            return ResponseEntity.ok(msg);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // Delete employee
    @DeleteMapping("/{email}")
    public ResponseEntity<String> deleteEmployee(@PathVariable String email) {
        if (userRepository.existsById(email)) {
            userRepository.deleteById(email);
            return ResponseEntity.ok("✅ Employee deleted successfully");
        }
        return ResponseEntity.status(404).body("❌ Employee not found");
    }

    // ---------------- LEAVE OFFICERS / DEPARTMENT ----------------

    // Get acting officers by department
    @GetMapping("/acting-officers/department/{department}")
    public ResponseEntity<?> getActingOfficersByDepartment(@PathVariable String department) {
        try {
            List<User> actingOfficers = leaveService.getActingOfficersByDepartment(department);
            return ResponseEntity.ok(actingOfficers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch acting officers for department");
        }
    }

    // Get approval officers by department
    @GetMapping("/approval-officers/department/{department}")
    public ResponseEntity<?> getApprovalOfficersByDepartment(@PathVariable String department) {
        try {
            List<User> approvalOfficers = leaveService.getApprovalOfficersByDepartment(department);
            return ResponseEntity.ok(approvalOfficers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch approval officers for department");
        }
    }

    // Get both acting and approval officers by department
    @GetMapping("/officers/department/{department}")
    public ResponseEntity<?> getOfficersByDepartment(@PathVariable String department) {
        try {
            Map<String, Object> officers = Map.of(
                    "acting", leaveService.getActingOfficersByDepartment(department),
                    "approval", leaveService.getApprovalOfficersByDepartment(department)
            );
            return ResponseEntity.ok(officers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch officers for department");
        }
    }

    // Get officers by department excluding current user
    @GetMapping("/officers/department/{department}/exclude/{email}")
    public ResponseEntity<?> getOfficersByDepartmentExcluding(
            @PathVariable String department,
            @PathVariable String email) {
        try {
            Map<String, Object> officers = Map.of(
                    "acting", leaveService.getActingOfficersByDepartmentExcluding(department, email),
                    "approval", leaveService.getApprovalOfficersByDepartmentExcluding(department, email)
            );
            return ResponseEntity.ok(officers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch officers for department");
        }
    }

    // Get all departments that have officers
    @GetMapping("/departments")
    public ResponseEntity<?> getAllDepartments() {
        try {
            List<String> departments = leaveService.getAllDepartmentsWithOfficers();
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch departments");
        }
    }

    // Get officers for current user's department
    @GetMapping("/my-department-officers")
    public ResponseEntity<?> getMyDepartmentOfficers(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractEmail(token.replace("Bearer ", ""));
            Map<String, Object> officers = leaveService.getOfficersForEmployee(email);
            return ResponseEntity.ok(officers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Failed to fetch department officers");
        }
    }


    //  Get approval officers + department heads (excluding current user)
    @GetMapping("/approval-officers/{department}")
    public ResponseEntity<List<User>> getApprovalOfficersByDepartment(
            @PathVariable String department,
            @RequestParam String excludeEmail) {

        List<User> officers = leaveService.getApprovalOfficersByDepartmentExcluding(department, excludeEmail);
        return ResponseEntity.ok(officers);
    }


}