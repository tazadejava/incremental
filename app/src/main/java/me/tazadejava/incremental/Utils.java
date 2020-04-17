package me.tazadejava.incremental;

public class Utils {

    public static String getDueDateTimeFormatted(int hour, int minute) {
        String demon = "AM";

        if(hour > 12) {
            hour -= 12;
            demon = "PM";
        }

        return hour + ":" + (minute < 10 ? "0" + minute : minute) + " " + demon;
    }
}
