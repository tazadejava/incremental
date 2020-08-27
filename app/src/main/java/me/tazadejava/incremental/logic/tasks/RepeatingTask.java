package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.ui.main.Utils;

public class RepeatingTask extends TaskGenerator {

    private DayOfWeek dayOfWeekTaskBegins;

    private String repeatingTaskNotes;

    private String[] taskNames;
    private int currentTaskIndex, totalTasksCount;

    private transient TimePeriod timePeriod;
    protected transient Group taskGroup;
    private int originalEstimatedMinutesCompletion;

    private boolean useAverageInWorktimeEstimate;
    private int totalMinutesWorked, totalTasksCompleted;

    public RepeatingTask(TaskManager taskManager, String[] taskNames, LocalDate startDate, DayOfWeek dayOfWeekTaskBegins, DayOfWeek dayOfWeekTaskDue, LocalTime timeTaskDue, Group taskGroup, TimePeriod timePeriod, int estimatedMinutesToCompletion, boolean useAverage) {
        super(taskManager, startDate);

        this.taskNames = taskNames;
        this.taskGroup = taskGroup;
        this.dayOfWeekTaskBegins = dayOfWeekTaskBegins;
        this.timePeriod = timePeriod;
        this.originalEstimatedMinutesCompletion = estimatedMinutesToCompletion;
        this.useAverageInWorktimeEstimate = useAverage;

        currentTaskIndex = 0;
        totalTasksCount = taskNames.length;

        totalMinutesWorked = 0;
        totalTasksCompleted = 0;

        //INVARIANT: the FINAL task will NOT BE EMPTY. THIS MUST BE HELD UP BY THE UI
        //notice: all tasks will start with the original estimated hours to completion. as tasks are completed, all subsequent tasks will be UPDATED with the new estimated hours, taken via an average of previous work done!
        allTasks = new Task[taskNames.length];
        for(int i = 0; i < taskNames.length; i++) {
            if(taskNames[i].isEmpty()) {
                allTasks[i] = null;
            } else {
                allTasks[i] = new Task(this, taskNames[i], getIndexDueDate(i, startDate, dayOfWeekTaskBegins, dayOfWeekTaskDue, timeTaskDue), taskGroup, timePeriod, originalEstimatedMinutesCompletion);
            }
        }
    }

    public static RepeatingTask createInstance(Gson gson, TaskManager taskManager, TimePeriod timePeriod, JsonObject data) {
        RepeatingTask task = gson.fromJson(data.get("serialized"), RepeatingTask.class);

        task.timePeriod = timePeriod;
        task.taskGroup = timePeriod.getGroupByName(data.get("group").getAsString());

        task.taskManager = taskManager;
        task.loadAllTasks(gson, timePeriod, data.getAsJsonArray("tasks"));

        return task;
    }

    @Override
    public JsonObject save(Gson gson) {
        JsonObject data = new JsonObject();

        data.add("serialized", JsonParser.parseString(gson.toJson(this)).getAsJsonObject());

        data.add("tasks", saveAllTasks(gson));

        data.addProperty("generatorType", "repeating");
        data.addProperty("group", taskGroup.getGroupName());

        return data;
    }

    private LocalDateTime getIndexDueDate(int taskIndex, LocalDate startDate, DayOfWeek dayOfWeekTaskBegins, DayOfWeek dayOfWeekTaskDue, LocalTime timeTaskDue) {
        return startDate.plusDays(Utils.getDaysBetweenDaysOfWeek(startDate.getDayOfWeek(), dayOfWeekTaskBegins) + Utils.getDaysBetweenDaysOfWeek(dayOfWeekTaskBegins, dayOfWeekTaskDue)).plusDays(7 * taskIndex).atTime(timeTaskDue);
    }

