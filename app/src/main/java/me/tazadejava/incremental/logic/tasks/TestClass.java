package me.tazadejava.incremental.logic.tasks;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

public class TestClass {

    public static void main(String[] args) {
        LocalDate now = LocalDate.now();
        WeekFields week = WeekFields.of(Locale.getDefault());

        System.out.println(now.get(week.weekOfWeekBasedYear()));
    }
}
