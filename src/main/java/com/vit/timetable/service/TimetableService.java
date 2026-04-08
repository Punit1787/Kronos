package com.vit.timetable.service;

import com.vit.timetable.model.*;
import com.vit.timetable.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * TimetableService — Core Algorithm
 *
 * Uses a Backtracking + Constraint Satisfaction approach to generate
 * a conflict-free timetable. Key constraints enforced:
 *
 *  1. No teacher double-booked at the same slot (INCLUDING across divisions)
 *  2. No room double-booked at the same slot (INCLUDING across divisions)
 *  3. No division has two classes at same slot
 *  4. Lunch break is never scheduled (isLunch = true slots are skipped)
 *  5. NO GAPS: all classes for a division on a day must be in consecutive slots
 *  6. Teacher weekly hour limit is respected
 *  7. NO consecutive lectures of the same subject on the same day
 *  8. Max 1 lecture of the same subject per day (spreads across week)
 *  9. Lab sessions occupy 2 consecutive time slots (2-hour block)
 */
@Service
public class TimetableService {

    @Autowired
    private SubjectRepository subjectRepo;

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private TimeSlotRepository timeSlotRepo;

    @Autowired
    private TimetableEntryRepository entryRepo;

    /**
     * Main entry point — generates timetable for a given division.
     * Deletes any existing entries for that division first.
     */
    @Transactional
    public boolean generate(String division) {
        // Step 1: Clear old timetable for this division
        entryRepo.deleteByDivision(division);

        // Step 2: Load all data
        List<Subject> subjects = subjectRepo.findAll();
        List<Teacher> teachers = teacherRepo.findAll();
        List<Room> rooms = roomRepo.findAll();

        // Step 3: Only use non-lunch slots, ordered by day and period
        List<TimeSlot> slots = timeSlotRepo.findByIsLunchFalseOrderByDayAscPeriodNumberAsc();

        if (slots.isEmpty() || subjects.isEmpty() || teachers.isEmpty() || rooms.isEmpty()) {
            return false;
        }

        // Step 4: Build a lookup for finding the next consecutive slot on same day
        // Key: "DAY-periodNumber" -> TimeSlot
        Map<String, TimeSlot> slotLookup = new HashMap<>();
        for (TimeSlot s : slots) {
            slotLookup.put(s.getDay() + "-" + s.getPeriodNumber(), s);
        }

        // Step 5: Build a list of "tasks" — each lecture/lab session that needs scheduling
        List<SchedulingTask> tasks = buildTaskList(subjects, teachers);
        if (tasks.isEmpty()) return false;

        // Step 6: Load ALL existing timetable entries from OTHER divisions
        List<TimetableEntry> existingEntries = entryRepo.findAll();

        // Step 7: Track teacher hours scheduled so far (including other divisions)
        Map<Long, Integer> teacherHoursUsed = new HashMap<>();
        for (Teacher t : teachers) {
            teacherHoursUsed.put(t.getId(), 0);
        }
        for (TimetableEntry existing : existingEntries) {
            Long teacherId = existing.getTeacher().getId();
            teacherHoursUsed.merge(teacherId, 1, Integer::sum);
        }

        // Step 8: Run backtracking algorithm
        List<TimetableEntry> result = new ArrayList<>();
        boolean success = backtrack(tasks, 0, slots, rooms, division,
                teacherHoursUsed, result, existingEntries, slotLookup);

        if (success) {
            entryRepo.saveAll(result);
            return true;
        }

        return false;
    }

