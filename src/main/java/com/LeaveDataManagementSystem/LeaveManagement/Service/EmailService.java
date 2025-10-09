package com.LeaveDataManagementSystem.LeaveManagement.Service;

import com.LeaveDataManagementSystem.LeaveManagement.Model.Leave;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    @Value("${app.company-name:Company}")
    private String companyName;

    @Async
    public void sendLeaveRequestNotification(String toEmail, String toName, Leave leave, String role) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, companyName + " Leave Management System");
            helper.setTo(toEmail);
            helper.setSubject(buildSubject(leave, role));
            helper.setText(buildEmailContent(leave, toName, role), true);

            emailSender.send(message);

            logger.info("Email sent successfully to {} ({}) for leave request {}",
                    toName, toEmail, leave.getId());

        } catch (Exception e) {
            logger.error("Failed to send email to {} - Error: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async
    public void sendLeaveStatusNotification(String toEmail, String toName, Leave leave,
                                            String status, String officerType, String comments) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, companyName + " Leave Management System");
            helper.setTo(toEmail);
            helper.setSubject(buildStatusSubject(leave, status, officerType));
            helper.setText(buildStatusEmailContent(leave, toName, status, officerType, comments), true);

            emailSender.send(message);

            logger.info("Status email sent successfully to {} ({}) for leave request {}",
                    toName, toEmail, leave.getId());

        } catch (Exception e) {
            logger.error("Failed to send status email to {} - Error: {}", toEmail, e.getMessage(), e);
        }
    }

    // NEW METHOD: Send maternity leave end date notification
    @Async
    public void sendMaternityLeaveEndDateNotification(String toEmail, String toName, Leave leave,
                                                      String adminEmail, String adminComments) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, companyName + " Leave Management System");
            helper.setTo(toEmail);
            helper.setSubject(buildMaternityEndDateSubject(leave));
            helper.setText(buildMaternityEndDateEmailContent(leave, toName, adminEmail, adminComments), true);

            emailSender.send(message);

            logger.info("Maternity leave end date notification sent successfully to {} ({}) for leave request {}",
                    toName, toEmail, leave.getId());

        } catch (Exception e) {
            logger.error("Failed to send maternity leave end date notification to {} - Error: {}",
                    toEmail, e.getMessage(), e);
        }
    }

    private String buildSubject(Leave leave, String role) {
        String actionText = getActionText(role);
        return String.format("[%s] %s - Leave Request from %s (%s)",
                companyName, actionText, leave.getEmployeeName(),
                formatLeaveType(leave.getLeaveType()));
    }

    private String buildStatusSubject(Leave leave, String status, String officerType) {
        return String.format("[%s] Leave Request %s by %s - %s",
                companyName, status, officerType, leave.getEmployeeName());
    }

    // NEW METHOD: Build maternity leave end date subject
    private String buildMaternityEndDateSubject(Leave leave) {
        return String.format("[%s] Maternity Leave End Date Confirmed - %s",
                companyName, leave.getEmployeeName());
    }

    private String getActionText(String role) {
        switch (role.toUpperCase()) {
            case "ACTING": return "Action Required - Acting Officer Review";
            case "SUPERVISING": return "Action Required - Supervising Officer Review";
            case "APPROVAL": return "Action Required - Final Approval";
            default: return "New Leave Request";
        }
    }

    private String buildEmailContent(Leave leave, String recipientName, String role) {
        StringBuilder content = new StringBuilder();

        content.append("<!DOCTYPE html>");
        content.append("<html><head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<title>Leave Request Notification</title>");
        content.append("<style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".header { background: #007bff; color: white; padding: 20px; text-align: center; }");
        content.append(".content { background: #f8f9fa; padding: 30px; border-radius: 5px; margin: 20px 0; }");
        content.append(".details { background: white; padding: 20px; border-radius: 5px; margin: 20px 0; }");
        content.append(".detail-row { display: flex; padding: 10px 0; border-bottom: 1px solid #eee; }");
        content.append(".detail-label { font-weight: bold; width: 150px; color: #666; }");
        content.append(".detail-value { flex: 1; }");
        content.append(".action-required { background: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #ffc107; }");
        content.append(".footer { text-align: center; padding: 20px; font-size: 14px; color: #666; }");
        content.append(".button { display: inline-block; padding: 12px 24px; background: #007bff; color: white; text-decoration: none; border-radius: 5px; margin: 10px 5px; }");
        content.append("</style>");
        content.append("</head><body>");

        content.append("<div class='container'>");
        content.append("<div class='header'>");
        content.append("<h2>").append(companyName).append(" Leave Management System</h2>");
        content.append("<p>").append(getActionText(role)).append("</p>");
        content.append("</div>");

        content.append("<div class='content'>");
        content.append("<h3>Dear ").append(recipientName).append(",</h3>");
        content.append("<p>A new leave request has been submitted and requires your attention as the <strong>")
                .append(role.toLowerCase()).append(" officer</strong>.</p>");

        content.append("<div class='action-required'>");
        content.append("<strong>Action Required:</strong> Please review and approve or reject this leave request in the system.");
        content.append("</div>");

        content.append("<div class='details'>");
        content.append("<h4>Leave Request Details</h4>");

        addDetailRow(content, "Employee", leave.getEmployeeName());
        addDetailRow(content, "Employee Email", leave.getEmployeeEmail());
        addDetailRow(content, "Leave Type", formatLeaveType(leave.getLeaveType()));
        addDetailRow(content, "Start Date", formatDate(leave.getStartDate()));
        addDetailRow(content, "End Date", formatDate(leave.getEndDate()));
        addDetailRow(content, "Duration", calculateDuration(leave));
        addDetailRow(content, "Reason", leave.getReason() != null ? leave.getReason() : "No reason provided");
        addDetailRow(content, "Submitted On", formatDateTime(leave.getCreatedAt()));

        if (leave.isHalfDay()) {
            addDetailRow(content, "Half Day Period", leave.getHalfDayPeriod());
        }

        if (leave.isShortLeave()) {
            addDetailRow(content, "Start Time", leave.getShortLeaveStartTime().toString());
            addDetailRow(content, "End Time", leave.getShortLeaveEndTime().toString());
        }

        content.append("</div>");

        content.append("<div style='text-align: center; margin: 30px 0;'>");
        content.append("<a href='").append(baseUrl).append("/dashboard' class='button' style='color: white;'>Review in System</a>");
        content.append("</div>");

        content.append("<p><strong>Note:</strong> Please process this request promptly to avoid delays in the approval workflow.</p>");
        content.append("</div>");

        content.append("<div class='footer'>");
        content.append("<p>This is an automated notification from ").append(companyName).append(" Leave Management System.</p>");
        content.append("<p>Please do not reply to this email.</p>");
        content.append("</div>");

        content.append("</div>");
        content.append("</body></html>");

        return content.toString();
    }

    private String buildStatusEmailContent(Leave leave, String recipientName, String status,
                                           String officerType, String comments) {
        StringBuilder content = new StringBuilder();

        content.append("<!DOCTYPE html>");
        content.append("<html><head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<title>Leave Request Status Update</title>");
        content.append("<style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");

        String headerColor = "APPROVED".equals(status) ? "#28a745" : "#dc3545";
        content.append(".header { background: ").append(headerColor).append("; color: white; padding: 20px; text-align: center; }");
        content.append(".content { background: #f8f9fa; padding: 30px; border-radius: 5px; margin: 20px 0; }");
        content.append(".details { background: white; padding: 20px; border-radius: 5px; margin: 20px 0; }");
        content.append(".detail-row { display: flex; padding: 10px 0; border-bottom: 1px solid #eee; }");
        content.append(".detail-label { font-weight: bold; width: 150px; color: #666; }");
        content.append(".detail-value { flex: 1; }");
        content.append(".footer { text-align: center; padding: 20px; font-size: 14px; color: #666; }");
        content.append("</style>");
        content.append("</head><body>");

        content.append("<div class='container'>");
        content.append("<div class='header'>");
        content.append("<h2>Leave Request ").append(status).append("</h2>");
        content.append("<p>by ").append(officerType).append("</p>");
        content.append("</div>");

        content.append("<div class='content'>");
        content.append("<h3>Dear ").append(recipientName).append(",</h3>");
        content.append("<p>Your leave request has been <strong>").append(status.toLowerCase())
                .append("</strong> by the ").append(officerType.toLowerCase()).append(".</p>");

        content.append("<div class='details'>");
        content.append("<h4>Leave Request Details</h4>");

        addDetailRow(content, "Leave Type", formatLeaveType(leave.getLeaveType()));
        addDetailRow(content, "Start Date", formatDate(leave.getStartDate()));
        addDetailRow(content, "End Date", formatDate(leave.getEndDate()));
        addDetailRow(content, "Duration", calculateDuration(leave));
        addDetailRow(content, "Status", status);
        addDetailRow(content, "Processed By", officerType);

        if (comments != null && !comments.trim().isEmpty()) {
            addDetailRow(content, "Comments", comments);
        }

        content.append("</div>");

        if ("APPROVED".equals(status)) {
            content.append("<p><strong>Next Steps:</strong> ");
            if ("Acting Officer".equals(officerType)) {
                content.append("Your leave request has been forwarded to the supervising officer for review.");
            } else if ("Supervising Officer".equals(officerType)) {
                content.append("Your leave request has been forwarded to the approval officer for final approval.");
            } else {
                content.append("Your leave request has been fully approved. Please ensure proper handover of responsibilities.");
            }
            content.append("</p>");
        }

        content.append("</div>");

        content.append("<div class='footer'>");
        content.append("<p>This is an automated notification from ").append(companyName).append(" Leave Management System.</p>");
        content.append("<p>Please do not reply to this email.</p>");

        content.append("</div>");

        content.append("</div>");
        content.append("</body></html>");

        return content.toString();
    }

    // NEW METHOD: Build maternity leave end date email content
    private String buildMaternityEndDateEmailContent(Leave leave, String recipientName,
                                                     String adminEmail, String adminComments) {
        StringBuilder content = new StringBuilder();

        content.append("<!DOCTYPE html>");
        content.append("<html><head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<title>Maternity Leave End Date Confirmation</title>");
        content.append("<style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".header { background: #e91e63; color: white; padding: 20px; text-align: center; }");
        content.append(".content { background: #f8f9fa; padding: 30px; border-radius: 5px; margin: 20px 0; }");
        content.append(".details { background: white; padding: 20px; border-radius: 5px; margin: 20px 0; }");
        content.append(".detail-row { display: flex; padding: 10px 0; border-bottom: 1px solid #eee; }");
        content.append(".detail-label { font-weight: bold; width: 150px; color: #666; }");
        content.append(".detail-value { flex: 1; }");
        content.append(".highlight { background: #fff3e0; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #ff9800; }");
        content.append(".footer { text-align: center; padding: 20px; font-size: 14px; color: #666; }");
        content.append(".maternity-badge { display: inline-block; padding: 5px 15px; background: #e91e63; color: white; border-radius: 20px; font-size: 14px; font-weight: bold; }");
        content.append("</style>");
        content.append("</head><body>");

        content.append("<div class='container'>");
        content.append("<div class='header'>");
        content.append("<h2>").append(companyName).append(" Leave Management System</h2>");
        content.append("<p>Maternity Leave End Date Confirmation</p>");
        content.append("</div>");

        content.append("<div class='content'>");
        content.append("<h3>Dear ").append(recipientName).append(",</h3>");
        content.append("<p>We are writing to inform you that the end date for your maternity leave has been officially set by the administration.</p>");

        content.append("<div class='highlight'>");
        content.append("<strong>Important Update:</strong> Your maternity leave period has been finalized. ");
        content.append("Please review the details below and plan accordingly for your return to work.");
        content.append("</div>");

        content.append("<div class='details'>");
        content.append("<h4>Maternity Leave Details</h4>");

        // Maternity leave type badge
        String maternityTypeDisplay = formatMaternityLeaveType(leave.getMaternityLeaveType());
        addDetailRowWithBadge(content, "Leave Type", "Maternity Leave", maternityTypeDisplay);

        addDetailRow(content, "Start Date", formatDate(leave.getStartDate()));
        addDetailRow(content, "End Date", formatDate(leave.getEndDate()));

        // Calculate total duration
        long totalDays = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        addDetailRow(content, "Total Duration", totalDays + " days (" + formatWeeksAndDays(totalDays) + ")");

        addDetailRow(content, "Payment Type", formatMaternityLeaveType(leave.getMaternityLeaveType()));
        addDetailRow(content, "Reason", leave.getReason() != null ? leave.getReason() : "Maternity Leave");
        addDetailRow(content, "End Date Set By", "Administration (" + adminEmail + ")");
        addDetailRow(content, "Date Confirmed", formatDateTime(LocalDateTime.now()));

        if (adminComments != null && !adminComments.trim().isEmpty()) {
            addDetailRow(content, "Admin Comments", adminComments);
        }

        content.append("</div>");

        // Important information section
        content.append("<div class='highlight'>");
        content.append("<h4 style='margin-top: 0;'>Important Information:</h4>");
        content.append("<ul style='margin: 10px 0; padding-left: 20px;'>");
        content.append("<li><strong>Return to Work Date:</strong> ").append(formatDate(leave.getEndDate().plusDays(1))).append("</li>");
        content.append("<li><strong>Payment Structure:</strong> ").append(getPaymentDescription(leave.getMaternityLeaveType())).append("</li>");
        content.append("<li>Please contact HR if you have any questions about your return to work arrangements</li>");
        content.append("<li>Ensure you complete any required medical clearances before returning to work</li>");
        content.append("</ul>");
        content.append("</div>");

        content.append("<div style='text-align: center; margin: 30px 0;'>");
        content.append("<a href='").append(baseUrl).append("/dashboard' class='button' style='display: inline-block; padding: 12px 24px; background: #e91e63; color: white; text-decoration: none; border-radius: 5px;'>View in System</a>");
        content.append("</div>");

        content.append("<p>If you have any questions or concerns regarding your maternity leave or return to work, ");
        content.append("please contact the HR department or your supervisor.</p>");

        content.append("<p><strong>Congratulations</strong> on your new addition to the family, and we look forward to your return!</p>");

        content.append("</div>");

        content.append("<div class='footer'>");
        content.append("<p>This is an automated notification from ").append(companyName).append(" Leave Management System.</p>");
        content.append("<p>For assistance, please contact HR at your earliest convenience.</p>");
        content.append("</div>");

        content.append("</div>");
        content.append("</body></html>");

        return content.toString();
    }

    // Helper method to add detail row with badge
    private void addDetailRowWithBadge(StringBuilder content, String label, String mainValue, String badgeValue) {
        content.append("<div class='detail-row'>");
        content.append("<div class='detail-label'>").append(label).append(":</div>");
        content.append("<div class='detail-value'>").append(mainValue);
        content.append(" <span class='maternity-badge'>").append(badgeValue).append("</span>");
        content.append("</div>");
        content.append("</div>");
    }

    // Helper method to format maternity leave type
    private String formatMaternityLeaveType(String maternityLeaveType) {
        if (maternityLeaveType == null) return "Full Pay";
        switch (maternityLeaveType.toUpperCase()) {
            case "FULL_PAY": return "Full Pay";
            case "HALF_PAY": return "Half Pay";
            case "NO_PAY": return "No Pay";
            default: return maternityLeaveType.replace("_", " ");
        }
    }

    // Helper method to get payment description
    private String getPaymentDescription(String maternityLeaveType) {
        if (maternityLeaveType == null) return "Full salary will be paid during the leave period";
        switch (maternityLeaveType.toUpperCase()) {
            case "FULL_PAY": return "Full salary will be paid during the leave period";
            case "HALF_PAY": return "50% of salary will be paid during the leave period";
            case "NO_PAY": return "No salary will be paid during the leave period";
            default: return "Payment terms as per company policy";
        }
    }

    // Helper method to format weeks and days
    private String formatWeeksAndDays(long totalDays) {
        long weeks = totalDays / 7;
        long remainingDays = totalDays % 7;

        if (weeks == 0) {
            return totalDays + " day" + (totalDays != 1 ? "s" : "");
        } else if (remainingDays == 0) {
            return weeks + " week" + (weeks != 1 ? "s" : "");
        } else {
            return weeks + " week" + (weeks != 1 ? "s" : "") +
                    " and " + remainingDays + " day" + (remainingDays != 1 ? "s" : "");
        }
    }

    private void addDetailRow(StringBuilder content, String label, String value) {
        content.append("<div class='detail-row'>");
        content.append("<div class='detail-label'>").append(label).append(":</div>");
        content.append("<div class='detail-value'>").append(value != null ? value : "N/A").append("</div>");
        content.append("</div>");
    }

    private String formatLeaveType(String leaveType) {
        switch (leaveType) {
            case "CASUAL": return "Casual Leave";
            case "SICK": return "Medical Leave";
            case "MATERNITY": return "Maternity Leave";
            case "SHORT": case "SHORT_LEAVE": return "Short Leave";
            case "HALF_DAY": return "Half Day Leave";
            default: return leaveType.replace("_", " ");
        }
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")) : "N/A";
    }

    private String calculateDuration(Leave leave) {
        if (leave.isShortLeave()) {
            return "Short Leave";
        } else if (leave.isHalfDay()) {
            return "0.5 day";
        } else if (leave.getStartDate() != null && leave.getEndDate() != null) {
            long days = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
            return days == 1 ? "1 day" : days + " days";
        }
        return "N/A";
    }
}