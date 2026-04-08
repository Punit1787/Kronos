package com.vit.timetable.controller;

import com.vit.timetable.model.Room;
import com.vit.timetable.repository.RoomRepository;
import com.vit.timetable.repository.TimetableEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private TimetableEntryRepository entryRepo;

    @GetMapping
    public List<Room> getAll() {
        return roomRepo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Room room) {
        // Duplicate check by room number (case-insensitive)
        boolean exists = roomRepo.findAll().stream()
            .anyMatch(r -> r.getRoomNumber().equalsIgnoreCase(room.getRoomNumber()));
        if (exists) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Room '" + room.getRoomNumber() + "' already exists"));
        }
        return ResponseEntity.ok(roomRepo.save(room));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!roomRepo.existsById(id)) return ResponseEntity.notFound().build();
        // Clear timetable entries referencing this room first
        entryRepo.deleteByRoomId(id);
        roomRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
