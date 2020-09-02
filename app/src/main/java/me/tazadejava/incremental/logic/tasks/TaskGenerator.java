package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;

public abstract class TaskGenerator {

    protected LocalDateTime creationTime;
    protected LocalDate startDate;

    protected transient TaskManager taskManager;
    protected transient Task[] allTasks;
    protected transient TimePeriod timePeriod;

    public TaskGenerator(TaskManager taskManager, LocalDate startDate, TimePeriod timePeriod) {
        this.taskManager = taskManager;
        this.startDate = startDate;
        this.timePeriod = timePeriod;

        creationTime = LocalDateTime.now();
    }

    public void completeTask(Task task, int totalMinutesWorked) {
        taskManager.completeTask(task);
    }

    public void saveTaskToFile() {
        taskManager.saveData(false, taskManager.getCurrentTimePeriod());
    }

    protected JsonArray saveAllTasks(Gson gson) {
        JsonArray tasksArray = new JsonArray();

        for(Task task : allTasks) {
            if(task == null) {
                tasksArray.add(JsonNull.INSTANCE);
            } else {
                tasksArray.add(task.save(gson));
            }
        }

        return tasksArray;
    }

    protected void loadAllTasks(Gson gson, TimePeriod timePeriod, JsonArray tasksArray) {
        allTasks = new Task[tasksArray.size()];

        int index = 0;
        for(JsonElement task : tasksArray) {
            if(task.isJsonNull()) {
                allTasks[index] = null;
            } else {
                allTasks[index] = Task.createInstance(gson, task.getAsJsonObject(), this, timePeriod);
            }
            index++;
        }
    }

    /**
     * Returns tasks only if not yet active. Empty otherwise.
     * @return
     */
    public abstract Task[] getPendingTasks();

    public abstract Task getNextUpcomingTask();
    protected abstract LocalDate getNextUpcomingTaskStartDate();

    public abstract boolean hasGeneratorCompletedAllTasks();

    public abstract JsonObject save(Gson gson);

    public String getGeneratorID() {
        return creationTime.toString();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void assignTasksToLoadingHashMap(HashMap<String, Task> tasksList) {
        for(Task task : allTasks) {
            //disabled weeks do not get counted
            if(task == null) {
                continue;
            }
            tasksList.put(task.getTaskID(), task);
        }
    }

    public Task[] getAllTasks() {
        return allTasks;
    }
}
