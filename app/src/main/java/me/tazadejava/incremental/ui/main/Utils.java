package me.tazadejava.incremental.ui.main;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Utils {

    public static String formatLocalDateWithDayOfWeek(LocalDate date) {
        String dayOfWeek = date.getDayOfWeek().toString();
        return (dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + date.getMonthValue()
                + "/" + date.getDayOfMonth() + "/" + date.getYear();
    }

    public static String formatLocalTime(LocalDateTime time) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("h:mm ");
        return time.format(format) + (time.getHour() >= 12 ? "pm" : "am");
    }

    public static String formatLocalTime(LocalTime time) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("h:mm ");
        return time.format(format) + (time.getHour() >= 12 ? "pm" : "am");
    }

    public static String formatHourMinuteTime(int minutes) {
        if(minutes < 60) {
            return minutes + " min";
        } else {
            int hours = minutes / 60;
            minutes %= 60;

            if(minutes == 0) {
                return hours + " hr";
            } else {
                return hours + " hr " + minutes + " min";
            }
        }
    }

    public static String formatHourMinuteTimeFull(int minutes) {
        if(minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        } else {
            int hours = minutes / 60;
            minutes %= 60;

            if(minutes == 0) {
                return hours + " hour" + (hours == 1 ? "" : "s");
            } else {
                return hours + " hour" + (hours == 1 ? "" : "s") + " " + minutes + " minute" + (minutes == 1 ? "" : "s");
            }
        }
    }

    public static int getDaysBetweenDaysOfWeek(DayOfWeek start, DayOfWeek end) {
        int dayOfWeekAdjustmentDays = end.getValue() - start.getValue();
        if(dayOfWeekAdjustmentDays < 0) {
            dayOfWeekAdjustmentDays += 7;
        }
        return dayOfWeekAdjustmentDays;
    }

    public static void hideKeyboard(View v) {
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }, 50);
    }
}
