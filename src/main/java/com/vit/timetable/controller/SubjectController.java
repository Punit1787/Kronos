package com.vit.timetable.controller;

import com.vit.timetable.model.Subject;
import com.vit.timetable.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = "*")
public class SubjectController {

    @Autowired
    private SubjectRepository subjectRepo;

    @GetMapping
    public List<Subject> getAll() {
        return subjectRepo.findAll();
    }

    @PostMapping
    public Subject create(@RequestBody Subject subject) {
        return subjectRepo.save(subject);
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
        subjectRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
