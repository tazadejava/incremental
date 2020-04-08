package me.dracorrein.incremental.logic.dashboard;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RepeatingTask {

    private String taskName;
    private DayOfWeek dayOfWeekTaskBegins, dayOfWeekTaskDue;
    private LocalTime timeTaskDue;
    private String taskClass;
    private float estimatedHoursCompletion;

    private List<String> eventNames;

    private LocalDate lastPendingTaskUpdate;

    public RepeatingTask(String taskName, DayOfWeek dayOfWeekTaskBegins, DayOfWeek dayOfWeekTaskDue, LocalTime timeTaskDue, String taskClass, float estimatedHoursCompletion) {
        this.taskName = taskName;

        this.dayOfWeekTaskBegins = dayOfWeekTaskBegins;
        this.dayOfWeekTaskDue = dayOfWeekTaskDue;
        this.timeTaskDue = timeTaskDue;
        this.taskClass = taskClass;
        this.estimatedHoursCompletion = estimatedHoursCompletion;

        lastPendingTaskUpdate = LocalDate.now().minusDays(1);
    }

    public void setDayOfWeekTaskBegins(DayOfWeek dayOfWeek) {
        dayOfWeekTaskBegins = dayOfWeek;
    }

    public void setEventNames(List<String> eventNames) {
        this.eventNames = eventNames;
    }

    public DayOfWeek getDayOfWeekTaskBegins() {
        return dayOfWeekTaskBegins;
    }

    public DayOfWeek getDayOfWeekTaskDue() {
        return dayOfWeekTaskDue;
    }

    public List<String> getEventNames() {
        return eventNames;
    }

    private int getDaysBetweenDaysOfWeek(DayOfWeek start, DayOfWeek end) {
        int dayOfWeekAdjustmentDays = start.getValue() - end.getValue();
        if(dayOfWeekAdjustmentDays < 0) {
            dayOfWeekAdjustmentDays += 7;
            dayOfWeekAdjustmentDays += 7;
        }
        return dayOfWeekAdjustmentDays;
    }

    private List<LocalDateTime> getMissedTaskDays() {
        List<LocalDateTime> missedTaskDueDates = new ArrayList<>();

        LocalDate now = LocalDate.now();

        if(lastPendingTaskUpdate.equals(now)) {
            return missedTaskDueDates;
        }

        LocalDate currentDateTraversal = lastPendingTaskUpdate;

        //update the lastPending to the current start day of the week, and add to the list
        currentDateTraversal.plusDays(getDaysBetweenDaysOfWeek(dayOfWeekTaskBegins, currentDateTraversal.getDayOfWeek()));

        if(currentDateTraversal.isAfter(now)) {
            return missedTaskDueDates;
        }

        missedTaskDueDates.add(LocalDateTime.of(currentDateTraversal.plusDays(getDaysBetweenDaysOfWeek(currentDateTraversal.getDayOfWeek(), dayOfWeekTaskDue)), timeTaskDue));

        //traverse the weeks, one at a time, until reach after now. stop there.
        //change now to a day afterwards so that we can check once a week increments, inclusive
        now = now.plusDays(1);
        while((currentDateTraversal = currentDateTraversal.plusDays(7)).isBefore(now)) {
            missedTaskDueDates.add(LocalDateTime.of(currentDateTraversal.plusDays(getDaysBetweenDaysOfWeek(currentDateTraversal.getDayOfWeek(), dayOfWeekTaskDue)), timeTaskDue));
        }

        lastPendingTaskUpdate = currentDateTraversal;

        return missedTaskDueDates;
    }

    public boolean hasPendingTasks() {
        return !getMissedTaskDays().isEmpty();
    }

    //release pending tasks, and empty the task list
    public List<Task> popAllPendingTasks() {
        List<LocalDateTime> pendingTaskDates = getMissedTaskDays();
        lastPendingTaskUpdate = LocalDate.now();

        List<Task> pendingTasks = new ArrayList<>();

        for(LocalDateTime dueDate : pendingTaskDates) {
            Task task = new Task(taskName, dueDate, taskClass, estimatedHoursCompletion);

            pendingTasks.add(task);
        }

        return pendingTasks;
    }
}
