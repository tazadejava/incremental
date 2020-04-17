package me.tazadejava.incremental.logic.dashboard;

import me.tazadejava.incremental.ui.main.IncrementalApplication;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class RepeatingTask {

    private DayOfWeek dayOfWeekTaskBegins, dayOfWeekTaskDue;
    private LocalTime timeTaskDue;
    private TimePeriod timePeriod;
    private float estimatedHoursCompletion;
    private Group group;

    private String[] taskNames;
    private int currentTaskIndex, totalTasksCompleted;
    private float totalHoursWorked;

    private LocalDate lastPendingTaskUpdate;

    public RepeatingTask(String[] taskNames, DayOfWeek dayOfWeekTaskBegins, DayOfWeek dayOfWeekTaskDue, LocalTime timeTaskDue, Group group, TimePeriod timePeriod, float estimatedHoursCompletion) {
        this.taskNames = taskNames;
        this.dayOfWeekTaskBegins = dayOfWeekTaskBegins;
        this.dayOfWeekTaskDue = dayOfWeekTaskDue;
        this.timeTaskDue = timeTaskDue;
        this.group = group;
        this.timePeriod = timePeriod;
        this.estimatedHoursCompletion = estimatedHoursCompletion;

        currentTaskIndex = 0;
        totalHoursWorked = 0;

        lastPendingTaskUpdate = LocalDate.now().minusDays(1);
    }

    public void setDayOfWeekTaskBegins(DayOfWeek dayOfWeek) {
        dayOfWeekTaskBegins = dayOfWeek;
    }

    public DayOfWeek getDayOfWeekTaskBegins() {
        return dayOfWeekTaskBegins;
    }

    public DayOfWeek getDayOfWeekTaskDue() {
        return dayOfWeekTaskDue;
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
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

    public void incrementTotalHours(float hours) {
        totalHoursWorked += hours;
        totalTasksCompleted += 1;
    }

    public boolean hasPendingTasks() {
        return !getMissedTaskDays().isEmpty();
    }

    //release pending tasks, and empty the task list
    public List<Task> popAllPendingTasks() {
        if(currentTaskIndex == taskNames.length) {
            return new ArrayList<>();
        }

        List<LocalDateTime> pendingTaskDates = getMissedTaskDays();
        lastPendingTaskUpdate = LocalDate.now();

        List<Task> pendingTasks = new ArrayList<>();

        for(LocalDateTime dueDate : pendingTaskDates) {
            Task task = new Task(taskNames[currentTaskIndex], LocalDate.now(), dueDate, group, timePeriod, estimatedHoursCompletion);

            task.setRepeatingTaskParent(this);

            pendingTasks.add(task);

            currentTaskIndex++;

            if(currentTaskIndex == taskNames.length) {
                IncrementalApplication.taskManager.completeRepeatingTask(this);
                break;
            }
        }

        return pendingTasks;
    }
}