    //release pending tasks, and empty the task list
    public Task[] getPendingTasks() {
        if(currentTaskIndex == totalTasksCount) {
            return new Task[0];
        }

        int goalIndex = getGoalTaskIndex();

        if(goalIndex <= currentTaskIndex) {
            return new Task[0];
        }

        List<Task> pendingTasks = new ArrayList<>();

        for(int i = currentTaskIndex; i < goalIndex; i++) {
            Task task = getIndexTask(i);

            if(task != null) {
                pendingTasks.add(task);
            }
        }

        currentTaskIndex = goalIndex;

        return pendingTasks.toArray(new Task[0]);
    }

    private Task getIndexTask(int index) {
        //update the task based on previous runs to determine how many hours will be needed to complete this task
        allTasks[index].setEstimatedTotalMinutesToCompletion(calculateRevisedAverageCompletionTime());

        return allTasks[index];
    }

    public void setUseAverageInWorktimeEstimate(boolean useAverage) {
        useAverageInWorktimeEstimate = useAverage;
    }

    private int calculateRevisedAverageCompletionTime() {
        double calculatedEstimatedTime = originalEstimatedMinutesCompletion;

        if(useAverageInWorktimeEstimate && totalTasksCompleted > 0) {
            //gradually incorporate the originalEstimatedTime with the new calculated average

            double estimatedAverageMinutes = totalMinutesWorked / totalTasksCompleted;

            if(totalTasksCompleted < 2) {
                //50 50
                calculatedEstimatedTime = (0.5 * originalEstimatedMinutesCompletion) + (0.5 * estimatedAverageMinutes);
            } else if(totalTasksCompleted < 4) {
                //25 75
                calculatedEstimatedTime = (0.25 * originalEstimatedMinutesCompletion) + (0.75 * estimatedAverageMinutes);
            } else {
                //0 100
                calculatedEstimatedTime = estimatedAverageMinutes;
            }
        }

        return (int) calculatedEstimatedTime;
    }

    @Override
    public void completeTask(Task task, int totalMinutesWorked) {
        super.completeTask(task, totalMinutesWorked);

        this.totalMinutesWorked += totalMinutesWorked;
        totalTasksCompleted++;
    }

    public boolean hasGeneratorCompletedAllTasks() {
        if(currentTaskIndex != totalTasksCount) {
            return false;
        }

        for(Task task : allTasks) {
            if(!task.isTaskComplete()) {
                return false;
            }
        }

        return true;
    }

    private int getGoalTaskIndex() {
        int daysDifference = (int) ChronoUnit.DAYS.between(startDate.plusDays(Utils.getDaysBetweenDaysOfWeek(startDate.getDayOfWeek(), dayOfWeekTaskBegins)), LocalDate.now());

        if(daysDifference < 0) {
            return 0;
        }

        int goalIndex = (daysDifference / 7) + 1;

        if(goalIndex > totalTasksCount) {
            goalIndex = totalTasksCount;
        }

        return goalIndex;
    }

