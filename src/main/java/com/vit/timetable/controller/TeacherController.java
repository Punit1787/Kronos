package com.vit.timetable.controller;

import com.vit.timetable.model.Teacher;
import com.vit.timetable.repository.TeacherRepository;
import com.vit.timetable.repository.TimetableEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*")
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private TimetableEntryRepository entryRepo;

    @GetMapping
    public List<Teacher> getAll() {
        return teacherRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Teacher> getById(@PathVariable Long id) {
        return teacherRepo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Teacher teacher) {
        // Duplicate check by email (case-insensitive)
        if (teacher.getEmail() != null && !teacher.getEmail().isBlank()) {
            boolean exists = teacherRepo.findAll().stream()
                .anyMatch(t -> t.getEmail() != null && t.getEmail().equalsIgnoreCase(teacher.getEmail()));
            if (exists) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Teacher with email '" + teacher.getEmail() + "' already exists"));
            }
        }
        // Duplicate check by name (case-insensitive)
        boolean nameExists = teacherRepo.findAll().stream()
            .anyMatch(t -> t.getName().equalsIgnoreCase(teacher.getName()));
        if (nameExists) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Teacher '" + teacher.getName() + "' already exists"));
        }
        return ResponseEntity.ok(teacherRepo.save(teacher));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Teacher> update(@PathVariable Long id, @RequestBody Teacher updated) {
        return teacherRepo.findById(id).map(teacher -> {
            teacher.setName(updated.getName());
            teacher.setEmail(updated.getEmail());
            teacher.setMaxHoursPerWeek(updated.getMaxHoursPerWeek());
            teacher.setSubjects(updated.getSubjects());
            return ResponseEntity.ok(teacherRepo.save(teacher));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!teacherRepo.existsById(id)) return ResponseEntity.notFound().build();
        // Clear timetable entries referencing this teacher first
        entryRepo.deleteByTeacherId(id);
        teacherRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
