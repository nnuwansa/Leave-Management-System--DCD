package com.LeaveDataManagementSystem.LeaveManagement.Controller;

import com.LeaveDataManagementSystem.LeaveManagement.Model.Department;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepo;

    @PostMapping
    public ResponseEntity<?> addDepartment(@RequestBody Department department) {
        if (departmentRepo.existsByName(department.getName())) {
            return ResponseEntity.badRequest().body("❌ Department Already Exists");
        }
        departmentRepo.save(department);
        return ResponseEntity.ok("✅ Department Added Successfully");
    }

    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentRepo.findAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable String id) {
        if (!departmentRepo.existsById(id)) {
            return ResponseEntity.status(404).body("❌ Department Not Found");
        }
        departmentRepo.deleteById(id);
        return ResponseEntity.ok("🗑️ Department Deleted Successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable String id, @RequestBody Department updatedDepartment) {
        return departmentRepo.findById(id)
                .map(department -> {
                    department.setName(updatedDepartment.getName());
                    departmentRepo.save(department);
                    return ResponseEntity.ok("✏️ Department Updated Successfully");
                })
                .orElseGet(() -> ResponseEntity.status(404).body("❌ Department Not Found"));
    }
}
