package com.LeaveDataManagementSystem.LeaveManagement.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
public class LoginResponse {
    private final String token;
    private final Set<String> roles;

    public LoginResponse(String token, Set<String> roles) {
        this.token = token;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
