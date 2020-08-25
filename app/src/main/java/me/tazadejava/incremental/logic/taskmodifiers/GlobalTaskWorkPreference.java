package me.tazadejava.incremental.logic.taskmodifiers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Set;

public class GlobalTaskWorkPreference {

    //represents the total number of hours that the user can work on any particular day of the week
    private Set<DayOfWeek> maximumWorkHoursForDayOfWeek = new HashMap<>();

    //represents the total number of hours that the user can work on any particular day
    private HashMap<LocalDate, Float> maximumWorkHoursForParticularDay = new HashMap<>();

    //when challenged, the minimum is taken

    public GlobalTaskWorkPreference() {
        maximumWorkHoursForDayOfWeek.put(DayOfWeek.MONDAY, 10f);
        maximumWorkHoursForDayOfWeek.put(DayOfWeek.WEDNESDAY, 12.5f);

        maximumWorkHoursForParticularDay.put(LocalDate.of(2020, 8, 24), 10.0f);
        maximumWorkHoursForParticularDay.put(LocalDate.of(2020, 8, 30), 17.0f);
    }

    public GlobalTaskWorkPreference(Gson gson, JsonObject data) {
        Type mappingDayOfWeek = new TypeToken<HashMap<DayOfWeek, Float>>(){}.getType();
        Type mappingDay = new TypeToken<HashMap<LocalDate, Float>>(){}.getType();

        maximumWorkHoursForDayOfWeek = gson.fromJson(data.get("maxForDayOfWeek").getAsString(), mappingDayOfWeek);
        maximumWorkHoursForParticularDay = gson.fromJson(data.get("maxForDay").getAsString(), mappingDay);
    }

    public JsonObject saveData(Gson gson) {
        JsonObject data = new JsonObject();

        data.addProperty("maxForDayOfWeek", gson.toJson(maximumWorkHoursForDayOfWeek));
        data.addProperty("maxForDay", gson.toJson(maximumWorkHoursForParticularDay));

        return data;
    }

    public int getDaysOffBetween(LocalDate begin, LocalDate end) {

    }

    //inclusive
    public int countDayOfWeekBetween(LocalDate begin, LocalDate end) {

    }

    public boolean doesMaxHoursRestrictionExist(LocalDate date) {
        return maximumWorkHoursForDayOfWeek.containsKey(date.getDayOfWeek()) || maximumWorkHoursForParticularDay.containsKey(date);
    }

    public float getMaxHoursRestriction(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if(maximumWorkHoursForDayOfWeek.containsKey(dayOfWeek)) {
            if(maximumWorkHoursForParticularDay.containsKey(date)) {
                return Math.min(maximumWorkHoursForDayOfWeek.get(dayOfWeek), maximumWorkHoursForParticularDay.get(date));
            } else {
                return maximumWorkHoursForDayOfWeek.get(dayOfWeek);
            }
        } else if(maximumWorkHoursForParticularDay.containsKey(date)) {
            return maximumWorkHoursForParticularDay.get(date);
        } else {
            return -1;
        }
    }
}
