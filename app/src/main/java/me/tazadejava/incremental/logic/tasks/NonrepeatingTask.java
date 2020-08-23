package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.time.LocalDateTime;

import me.tazadejava.incremental.logic.dashboard.Group;
import me.tazadejava.incremental.logic.dashboard.TimePeriod;

public class NonrepeatingTask extends TaskGenerator {

    private boolean hasTaskStarted;

    public NonrepeatingTask(TaskManager taskManager, LocalDate startDate, TimePeriod timePeriod, String taskName, LocalDateTime dueDateTime, Group taskGroup, float estimatedHoursToCompletion) {
        super(taskManager, startDate);

        hasTaskStarted = false;
        latestTask = new Task(this, taskName, dueDateTime, taskGroup, timePeriod, estimatedHoursToCompletion);
    }

    public static NonrepeatingTask createInstance(Gson gson, TaskManager taskManager, TimePeriod timePeriod, JsonObject data) {
        NonrepeatingTask task = gson.fromJson(data.get("serialized"), NonrepeatingTask.class);

        task.taskManager = taskManager;

        if(data.has("taskData")) {
            task.latestTask = Task.createInstance(gson, data.getAsJsonObject("taskData"), task, timePeriod);
        }

        return task;
    }

    @Override
    public JsonObject save(Gson gson) {
        JsonObject data = new JsonObject();

        data.add("serialized", JsonParser.parseString(gson.toJson(this)).getAsJsonObject());

        if(!hasTaskStarted) {
            data.add("taskData", latestTask.save(gson));
        }

        return data;
    }

    public void updateAndSaveTask(LocalDate startDate) {
        //update changes
        this.startDate = startDate;

        //purge the task from any list
        hasTaskStarted = false;
        taskManager.getCurrentTimePeriod().removeTask(latestTask);

        //re-add the task to all lists
        taskManager.getCurrentTimePeriod().processPendingTasks(this);

        //save changes
        saveTaskToFile();
    }

    @Override
    public Task[] getPendingTasks() {
        if(!hasTaskStarted && (startDate.isEqual(LocalDate.now()) || startDate.isBefore(LocalDate.now()))) {
            hasTaskStarted = true;
            return new Task[] {latestTask};
        } else {
            return new Task[0];
        }
    }

    @Override
    public boolean hasGeneratorCompletedAllTasks() {
        return hasTaskStarted && latestTask.isTaskComplete();
    }

    @Override
    public Task getNextUpcomingTask() {
        if(!hasTaskStarted) {
            return latestTask;
        } else {
            return null;
        }
    }

    @Override
    public LocalDate getNextUpcomingTaskStartDate() {
        if(!hasTaskStarted) {
            return startDate;
        } else {
            return null;
        }
    }
}
