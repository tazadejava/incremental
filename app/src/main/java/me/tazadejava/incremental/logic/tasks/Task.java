package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import me.tazadejava.incremental.logic.dashboard.Group;
import me.tazadejava.incremental.logic.dashboard.TimePeriod;

public class Task {

    private transient TaskGenerator parent;

    private String name;
    private LocalDateTime dueDateTime;
    private transient Group group;
    private transient TimePeriod timePeriod;
    private float estimatedTotalHoursToCompletion;

    private boolean isTaskComplete, isTaskCurrentlyWorkedOn, isDoneWithTaskToday;
    private LocalDateTime lastTaskWorkEndTime, lastTaskWorkStartTime;
    private float loggedHoursOfWork;

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

        loggedHoursOfWork = 0;
    }

    //load data from file
    public static Task createInstance(Gson gson, JsonObject data, TaskGenerator parent, TimePeriod timePeriod) {
        Task task = gson.fromJson(data.get("serialized"), Task.class);

        task.parent = parent;
        task.timePeriod = timePeriod;
        task.group = timePeriod.getGroupByName(data.get("group").getAsString());

        return task;
    }

    public JsonObject save(Gson gson) {
        JsonObject data = new JsonObject();

        JsonObject serialized = JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
        data.add("serialized", serialized);

        data.addProperty("group", group.getGroupName());
        data.addProperty("parent", parent.getInstanceReference());

        return data;
    }

    public void startWorkingOnTask() {
        lastTaskWorkStartTime = LocalDateTime.now();
        isTaskCurrentlyWorkedOn = true;
    }

    public float getTodaysHoursOfWork() {
        LocalDate now = LocalDate.now();
        LocalDate date = dueDateTime.toLocalDate();

        if(date.equals(now) || date.equals(now.plusDays(1))) {
            return getTotalHoursLeftOfWork();
        } else {
            return (float) Math.ceil(getTotalHoursLeftOfWork() / (ChronoUnit.DAYS.between(now, date) + 1) * 2f) / 2f;
        }
    }

    public float getTaskCompletionPercentage() {
        return loggedHoursOfWork / estimatedTotalHoursToCompletion;
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
        return estimatedTotalHoursToCompletion - loggedHoursOfWork;
    }

    public float getEstimatedCompletionTime() {
        return estimatedTotalHoursToCompletion;
    }

    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(dueDateTime);
    }

    public float getCurrentWorkedHours() {
        float hoursSpent = lastTaskWorkStartTime.until(LocalDateTime.now(), ChronoUnit.MINUTES) / 60f;
        return (float) Math.ceil(hoursSpent * 10f) / 10f;
    }

    public void completeTaskForTheDay() {
        isDoneWithTaskToday = true;
    }

    public void incrementTaskHours(float loggedHours, boolean completedTask) {
        lastTaskWorkEndTime = LocalDateTime.now();
        loggedHoursOfWork += loggedHours;

        if(completedTask) {
            isTaskComplete = true;
            parent.completeTask(this, loggedHoursOfWork);
        }
    }

    //TODO: this should maybe be not put in the task itself?
    public boolean shouldTaskBeActive(LocalDate date) {
        if(isTaskComplete) {
            return false;
        }
        if(date.isAfter(dueDateTime.toLocalDate())) {
            return false;
        }
        //don't show up if the user doesn't want to work on it anymore today
        if(lastTaskWorkEndTime != null && lastTaskWorkEndTime.toLocalDate().equals(LocalDate.now()) && isDoneWithTaskToday) {
            return false;
        }

        return true;
    }

    public boolean isTaskComplete() {
        return isTaskComplete;
    }

    public boolean isTaskCurrentlyWorkedOn() {
        return isTaskCurrentlyWorkedOn;
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public Group getGroup() {
        return group;
    }
}
