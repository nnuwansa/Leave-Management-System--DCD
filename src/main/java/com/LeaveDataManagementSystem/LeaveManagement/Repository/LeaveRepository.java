package com.LeaveDataManagementSystem.LeaveManagement.Repository;

import com.LeaveDataManagementSystem.LeaveManagement.Model.Leave;
import com.LeaveDataManagementSystem.LeaveManagement.Model.LeaveStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface LeaveRepository extends MongoRepository<Leave, String> {

    // Existing methods (keep these)
    List<Leave> findByEmployeeEmailOrderByCreatedAtDesc(String employeeEmail);
    List<Leave> findByActingOfficerEmailAndStatusOrderByCreatedAtAsc(String actingOfficerEmail, LeaveStatus status);
    List<Leave> findByApprovalOfficerEmailAndStatusOrderByCreatedAtAsc(String approvalOfficerEmail, LeaveStatus status);
    List<Leave> findByActingOfficerEmailOrderByCreatedAtDesc(String actingOfficerEmail);
    List<Leave> findByApprovalOfficerEmailOrderByCreatedAtDesc(String approvalOfficerEmail);
    List<Leave> findByStatusOrderByCreatedAtDesc(LeaveStatus status);

    @Query(value = "{ 'actingOfficerEmail': ?0, 'status': 'PENDING_ACTING_OFFICER' }", count = true)
    long countByActingOfficerEmailAndStatus(String actingOfficerEmail, LeaveStatus status);

    @Query(value = "{ 'approvalOfficerEmail': ?0, 'status': 'PENDING_APPROVAL_OFFICER' }", count = true)
    long countByApprovalOfficerEmailAndStatus(String approvalOfficerEmail, LeaveStatus status);

    @Query("{ 'employeeEmail': ?0, $or: [ " +
            "{ $and: [ { 'startDate': { $gte: ?1 } }, { 'startDate': { $lte: ?2 } } ] }, " +
            "{ $and: [ { 'endDate': { $gte: ?1 } }, { 'endDate': { $lte: ?2 } } ] }, " +
            "{ $and: [ { 'startDate': { $lte: ?1 } }, { 'endDate': { $gte: ?2 } } ] } " +
            "] }")
    List<Leave> findOverlappingLeaves(String employeeEmail, LocalDate startDate, LocalDate endDate);

    @Query("{ 'employeeEmail': ?0, 'startDate': { $gte: ?1 }, 'endDate': { $lte: ?2 } }")
    List<Leave> findByEmployeeEmailAndDateRange(String employeeEmail, LocalDate startDate, LocalDate endDate);

    @Query("{ 'employeeEmail': { $regex: ?0 } }")
    List<Leave> findByDepartment(String departmentPattern);

    @Query("{ 'employeeEmail': ?0, 'startDate': { $gte: ?1 }, 'endDate': { $lte: ?2 } }")
    List<Leave> findByEmployeeEmailAndStartDateBetween(String employeeEmail, LocalDate startDate, LocalDate endDate);

    List<Leave> findBySupervisingOfficerEmailAndStatusOrderByCreatedAtAsc(String supervisingOfficerEmail, LeaveStatus status);
    long countBySupervisingOfficerEmailAndStatus(String supervisingOfficerEmail, LeaveStatus status);

    @Query("{ 'employeeEmail': ?0, 'isCancelled': false, 'startDate': { $gt: ?1 }, " +
            "'status': { $nin: ['REJECTED_BY_ACTING_OFFICER', 'REJECTED_BY_SUPERVISING_OFFICER', 'REJECTED_BY_APPROVAL_OFFICER'] } }")
    List<Leave> findCancellableLeaves(String employeeEmail, LocalDate currentDate);

    List<Leave> findByEmployeeEmailAndIsCancelledTrueOrderByCreatedAtDesc(String employeeEmail);

    // NEW METHODS NEEDED FOR ADMIN FUNCTIONALITY

    // Find all leaves ordered by creation date (most recent first)
    List<Leave> findAllByOrderByCreatedAtDesc();

    // Find leaves by start date between two dates (for date range filtering)
    List<Leave> findByStartDateBetweenOrderByCreatedAtDesc(LocalDate startDate, LocalDate endDate);

    // Find leaves by multiple employee emails (for department filtering)
    List<Leave> findByEmployeeEmailInOrderByCreatedAtDesc(List<String> employeeEmails);

    // Find leaves by employee email and year
    @Query("{ 'employeeEmail': ?0, 'startDate': { $gte: ?1, $lte: ?2 } }")
    List<Leave> findByEmployeeEmailAndYear(String employeeEmail, LocalDate yearStart, LocalDate yearEnd);

    // Find leaves by leave type
    List<Leave> findByLeaveTypeOrderByCreatedAtDesc(String leaveType);

    // Find leaves by status
    List<Leave> findByStatusInOrderByCreatedAtDesc(List<LeaveStatus> statuses);

    // Find leaves by date range (for statistics)
    @Query("{ 'startDate': { $gte: ?0, $lte: ?1 } }")
    List<Leave> findByDateRange(LocalDate startDate, LocalDate endDate);

    // Count leaves by status
    long countByStatus(LeaveStatus status);

    // Count leaves by leave type
    long countByLeaveType(String leaveType);

    // Find leaves created between dates
    List<Leave> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDateTime, LocalDateTime endDateTime);

    // Find approved leaves for entitlement calculation
    @Query("{ 'employeeEmail': ?0, 'status': 'APPROVED', 'isCancelled': false, 'startDate': { $gte: ?1, $lte: ?2 } }")
    List<Leave> findApprovedLeavesByEmployeeAndDateRange(String employeeEmail, LocalDate startDate, LocalDate endDate);

    // Find leaves by multiple criteria (for complex filtering)
    @Query("{ $and: [" +
            "{ $or: [ { 'employeeEmail': { $regex: ?0, $options: 'i' } }, { ?0: { $eq: '' } } ] }," +
            "{ $or: [ { 'leaveType': ?1 }, { ?1: { $eq: '' } } ] }," +
            "{ $or: [ { 'status': ?2 }, { ?2: null } ] }" +
            "] }")
    List<Leave> findByMultipleCriteria(String employeeSearch, String leaveType, LeaveStatus status);