    public void updateAndSaveTask(String[] taskNames, DayOfWeek dayOfWeekTaskBegins, DayOfWeek dayOfWeekTaskDue, LocalTime timeTaskDue, Group taskGroup, int originalEstimatedMinutesCompletion, boolean useAverage) {
        //update changes; notice that the start date cannot change
        this.dayOfWeekTaskBegins = dayOfWeekTaskBegins;
        this.taskGroup = taskGroup;
        this.originalEstimatedMinutesCompletion = originalEstimatedMinutesCompletion;
        this.useAverageInWorktimeEstimate = useAverage;

        int currentTaskIndexIncludingActive;
        if(currentTaskIndex == 0) {
            currentTaskIndexIncludingActive = 0;
        } else {
            if(currentTaskIndex >= allTasks.length) {
                currentTaskIndexIncludingActive = allTasks.length - 1;
            } else {
                if (allTasks[currentTaskIndex].isTaskComplete()) {
                    currentTaskIndexIncludingActive = currentTaskIndex;
                } else {
                    currentTaskIndexIncludingActive = currentTaskIndex - 1;
                }
            }
        }

        //removed tasks from the list, so shorten the list
        if(taskNames.length < this.taskNames.length) {
            //cannot shorten to less than the active task
            Task[] newAllTasks = new Task[Math.max(taskNames.length, currentTaskIndexIncludingActive)];

            for(int i = 0; i < newAllTasks.length; i++) {
                newAllTasks[i] = allTasks[i];
            }

            allTasks = newAllTasks;
        }

        //update the existing tasks; excluding the ones that have been finished
        for(int i = currentTaskIndexIncludingActive; i < allTasks.length; i++) {
            allTasks[i].editTask(taskNames[i], getIndexDueDate(i, startDate, dayOfWeekTaskBegins, dayOfWeekTaskDue, timeTaskDue), taskGroup, calculateRevisedAverageCompletionTime());
        }

        //added more tasks onto the list
        if(taskNames.length > this.taskNames.length) {
            Task[] newAllTasks = new Task[taskNames.length];

            for(int i = 0; i < newAllTasks.length; i++) {
                if(i < allTasks.length) {
                    newAllTasks[i] = allTasks[i];
                } else {
                    newAllTasks[i] = new Task(this, taskNames[i], getIndexDueDate(i, startDate, dayOfWeekTaskBegins, dayOfWeekTaskDue, timeTaskDue), taskGroup, timePeriod, calculateRevisedAverageCompletionTime());
                }
            }

            allTasks = newAllTasks;
        }

        this.taskNames = taskNames;

        //purge the task from any list
        taskManager.getCurrentTimePeriod().removeAllDailyTasksByParent(this);
        List<Task> removedTasks = taskManager.getCurrentTimePeriod().removeActiveTasksByParent(this);

        //if any tasks were removed in the process
        //invariant; the removed items are actually in this repeating task generator. if not, that's quite strange
        if(removedTasks.size() > 0) {
            int minTaskIndex = Integer.MAX_VALUE;
            for(Task removedTask : removedTasks) {
                int taskIndex = Integer.MAX_VALUE;
                for(int i = 0; i < allTasks.length; i++) {
                    if(allTasks[i] == removedTask) {
                        taskIndex = i;
                        break;
                    }
                }

                minTaskIndex = Math.min(minTaskIndex, taskIndex);
            }

            currentTaskIndex = minTaskIndex;
        }

        //if the starting date changed, then we must update accordingly
        currentTaskIndex = Math.min(currentTaskIndex, getGoalTaskIndex());

        //re-add the task to all lists
        taskManager.getCurrentTimePeriod().processPendingTasks(this);

        //save changes
        saveTaskToFile();
    }

    @Override
    public Task getNextUpcomingTask() {
        if(currentTaskIndex == totalTasksCount) {
            return null;
        }
        if(taskNames[currentTaskIndex].isEmpty()) {
            return null;
        }

        Task task = getIndexTask(currentTaskIndex);
        task.setStartDate(getNextUpcomingTaskStartDate());

        return task;
    }

    @Override
    protected LocalDate getNextUpcomingTaskStartDate() {
        if(currentTaskIndex == totalTasksCount) {
            return null;
        }
        if(taskNames[currentTaskIndex].isEmpty()) {
            return null;
        }

        if (currentTaskIndex == 0) {
            return startDate.plusDays(Utils.getDaysBetweenDaysOfWeek(startDate.getDayOfWeek(), dayOfWeekTaskBegins));
        } else {
            int daysBetween = Utils.getDaysBetweenDaysOfWeek(allTasks[currentTaskIndex - 1].getDueDateTime().getDayOfWeek(), dayOfWeekTaskBegins);

            if (daysBetween == 0) {
                daysBetween += 7;
            }

            return allTasks[currentTaskIndex - 1].getDueDateTime().toLocalDate().plusDays(daysBetween);
        }
    }

    public DayOfWeek getDayOfWeekTaskBegins() {
        return dayOfWeekTaskBegins;
    }

    public void setTaskNotes(String notes) {
        this.repeatingTaskNotes = notes;
        saveTaskToFile();
    }

    public String getTaskNotes() {
        return repeatingTaskNotes;
    }

    public String[] getTaskNames() {
        return taskNames;
    }

    public boolean getUseAverageInWorktimeEstimate() {
        return useAverageInWorktimeEstimate;
    }
}
