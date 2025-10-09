package com.LeaveDataManagementSystem.LeaveManagement.Repository;

import com.LeaveDataManagementSystem.LeaveManagement.Model.LeaveEntitlement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveEntitlementRepository extends MongoRepository<LeaveEntitlement, String> {

    // Find entitlements for a specific employee and year
    List<LeaveEntitlement> findByEmployeeEmailAndYear(String employeeEmail, int year);

    // Find entitlement for specific employee, leave type, and year
    Optional<LeaveEntitlement> findByEmployeeEmailAndLeaveTypeAndYear(String employeeEmail, String leaveType, int year);

    // Find all entitlements for a specific employee
    List<LeaveEntitlement> findByEmployeeEmailOrderByLeaveType(String employeeEmail);

    // Find all entitlements for a specific year
    List<LeaveEntitlement> findByYear(int year);

    // Check if entitlement exists for employee, leave type, and year
    boolean existsByEmployeeEmailAndLeaveTypeAndYear(String employeeEmail, String leaveType, int year);

    // Delete all entitlements for a specific employee (useful when employee is deleted)
    void deleteByEmployeeEmail(String employeeEmail);

    // Find employees with specific leave type and year
    @Query("{ 'leaveType': ?0, 'year': ?1 }")
    List<LeaveEntitlement> findByLeaveTypeAndYear(String leaveType, int year);

    // Find employees with remaining days greater than specified amount
    @Query("{ 'employeeEmail': ?0, 'year': ?1, 'remainingDays': { $gt: ?2 } }")
    List<LeaveEntitlement> findByEmployeeEmailAndYearAndRemainingDaysGreaterThan(String employeeEmail, int year, int days);
}