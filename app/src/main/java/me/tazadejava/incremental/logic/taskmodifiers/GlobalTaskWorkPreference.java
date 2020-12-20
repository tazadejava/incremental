package me.tazadejava.incremental.logic.taskmodifiers;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GlobalTaskWorkPreference {

    private Set<LocalDate> blackedOutDays = new HashSet<>();

    public boolean updateData() {
        boolean updated = false;
        //remove old dates from the set
        if(!blackedOutDays.isEmpty()) {
            LocalDate now = LocalDate.now();

            Iterator<LocalDate> days = blackedOutDays.iterator();
            while(days.hasNext()) {
                LocalDate next = days.next();

                if(now.isAfter(next)) {
                    days.remove();
                    updated = true;
                }
            }
        }

        return updated;
    }

    public int countBlackedOutDaysBetween(LocalDate begin, LocalDate end) {
        if(blackedOutDays.isEmpty()) {
            return 0;
        }
        if(begin.isAfter(end)) {
            return 0;
        }

        int count = 0;

        LocalDate currentDate = begin;
        for(int i = 0; i < 7; i++) {
            if(blackedOutDays.contains(currentDate)) {
                count++;
            }

            currentDate = currentDate.plusDays(1);

            if(currentDate.isAfter(end)) {
                break;
            }
        }

        return count;
    }

    public boolean isBlackedOutDay(LocalDate date) {
        return blackedOutDays.contains(date);
    }

    public void addBlackedOutDay(LocalDate date) {
        blackedOutDays.add(date);
    }

    public void removeBlackedOutDay(LocalDate date) {
        blackedOutDays.remove(date);
    }
}
