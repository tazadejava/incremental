package me.dracorrein.incremental.logic.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Day {

    private LocalDateTime date;
    private List<Task> tasks;

    public Day(LocalDateTime date, List<Task> allTasks) {
        this.date = date;

        tasks = new ArrayList<>();
        for(Task task : allTasks) {
            System.out.println(date + " " + task);
            if(task.shouldTaskBeActive(date.toLocalDate())) {
                tasks.add(task);
            }
        }
    }

    public float getEstimatedHoursOfWork() {
        float totalHours = 0;

        for(Task task : tasks) {
            totalHours += task.getTodaysHoursOfWork();
        }

        return Math.round(totalHours * 2) / 2.0f;
    }

    public String getEstimatedHoursOfWorkFormatted() {
        float estimation = getEstimatedHoursOfWork();
        return (estimation == (int) estimation) ? "" + (int) estimation : "" + estimation;
    }

    public void completeTask(Task task) {
        tasks.remove(task);
    }

    public String getDayFormatted() {
        if(date.toLocalDate().equals(LocalDate.now())) {
            return "Today, " + date.getMonthValue() + "/" + date.getDayOfMonth();
        }

        String dayOfWeek = date.getDayOfWeek().toString();

        return dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase() + ", " + date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    public boolean isToday() {
        return date.toLocalDate().equals(LocalDate.now());
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
