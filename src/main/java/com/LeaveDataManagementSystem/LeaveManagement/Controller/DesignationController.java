package com.LeaveDataManagementSystem.LeaveManagement.Controller;


import com.LeaveDataManagementSystem.LeaveManagement.Model.Designation;
import com.LeaveDataManagementSystem.LeaveManagement.Repository.DesignationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/designations")
public class DesignationController {

    @Autowired
    private DesignationRepository designationRepo;

    // ✅ Add new designation
    @PostMapping
    public ResponseEntity<?> addDesignation(@RequestBody Designation designation) {
        if (designationRepo.existsByName(designation.getName())) {
            return ResponseEntity.badRequest().body("❌ Designation already exists");
        }
        designationRepo.save(designation);
        return ResponseEntity.ok("✅ Designation added successfully");
    }

    // ✅ Get all designations
    @GetMapping
    public ResponseEntity<List<Designation>> getAllDesignations() {
        return ResponseEntity.ok(designationRepo.findAll());
    }

    // ✅ Delete designation
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDesignation(@PathVariable String id) {
        if (!designationRepo.existsById(id)) {
            return ResponseEntity.status(404).body("❌ Designation not found");
        }
        designationRepo.deleteById(id);
        return ResponseEntity.ok("🗑️ Designation deleted successfully");
    }
}

