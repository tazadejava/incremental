package me.tazadejava.incremental.logic.tasks;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDate;
import java.time.LocalDateTime;

import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.SubGroup;

public class NonrepeatingTask extends TaskGenerator {

    private boolean hasTaskStarted;

    public NonrepeatingTask(TaskManager taskManager, LocalDate startDate, TimePeriod timePeriod, String taskName, LocalDateTime dueDateTime, Group taskGroup, @Nullable SubGroup subgroup, int estimatedMinutesToCompletion) {
        super(taskManager, startDate, timePeriod);

        hasTaskStarted = false;
        allTasks = new Task[] {new Task(this, taskName, dueDateTime, taskGroup, subgroup, timePeriod, estimatedMinutesToCompletion)};
    }

    public static NonrepeatingTask createInstance(Gson gson, TaskManager taskManager, TimePeriod timePeriod, JsonObject data) {
        NonrepeatingTask task = gson.fromJson(data.get("serialized"), NonrepeatingTask.class);

        task.taskManager = taskManager;
        task.timePeriod = timePeriod;

        task.loadAllTasks(gson, timePeriod, data.getAsJsonArray("tasks"));

        return task;
    }

    @Override
    public JsonObject save(Gson gson) {
        JsonObject data = new JsonObject();

        data.add("serialized", JsonParser.parseString(gson.toJson(this)).getAsJsonObject());

        data.add("tasks", saveAllTasks(gson));

        return data;
    }

    public void updateAndSaveTask(LocalDate startDate, String name, LocalDateTime dueDateTime, Group group, SubGroup subgroup, int estimatedMinutesToCompletion) {
        //update changes
        allTasks[0].editTask(name, dueDateTime, group, subgroup, estimatedMinutesToCompletion);

        this.startDate = startDate;

        //purge the task from any list and re-add it to all lists
        hasTaskStarted = false;
        timePeriod.resetTask(allTasks[0], this);

        //save changes
        saveTaskToFile();
    }

    @Override
    public Task[] getPendingTasks() {
        if(!hasTaskStarted && (startDate.isEqual(LocalDate.now()) || startDate.isBefore(LocalDate.now()))) {
            hasTaskStarted = true;
            return new Task[] {allTasks[0]};
        } else {
            return new Task[0];
        }
    }

    @Override
    public boolean hasGeneratorCompletedAllTasks() {
        return allTasks[0].isTaskComplete();
    }

    @Override
    public Task getNextUpcomingTask() {
        if(!hasTaskStarted) {
            allTasks[0].setStartDate(startDate);
            return allTasks[0];
        } else {
            return null;
        }
    }

    @Override
    protected LocalDate getNextUpcomingTaskStartDate() {
        if(!hasTaskStarted) {
            return startDate;
        } else {
            return null;
        }
    }
}
