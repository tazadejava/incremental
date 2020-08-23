package me.tazadejava.incremental.logic.tasks;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TEST {

    public static void main(String[] args) {
        int daysDifference = (int) ChronoUnit.DAYS.between(LocalDate.of(2020, 8, 16), LocalDate.now());
        System.out.println((daysDifference / 7));

        for(int i = 0; i < 3; i++) {
            LocalDate dueDate = LocalDate.of(2020, 8, 14).plusDays(getDaysBetweenDaysOfWeek(DayOfWeek.FRIDAY, DayOfWeek.TUESDAY)).plusDays(7 * i);
            System.out.println(dueDate);
        }
    }

    private static int getDaysBetweenDaysOfWeek(DayOfWeek start, DayOfWeek end) {
        int dayOfWeekAdjustmentDays = end.getValue() - start.getValue();
        if(dayOfWeekAdjustmentDays < 0) {
            dayOfWeekAdjustmentDays += 7;
        }
        return dayOfWeekAdjustmentDays;
    }
}
