package com.vit.timetable.controller;

import com.vit.timetable.model.Subject;
import com.vit.timetable.repository.SubjectRepository;
import com.vit.timetable.repository.TimetableEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = "*")
public class SubjectController {

    @Autowired
    private SubjectRepository subjectRepo;

    @Autowired
    private TimetableEntryRepository entryRepo;

    @GetMapping
    public List<Subject> getAll() {
        return subjectRepo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Subject subject) {
        // Duplicate check by code (case-insensitive)
        if (subject.getCode() != null && !subject.getCode().isBlank()) {
            boolean exists = subjectRepo.findAll().stream()
                .anyMatch(s -> s.getCode().equalsIgnoreCase(subject.getCode()));
            if (exists) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Subject with code '" + subject.getCode() + "' already exists"));
            }
        }
        return ResponseEntity.ok(subjectRepo.save(subject));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Subject> update(@PathVariable Long id, @RequestBody Subject updated) {
        return subjectRepo.findById(id).map(s -> {
            s.setName(updated.getName());
            s.setCode(updated.getCode());
            s.setHoursPerWeek(updated.getHoursPerWeek());
            s.setLab(updated.isLab());
            return ResponseEntity.ok(subjectRepo.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!subjectRepo.existsById(id)) return ResponseEntity.notFound().build();
        // Clear timetable entries referencing this subject first
        entryRepo.deleteBySubjectId(id);
        subjectRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
