package com.LeaveDataManagementSystem.LeaveManagement.Service;

import com.LeaveDataManagementSystem.LeaveManagement.Config.JwtUtil;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.LoginRequest;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.LoginResponse;
import com.LeaveDataManagementSystem.LeaveManagement.DTO.RegisterRequest;
import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public String register(RegisterRequest registerRequest) {
        if (userRepo.existsById(registerRequest.getEmail())) {
            throw new RuntimeException("User already exists");
        }

        Set<String> roles = registerRequest.getRoles();
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add("EMPLOYEE"); // default role
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setName(registerRequest.getName());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRoles(roles);

        // 🔹 Map extra employee details
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

        userRepo.save(user);

        return "✅ User registered successfully with roles: " + roles;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        User user = userRepo.findById(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRoles());

        return new LoginResponse(token, user.getRoles());
    }
}
