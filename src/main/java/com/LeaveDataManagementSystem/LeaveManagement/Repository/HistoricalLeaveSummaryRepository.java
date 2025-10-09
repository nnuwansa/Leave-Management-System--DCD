package com.LeaveDataManagementSystem.LeaveManagement.Repository;

import com.LeaveDataManagementSystem.LeaveManagement.Model.HistoricalLeaveSummary;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface HistoricalLeaveSummaryRepository extends MongoRepository<HistoricalLeaveSummary, String> {

    // Find historical summary for specific employee and year
    Optional<HistoricalLeaveSummary> findByEmployeeEmailAndYear(String employeeEmail, int year);

    // Check if historical summary exists for employee and year
    boolean existsByEmployeeEmailAndYear(String employeeEmail, int year);

    // Find all historical summaries for a specific employee
    List<HistoricalLeaveSummary> findByEmployeeEmailOrderByYearDesc(String employeeEmail);

    // Find all historical summaries for a specific year
    List<HistoricalLeaveSummary> findByYearOrderByEmployeeEmail(int year);

    // Find all years that have historical data
    @Query(value = "{}", fields = "{ 'year' : 1 }")
    List<HistoricalLeaveSummary> findDistinctYears();

    // Delete all historical summaries for a specific employee
    void deleteByEmployeeEmail(String employeeEmail);

    // Find historical summaries added by a specific admin
    List<HistoricalLeaveSummary> findByAddedBy(String addedBy);

    // Find historical summaries for employees in a specific department
    // This would require a join with User collection or storing department in historical summary
    @Query("{ 'year': ?0 }")
    List<HistoricalLeaveSummary> findByYear(int year);

    // Custom query to get years with data count
    @Aggregation(pipeline = {
            "{ $group: { _id: '$year', count: { $sum: 1 } } }",
            "{ $sort: { _id: -1 } }"
    })
    List<Map<String, Object>> getYearSummaries();
}