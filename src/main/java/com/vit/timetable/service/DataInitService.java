package com.vit.timetable.service;

import com.vit.timetable.model.*;
import com.vit.timetable.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * DataInitService — Seeds the database with VIT default data on startup.
 *
 * This runs once when the application starts (if DB is empty).
 * It adds:
 *  - Default time slots (8:00 - 17:00) with lunch at 13:00–14:00
 *  - Sample subjects (you can replace with real VIT subjects)
 *  - Sample teachers
 *  - Sample rooms
 *
 * Students can edit this data via the Admin UI.
 */
@Service
public class DataInitService implements CommandLineRunner {

    @Autowired
    private TimeSlotRepository timeSlotRepo;

    @Autowired
    private SubjectRepository subjectRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Override
    public void run(String... args) {
        // Only seed if DB is empty
        if (timeSlotRepo.count() == 0) {
            seedTimeSlots();
        }
        if (subjectRepo.count() == 0) {
            seedSubjects();
        }
        if (roomRepo.count() == 0) {
            seedRooms();
        }
        if (teacherRepo.count() == 0) {
            seedTeachers();
        }

        System.out.println("=== VIT Timetable Generator Ready ===");
    }

    /**
     * Seed time slots for Mon–Sat
     * College hours: 8:00 AM to 5:00 PM
     * Lunch: 1:00 PM – 2:00 PM (isLunch = true, never scheduled)
     *
     * Periods per day:
     * Period 1: 08:00 – 09:00
     * Period 2: 09:00 – 10:00
     * Period 3: 10:00 – 11:00
     * Period 4: 11:00 – 12:00
     * Period 5: 12:00 – 13:00
     * LUNCH:    13:00 – 14:00  ← isLunch = true
     * Period 6: 14:00 – 15:00
     * Period 7: 15:00 – 16:00
     * Period 8: 16:00 – 17:00
     */
    private void seedTimeSlots() {
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};

        String[][] periods = {
            {"08:00", "09:00", "1", "false"},
            {"09:00", "10:00", "2", "false"},
            {"10:00", "11:00", "3", "false"},
            {"11:00", "12:00", "4", "false"},
            {"12:00", "13:00", "5", "false"},
            {"13:00", "14:00", "6", "true"},   // LUNCH — isLunch=true
            {"14:00", "15:00", "7", "false"},
            {"15:00", "16:00", "8", "false"},
            {"16:00", "17:00", "9", "false"},
        };

        for (String day : days) {
            for (String[] p : periods) {
                TimeSlot slot = TimeSlot.builder()
                    .day(day)
                    .startTime(p[0])
                    .endTime(p[1])
                    .periodNumber(Integer.parseInt(p[2]))
                    .isLunch(Boolean.parseBoolean(p[3]))
                    .build();
                timeSlotRepo.save(slot);
            }
        }
        System.out.println("[Init] Time slots seeded.");
    }

    /** Seed generic subjects */
    private void seedSubjects() {
        List<Subject> subjects = Arrays.asList(
            Subject.builder().name("Discrete Mathematics").code("DM").hoursPerWeek(4).isLab(false).build(),
            Subject.builder().name("Operating Systems").code("OS").hoursPerWeek(4).isLab(false).build(),
            Subject.builder().name("Database Management Systems").code("DBMS").hoursPerWeek(3).isLab(false).build(),
            Subject.builder().name("Computer Networks").code("CN").hoursPerWeek(3).isLab(false).build(),
            Subject.builder().name("Software Engineering").code("SE").hoursPerWeek(3).isLab(false).build(),
            Subject.builder().name("OS Lab").code("OSLAB").hoursPerWeek(2).isLab(true).build(),
            Subject.builder().name("DBMS Lab").code("DBMSLAB").hoursPerWeek(2).isLab(true).build()
        );
        subjectRepo.saveAll(subjects);
        System.out.println("[Init] Subjects seeded.");
    }

    /** Seed generic rooms */
    private void seedRooms() {
        List<Room> rooms = Arrays.asList(
            Room.builder().roomNumber("201").capacity(60).isLab(false).build(),
            Room.builder().roomNumber("202").capacity(60).isLab(false).build(),
            Room.builder().roomNumber("203").capacity(60).isLab(false).build(),
            Room.builder().roomNumber("204").capacity(60).isLab(false).build(),
            Room.builder().roomNumber("Lab-1").capacity(30).isLab(true).build(),
            Room.builder().roomNumber("Lab-2").capacity(30).isLab(true).build()
        );
        roomRepo.saveAll(rooms);
        System.out.println("[Init] Rooms seeded.");
    }

    /** Seed generic teachers with random-style names */
    private void seedTeachers() {
        List<Subject> all = subjectRepo.findAll();

        Subject dm     = findByCode(all, "DM");
        Subject os     = findByCode(all, "OS");
        Subject dbms   = findByCode(all, "DBMS");
        Subject cn     = findByCode(all, "CN");
        Subject se     = findByCode(all, "SE");
        Subject osLab  = findByCode(all, "OSLAB");
        Subject dbmsLab= findByCode(all, "DBMSLAB");

        teacherRepo.saveAll(Arrays.asList(
            Teacher.builder().name("Prof. K. Joshi").email("kjoshi@college.edu").maxHoursPerWeek(18)
                .subjects(Arrays.asList(os, osLab)).build(),
            Teacher.builder().name("Prof. N. Desai").email("ndesai@college.edu").maxHoursPerWeek(18)
                .subjects(Arrays.asList(dbms, dbmsLab)).build(),
            Teacher.builder().name("Prof. P. Verma").email("pverma@college.edu").maxHoursPerWeek(15)
                .subjects(Arrays.asList(dm)).build(),
            Teacher.builder().name("Prof. R. Kulkarni").email("rkulkarni@college.edu").maxHoursPerWeek(15)
                .subjects(Arrays.asList(cn)).build(),
            Teacher.builder().name("Prof. S. Iyer").email("siyer@college.edu").maxHoursPerWeek(15)
                .subjects(Arrays.asList(se)).build()
        ));
        System.out.println("[Init] Teachers seeded.");
    }

    private Subject findByCode(List<Subject> subjects, String code) {
        return subjects.stream()
            .filter(s -> s.getCode().equals(code))
            .findFirst()
            .orElse(null);
    }
}
