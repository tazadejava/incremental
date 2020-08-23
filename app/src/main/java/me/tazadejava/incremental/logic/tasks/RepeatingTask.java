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
import java.time.temporal.ChronoUnit;

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

    public RepeatingTask(TaskManager taskManager, String[] taskNames, LocalDate startDate, DayOfWeek dayOfWeekTaskBegins, DayOfWeek dayOfWeekTaskDue, LocalTime timeTaskDue, Group taskGroup, TimePeriod timePeriod, float originalEstimatedHoursCompletion) {
        super(taskManager, startDate);

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

<<<<<<< Updated upstream
        List<LocalDateTime> pendingTaskDates = getMissedTaskDays();
        lastPendingTaskUpdateDate = LocalDate.now();
=======
        int goalIndex = getGoalTaskIndex();
>>>>>>> Stashed changes

        if(goalIndex == currentTaskIndex) {
            return new Task[0];
        }

<<<<<<< Updated upstream
=======
        Task[] pendingTasks = new Task[goalIndex - currentTaskIndex];

        for(int i = currentTaskIndex; i < goalIndex; i++) {
            Task task = getIndexTask(i);

            if(!task.getName().isEmpty()) {
                pendingTasks[i - currentTaskIndex] = task;
                latestTask = task;
            }
        }

        currentTaskIndex = goalIndex;

        return pendingTasks;
    }

    private Task getIndexTask(int index) {
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
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
=======
        return new Task(this, taskNames[index], getIndexDueDate(index), taskGroup, timePeriod, calculatedEstimatedTime);
>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
    //returns a list of tasks that have to be done and is in the backlog
    private List<LocalDateTime> getMissedTaskDays() {
        List<LocalDateTime> missedTaskDueDates = new ArrayList<>();

        LocalDate now = LocalDate.now();

        if(lastPendingTaskUpdateDate.equals(now)) {
            return missedTaskDueDates;
=======
//    private List<LocalDateTime> getMissedTaskDayDueDates() {
//        return getMissedTaskDayDueDates(LocalDate.now());
//    }

    private int getGoalTaskIndex() {
        int daysDifference = (int) ChronoUnit.DAYS.between(startDate, LocalDate.now());

        int goalIndex = (daysDifference / 7) + 1;

        if(goalIndex > totalTasksCount) {
            goalIndex = totalTasksCount;
>>>>>>> Stashed changes
        }

        return goalIndex;
    }

<<<<<<< Updated upstream
        //update the lastPending to the current start day of the week, and add to the list
        currentDateTraversal.plusDays(getDaysBetweenDaysOfWeek(dayOfWeekTaskBegins, currentDateTraversal.getDayOfWeek()));

        if(currentDateTraversal.isAfter(now)) {
            return missedTaskDueDates;
        }
=======
    private LocalDateTime getIndexDueDate(int taskIndex) {
        return startDate.plusDays(getDaysBetweenDaysOfWeek(dayOfWeekTaskBegins, dayOfWeekTaskDue)).plusDays(7 * taskIndex).atTime(timeTaskDue);
    }

    public void updateAndSaveTask(String[] taskNames, DayOfWeek dayOfWeekTaskBegins, DayOfWeek dayOfWeekTaskDue, LocalTime timeTaskDue, Group taskGroup, float originalEstimatedHoursCompletion) {
        //update changes
        this.taskNames = taskNames;
        this.dayOfWeekTaskBegins = dayOfWeekTaskBegins;
        this.dayOfWeekTaskDue = dayOfWeekTaskDue;
        this.timeTaskDue = timeTaskDue;
        this.taskGroup = taskGroup;
        this.originalEstimatedHoursCompletion = originalEstimatedHoursCompletion;

        //purge the task from any list
        //TODO: this will not store the hours done in the tasks, and will be forgotten after this operation! if this becomes something that is worthwhile to fix, then do it
        int removed = taskManager.getCurrentTimePeriod().removeTaskByParent(this);
>>>>>>> Stashed changes

        int originalIndex = currentTaskIndex;

<<<<<<< Updated upstream
        //traverse the weeks, one at a time, until reach after now. stop there.
        //change now to a day afterwards so that we can check once a week increments, inclusive
        now = now.plusDays(1);
        while((currentDateTraversal = currentDateTraversal.plusDays(7)).isBefore(now)) {
            missedTaskDueDates.add(LocalDateTime.of(currentDateTraversal.plusDays(getDaysBetweenDaysOfWeek(currentDateTraversal.getDayOfWeek(), dayOfWeekTaskDue)), timeTaskDue));
=======
        if(removed > 0) {
            for(int i = 0; i < removed; i++) {
                currentTaskIndex--;

                while (currentTaskIndex > 0 && taskNames[currentTaskIndex].isEmpty()) {
                    currentTaskIndex--;
                }
            }
>>>>>>> Stashed changes
        }

        System.out.println("WENT FROM INDEX " + originalIndex + " TO INDEX " + currentTaskIndex + ", SIZE " + totalTasksCount);

        //re-add the task to all lists
        taskManager.getCurrentTimePeriod().processPendingTasks(this);

        //save changes
        saveTaskToFile();
    }

<<<<<<< Updated upstream
=======
    @Override
    public Task getNextUpcomingTask() {
        if(currentTaskIndex == totalTasksCount) {
            return null;
        }
        if(taskNames[currentTaskIndex].isEmpty()) {
            return null;
        }

        return getIndexTask(currentTaskIndex);
    }

    @Override
    public LocalDate getNextUpcomingTaskStartDate() {
        if(currentTaskIndex == totalTasksCount) {
            return null;
        }
        if(taskNames[currentTaskIndex].isEmpty()) {
            return null;
        }

        int daysBetween = getDaysBetweenDaysOfWeek(LocalDate.now().getDayOfWeek(), dayOfWeekTaskBegins);

        if(daysBetween == 0) {
            daysBetween += 7;
        }

        return LocalDate.now().plusDays(daysBetween);
    }

>>>>>>> Stashed changes
    private int getDaysBetweenDaysOfWeek(DayOfWeek start, DayOfWeek end) {
        int dayOfWeekAdjustmentDays = start.getValue() - end.getValue();
        if(dayOfWeekAdjustmentDays < 0) {
            dayOfWeekAdjustmentDays += 7;
        }
        return dayOfWeekAdjustmentDays;
    }
<<<<<<< Updated upstream
=======

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
>>>>>>> Stashed changes
}
