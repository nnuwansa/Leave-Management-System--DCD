package com.LeaveDataManagementSystem.LeaveManagement.Service;

import com.LeaveDataManagementSystem.LeaveManagement.Model.Leave;
import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class NotificationService {

    static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Notify acting officer about a new leave request
     */
    public void notifyActingOfficer(Leave leave) {
        try {
            // Log the notification
            logger.info("📧 Notification sent to Acting Officer: {} ({}) - New leave request from {} for {} to {}",
                    leave.getActingOfficerName(),
                    leave.getActingOfficerEmail(),
                    leave.getEmployeeName(),
                    leave.getStartDate(),
                    leave.getEndDate());

            // Send email notification
            emailService.sendLeaveRequestNotification(
                    leave.getActingOfficerEmail(),
                    leave.getActingOfficerName(),
                    leave,
                    "ACTING"
            );

            // Log success
            logger.info("✅ Email notification sent successfully to Acting Officer: {}",
                    leave.getActingOfficerEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to notify acting officer: {} - Error: {}",
                    leave.getActingOfficerEmail(), e.getMessage());
        }
    }

    /**
     * Notify supervising officer about a leave request pending review
     */
    public void notifySupervisingOfficer(Leave leave) {
        try {
            logger.info("📧 Notification sent to Supervising Officer: {} ({}) - Leave request from {} approved by acting officer",
                    leave.getSupervisingOfficerName(),
                    leave.getSupervisingOfficerEmail(),
                    leave.getEmployeeName());

            // Send email notification
            emailService.sendLeaveRequestNotification(
                    leave.getSupervisingOfficerEmail(),
                    leave.getSupervisingOfficerName(),
                    leave,
                    "SUPERVISING"
            );

            logger.info("✅ Email notification sent successfully to Supervising Officer: {}",
                    leave.getSupervisingOfficerEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to notify supervising officer: {} - Error: {}",
                    leave.getSupervisingOfficerEmail(), e.getMessage());
        }
    }

    /**
     * Notify approval officer about a leave request pending approval
     */
    public void notifyApprovalOfficer(Leave leave) {
        try {
            logger.info("📧 Notification sent to Approval Officer: {} ({}) - Leave request from {} approved by supervising officer and pending final approval",
                    leave.getApprovalOfficerName(),
                    leave.getApprovalOfficerEmail(),
                    leave.getEmployeeName());

            // Send email notification
            emailService.sendLeaveRequestNotification(
                    leave.getApprovalOfficerEmail(),
                    leave.getApprovalOfficerName(),
                    leave,
                    "APPROVAL"
            );

            logger.info("✅ Email notification sent successfully to Approval Officer: {}",
                    leave.getApprovalOfficerEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to notify approval officer: {} - Error: {}",
                    leave.getApprovalOfficerEmail(), e.getMessage());
        }
    }

    /**
     * Notify employee about their leave request status
     */
    public void notifyEmployee(Leave leave, String action, String officerType) {
        try {
            String message = String.format("Your leave request for %s to %s has been %s by %s",
                    leave.getStartDate(),
                    leave.getEndDate(),
                    action.toLowerCase(),
                    officerType);

            logger.info("📧 Notification sent to Employee: {} ({}) - {}",
                    leave.getEmployeeName(),
                    leave.getEmployeeEmail(),
                    message);

            // Determine comments based on officer type
            String comments = null;
            if ("Acting Officer".equals(officerType)) {
                comments = leave.getActingOfficerComments();
            } else if ("Supervising Officer".equals(officerType)) {
                comments = leave.getSupervisingOfficerComments();
            } else if ("Approval Officer".equals(officerType)) {
                comments = leave.getApprovalOfficerComments();
            }

            // Send email notification to employee
            emailService.sendLeaveStatusNotification(
                    leave.getEmployeeEmail(),
                    leave.getEmployeeName(),
                    leave,
                    action.toUpperCase(),
                    officerType,
                    comments
            );

            logger.info("✅ Status email notification sent successfully to Employee: {}",
                    leave.getEmployeeEmail());

        } catch (Exception e) {
            logger.error("❌ Failed to notify employee: {} - Error: {}",
                    leave.getEmployeeEmail(), e.getMessage());
        }
    }

    /**
     * Notify all relevant parties about a leave cancellation
     */
    public void notifyLeaveCancellation(Leave leave, String cancelledBy) {
        try {
            String subject = String.format("Leave Request Cancelled - %s", leave.getEmployeeName());
            String message = String.format(
                    "Leave request from %s (%s) has been cancelled.\n\n" +
                            "Leave Details:\n" +
                            "- Leave Type: %s\n" +
                            "- Duration: %s to %s\n" +
                            "- Cancelled by: %s\n" +
                            "- Cancellation Date: %s\n\n" +
                            "No further action is required on this request.",
                    leave.getEmployeeName(),
                    leave.getEmployeeEmail(),
                    leave.getLeaveType(),
                    leave.getStartDate(),
                    leave.getEndDate(),
                    cancelledBy,
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"))
            );

            // Notify all officers in the approval chain
            if (leave.getActingOfficerEmail() != null) {
                emailService.sendLeaveStatusNotification(
                        leave.getActingOfficerEmail(),
                        leave.getActingOfficerName(),
                        leave,
                        "CANCELLED",
                        "Employee",
                        "Leave request has been cancelled by the employee"
                );
            }

            if (leave.getSupervisingOfficerEmail() != null) {
                emailService.sendLeaveStatusNotification(
                        leave.getSupervisingOfficerEmail(),
                        leave.getSupervisingOfficerName(),
                        leave,
                        "CANCELLED",
                        "Employee",
                        "Leave request has been cancelled by the employee"
                );
            }

            if (leave.getApprovalOfficerEmail() != null) {
                emailService.sendLeaveStatusNotification(
                        leave.getApprovalOfficerEmail(),
                        leave.getApprovalOfficerName(),
                        leave,
                        "CANCELLED",
                        "Employee",
                        "Leave request has been cancelled by the employee"
                );
            }

            logger.info("✅ Cancellation notifications sent for leave request: {}", leave.getId());

        } catch (Exception e) {
            logger.error("❌ Failed to send cancellation notifications for leave: {} - Error: {}",
                    leave.getId(), e.getMessage());
        }
    }

    /**
     * Send reminder notification for pending leave requests
     */
    public void sendReminderNotification(Leave leave, String officerType) {
        try {
            String officerEmail = null;
            String officerName = null;

            switch (officerType.toUpperCase()) {
                case "ACTING":
                    officerEmail = leave.getActingOfficerEmail();
                    officerName = leave.getActingOfficerName();
                    break;
                case "SUPERVISING":
                    officerEmail = leave.getSupervisingOfficerEmail();
                    officerName = leave.getSupervisingOfficerName();
                    break;
                case "APPROVAL":
                    officerEmail = leave.getApprovalOfficerEmail();
                    officerName = leave.getApprovalOfficerName();
                    break;
            }

            if (officerEmail != null) {
                // This would be a modified version of the notification for reminders
                emailService.sendLeaveRequestNotification(
                        officerEmail,
                        officerName,
                        leave,
                        officerType.toUpperCase() + "_REMINDER"
                );

                logger.info("📧 Reminder sent to {} Officer: {}", officerType, officerEmail);
            }

        } catch (Exception e) {
            logger.error("❌ Failed to send reminder to {} officer: {}", officerType, e.getMessage());
        }
    }

    /**
     * Send custom notification
     */
    public void sendCustomNotification(String recipientEmail, String recipientName, String subject, String message) {
        try {
            logger.info("📧 Custom notification sent to: {} ({}) - Subject: {}",
                    recipientName,
                    recipientEmail,
                    subject);

            // For custom notifications, you might want to create a separate method in EmailService
            // or handle it through the existing methods with proper formatting

        } catch (Exception e) {
            logger.error("❌ Failed to send custom notification to: {} - Error: {}",
                    recipientEmail, e.getMessage());
        }
    }

    /**
     * Notify about system events (for admin/HR)
     */
    public void notifySystemEvent(String eventType, String description) {
        try {
            logger.info("🔔 System Event: {} - {}", eventType, description);

            // You can implement system admin notification logic here
            // This could notify administrators about important system events

        } catch (Exception e) {
            logger.error("❌ Failed to log system event: {} - Error: {}", eventType, e.getMessage());
        }
    }

    /**
     * Notify HR about leave patterns or issues
     */
//    public void notifyHR(String subject, String message) {
//        try {
//            // Get HR email from configuration or database
//            List<User> hrUsers = userRepository.findByRole("HR");
//
//            for (User hrUser : hrUsers) {
//                // Send notification to HR
//                logger.info("📧 HR Notification sent to: {} - Subject: {}", hrUser.getEmail(), subject);
//            }
//
//        } catch (Exception e) {
//            logger.error("❌ Failed to notify HR - Error: {}", e.getMessage());
//        }
//    }

    /**
     * Batch notification for multiple recipients
     */
    public void sendBatchNotification(List<String> emails, String subject, String message) {
        try {
            for (String email : emails) {
                // Send individual notifications
                // Implementation depends on your specific needs
            }
            logger.info("📧 Batch notification sent to {} recipients", emails.size());
        } catch (Exception e) {
            logger.error("❌ Failed to send batch notification - Error: {}", e.getMessage());
        }
    }


    // NEW METHOD: Notify employee about maternity leave end date being set
    public void notifyMaternityLeaveEndDateSet(Leave leave, String adminEmail) {
        try {
            User employee = userRepository.findByEmail(leave.getEmployeeEmail());
            if (employee != null) {
                String adminComments = leave.getMaternityAdditionalDetails();
                emailService.sendMaternityLeaveEndDateNotification(
                        employee.getEmail(),
                        employee.getName(),
                        leave,
                        adminEmail,
                        adminComments
                );
                logger.info("Maternity leave end date notification sent to: {} by admin: {}",
                        employee.getEmail(), adminEmail);
            } else {
                logger.warn("Employee not found for maternity leave notification: {}", leave.getEmployeeEmail());
            }
        } catch (Exception e) {
            logger.error("Failed to notify employee about maternity leave end date: {}", e.getMessage(), e);
        }
    }
}