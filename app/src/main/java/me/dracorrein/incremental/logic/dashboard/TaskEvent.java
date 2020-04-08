package me.dracorrein.incremental.logic.dashboard;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

public class TaskEvent extends Task {

    private int adjustedCompletionTime;
    private DayOfWeek dayOfWeekEventBegins;
    private List<String> eventNames;

    public TaskEvent(String name, LocalDateTime dueDate) {
        super(name, dueDate);
    }

    public void setDayOfWeekEventBegins(DayOfWeek dayOfWeek) {
        dayOfWeekEventBegins = dayOfWeek;
    }

    public void setEventNames(List<String> eventNames) {
        this.eventNames = eventNames;
    }

    public int getAdjustedCompletionTime() {
        return adjustedCompletionTime;
    }

    public DayOfWeek getDayOfWeekEventBegins() {
        return dayOfWeekEventBegins;
    }

    public List<String> getEventNames() {
        return eventNames;
    }
}
