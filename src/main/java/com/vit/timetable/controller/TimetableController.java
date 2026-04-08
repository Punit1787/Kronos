package com.vit.timetable.controller;

import com.vit.timetable.model.TimetableEntry;
import com.vit.timetable.service.TimetableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/timetable")
@CrossOrigin(origins = "*")
public class TimetableController {

    @Autowired
    private TimetableService timetableService;

    /**
     * POST /api/timetable/generate?division=A
     * Triggers the backtracking algorithm and generates the timetable.
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(@RequestParam String division) {
        Map<String, Object> response = new HashMap<>();
        boolean success = timetableService.generate(division);

        if (success) {
            response.put("success", true);
            response.put("message", "Timetable generated successfully for Division " + division);
        } else {
            response.put("success", false);
            response.put("message", "Could not generate timetable. Check if all teachers, subjects, and rooms are configured.");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/timetable?division=A
     * Returns the generated timetable for a division as a structured grid.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTimetable(@RequestParam String division) {
        List<TimetableEntry> entries = timetableService.getTimetable(division);

        // Structure the response as: { day -> [ {slot, subject, teacher, room} ] }
        Map<String, List<Map<String, Object>>> grid = new LinkedHashMap<>();
        String[] dayOrder = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};

        for (String day : dayOrder) {
            grid.put(day, new ArrayList<>());
        }

        for (TimetableEntry entry : entries) {
            String day = entry.getTimeSlot().getDay();
            Map<String, Object> cell = new HashMap<>();
            cell.put("slotId", entry.getTimeSlot().getId());
            cell.put("startTime", entry.getTimeSlot().getStartTime());
            cell.put("endTime", entry.getTimeSlot().getEndTime());
            cell.put("periodNumber", entry.getTimeSlot().getPeriodNumber());
            cell.put("subjectName", entry.getSubject().getName());
            cell.put("subjectCode", entry.getSubject().getCode());
            cell.put("isLab", entry.getSubject().isLab());
            cell.put("teacherName", entry.getTeacher().getName());
            cell.put("roomNumber", entry.getRoom().getRoomNumber());

            grid.get(day).add(cell);
        }

        // Sort each day's entries by period number
        for (List<Map<String, Object>> dayEntries : grid.values()) {
            dayEntries.sort(Comparator.comparingInt(e -> (int) e.get("periodNumber")));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("division", division);
        response.put("timetable", grid);
        response.put("totalEntries", entries.size());

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/timetable?division=A
     * Clears the timetable for a division.
     */
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clear(@RequestParam String division) {
        timetableService.clear(division);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Timetable cleared for Division " + division);
        return ResponseEntity.ok(resp);
    }
}
