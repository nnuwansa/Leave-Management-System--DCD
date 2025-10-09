package com.LeaveDataManagementSystem.LeaveManagement.Repository;


import com.LeaveDataManagementSystem.LeaveManagement.Model.Designation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DesignationRepository extends MongoRepository<Designation, String> {
    boolean existsByName(String name);
}
