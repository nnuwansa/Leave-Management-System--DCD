package com.LeaveDataManagementSystem.LeaveManagement.Controller;

import com.LeaveDataManagementSystem.LeaveManagement.DTO.LoginRequest;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.LoginResponse;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.RegisterRequest;
import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository usersRepo;


    private final String ADMIN_EMAIL = "admin@gmail.com";
    private final String ADMIN_PASSWORD = "admin123";


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (ADMIN_EMAIL.equals(loginRequest.getEmail()) && ADMIN_PASSWORD.equals(loginRequest.getPassword())) {
            String token = jwtUtil.generateToken(ADMIN_EMAIL, Set.of("ADMIN"));
            return ResponseEntity.ok(new LoginResponse(token, Set.of("ADMIN")));
        }


        Optional<User> userOpt = usersRepo.findById(loginRequest.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();


            if (loginRequest.getPassword().equals(user.getPassword())) {
                String token = jwtUtil.generateToken(user.getEmail(), user.getRoles());
                return ResponseEntity.ok(new LoginResponse(token, user.getRoles()));
            } else {
                return ResponseEntity.status(401).body("❌ Invalid password");
            }
        }

        return ResponseEntity.status(404).body("❌ User not found");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {

        if (usersRepo.existsById(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("❌ User already exists");
        }

        Set<String> roles;
        if (registerRequest.getRoles() == null || registerRequest.getRoles().isEmpty()) {
            roles = Set.of("EMPLOYEE");
        } else {
            roles = new HashSet<>(registerRequest.getRoles());
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setName(registerRequest.getName());
        user.setPassword(registerRequest.getPassword());

        user.setRoles(roles);


        user.setFullName(registerRequest.getFullName());
        user.setDepartment(registerRequest.getDepartment());
        user.setDesignation(registerRequest.getDesignation());
        user.setJoinDate(registerRequest.getJoinDate());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setAddress(registerRequest.getAddress());
        user.setDateOfBirth(registerRequest.getDateOfBirth());
        user.setGender(registerRequest.getGender());
        user.setMaritalStatus(registerRequest.getMaritalStatus());
        user.setEmploymentType(registerRequest.getEmploymentType());
        user.setNationalId(registerRequest.getNationalId());
        user.setEmergencyContact(registerRequest.getEmergencyContact());

        usersRepo.save(user);
        return ResponseEntity.ok("✅ Employee registered successfully with roles: " + roles);
    }
}
