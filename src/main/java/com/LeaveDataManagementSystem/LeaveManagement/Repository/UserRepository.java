package com.LeaveDataManagementSystem.LeaveManagement.Repository;

import com.LeaveDataManagementSystem.LeaveManagement.Model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;



import java.util.List;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // ✅ Find a user by email
    User findByEmail(String email);

    // ✅ Find all users by department
    List<User> findByDepartment(String department);

}
