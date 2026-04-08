package com.vit.timetable.repository;

import com.vit.timetable.model.TimetableEntry;
import com.vit.timetable.model.TimeSlot;
import com.vit.timetable.model.Teacher;
import com.vit.timetable.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    // Get all entries for a division
    List<TimetableEntry> findByDivision(String division);

    // Delete all entries for a division (before regenerating)
    void deleteByDivision(String division);

    // Check if a teacher is already booked at a slot
    boolean existsByTeacherAndTimeSlot(Teacher teacher, TimeSlot timeSlot);

    // Check if a room is already booked at a slot
    boolean existsByRoomAndTimeSlot(Room room, TimeSlot timeSlot);

    // Check if a division already has a class at a slot
    boolean existsByDivisionAndTimeSlot(String division, TimeSlot timeSlot);
    
    // Get entries for a division on a specific day
    List<TimetableEntry> findByDivisionAndTimeSlot_DayOrderByTimeSlot_PeriodNumberAsc(String division, String day);
}
