package me.dracorrein.incremental.logic.dashboard;

import android.graphics.Color;

import com.chroma.Chroma;
import com.chroma.ColorSpace;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import me.dracorrein.incremental.Utils;

public class Task {

    private String name, className;
    private LocalDateTime dueDate;
    private float originalEstimatedCompletionTime, calculatedEstimatedCompletionTime;
    private List<String> subTasks;

    private boolean isTaskComplete;
    private LocalDateTime lastTaskWorkTime;
    private float loggedHoursToComplete;

    private int mainColor, endColor;

    public Task(String name, LocalDateTime dueDate, String taskClass, float calculatedEstimatedCompletionTime) {
        this.name = name;
        this.dueDate = dueDate;

        isTaskComplete = false;
        loggedHoursToComplete = 0;

        setEstimatedCompletionTime(calculatedEstimatedCompletionTime);
        setClassName(taskClass);

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
        if(subTasks != null) {
            return (float) getCompletedSubtasks() / subTasks.size();
        } else {
            if(originalEstimatedCompletionTime == 0) {
                return 1;
            }

            return (float) (originalEstimatedCompletionTime - calculatedEstimatedCompletionTime) / originalEstimatedCompletionTime;
        }
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

    public float getDailyHoursOfWork() {
        LocalDate now = LocalDate.now();
        LocalDate date = dueDate.toLocalDate();

        if(date.equals(now) || date.equals(now.plusDays(1))) {
            return calculatedEstimatedCompletionTime;
        } else {
            return (float) Math.ceil(calculatedEstimatedCompletionTime / java.time.temporal.ChronoUnit.DAYS.between(now, date) * 2f) / 2f;
        }
    }

    public float getEstimatedCompletionTime() {
        return calculatedEstimatedCompletionTime;
    }

    public List<String> getSubTasks() {
        return subTasks;
    }

    public int getCompletedSubtasks() {
        //TODO: STORE AS VARIABLE
        return 0;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(dueDate);
    }

    public void incrementTask(float loggedHours) {
        lastTaskWorkTime = LocalDateTime.now();
        loggedHoursToComplete += loggedHours;
    }

    public void completeTask(float loggedHours) {
        isTaskComplete = true;
        loggedHoursToComplete += loggedHours;
    }

    public boolean shouldTaskBeActive(LocalDate date) {
        return !lastTaskWorkTime.toLocalDate().equals(LocalDate.now()) && !isTaskComplete && !date.isAfter(dueDate.toLocalDate());
    }
}
