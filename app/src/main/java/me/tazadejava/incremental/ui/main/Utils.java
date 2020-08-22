package me.tazadejava.incremental.ui.main;

import java.time.LocalTime;

public class Utils {

    public static String formatLocalTime(LocalTime time) {
        return formatLocalTime(time.getHour(), time.getMinute());
    }

    public static String formatLocalTime(int hour, int minute) {
        String demon = "AM";

        if(hour > 12) {
            hour -= 12;
            demon = "PM";
        }

        return hour + ":" + (minute < 10 ? "0" + minute : minute) + " " + demon;
    }
}
