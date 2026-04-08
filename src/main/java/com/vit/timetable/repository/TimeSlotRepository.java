package com.vit.timetable.repository;

import com.vit.timetable.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    // Fetch all non-lunch slots for scheduling
    List<TimeSlot> findByIsLunchFalseOrderByDayAscPeriodNumberAsc();
    
    // Fetch slots by day
    List<TimeSlot> findByDayOrderByPeriodNumberAsc(String day);
}