    /**
     * Recursive backtracking function.
     *
     * For regular lectures: assigns 1 slot.
     * For lab sessions: assigns 2 consecutive slots on the same day.
     */
    private boolean backtrack(
            List<SchedulingTask> tasks,
            int taskIndex,
            List<TimeSlot> slots,
            List<Room> rooms,
            String division,
            Map<Long, Integer> teacherHoursUsed,
            List<TimetableEntry> result,
            List<TimetableEntry> existingEntries,
            Map<String, TimeSlot> slotLookup
    ) {
        if (taskIndex == tasks.size()) {
            return true;
        }

        SchedulingTask task = tasks.get(taskIndex);
        Teacher teacher = task.teacher;
        Subject subject = task.subject;
        boolean isLab = subject.isLab();

        for (TimeSlot slot : slots) {

            if (slot.isLunch()) continue;

            // For labs, we need the NEXT consecutive slot too
            TimeSlot nextSlot = null;
            if (isLab) {
                String nextKey = slot.getDay() + "-" + (slot.getPeriodNumber() + 1);
                nextSlot = slotLookup.get(nextKey);
                // If no next slot exists, or it's a lunch slot, skip
                if (nextSlot == null || nextSlot.isLunch()) continue;
            }

            // === Check constraints for slot 1 ===
            if (isTeacherBusy(teacher, slot, result)) continue;
            if (isTeacherBusyGlobal(teacher, slot, existingEntries)) continue;
            if (isDivisionBusy(division, slot, result)) continue;
            if (!isConsecutiveOrFirstOnDay(division, slot, result)) continue;

            // For lectures: no same subject on same day
            // For labs: we allow the 2-slot block (they're the same subject by design)
            if (!isLab && isSameSubjectOnDay(subject, slot, result, division)) continue;

            // For labs: also check constraints on the 2nd slot
            if (isLab) {
                if (isTeacherBusy(teacher, nextSlot, result)) continue;
                if (isTeacherBusyGlobal(teacher, nextSlot, existingEntries)) continue;
                if (isDivisionBusy(division, nextSlot, result)) continue;
                // The next slot must also be consecutive
                if (!isConsecutiveOrFirstOnDay(division, nextSlot,
                        addTempEntry(result, division, subject, teacher, null, slot))) continue;
            }

            // === Teacher weekly hour limit ===
            int hoursNeeded = isLab ? 2 : 1;
            int hoursUsed = teacherHoursUsed.getOrDefault(teacher.getId(), 0);
            if (hoursUsed + hoursNeeded > teacher.getMaxHoursPerWeek()) continue;

            // Try each room
            for (Room room : rooms) {

                // Room type must match subject type
                if (subject.isLab() != room.isLab()) continue;

                // Room not already booked at slot 1
                if (isRoomBusy(room, slot, result)) continue;
                if (isRoomBusyGlobal(room, slot, existingEntries)) continue;

                // For labs: room not booked at slot 2 either
                if (isLab) {
                    if (isRoomBusy(room, nextSlot, result)) continue;
                    if (isRoomBusyGlobal(room, nextSlot, existingEntries)) continue;
                }

                // === All constraints passed — assign ===
                TimetableEntry entry1 = TimetableEntry.builder()
                        .division(division)
                        .subject(subject)
                        .teacher(teacher)
                        .room(room)
                        .timeSlot(slot)
                        .build();
                result.add(entry1);

                TimetableEntry entry2 = null;
                if (isLab) {
                    entry2 = TimetableEntry.builder()
                            .division(division)
                            .subject(subject)
                            .teacher(teacher)
                            .room(room)
                            .timeSlot(nextSlot)
                            .build();
                    result.add(entry2);
                }

                teacherHoursUsed.put(teacher.getId(), hoursUsed + hoursNeeded);

                // Recurse to next task
                if (backtrack(tasks, taskIndex + 1, slots, rooms, division,
                        teacherHoursUsed, result, existingEntries, slotLookup)) {
                    return true;
                }

                // Backtrack: remove assignment(s)
                if (isLab) {
                    result.remove(result.size() - 1); // remove entry2
                }
                result.remove(result.size() - 1); // remove entry1
                teacherHoursUsed.put(teacher.getId(), hoursUsed);
            }
        }

        return false;
    }

    /**
     * Helper: create a temporary list with one extra entry to test consecutiveness.
     * Used when checking if the 2nd lab slot would be consecutive.
     */
    private List<TimetableEntry> addTempEntry(List<TimetableEntry> result,
            String division, Subject subject, Teacher teacher, Room room, TimeSlot slot) {
        List<TimetableEntry> temp = new ArrayList<>(result);
        TimetableEntry e = new TimetableEntry();
        e.setDivision(division);
        e.setSubject(subject);
        e.setTeacher(teacher);
        e.setRoom(room);
        e.setTimeSlot(slot);
        temp.add(e);
        return temp;
    }

    /**
     * NO-GAP CONSTRAINT:
     * On any given day, the division's lectures must be consecutive.
     */
    private boolean isConsecutiveOrFirstOnDay(String division, TimeSlot slot, List<TimetableEntry> result) {
        String day = slot.getDay();

        List<Integer> existingPeriods = new ArrayList<>();
        for (TimetableEntry e : result) {
            if (e.getDivision().equals(division) && e.getTimeSlot().getDay().equals(day)) {
                existingPeriods.add(e.getTimeSlot().getPeriodNumber());
            }
        }

        if (existingPeriods.isEmpty()) {
            return slot.getPeriodNumber() == 1;
        }

        int maxPeriod = Collections.max(existingPeriods);
        return slot.getPeriodNumber() == maxPeriod + 1;
    }

    /**
     * No same subject on the same day (for regular lectures).
     * Labs are exempt — they need 2 slots on the same day by design.
     */
    private boolean isSameSubjectOnDay(Subject subject, TimeSlot slot,
                                        List<TimetableEntry> result, String division) {
        String day = slot.getDay();
        for (TimetableEntry e : result) {
            if (e.getDivision().equals(division)
                    && e.getTimeSlot().getDay().equals(day)
                    && e.getSubject().getId().equals(subject.getId())) {
                return true;
            }
        }
        return false;
    }

