package me.tazadejava.incremental.logic.taskmodifiers;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.HashSet;
import java.util.Set;

public class GlobalTaskWorkPreference {

    private int currentWeekNumber;
    private Set<DayOfWeek> blackedOutDaysOfWeek = new HashSet<>();

    public GlobalTaskWorkPreference() {
        currentWeekNumber = getWeekNumber(Instant.now());
    }

    //monday is a new week
    private int getWeekNumber(Instant instant) {
        return ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }

    public void checkForWeekChanges() {
        if(getWeekNumber(Instant.now()) != currentWeekNumber) {
            currentWeekNumber = getWeekNumber(Instant.now());
            blackedOutDaysOfWeek.clear();
        }
    }

    public int countBlackedOutDaysBetween(LocalDate begin, LocalDate end) {
        if(blackedOutDaysOfWeek.isEmpty()) {
            return 0;
        }
        if(begin.isAfter(end)) {
            return 0;
        }

        int count = 0;

        LocalDate currentDate = begin;
        for(int i = 0; i < 7; i++) {
            if(blackedOutDaysOfWeek.contains(currentDate.getDayOfWeek())) {
                count++;
            }

            currentDate = currentDate.plusDays(1);

            if(currentDate.isAfter(end)) {
                break;
            }
            if(currentWeekNumber != getWeekNumber(currentDate.atTime(12, 0).toInstant(ZoneOffset.UTC))) {
                break;
            }
        }

        return count;
    }

    public boolean isBlackedOutDay(LocalDate date) {
        return blackedOutDaysOfWeek.contains(date.getDayOfWeek()) && getWeekNumber(date.atTime(12, 0).toInstant(ZoneOffset.UTC)) == currentWeekNumber;
    }

    public boolean isBlackedOutDayOfWeek(DayOfWeek day) {
        return blackedOutDaysOfWeek.contains(day);
    }

    public void addBlackedOutDayOfWeek(DayOfWeek day) {
        blackedOutDaysOfWeek.add(day);
    }

    public void removeBlackedOutDayOfWeek(DayOfWeek day) {
        blackedOutDaysOfWeek.remove(day);
    }
}