//-----------------------

    // Find leaves by status and date range
    List<Leave> findByStatusAndStartDateBetweenOrderByCreatedAtDesc(
            LeaveStatus status, LocalDate startDate, LocalDate endDate);

    // Find leaves by leave type and date range
    List<Leave> findByLeaveTypeAndStartDateBetweenOrderByCreatedAtDesc(
            String leaveType, LocalDate startDate, LocalDate endDate);

    // Find leaves by employee email and status
    List<Leave> findByEmployeeEmailAndStatusOrderByCreatedAtDesc(String employeeEmail, LeaveStatus status);

    // Find leaves by employee email and date range
    List<Leave> findByEmployeeEmailAndStartDateBetweenOrderByCreatedAtDesc(
            String employeeEmail, LocalDate startDate, LocalDate endDate);

    // Count all leaves for current year
    @Query("{ 'startDate': { $gte: ?0, $lte: ?1 } }")
    long countByStartDateBetween(LocalDate startDate, LocalDate endDate);

    // Find pending leaves (all types of pending)
    @Query("{ 'status': { $regex: 'PENDING.*' } }")
    List<Leave> findAllPendingLeavesOrderByCreatedAtDesc();

    // Find approved leaves for a specific year
    @Query("{ 'status': 'APPROVED', 'startDate': { $gte: ?0, $lte: ?1 } }")
    List<Leave> findApprovedLeavesByYear(LocalDate yearStart, LocalDate yearEnd);

    // Find cancelled leaves
    @Query("{ 'cancelled': true }")
    List<Leave> findCancelledLeavesOrderByCreatedAtDesc();

    // Count leaves by employee for a specific year
    @Query(value = "{ 'employeeEmail': ?0, 'startDate': { $gte: ?1, $lte: ?2 } }", count = true)
    long countByEmployeeEmailAndYear(String employeeEmail, LocalDate yearStart, LocalDate yearEnd);

    // Find most recent leaves (for dashboard)
    List<Leave> findTop10ByOrderByCreatedAtDesc();

    // Count leaves by department (requires aggregation)
    @Aggregation(pipeline = {
            "{ $lookup: { from: 'users', localField: 'employeeEmail', foreignField: 'email', as: 'employee' } }",
            "{ $unwind: '$employee' }",
            "{ $group: { _id: '$employee.department', count: { $sum: 1 } } }"
    })
    List<Map<String, Object>> countLeavesByDepartment();

    // Find leaves that need attention (pending for too long)
    @Query("{ 'status': { $regex: 'PENDING.*' }, 'createdAt': { $lt: ?0 } }")
    List<Leave> findLeavesNeedingAttention(LocalDateTime cutoffDate);



    // Custom query to find overlapping leaves for conflict checking
    @Query("{ 'employeeEmail': ?0, 'startDate': { $lte: ?2 }, 'endDate': { $gte: ?1 }, " +
            "'status': { $nin: ['REJECTED_BY_ACTING_OFFICER', 'REJECTED_BY_SUPERVISING_OFFICER', 'REJECTED_BY_APPROVAL_OFFICER'] } }")
    List<Leave> findOverlappingLeavesExcludingRejected(String employeeEmail, LocalDate startDate, LocalDate endDate);


    List<Leave> findBySupervisingOfficerEmailOrderByCreatedAtDesc(String supervisingOfficerEmail);


}
