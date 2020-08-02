package me.tazadejava.incremental.logic.dashboard;

import android.graphics.Color;

import com.chroma.Chroma;
import com.chroma.ColorSpace;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import me.tazadejava.incremental.Utils;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

public class Task {

    private String name;
    private LocalDate startDate;
    private LocalDateTime dueDate;
    private Group group;
    private TimePeriod timePeriod;

    private boolean isTaskComplete, isTaskInProgress, doneWithTaskForDay;
    private LocalDateTime lastTaskWorkTime, lastTaskWorkStartTime;
    private float loggedHoursOfWork, estimatedTotalHoursToCompletion;

    private int mainColor, endColor;

    private RepeatingTask parent;

    public Task(String name, LocalDate startDate, LocalDateTime dueDate, Group group, TimePeriod timePeriod, float estimatedTotalHoursToCompletion) {
        this.name = name;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.group = group;
        this.timePeriod = timePeriod;
        this.estimatedTotalHoursToCompletion = estimatedTotalHoursToCompletion;

        isTaskComplete = false;
        loggedHoursOfWork = 0;
        doneWithTaskForDay = false;

        calculateCardColors();
    }

    public void setRepeatingTaskParent(RepeatingTask parent) {
        this.parent = parent;
    }

    private void calculateCardColors() {
        Chroma originalColor = new Chroma("#1d98cb");
        double[] lch = originalColor.getLCH();

        lch[2] = group.getColor();

        mainColor = Color.parseColor(new Chroma(ColorSpace.LCH, lch[0], lch[1], lch[2], 255).hexString());
        endColor = Color.parseColor(new Chroma(ColorSpace.LCH, 100, lch[1], lch[2], 255).hexString());
    }

    public int getCardBeginColor() {
        return mainColor;
    }

    public int getCardEndColor() {
        return endColor;
    }

    public float getTaskCompletionPercentage() {
//        if(originalEstimatedCompletionTime == 0) {
//            return 1;
//        }
//
//        return (float) (originalEstimatedCompletionTime - calculatedEstimatedCompletionTime) / originalEstimatedCompletionTime;
        return loggedHoursOfWork / estimatedTotalHoursToCompletion;
    }

    public String getName() {
        return name;
    }

    public String getGroupName() {
        return group.getGroupName();
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public String getDueDateFormatted() {
        if(dueDate.toLocalDate().equals(LocalDate.now())) {
            return "Due TODAY" +
                    " @ " + Utils.getDueDateTimeFormatted(dueDate.getHour(), dueDate.getMinute());
        }

        return "Due on " + dueDate.getMonthValue() + "/" + dueDate.getDayOfMonth() +
                " @ " + Utils.getDueDateTimeFormatted(dueDate.getHour(), dueDate.getMinute());
    }

    public String getEstimatedCompletionTimeFormatted() {
        float hoursLeft = getHoursLeftOfWork();
        return String.valueOf((hoursLeft == (int) hoursLeft) ? (int) hoursLeft : hoursLeft);
    }

    public float getHoursLeftOfWork() {
        return estimatedTotalHoursToCompletion - loggedHoursOfWork;
    }

    public float getTodaysHoursOfWork() {
        LocalDate now = LocalDate.now();
        LocalDate date = dueDate.toLocalDate();

        if(date.equals(now) || date.equals(now.plusDays(1))) {
            return getHoursLeftOfWork();
        } else {
            return (float) Math.ceil(getHoursLeftOfWork() / ChronoUnit.DAYS.between(now, date) * 2f) / 2f;
        }
    }

    public String getTodaysHoursOfWorkFormatted() {
        float hours = getTodaysHoursOfWork();

        if(hours == (int) hours) {
            return "" + (int) hours;
        } else {
            return "" + hours;
        }
    }

    public float getEstimatedCompletionTime() {
        return estimatedTotalHoursToCompletion;
    }

    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(dueDate);
    }

    public void startTask() {
        lastTaskWorkStartTime = LocalDateTime.now();
        isTaskInProgress = true;
    }

    public float getCurrentTaskWorkHours() {
        float hoursSpent = lastTaskWorkStartTime.until(LocalDateTime.now(), ChronoUnit.MINUTES) / 60f;
        return (float) Math.ceil(hoursSpent * 10f) / 10f;
    }

    public void completeTaskForTheDay() {
        doneWithTaskForDay = true;
    }

    public void incrementTask(float loggedHours) {
        lastTaskWorkTime = LocalDateTime.now();
        loggedHoursOfWork += loggedHours;
    }

    public void completeTask(float loggedHours) {
        isTaskComplete = true;

        loggedHoursOfWork += loggedHours;

        if(parent != null) {
            parent.incrementTotalHours(loggedHoursOfWork);
        }

        IncrementalApplication.taskManager.completeTask(this);
    }

    public boolean shouldTaskBeActive(LocalDate date) {
        if(isTaskComplete) {
            return false;
        }
        if(date.isAfter(dueDate.toLocalDate())) {
            return false;
        }
        if(date.isBefore(startDate)) {
            return false;
        }
        //don't show up if the user doesn't want to work on it anymore today
        if(lastTaskWorkTime != null && lastTaskWorkTime.toLocalDate().equals(LocalDate.now()) && doneWithTaskForDay) {
            return false;
        }

        return true;
    }

    public boolean isTaskInProgress() {
        return isTaskInProgress;
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }
}
