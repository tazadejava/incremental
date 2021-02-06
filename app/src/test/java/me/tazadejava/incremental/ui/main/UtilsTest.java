package me.tazadejava.incremental.ui.main;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import me.tazadejava.incremental.logic.LogicalUtils;

import static org.junit.Assert.*;

public class UtilsTest {

//    @Test
//    public void daysUntilEndOfWeekTest() {
//        for(LocalDate startDate : LogicalUtils.getWorkWeekDates()) {
//            LocalDate date = startDate;
//
//            date = date.plusDays(Utils.getDaysBetweenDaysOfWeek(date.getDayOfWeek(), DayOfWeek.SUNDAY));
//            date = date.plusDays(7 * (2 - 1));
//
//            assertEquals(date, LocalDate.of(2021, 2, 14));
//        }
//    }
}