    /** Check if teacher is already in result list at this slot */
    private boolean isTeacherBusy(Teacher teacher, TimeSlot slot, List<TimetableEntry> result) {
        for (TimetableEntry e : result) {
            if (e.getTeacher().getId().equals(teacher.getId())
                    && e.getTimeSlot().getId().equals(slot.getId())) {
                return true;
            }
        }
        return false;
    }

    /** Check if teacher is already booked at this slot in OTHER divisions */
    private boolean isTeacherBusyGlobal(Teacher teacher, TimeSlot slot,
                                         List<TimetableEntry> existingEntries) {
        for (TimetableEntry e : existingEntries) {
            if (e.getTeacher().getId().equals(teacher.getId())
                    && e.getTimeSlot().getId().equals(slot.getId())) {
                return true;
            }
        }
        return false;
    }

    /** Check if division already has a class at this slot */
    private boolean isDivisionBusy(String division, TimeSlot slot, List<TimetableEntry> result) {
        for (TimetableEntry e : result) {
            if (e.getDivision().equals(division)
                    && e.getTimeSlot().getId().equals(slot.getId())) {
                return true;
            }
        }
        return false;
    }

    /** Check if room is already booked at this slot (current division) */
    private boolean isRoomBusy(Room room, TimeSlot slot, List<TimetableEntry> result) {
        for (TimetableEntry e : result) {
            if (e.getRoom().getId().equals(room.getId())
                    && e.getTimeSlot().getId().equals(slot.getId())) {
                return true;
            }
        }
        return false;
    }

    /** Check if room is already booked at this slot in OTHER divisions */
    private boolean isRoomBusyGlobal(Room room, TimeSlot slot,
                                      List<TimetableEntry> existingEntries) {
        for (TimetableEntry e : existingEntries) {
            if (e.getRoom().getId().equals(room.getId())
                    && e.getTimeSlot().getId().equals(slot.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Build scheduling tasks from subjects.
     *
     * For regular lectures: 1 task per hour (e.g. hoursPerWeek=3 → 3 tasks)
     * For lab sessions: 1 task per 2-hour block (e.g. hoursPerWeek=2 → 1 task that books 2 slots)
     */
    private List<SchedulingTask> buildTaskList(List<Subject> subjects, List<Teacher> teachers) {
        List<SchedulingTask> tasks = new ArrayList<>();

        for (Subject subject : subjects) {
            Teacher assignedTeacher = findTeacherForSubject(subject, teachers);
            if (assignedTeacher == null) continue;

            if (subject.isLab()) {
                // Labs: each session is a 2-hour block
                // hoursPerWeek=2 means 1 lab session, hoursPerWeek=4 means 2 sessions, etc.
                int labSessions = Math.max(1, subject.getHoursPerWeek() / 2);
                for (int i = 0; i < labSessions; i++) {
                    tasks.add(new SchedulingTask(subject, assignedTeacher));
                }
            } else {
                // Regular lectures: 1 task per hour
                for (int i = 0; i < subject.getHoursPerWeek(); i++) {
                    tasks.add(new SchedulingTask(subject, assignedTeacher));
                }
            }
        }

        // Interleave tasks so different subjects alternate
        tasks = interleaveTasksBySubject(tasks);

        return tasks;
    }

    /**
     * Interleave tasks so that lectures of different subjects alternate.
     * Helps spread subjects across the week naturally.
     */
    private List<SchedulingTask> interleaveTasksBySubject(List<SchedulingTask> tasks) {
        Map<Long, Queue<SchedulingTask>> bySubject = new LinkedHashMap<>();
        for (SchedulingTask task : tasks) {
            bySubject.computeIfAbsent(task.subject.getId(), k -> new LinkedList<>()).add(task);
        }

        List<SchedulingTask> interleaved = new ArrayList<>();
        boolean added = true;
        while (added) {
            added = false;
            for (Queue<SchedulingTask> queue : bySubject.values()) {
                if (!queue.isEmpty()) {
                    interleaved.add(queue.poll());
                    added = true;
                }
            }
        }

        return interleaved;
    }

    /** Find the first teacher who is assigned to teach this subject */
    private Teacher findTeacherForSubject(Subject subject, List<Teacher> teachers) {
        for (Teacher teacher : teachers) {
            if (teacher.getSubjects() != null) {
                for (Subject s : teacher.getSubjects()) {
                    if (s.getId().equals(subject.getId())) {
                        return teacher;
                    }
                }
            }
        }
        return null;
    }

    /** Delete all timetable entries for a division */
    @Transactional
    public void clear(String division) {
        entryRepo.deleteByDivision(division);
    }

    /** Get generated timetable for a division */
    public List<TimetableEntry> getTimetable(String division) {
        return entryRepo.findByDivision(division);
    }

    /** Internal helper class to represent a scheduling task */
    private static class SchedulingTask {
        Subject subject;
        Teacher teacher;

        SchedulingTask(Subject subject, Teacher teacher) {
            this.subject = subject;
            this.teacher = teacher;
        }
    }
}
