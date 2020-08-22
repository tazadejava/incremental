package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDateTime;

import me.tazadejava.incremental.logic.dashboard.Group;
import me.tazadejava.incremental.logic.dashboard.TimePeriod;

public class NonrepeatingTask extends TaskGenerator {

    private LocalDateTime startDateTime;

    private boolean hasTaskStarted;

    public NonrepeatingTask(TaskManager taskManager, LocalDateTime startDateTime, TimePeriod timePeriod, String taskName, LocalDateTime dueDateTime, Group taskGroup, float estimatedHoursToCompletion) {
        super(taskManager);

        this.startDateTime = startDateTime;

        hasTaskStarted = false;
        latestTask = new Task(this, taskName, dueDateTime, taskGroup, timePeriod, estimatedHoursToCompletion);
    }

    public static NonrepeatingTask createInstance(Gson gson, TaskManager taskManager, JsonObject data) {
        NonrepeatingTask task = gson.fromJson(data.get("serialized"), NonrepeatingTask.class);

        task.taskManager = taskManager;

        return task;
    }

    @Override
    public JsonObject save(Gson gson) {
        JsonObject data = new JsonObject();

        data.add("serialized", JsonParser.parseString(gson.toJson(this)).getAsJsonObject());

        return data;
    }

    @Override
    public Task[] getPendingTasks() {
        if(!hasTaskStarted && startDateTime.isBefore(LocalDateTime.now())) {
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
}
