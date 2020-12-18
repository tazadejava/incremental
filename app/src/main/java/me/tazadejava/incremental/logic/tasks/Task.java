package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.SubGroup;

public class Task {

    private transient TaskGenerator parent;

    private LocalDate startDate;

    private String name;
    private LocalDateTime dueDateTime;
    private transient Group group;
    private transient SubGroup subgroup;
    private transient TimePeriod timePeriod;
    private int estimatedTotalMinutesToCompletion;

    private MinutesNotes minutesNotesManager;

    private boolean isTaskComplete, isTaskCurrentlyWorkedOn, isDoneWithTaskToday;

    //carryOverSeconds are used to account for rounding error when logging time from previous tasks
    private int totalLoggedMinutesOfWork, loggedMinutesOfWorkToday, carryOverSeconds;
    private LocalDateTime lastTaskWorkEndTime, lastTaskWorkStartTime;

    public Task(TaskGenerator parent, String name, LocalDateTime dueDateTime, Group group, SubGroup subgroup, TimePeriod timePeriod, int estimatedTotalMinutesToCompletion) {
        this.parent = parent;
        this.name = name;
        this.dueDateTime = dueDateTime;
        this.group = group;
        this.subgroup = subgroup;
        this.timePeriod = timePeriod;
        this.estimatedTotalMinutesToCompletion = estimatedTotalMinutesToCompletion;

        minutesNotesManager = new MinutesNotes();

        isTaskComplete = false;
        isTaskCurrentlyWorkedOn = false;
        isDoneWithTaskToday = false;

        totalLoggedMinutesOfWork = 0;
    }

    //load data from file
    public static Task createInstance(Gson gson, JsonObject data, TaskGenerator parent, TimePeriod timePeriod) {
        Task task = gson.fromJson(data.get("serialized"), Task.class);

        //if not yet initialized for the first time
        if(task.minutesNotesManager == null) {
            task.minutesNotesManager = new MinutesNotes();
        }

        task.parent = parent;
        task.timePeriod = timePeriod;
        task.group = timePeriod.getGroupByName(data.get("group").getAsString());

        if(data.has("subGroup")) {
            task.subgroup = task.group.getSubGroupByName(data.get("subGroup").getAsString());
        }

        if(task.lastTaskWorkStartTime != null && !task.lastTaskWorkStartTime.toLocalDate().equals(LocalDate.now())) {
            task.loggedMinutesOfWorkToday = 0;
        }

        return task;
    }

    public List<LocalDateTime> getMinutesNotesTimestamps() {
        return minutesNotesManager.getMinutesNotesTimestamps();
    }

    public int getMinutesFromTimestamp(LocalDateTime dateTime) {
        return minutesNotesManager.getMinutesFromTimestamp(dateTime);
    }

    public String getNotesFromTimestamp(LocalDateTime dateTime) {
        return minutesNotesManager.getNotesFromTimestamp(dateTime);
    }

