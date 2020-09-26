package me.tazadejava.incremental.logic;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;

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

    public static int getWeekNumber(LocalDate date) {
        return ZonedDateTime.ofInstant(date.atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault()).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
}
