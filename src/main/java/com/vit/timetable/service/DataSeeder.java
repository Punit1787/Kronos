package com.vit.timetable.service;

import com.vit.timetable.model.*;
import com.vit.timetable.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.*;

// CommandLineRunner = runs automatically once when Spring Boot starts
// Used to seed the database with VIT's time slots on first run
@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired private TimeSlotRepository timeSlotRepo;
    @Autowired private RoomRepository roomRepo;

    @Override
    public void run(String... args) throws Exception {
        seedTimeSlots();
        seedRooms();
    }

    private void seedTimeSlots() {
        // Only seed if table is empty (avoid duplicates on restart)
        if (timeSlotRepo.count() > 0) return;

        System.out.println("Seeding time slots...");

        // VIT College Schedule:
        // Period 1: 09:00-10:00
        // Period 2: 10:00-11:00
        // Period 3: 11:00-12:00
        // LUNCH:    12:00-13:00  ← marked as isLunch=true, never scheduled
        // Period 4: 13:00-14:00
        // Period 5: 14:00-15:00
        // Period 6: 15:00-16:00

        String[] days = {"MON", "TUE", "WED", "THU", "FRI", "SAT"};

        // Define the periods for each day
        // Format: {startTime, endTime, periodNumber, isLunch}
        Object[][] periods = {
            {"09:00", "10:00", 1, false},
            {"10:00", "11:00", 2, false},
            {"11:00", "12:00", 3, false},
            {"12:00", "13:00", 4, true},   // LUNCH BREAK
            {"13:00", "14:00", 5, false},
            {"14:00", "15:00", 6, false},
            {"15:00", "16:00", 7, false},
        };

        List<TimeSlot> slots = new ArrayList<>();

        for (String day : days) {
            for (Object[] period : periods) {
                TimeSlot slot = new TimeSlot();
                slot.setDay(day);
                slot.setStartTime((String) period[0]);
                slot.setEndTime((String) period[1]);
                slot.setPeriodNumber((int) period[2]);
                slot.setLunch((boolean) period[3]);
                slots.add(slot);
            }
        }

        // saveAll() = one DB call for all records (efficient)
        timeSlotRepo.saveAll(slots);
        System.out.println("✓ Seeded " + slots.size() + " time slots (including lunch breaks)");
    }

    private void seedRooms() {
        if (roomRepo.count() > 0) return;

        System.out.println("Seeding rooms...");

        // Sample VIT rooms - update with actual room numbers
        List<Room> rooms = Arrays.asList(
            new Room(null, "A-301", 60, false),
            new Room(null, "A-302", 60, false),
            new Room(null, "A-303", 60, false),
            new Room(null, "B-201", 70, false),
            new Room(null, "B-202", 70, false),
            new Room(null, "Lab-1",  30, true),   // CS Lab
            new Room(null, "Lab-2",  30, true),   // Electronics Lab
            new Room(null, "Lab-3",  30, true)    // Circuit Lab
        );

        roomRepo.saveAll(rooms);
        System.out.println("✓ Seeded " + rooms.size() + " rooms");
    }
}
