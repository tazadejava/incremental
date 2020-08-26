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
}
