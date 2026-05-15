package com.dms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.entity.InstructorSchedule;
import com.dms.mapper.InstructorScheduleMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class InstructorScheduleService {

    private final InstructorScheduleMapper scheduleMapper;

    public InstructorScheduleService(InstructorScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    public List<InstructorSchedule> getSchedule(Long instructorId, LocalDate startDate, LocalDate endDate) {
        return scheduleMapper.selectList(new QueryWrapper<InstructorSchedule>()
                .eq("instructor_id", instructorId)
                .between("work_date", startDate, endDate));
    }

    public void setBusy(Long instructorId, LocalDate date, String timeSlot, boolean isBusy) {
        InstructorSchedule schedule = scheduleMapper.selectOne(new QueryWrapper<InstructorSchedule>()
                .eq("instructor_id", instructorId)
                .eq("work_date", date)
                .eq("time_slot", timeSlot));

        if (schedule == null) {
            schedule = new InstructorSchedule();
            schedule.setInstructorId(instructorId);
            schedule.setWorkDate(date);
            schedule.setTimeSlot(timeSlot);
            schedule.setIsBusy(isBusy ? 1 : 0);
            scheduleMapper.insert(schedule);
        } else {
            schedule.setIsBusy(isBusy ? 1 : 0);
            scheduleMapper.updateById(schedule);
        }
    }
}
