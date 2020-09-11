package me.tazadejava.incremental.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class LogicalUtils {

    public static LocalDate[] getWorkWeekDates(LocalDate startDate) {
        while(startDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            startDate = startDate.minusDays(1);
        }

        LocalDate[] dates = new LocalDate[7];

        for(int i = 0; i < 7; i++) {
            dates[i] = startDate;
            startDate = startDate.plusDays(1);
        }

        return dates;
    }

    public static LocalDate[] getWorkWeekDates() {
        return getWorkWeekDates(LocalDate.now());
    }
}
