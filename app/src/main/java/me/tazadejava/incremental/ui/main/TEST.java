package me.tazadejava.incremental.ui.main;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TEST {

    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("h:mm a");

        System.out.println(now.format(format));
    }
}
