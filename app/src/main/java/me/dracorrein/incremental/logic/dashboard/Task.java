package me.dracorrein.incremental.logic.dashboard;

import android.graphics.Color;

import com.chroma.Chroma;
import com.chroma.ColorSpace;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import me.dracorrein.incremental.Utils;

public class Task {

    protected String name, className;
    protected LocalDateTime dueDate;
    protected float originalEstimatedCompletionTime, estimatedCompletionTime;
    protected List<String> subTasks;

    private int mainColor, endColor;

    public Task(String name, LocalDateTime dueDate) {
        this.name = name;
        this.dueDate = dueDate;

        calculateCardColors();
    }

    public void setEstimatedCompletionTime(float hours) {
        originalEstimatedCompletionTime = hours;
        estimatedCompletionTime = hours;
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

            return (float) (originalEstimatedCompletionTime - estimatedCompletionTime) / originalEstimatedCompletionTime;
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
        if(estimatedCompletionTime == (int) estimatedCompletionTime) {
            return String.valueOf((int) estimatedCompletionTime);
        } else {
            return String.valueOf(estimatedCompletionTime);
        }
    }

    public float getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }

    public List<String> getSubTasks() {
        return subTasks;
    }

    public int getCompletedSubtasks() {
        //TODO: STORE AS VARIABLE
        return 0;
    }
}
