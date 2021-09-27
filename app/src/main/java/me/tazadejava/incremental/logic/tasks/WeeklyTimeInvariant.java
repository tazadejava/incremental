package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import me.tazadejava.incremental.logic.taskmodifiers.Group;

public class WeeklyTimeInvariant {

    private int[] addedMinutesPerDay = new int[7]; //order: monday - sunday
    private Set<LocalDate> exceptionDays = new HashSet<>();

    public WeeklyTimeInvariant() {
    }

    public WeeklyTimeInvariant(JsonObject data) {
        JsonArray days = data.getAsJsonArray("addedMinutesPerDay");

        for(int i = 0; i < 7; i++) {
            addedMinutesPerDay[i] = days.get(i).getAsInt();
        }

        JsonArray exceptionDaysArray = data.getAsJsonArray("exceptionDays");

        for(JsonElement val : exceptionDaysArray) {
            exceptionDays.add(LocalDate.parse(val.getAsString()));
        }
    }

    public JsonObject save() {
        JsonObject data = new JsonObject();

        JsonArray days = new JsonArray();
        for(int val : addedMinutesPerDay) {
            days.add(val);
        }
        data.add("addedMinutesPerDay", days);

        JsonArray exceptionDaysArray = new JsonArray();
        for(LocalDate date : exceptionDays) {
            days.add(date.toString());
        }
        data.add("exceptionDays", exceptionDaysArray);

        return data;
    }

    public void setAddedMinutes(DayOfWeek dayOfWeek, int minutes) {
        addedMinutesPerDay[dayOfWeek.getValue() - 1] = minutes;
    }

    public void addExceptionDay(LocalDate date) {
        exceptionDays.add(date);
    }

    public int getMinutes(TimePeriod timePeriod, LocalDate date) {
        if(exceptionDays.contains(date)) {
            return 0;
        }
        if(timePeriod.getBeginDate() != null && date.isBefore(timePeriod.getBeginDate())) {
            return 0;
        }
        if(timePeriod.getEndDate() != null && date.isAfter(timePeriod.getEndDate())) {
            return 0;
        }
        if(LocalDate.now().isBefore(date)) {
            return 0;
        }

        return addedMinutesPerDay[date.getDayOfWeek().getValue() - 1];
    }

    public int getMinutes(DayOfWeek dayOfWeek) {
        return addedMinutesPerDay[dayOfWeek.getValue() - 1];
    }
}
