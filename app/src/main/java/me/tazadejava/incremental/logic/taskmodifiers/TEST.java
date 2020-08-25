package me.tazadejava.incremental.logic.taskmodifiers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import me.tazadejava.incremental.logic.customserializers.LocalDateAdapter;
import me.tazadejava.incremental.logic.customserializers.LocalDateTimeAdapter;

public class TEST {

    public static void main(String[] args) {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .create();

        HashMap<LocalDate, HashMap<String, Float>> totalHoursWorkedByGroup = new HashMap<>();

        totalHoursWorkedByGroup.put(LocalDate.now(), new HashMap<>());
        totalHoursWorkedByGroup.get(LocalDate.now()).put("test", 10F);

        JsonElement test = gson.toJsonTree(totalHoursWorkedByGroup);

        System.out.println(test);

        Type mapping = new TypeToken<HashMap<LocalDate, HashMap<String, Float>>>(){}.getType();
        HashMap<LocalDate, HashMap<String, Float>> restored = gson.fromJson(test, mapping);

        System.out.println(restored);
        System.out.println(restored);

//        GlobalTaskWorkPreference test = new GlobalTaskWorkPreference();
//
//        JsonElement element  = gson.toJsonTree(test);
//
//        GlobalTaskWorkPreference restored = gson.fromJson(element.toString(), GlobalTaskWorkPreference.class);
//
//        System.out.println(restored);
//
//        restored.addBlackedOutDayOfWeek(DayOfWeek.MONDAY);
//        restored.addBlackedOutDayOfWeek(DayOfWeek.TUESDAY);
//        restored.addBlackedOutDayOfWeek(DayOfWeek.WEDNESDAY);
//
//        System.out.println(restored.countBlackedOutDaysBetween(LocalDate.of(2020, 8, 27), LocalDate.of(2020, 9, 10)));
//
//        System.out.println(ZonedDateTime.ofInstant(LocalDate.of(2020, 8, 24).atTime(12, 0).toInstant(ZoneOffset.UTC), ZoneId.systemDefault()).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}
