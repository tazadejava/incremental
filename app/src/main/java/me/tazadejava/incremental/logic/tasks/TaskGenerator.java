package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;

public abstract class TaskGenerator {

    protected LocalDateTime creationTime;
    protected LocalDate startDate;

    protected transient TaskManager taskManager;
    protected transient Task latestTask;

    public TaskGenerator(TaskManager taskManager, LocalDate startDate) {
        this.taskManager = taskManager;
        this.startDate = startDate;

        creationTime = LocalDateTime.now();
    }

    public void completeTask(Task task, float totalHoursWorked) {
        taskManager.completeTask(task);
    }

    public void loadLatestTaskFromFile(Task task) {
        this.latestTask = task;
    }

    /**
     * Returns tasks only if not yet active. Empty otherwise.
     * @return
     */
    public abstract Task[] getPendingTasks();
    public abstract boolean hasGeneratorCompletedAllTasks();

    public abstract JsonObject save(Gson gson);

    public String getInstanceReference() {
        return creationTime.toString();
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}
