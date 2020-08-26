package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;

public class Task {

    private transient TaskGenerator parent;

    private LocalDate startDate;

    private String name, taskNotes;
    private LocalDateTime dueDateTime;
    private transient Group group;
    private transient TimePeriod timePeriod;
    private int estimatedTotalMinutesToCompletion;

    private boolean isTaskComplete, isTaskCurrentlyWorkedOn, isDoneWithTaskToday;

    private int totalLoggedMinutesOfWork, loggedMinutesOfWorkToday;
    private LocalDateTime lastTaskWorkEndTime, lastTaskWorkStartTime;

    public Task(TaskGenerator parent, String name, LocalDateTime dueDateTime, Group group, TimePeriod timePeriod, int estimatedTotalMinutesToCompletion) {
        this.parent = parent;
        this.name = name;
        this.dueDateTime = dueDateTime;
        this.group = group;
        this.timePeriod = timePeriod;
        this.estimatedTotalMinutesToCompletion = estimatedTotalMinutesToCompletion;

        isTaskComplete = false;
        isTaskCurrentlyWorkedOn = false;
        isDoneWithTaskToday = false;

        totalLoggedMinutesOfWork = 0;
    }

    //load data from file
    public static Task createInstance(Gson gson, JsonObject data, TaskGenerator parent, TimePeriod timePeriod) {
        Task task = gson.fromJson(data.get("serialized"), Task.class);

        task.parent = parent;
        task.timePeriod = timePeriod;
        task.group = timePeriod.getGroupByName(data.get("group").getAsString());

        if(task.lastTaskWorkStartTime != null && !task.lastTaskWorkStartTime.toLocalDate().equals(LocalDate.now())) {
            task.loggedMinutesOfWorkToday = 0;
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

    public int getDayMinutesOfWorkTotal(LocalDate date) {
        return getDayMinutesOfWorkTotal(date, true);
    }

    public int getDayMinutesOfWorkTotal(LocalDate date, boolean ignoreTodayWorkTime) {
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
                return getTotalMinutesLeftOfWork();
            } else {
                return 0;
            }
        }

        int daysBetweenStartAndDueDate = (int) ChronoUnit.DAYS.between(startDate, dueDate);

        //we've already finished working for today, so this day is no more
        if(isDoneWithTaskToday) {
            daysBetweenStartAndDueDate--;
        }

        //if there are work constraints, we will subtract them here
        int blackedOutDaysCount = timePeriod.getWorkPreferences().countBlackedOutDaysBetween(startDate, dueDate);
        //less than is impossible temporal constraint
        if(daysBetweenStartAndDueDate - blackedOutDaysCount >= 0) {
            daysBetweenStartAndDueDate -= blackedOutDaysCount;
        } else {
            //if impossible, assume the worst
            daysBetweenStartAndDueDate = 0;
        }

        double dailyWorkloadMinutes = estimatedTotalMinutesToCompletion - totalLoggedMinutesOfWork;

        if(ignoreTodayWorkTime && date.equals(startDate)) {
            dailyWorkloadMinutes += loggedMinutesOfWorkToday;
        }

        dailyWorkloadMinutes /= daysBetweenStartAndDueDate + 1;

        return (int) dailyWorkloadMinutes;
    }

    public float getTodaysTaskCompletionPercentage() {
        float percentage = (float) loggedMinutesOfWorkToday / getDayMinutesOfWorkTotal(LocalDate.now());

        if (percentage > 1) {
            percentage = 1;
        }

        return percentage;
    }

    public float getTaskCompletionPercentage() {
        return (float) totalLoggedMinutesOfWork / estimatedTotalMinutesToCompletion;
    }

    public int getTodaysMinutesLeft() {
        return getDayMinutesOfWorkTotal(LocalDate.now()) - loggedMinutesOfWorkToday;
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

    public int getTotalMinutesLeftOfWork() {
        return estimatedTotalMinutesToCompletion - totalLoggedMinutesOfWork;
    }

    public int getEstimatedCompletionTime() {
        return estimatedTotalMinutesToCompletion;
    }

    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(dueDateTime);
    }

    public int getOverdueDays() {
        return (int) ChronoUnit.DAYS.between(dueDateTime.toLocalDate(), LocalDate.now());
    }

    public int getCurrentWorkedMinutes() {
        return (int) lastTaskWorkStartTime.until(LocalDateTime.now(), ChronoUnit.MINUTES);
    }

    public void setEstimatedTotalMinutesToCompletion(int estimatedTotalMinutesToCompletion) {
        this.estimatedTotalMinutesToCompletion = estimatedTotalMinutesToCompletion;
    }

    public void completeTaskForTheDay() {
        isDoneWithTaskToday = true;
    }

    public void incrementTaskMinutes(int loggedMinutes, boolean completedTask) {
        timePeriod.getStatsManager().appendMinutes(group, lastTaskWorkStartTime.toLocalDate(), loggedMinutes, completedTask);

        isTaskCurrentlyWorkedOn = false;

        lastTaskWorkEndTime = LocalDateTime.now();
        totalLoggedMinutesOfWork += loggedMinutes;

        if(!lastTaskWorkStartTime.toLocalDate().equals(LocalDate.now())) {
            loggedMinutesOfWorkToday = 0;
        }
        loggedMinutesOfWorkToday += loggedMinutes;

        if(completedTask) {
            isTaskComplete = true;
            parent.completeTask(this, totalLoggedMinutesOfWork);
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
     * @param estimatedTotalMinutesToCompletion
     */
    public void editTask(String name, LocalDateTime dueDateTime, Group group, int estimatedTotalMinutesToCompletion) {
        this.name = name;
        this.dueDateTime = dueDateTime;
        this.group = group;
        this.estimatedTotalMinutesToCompletion = estimatedTotalMinutesToCompletion;
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

    /**
     * Override the user saying that they are done with the task. Only use if scheduling conflicts makes it so that the user HAS to do the task today!
     */
    public void overrideDoneWithTaskMarker() {
        isDoneWithTaskToday = false;
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
        if(isDoneWithTaskToday && lastTaskWorkStartTime.toLocalDate().isBefore(LocalDate.now())) {
            isDoneWithTaskToday = false;
            parent.saveTaskToFile();
        }

        return isDoneWithTaskToday;
    }

    private int getEstimatedTotalMinutesToCompletion() {
        return estimatedTotalMinutesToCompletion;
    }

    public String getTaskID() {
        return parent.getGeneratorID() + " " + name + " " + dueDateTime.toString();
    }
}
