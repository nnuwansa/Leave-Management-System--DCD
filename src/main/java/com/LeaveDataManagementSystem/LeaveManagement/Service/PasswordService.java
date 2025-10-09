package com.LeaveDataManagementSystem.LeaveManagement.Service;

import com.LeaveDataManagementSystem.LeaveManagement.DTO.ChangePasswordRequest;
import com.LeaveDataManagementSystem.LeaveManagement.Model.Notification;
import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.NotificationRepository;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private final String ADMIN_EMAIL = "admin@example.com";

    public String changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        if (!request.getOldPassword().equals(user.getPassword())) {
            throw new RuntimeException("Old Password Is Incorrect");
        }

        user.setPassword(request.getNewPassword());
        userRepository.save(user);

        Notification notification = new Notification();
        notification.setRecipient(ADMIN_EMAIL);
        notification.setEmail(user.getEmail());
        notification.setOldPassword(request.getOldPassword());
        notification.setNewPassword(request.getNewPassword());
        notification.setMessage("Employee " + user.getFullName() + " has changed their password");
        notificationRepository.save(notification);

        return "Password Changed Successfully!";
    }
}