    public JsonObject save(Gson gson) {
        JsonObject data = new JsonObject();

        JsonObject serialized = JsonParser.parseString(gson.toJson(this)).getAsJsonObject();
        data.add("serialized", serialized);

        data.addProperty("group", group.getGroupName());
        data.addProperty("parent", parent.getGeneratorID());

        if(subgroup != null) {
            data.addProperty("subGroup", subgroup.getName());
        }

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
        //only use the task's start date if it starts after now
        if(this.startDate != null && this.startDate.isAfter(LocalDate.now())) {
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
        //if we have no more time, we have to do it all; THIS SHOULDN'T MATTER NOW IF THE ABOVE START DATE IS SET CORRECTLY
//        if(LocalDate.now().equals(date) && date.equals(dueDate)) {
//            daysBetweenStartAndDueDate = 0;
//        }

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

        if(dailyWorkloadMinutes < 0) {
            return 0;
        }

        return (int) dailyWorkloadMinutes;
    }

    public float getTodaysTaskCompletionPercentage() {
        float percentage;
        if(isTaskCurrentlyWorkedOn) {
            percentage = getCurrentWorkedMinutesWithCarryover();
        } else {
            percentage = loggedMinutesOfWorkToday;
        }
        percentage /= getDayMinutesOfWorkTotal(LocalDate.now());

        if (percentage > 1) {
            percentage = 1;
        }

        return percentage;
    }

    public float getTaskCompletionPercentage() {
        return (float) totalLoggedMinutesOfWork / estimatedTotalMinutesToCompletion;
    }

    public int getTodaysMinutesLeft() {
        return Math.max(0, getDayMinutesOfWorkTotal(LocalDate.now()) - loggedMinutesOfWorkToday);
    }

    public int getTodaysMinutesLeftIncludingCurrentWork() {
        return Math.max(0, Math.min(getDayMinutesOfWorkTotal(LocalDate.now()) - loggedMinutesOfWorkToday, lastTaskWorkStartTime == null
                || !isTaskCurrentlyWorkedOn ? Integer.MAX_VALUE : getDayMinutesOfWorkTotal(LocalDate.now()) - getCurrentWorkedMinutesWithCarryover()));
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
        return Math.max(0, estimatedTotalMinutesToCompletion - totalLoggedMinutesOfWork);
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

    /**
     * Includes carryover minutes
     * @return
     */
    public int getCurrentWorkedMinutesWithCarryover() {
        return (int) lastTaskWorkStartTime.minusSeconds(carryOverSeconds).until(LocalDateTime.now(), ChronoUnit.MINUTES);
    }

    public boolean hasCarryoverSeconds() {
        return carryOverSeconds >= 60;
    }

    public LocalDateTime getLastTaskWorkStartTime() {
        return lastTaskWorkStartTime;
    }

    public void setEstimatedTotalMinutesToCompletion(int estimatedTotalMinutesToCompletion) {
        this.estimatedTotalMinutesToCompletion = estimatedTotalMinutesToCompletion;
    }

    public void completeTaskForTheDay() {
        isDoneWithTaskToday = true;
    }

    public void incrementTaskMinutes(int loggedMinutes, String minutesNotes, boolean completedTask) {
        incrementTaskMinutes(loggedMinutes, minutesNotes, completedTask, false, null);
    }

    public void incrementTaskMinutes(int loggedMinutes, String minutesNotes, boolean completedTask, boolean usedEstimatedTime, LocalDateTime estimatedStartTime) {
        timePeriod.getStatsManager().appendMinutes(group, lastTaskWorkStartTime.toLocalDate(), loggedMinutes, completedTask);

        if(!minutesNotes.isEmpty() || loggedMinutes > 0) {
            minutesNotesManager.addNotes(lastTaskWorkStartTime, loggedMinutes, minutesNotes);
        }

        isTaskCurrentlyWorkedOn = false;

        lastTaskWorkEndTime = LocalDateTime.now();
        totalLoggedMinutesOfWork += loggedMinutes;

        if(!lastTaskWorkStartTime.toLocalDate().equals(LocalDate.now())) {
            loggedMinutesOfWorkToday = 0;
        }
        loggedMinutesOfWorkToday += loggedMinutes;

        if(usedEstimatedTime) {
            carryOverSeconds %= 60;

            int seconds = ((int) estimatedStartTime.until(LocalDateTime.now(), ChronoUnit.SECONDS)) % 60;
            carryOverSeconds += seconds;
            System.out.println("SECONDS NOW: " + carryOverSeconds);
        }

        if(completedTask) {
            isTaskComplete = true;
            parent.completeTask(this, totalLoggedMinutesOfWork);
        }

        parent.saveTaskToFile();
    }

    /**
     * Resets the minutes worked today if workday is different
     */
    public void verifyDayChangeReset() {
        if(!lastTaskWorkStartTime.toLocalDate().equals(LocalDate.now())) {
            loggedMinutesOfWorkToday = 0;
        }
    }

    public void setSubgroup(SubGroup subgroup) {
        this.subgroup = subgroup;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Edit the current task. Does not change any generator information. DOES NOT save data. MUST SAVE afterwards to apply to files.
     * @param name
     * @param dueDateTime
     * @param group
     * @param estimatedTotalMinutesToCompletion
     */
    public void editTask(String name, LocalDateTime dueDateTime, Group group, SubGroup subGroup, int estimatedTotalMinutesToCompletion) {
        this.name = name;
        this.dueDateTime = dueDateTime;
        this.group = group;
        this.subgroup = subGroup;
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

    public boolean isInSubGroup() {
        return subgroup != null;
    }

    public SubGroup getSubgroup() {
        return subgroup;
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

    public LocalDateTime getLastTaskWorkedTime() {
        return lastTaskWorkEndTime;
    }

    public int getTotalLoggedMinutesOfWork() {
        return totalLoggedMinutesOfWork;
    }
}
