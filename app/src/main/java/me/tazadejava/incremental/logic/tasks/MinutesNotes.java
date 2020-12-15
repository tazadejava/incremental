package me.tazadejava.incremental.logic.tasks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MinutesNotes {

    //invariant: the relative ordering of these values MUST be maintained.
    private List<LocalDateTime> dates = new ArrayList<>();
    private List<Integer> minutes = new ArrayList<>();
    private List<String> notes = new ArrayList<>();

    public void addNotes(LocalDateTime startDateTime, int minutes, String notes) {
        dates.add(startDateTime);
        this.minutes.add(minutes);
        this.notes.add(notes);
    }

    public List<LocalDateTime> getMinutesNotesTimestamps() {
        return Collections.unmodifiableList(dates);
    }

    public int getMinutesFromTimestamp(LocalDateTime dateTime) {
        int index = dates.indexOf(dateTime);

        if(index == -1) {
            return 0;
        }

        return minutes.get(index);
    }

    public String getNotesFromTimestamp(LocalDateTime dateTime) {
        int index = dates.indexOf(dateTime);

        if(index == -1) {
            return "";
        }

        return notes.get(index);
    }
}
