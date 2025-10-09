package com.LeaveDataManagementSystem.LeaveManagement.Service;

import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Model.User; // <-- your entity
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository usersRepo;

    private final String ADMIN_EMAIL = "admin@example.com";
    private final String ADMIN_PASSWORD = "{noop}admin123";


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {


        if (ADMIN_EMAIL.equals(username)) {
            return new org.springframework.security.core.userdetails.User(
                    ADMIN_EMAIL,
                    ADMIN_PASSWORD,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        }

        // ✅ Otherwise load Employee from DB
        User user = usersRepo.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }
}
