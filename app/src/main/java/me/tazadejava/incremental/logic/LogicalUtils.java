package me.tazadejava.incremental.logic;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class LogicalUtils {

    public static LocalDate[] getWorkWeekDates() {
        LocalDate monday = LocalDate.now();

        while(monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.minusDays(1);
        }

        LocalDate[] dates = new LocalDate[7];

        for(int i = 0; i < 7; i++) {
            dates[i] = monday;
            monday = monday.plusDays(1);
        }

        return dates;
    }
}
