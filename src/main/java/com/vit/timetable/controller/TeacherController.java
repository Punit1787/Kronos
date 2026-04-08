package com.vit.timetable.controller;

import com.vit.timetable.model.Teacher;
import com.vit.timetable.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*")
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepo;

    /** GET all teachers */
    @GetMapping
    public List<Teacher> getAll() {
        return teacherRepo.findAll();
    }

    /** GET teacher by ID */
    @GetMapping("/{id}")
    public ResponseEntity<Teacher> getById(@PathVariable Long id) {
        return teacherRepo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /** POST create new teacher */
    @PostMapping
    public Teacher create(@RequestBody Teacher teacher) {
        return teacherRepo.save(teacher);
    }

    /** PUT update teacher */
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

    /** DELETE teacher */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!teacherRepo.existsById(id)) return ResponseEntity.notFound().build();
        teacherRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
