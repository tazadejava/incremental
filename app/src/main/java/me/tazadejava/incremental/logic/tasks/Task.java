package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import me.tazadejava.incremental.logic.dashboard.Group;
import me.tazadejava.incremental.logic.dashboard.TimePeriod;

public class Task {

    private transient TaskGenerator parent;

    private LocalDate startDate;

    private String name, taskNotes;
    private LocalDateTime dueDateTime;
    private transient Group group;
    private transient TimePeriod timePeriod;
    private float estimatedTotalHoursToCompletion;

    private boolean isTaskComplete, isTaskCurrentlyWorkedOn, isDoneWithTaskToday;

    private float totalLoggedHoursOfWork, loggedHoursOfWorkToday;
    private LocalDateTime lastTaskWorkEndTime, lastTaskWorkStartTime;

    public Task(TaskGenerator parent, String name, LocalDateTime dueDateTime, Group group, TimePeriod timePeriod, float estimatedTotalHoursToCompletion) {
        this.parent = parent;
        this.name = name;
        this.dueDateTime = dueDateTime;
        this.group = group;
        this.timePeriod = timePeriod;
        this.estimatedTotalHoursToCompletion = estimatedTotalHoursToCompletion;

        isTaskComplete = false;
        isTaskCurrentlyWorkedOn = false;
        isDoneWithTaskToday = false;

        totalLoggedHoursOfWork = 0;
    }

    //load data from file
    public static Task createInstance(Gson gson, JsonObject data, TaskGenerator parent, TimePeriod timePeriod) {
        Task task = gson.fromJson(data.get("serialized"), Task.class);

        task.parent = parent;
        task.timePeriod = timePeriod;
        task.group = timePeriod.getGroupByName(data.get("group").getAsString());

        if(task.lastTaskWorkStartTime != null && !task.lastTaskWorkStartTime.toLocalDate().equals(LocalDate.now())) {
            task.loggedHoursOfWorkToday = 0;
        }

        return task;
    }

    public JsonObject save(Gson gson) {
        JsonObject data = new JsonObject();

        JsonObject serialized = JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
        data.add("serialized", serialized);

        data.addProperty("group", group.getGroupName());
        data.addProperty("parent", parent.getGeneratorID());

        return data;
    }

    public void startWorkingOnTask() {
        lastTaskWorkStartTime = LocalDateTime.now();
        isTaskCurrentlyWorkedOn = true;
    }

    public float getDayHoursOfWorkTotal(LocalDate date) {
        return getDayHoursOfWorkTotal(date, true);
    }

    public float getDayHoursOfWorkTotal(LocalDate date, boolean ignoreTodayHours) {
        LocalDate startDate;
        if(this.startDate != null) {
            startDate = this.startDate;
        } else {
            startDate = LocalDate.now();
        }

        LocalDate dueDate = dueDateTime.toLocalDate();

        if(date.isBefore(startDate)) {
            return 0;
        }
        if(date.isAfter(dueDate)) {
            if(isOverdue()) {
                return getTotalHoursLeftOfWork();
            } else {
                return 0;
            }
        }

        int daysbeforeDueDate = (int) ChronoUnit.DAYS.between(startDate, dueDate);

        float dailyWorkloadHours = estimatedTotalHoursToCompletion - totalLoggedHoursOfWork;

        if(ignoreTodayHours && date.equals(startDate)) {
            dailyWorkloadHours += loggedHoursOfWorkToday;
        }

        dailyWorkloadHours /= daysbeforeDueDate + 1;

        return dailyWorkloadHours;
    }

    public float getTodaysTaskCompletionPercentage() {
        float percentage = loggedHoursOfWorkToday / getDayHoursOfWorkTotal(LocalDate.now());

        if (percentage > 1) {
            percentage = 1;
        }

        return percentage;
    }

    public float getTodaysHoursLeft() {
        return getDayHoursOfWorkTotal(LocalDate.now()) - loggedHoursOfWorkToday;
    }

    public float getTaskCompletionPercentage() {
        return totalLoggedHoursOfWork / estimatedTotalHoursToCompletion;
    }

    public String getName() {
        return name;
    }

    public String getGroupName() {
        return group.getGroupName();
    }

    public LocalDateTime getDueDateTime() {
        return dueDateTime;
    }

    public float getTotalHoursLeftOfWork() {
        return estimatedTotalHoursToCompletion - totalLoggedHoursOfWork;
    }

    public float getEstimatedCompletionTime() {
        return estimatedTotalHoursToCompletion;
    }

    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(dueDateTime);
    }

    public int getOverdueDays() {
        return (int) ChronoUnit.DAYS.between(dueDateTime, LocalDateTime.now());
    }

    public float getCurrentWorkedHours() {
        float hoursSpent = lastTaskWorkStartTime.until(LocalDateTime.now(), ChronoUnit.MINUTES) / 60f;
        return (float) Math.ceil(hoursSpent * 10f) / 10f;
    }

    public void setEstimatedTotalHoursToCompletion(float estimatedTotalHoursToCompletion) {
        this.estimatedTotalHoursToCompletion = estimatedTotalHoursToCompletion;
    }

    public void completeTaskForTheDay() {
        isDoneWithTaskToday = true;
    }

    public void incrementTaskHours(float loggedHours, boolean completedTask) {
        isTaskCurrentlyWorkedOn = false;

        lastTaskWorkEndTime = LocalDateTime.now();
        totalLoggedHoursOfWork += loggedHours;

        if(!lastTaskWorkStartTime.toLocalDate().equals(LocalDate.now())) {
            loggedHoursOfWorkToday = 0;
        }
        loggedHoursOfWorkToday += loggedHours;

        if(completedTask) {
            isTaskComplete = true;
            parent.completeTask(this, totalLoggedHoursOfWork);
        }

        parent.saveTaskToFile();
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setTaskNotes(String notes) {
        if(parent instanceof RepeatingTask) {
            ((RepeatingTask) parent).setTaskNotes(notes);
        } else {
            taskNotes = notes;
            parent.saveTaskToFile();
        }
    }

    /**
     * Edit the current task. Does not change any generator information. DOES NOT save data. MUST SAVE afterwards to apply to files.
     * @param name
     * @param dueDateTime
     * @param group
     * @param estimatedTotalHoursToCompletion
     */
    public void editTask(String name, LocalDateTime dueDateTime, Group group, float estimatedTotalHoursToCompletion) {
        this.name = name;
        this.dueDateTime = dueDateTime;
        this.group = group;
        this.estimatedTotalHoursToCompletion = estimatedTotalHoursToCompletion;
    }

    public boolean isTaskComplete() {
        return isTaskComplete;
    }

    public boolean isTaskCurrentlyWorkedOn() {
        return isTaskCurrentlyWorkedOn;
    }

    public Group getGroup() {
        return group;
    }

    public String getTaskNotes() {
        if(parent instanceof RepeatingTask) {
            return ((RepeatingTask) parent).getTaskNotes();
        } else {
            return taskNotes;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(name, task.name) &&
                Objects.equals(dueDateTime, task.dueDateTime) &&
                Objects.equals(group, task.group) &&
                Objects.equals(timePeriod, task.timePeriod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dueDateTime, group, timePeriod);
    }

    public boolean isRepeatingTask() {
        return parent instanceof RepeatingTask;
    }

    public TaskGenerator getParent() {
        return parent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public boolean isDoneWithTaskToday() {
        return isDoneWithTaskToday;
    }

    public String getTaskID() {
        return parent.getGeneratorID() + " " + name + " " + dueDateTime.toString();
    }
}
