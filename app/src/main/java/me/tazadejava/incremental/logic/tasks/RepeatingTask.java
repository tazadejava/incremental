package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.logic.dashboard.Group;
import me.tazadejava.incremental.logic.dashboard.TimePeriod;

public class RepeatingTask extends TaskGenerator {

    private DayOfWeek dayOfWeekTaskBegins, dayOfWeekTaskDue;
    private LocalTime timeTaskDue;

    private String[] taskNames;
    private int currentTaskIndex, totalTasksCount;

    private transient TimePeriod timePeriod;
    protected transient Group taskGroup;
    private float originalEstimatedHoursCompletion;

    private float totalHoursWorked;
    private int totalTasksCompleted;

    private LocalDate lastPendingTaskUpdateDate;

    public RepeatingTask(TaskManager taskManager, String[] taskNames, DayOfWeek dayOfWeekTaskBegins, DayOfWeek dayOfWeekTaskDue, LocalTime timeTaskDue, Group taskGroup, TimePeriod timePeriod, float originalEstimatedHoursCompletion) {
        super(taskManager);

        this.taskNames = taskNames;
        this.taskGroup = taskGroup;
        this.dayOfWeekTaskBegins = dayOfWeekTaskBegins;
        this.dayOfWeekTaskDue = dayOfWeekTaskDue;
        this.timeTaskDue = timeTaskDue;
        this.timePeriod = timePeriod;
        this.originalEstimatedHoursCompletion = originalEstimatedHoursCompletion;

        currentTaskIndex = 0;
        totalTasksCount = taskNames.length;

        totalHoursWorked = 0;
        totalTasksCompleted = 0;

        lastPendingTaskUpdateDate = LocalDate.now().minusDays(1);
    }

    public static RepeatingTask createInstance(Gson gson, TaskManager taskManager, TimePeriod timePeriod, JsonObject data) {
        RepeatingTask task = gson.fromJson(data.get("serialized"), RepeatingTask.class);

        task.taskManager = taskManager;
        task.timePeriod = timePeriod;
        task.taskGroup = timePeriod.getGroupByName(data.get("group").getAsString());

        return task;
    }

    @Override
    public JsonObject save(Gson gson) {
        JsonObject data = new JsonObject();

        data.add("serialized", JsonParser.parseString(gson.toJson(this)).getAsJsonObject());

        data.addProperty("generatorType", "repeating");
        data.addProperty("group", taskGroup.getGroupName());

        return data;
    }

    //release pending tasks, and empty the task list
    public Task[] getPendingTasks() {
        if(currentTaskIndex == totalTasksCount) {
            return new Task[0];
        }

        List<LocalDateTime> pendingTaskDates = getMissedTaskDays();
        lastPendingTaskUpdateDate = LocalDate.now();

        List<Task> pendingTasks = new ArrayList<>();

        float calculatedEstimatedTime = originalEstimatedHoursCompletion;

        if(totalTasksCompleted > 0) {
            //gradually incorporate the originalEstimatedTime with the new calculated average

            float estimatedAverageHours = totalHoursWorked / totalTasksCompleted;

            if(totalTasksCompleted < 3) {
                //50 50
                calculatedEstimatedTime = (0.5f * originalEstimatedHoursCompletion) + (0.5f * estimatedAverageHours);
            } else if(totalTasksCompleted < 5) {
                //25 75
                calculatedEstimatedTime = (0.25f * originalEstimatedHoursCompletion) + (0.75f * estimatedAverageHours);
            } else {
                //0 100
                calculatedEstimatedTime = estimatedAverageHours;
            }
        }

        for(LocalDateTime dueDate : pendingTaskDates) {
            Task task = new Task(this, taskNames[currentTaskIndex], dueDate, taskGroup, timePeriod, calculatedEstimatedTime);

            pendingTasks.add(task);

            currentTaskIndex++;

            if(currentTaskIndex == totalTasksCount) {
                latestTask = task;
                break;
            }
        }

        return pendingTasks.toArray(new Task[0]);
    }

    @Override
    public void completeTask(Task task, float totalHoursWorked) {
        super.completeTask(task, totalHoursWorked);

        this.totalHoursWorked += totalHoursWorked;
        totalTasksCompleted++;
    }

    @Override
    public void loadLatestTaskFromFile(Task task) {
        if(task.getName().equals(taskNames[taskNames.length - 1])) {
            this.latestTask = task;
        }
    }

    public boolean hasGeneratorCompletedAllTasks() {
        return currentTaskIndex == totalTasksCount && latestTask.isTaskComplete();
    }

    //returns a list of tasks that have to be done and is in the backlog
    private List<LocalDateTime> getMissedTaskDays() {
        List<LocalDateTime> missedTaskDueDates = new ArrayList<>();

        LocalDate now = LocalDate.now();

        if(lastPendingTaskUpdateDate.equals(now)) {
            return missedTaskDueDates;
        }

        LocalDate currentDateTraversal = lastPendingTaskUpdateDate;

        //update the lastPending to the current start day of the week, and add to the list
        currentDateTraversal.plusDays(getDaysBetweenDaysOfWeek(dayOfWeekTaskBegins, currentDateTraversal.getDayOfWeek()));

        if(currentDateTraversal.isAfter(now)) {
            return missedTaskDueDates;
        }

        missedTaskDueDates.add(LocalDateTime.of(currentDateTraversal.plusDays(getDaysBetweenDaysOfWeek(currentDateTraversal.getDayOfWeek(), dayOfWeekTaskDue)), timeTaskDue));

        //traverse the weeks, one at a time, until reach after now. stop there.
        //change now to a day afterwards so that we can check once a week increments, inclusive
        now = now.plusDays(1);
        while((currentDateTraversal = currentDateTraversal.plusDays(7)).isBefore(now)) {
            missedTaskDueDates.add(LocalDateTime.of(currentDateTraversal.plusDays(getDaysBetweenDaysOfWeek(currentDateTraversal.getDayOfWeek(), dayOfWeekTaskDue)), timeTaskDue));
        }

        lastPendingTaskUpdateDate = currentDateTraversal;

        return missedTaskDueDates;
    }

    private int getDaysBetweenDaysOfWeek(DayOfWeek start, DayOfWeek end) {
        int dayOfWeekAdjustmentDays = start.getValue() - end.getValue();
        if(dayOfWeekAdjustmentDays < 0) {
            dayOfWeekAdjustmentDays += 7;
        }
        return dayOfWeekAdjustmentDays;
    }
}
