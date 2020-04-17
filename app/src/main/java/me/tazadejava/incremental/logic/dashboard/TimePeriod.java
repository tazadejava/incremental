package me.tazadejava.incremental.logic.dashboard;

import java.time.LocalDate;

public class TimePeriod {

    private String timePeriodName;
    private LocalDate beginDate, endDate;

    public TimePeriod(String name, LocalDate beginDate, LocalDate endDate) {
        this.timePeriodName = name;
        this.beginDate = beginDate;
        this.endDate = endDate;
    }

    public String getName() {
        return timePeriodName;
    }

    public boolean isInTimePeriod(LocalDate date) {
        if(beginDate == null || endDate == null) {
            return false;
        }

        return !date.isBefore(beginDate) && !date.isAfter(endDate);
    }

    public boolean isInTimePeriod(LocalDate start, LocalDate end) {
        if(beginDate == null || endDate == null) {
            return false;
        }

        return !beginDate.isBefore(start) && !endDate.isAfter(end);
    }

    public void extendEndDate(int days) {
        endDate = endDate.plusDays(days);
    }
}
