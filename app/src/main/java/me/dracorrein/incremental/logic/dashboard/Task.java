package me.dracorrein.incremental.logic.dashboard;

import android.graphics.Color;

import com.chroma.Chroma;
import com.chroma.ColorSpace;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import me.dracorrein.incremental.Utils;
import me.dracorrein.incremental.ui.main.IncrementalApplication;

public class Task {

    private String name, className, timePeriod;
    private LocalDateTime dueDate;
    private float originalEstimatedCompletionTime, calculatedEstimatedCompletionTime;

    private boolean isTaskComplete, isTaskInProgress, doneWithTaskForDay;
    private LocalDateTime lastTaskWorkTime, lastTaskWorkStartTime;
    private float loggedHoursToComplete, beginningLoggedHoursToComplete;

    private int mainColor, endColor;

    public Task(String name, LocalDateTime dueDate, String taskClass, String timePeriod, float calculatedEstimatedCompletionTime) {
        this.name = name;
        this.dueDate = dueDate;

        isTaskComplete = false;
        loggedHoursToComplete = 0;

        //TODO: ADD A CHECK: IF A NEW DAY, need to update beginningLoggedHoursToComplete
        beginningLoggedHoursToComplete = loggedHoursToComplete;
        doneWithTaskForDay = false;

        setEstimatedCompletionTime(calculatedEstimatedCompletionTime);
        setClassName(taskClass);

        this.timePeriod = timePeriod;

        calculateCardColors();
    }

    public void setEstimatedCompletionTime(float hours) {
        originalEstimatedCompletionTime = hours;
        calculatedEstimatedCompletionTime = hours;
    }

    private void calculateCardColors() {
        Chroma originalColor = new Chroma("#1d98cb");
        double[] lch = originalColor.getLCH();

        lch[2] = Math.random() * 360d;

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
        return loggedHoursToComplete / originalEstimatedCompletionTime;
    }

    public String getName() {
        return name;
    }

    public String getClassName() {
        return className;
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
        if(calculatedEstimatedCompletionTime == (int) calculatedEstimatedCompletionTime) {
            return String.valueOf((int) calculatedEstimatedCompletionTime);
        } else {
            return String.valueOf(calculatedEstimatedCompletionTime);
        }
    }

    public float getTodaysHoursOfWork() {
        LocalDate now = LocalDate.now();
        LocalDate date = dueDate.toLocalDate();

        if(date.equals(now) || date.equals(now.plusDays(1))) {
            return calculatedEstimatedCompletionTime;
        } else {
            return (float) Math.ceil(calculatedEstimatedCompletionTime / ChronoUnit.DAYS.between(now, date) * 2f) / 2f;
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
        return calculatedEstimatedCompletionTime;
    }

    public void setClassName(String className) {
        this.className = className;
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
        loggedHoursToComplete += loggedHours;
    }

    public void completeTask(float loggedHours) {
        isTaskComplete = true;

        loggedHoursToComplete += loggedHours;

        IncrementalApplication.taskManager.completeTask(this);
    }

    public boolean shouldTaskBeActive(LocalDate date) {
        if(isTaskComplete) {
            return false;
        }
        if(date.isAfter(dueDate.toLocalDate())) {
            return false;
        }
        if(lastTaskWorkTime != null && lastTaskWorkTime.toLocalDate().equals(LocalDate.now()) && doneWithTaskForDay) {
            return false;
        }

        return true;
    }

    public boolean isTaskInProgress() {
        return isTaskInProgress;
    }

    public String getTimePeriod() {
        return timePeriod;
    }
}
