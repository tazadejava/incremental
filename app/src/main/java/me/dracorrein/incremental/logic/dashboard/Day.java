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
            //TODO: revise when it is overdue; if so, then show up again on future dates
            if(task.getDueDate().toLocalDate().isAfter(date.plusDays(-1).toLocalDate())) {
                tasks.add(task);
            }
        }
    }

    public float getEstimatedHoursOfWork() {
        float totalHours = 0;

        LocalDate now = LocalDate.now();

        for(Task task : tasks) {
            LocalDate date = task.getDueDate().toLocalDate();

            if(date.equals(now) || date.equals(now.plusDays(1))) {
                totalHours += task.getEstimatedCompletionTime();
            } else {
                totalHours += task.getEstimatedCompletionTime() / java.time.temporal.ChronoUnit.DAYS.between(date, now);
            }
        }

        return Math.round(totalHours * 2) / 2.0f;
    }

    public String getEstimatedHoursOfWorkFormatted() {
        float estimation = getEstimatedHoursOfWork();
        return (estimation == (int) estimation) ? "" + (int) estimation : "" + estimation;
    }

    public String getDayFormatted() {
        if(date.toLocalDate().equals(LocalDate.now())) {
            return "Today, " + date.getMonthValue() + "/" + date.getDayOfMonth();
        }

        String dayOfWeek = date.getDayOfWeek().toString();

        return dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase() + ", " + date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
