package me.tazadejava.incremental.logic.taskmodifiers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.tasks.NonrepeatingTask;
import me.tazadejava.incremental.logic.tasks.RepeatingTask;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskGenerator;
import me.tazadejava.incremental.logic.tasks.TaskManager;

public class TimePeriod {

    public static final int DAILY_LOGS_AHEAD_COUNT = 6;

    private TaskManager taskManager;

    private StatsManager statsManager;

    private String timePeriodName, timePeriodID;
    private LocalDate beginDate, endDate;

    private GlobalTaskWorkPreference workPreferences;

    private List<TaskGenerator> allTaskGenerators = new ArrayList<>();
    private List<TaskGenerator> allCompletedTaskGenerators = new ArrayList<>();

    //tasks that are to do today; represents all tasks (as well as overdue ones)
    private List<Task> allActiveTasks = new ArrayList<>();
    private HashMap<String, Group> groups = new HashMap<>();

    //representing index in allTasks for each day; INDEX 0 IS TOMORROW; does not save and is recreated each time
    private List<Task>[] tasksByDay;

    private TimePeriod(TaskManager taskManager) {
        this.taskManager = taskManager;

        tasksByDay = new List[DAILY_LOGS_AHEAD_COUNT];

        for (int i = 0; i < tasksByDay.length; i++) {
            tasksByDay[i] = new ArrayList<>();
        }
    }

    public TimePeriod(TaskManager taskManager, Gson gson, String name, LocalDate beginDate, LocalDate endDate) {
        this(taskManager);
        this.timePeriodName = name;
        this.beginDate = beginDate;
        this.endDate = endDate;

        statsManager = new StatsManager(gson, this);

        workPreferences = new GlobalTaskWorkPreference();

        timePeriodID = UUID.randomUUID().toString().replace("-", "") + beginDate.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    //load from file
    //TODO: DON'T ALWAYS LOAD THE TASKS IF NOT THE CURRENT TASK
    public TimePeriod(TaskManager taskManager, JsonObject data, Gson gson, File dataFolder) {
        this(taskManager);

        //load time period info
        timePeriodName = data.get("timePeriodName").getAsString();
        timePeriodID = data.get("timePeriodID").getAsString();

        if(data.has("beginDate")) {
            beginDate = LocalDate.parse(data.get("beginDate").getAsString());
        }
        if(data.has("endDate")) {
            endDate = LocalDate.parse(data.get("endDate").getAsString());
        }

        if(data.has("groupData")) {
            JsonArray groupData = data.getAsJsonArray("groupData");

            for(JsonElement group : groupData) {
                Group groupObject = new Group(group.getAsJsonObject());
                groups.put(groupObject.getGroupName(), groupObject);
            }
        }

        if(data.has("workPreferences")) {
            workPreferences = gson.fromJson(data.get("workPreferences"), GlobalTaskWorkPreference.class);
        } else {
            workPreferences = new GlobalTaskWorkPreference();
        }

        loadTaskData(taskManager, gson, dataFolder);
    }

    /**
     * Run after all time periods have been defined
     * @param gson
     */
    public void initializeStatsManager(Gson gson) {
        statsManager = new StatsManager(gson, this, taskManager.getAllCurrentGroupsHashed());
    }

    //TODO: DON"T ALWAYS LOAD THE OLD TASKS
    private void loadTaskData(TaskManager taskManager, Gson gson, File dataFolder) {
        try {
            File timePeriodFolder = new File(dataFolder.getAbsolutePath() + "/" + timePeriodID + "/");

            if(timePeriodFolder.exists()) {
                File currentTasksFile = new File(timePeriodFolder + "/currentTasks.json");
                loadTasksFromFile(taskManager, gson, currentTasksFile, allTaskGenerators, allActiveTasks);

                //load preview tasks from the generator and add any new pending tasks
                for(TaskGenerator generator : allTaskGenerators) {
                    processPendingTasks(generator);
                }

                //add active tasks to the daily lists
                for(Task task : allActiveTasks) {
                    addTaskToDailyLists(task);
                }

                File completedTasksFile = new File(timePeriodFolder + "/completedTasks.json");
                loadTasksFromFile(taskManager, gson, completedTasksFile, allCompletedTaskGenerators, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JsonObject saveTimePeriodInfo(Gson gson) {
        JsonObject data = new JsonObject();

        data.addProperty("timePeriodName", timePeriodName);
        data.addProperty("timePeriodID", timePeriodID);

        if(beginDate != null) {
            data.addProperty("beginDate", beginDate.toString());
        }
        if(endDate != null) {
            data.addProperty("endDate", endDate.toString());
        }

        if(!groups.isEmpty()) {
            JsonArray groupData = new JsonArray();

            for(Group group : groups.values()) {
                groupData.add(group.save());
            }

            data.add("groupData", groupData);
        }

        data.add("workPreferences", gson.toJsonTree(workPreferences));

        return data;
    }

    public void saveTaskData(Gson gson, File dataFolder) {
        try {
            File timePeriodFolder = new File(dataFolder.getAbsolutePath() + "/" + timePeriodID + "/");

            if(!timePeriodFolder.exists()) {
                timePeriodFolder.mkdirs();
            }

            File currentTasksFile = new File(timePeriodFolder + "/currentTasks.json");
            saveTasksToFile(gson, currentTasksFile, allTaskGenerators, allActiveTasks);

            File completedTasksFile = new File(timePeriodFolder + "/completedTasks.json");
            saveTasksToFile(gson, completedTasksFile, allCompletedTaskGenerators, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTasksFromFile(TaskManager taskManager, Gson gson, File file, List<TaskGenerator> generators, List<Task> tasks) throws IOException {
        if(!file.exists()) {
            return;
        }

        FileReader reader = new FileReader(file);
        JsonObject data = gson.fromJson(reader, JsonObject.class);
        reader.close();

        HashMap<String, Task> tasksList = new HashMap<>();

        //load generators, then store tasks in a list
        JsonArray generatorsObject = data.getAsJsonArray("generators");
        for(JsonElement generator : generatorsObject) {
            JsonObject generatorObject = generator.getAsJsonObject();

            TaskGenerator generatedTask;
            if(generatorObject.has("generatorType") && generatorObject.get("generatorType").getAsString().equals("repeating")) {
                generatedTask = RepeatingTask.createInstance(gson, taskManager, this, generatorObject);
            } else {
                generatedTask = NonrepeatingTask.createInstance(gson, taskManager, this, generatorObject);
            }

            generators.add(generatedTask);

            generatedTask.assignTasksToLoadingHashMap(tasksList);
        }

        if(tasks != null) {
            JsonArray tasksObject = data.getAsJsonArray("activeTasks");

            for (JsonElement taskID : tasksObject) {
                //assert: this call will never fail if implemented correctly
                tasks.add(tasksList.get(taskID.getAsString()));
            }
        }
    }

    private void saveTasksToFile(Gson gson, File file, List<TaskGenerator> generators, List<Task> tasks) throws IOException {
        if(!file.exists()) {
            file.createNewFile();
        }

        JsonObject data = new JsonObject();

        JsonArray generatorsObject = new JsonArray();

        for(TaskGenerator generator : generators) {
            generatorsObject.add(generator.save(gson));
        }

        data.add("generators", generatorsObject);

        if(tasks != null) {
            JsonArray tasksObject = new JsonArray();

            for (Task task : tasks) {
                tasksObject.add(task.getTaskID());
            }

            data.add("activeTasks", tasksObject);
        }

        FileWriter writer = new FileWriter(file);
        gson.toJson(data, writer);
        writer.close();
    }

    public void checkForPendingTasks() {
        Iterator<TaskGenerator> iterator = allTaskGenerators.iterator();
        boolean hasChanged = false;
        while(iterator.hasNext()) {
            TaskGenerator generator = iterator.next();

            if(generator.hasGeneratorCompletedAllTasks()) {
                allCompletedTaskGenerators.add(generator);
                iterator.remove();
                continue;
            }

            if(processPendingTasks(generator)) {
                hasChanged = true;
            }
        }

        if(hasChanged) {
            taskManager.saveData(false, this);
        }
    }

    public void addNewTaskGenerator(TaskGenerator taskGenerator) {
        processPendingTasks(taskGenerator);

        allTaskGenerators.add(taskGenerator);
    }

    public void completeTask(Task task) {
        allActiveTasks.remove(task);

        if(task.getParent().hasGeneratorCompletedAllTasks()) {
            allTaskGenerators.remove(task.getParent());
            allCompletedTaskGenerators.add(task.getParent());
        }

        removeTaskFromDailyLists(task);
    }

    /**
     * Removes and readds the task according to generator rules. used for nonrepeating task
     * @param task
     * @param generator
     */
    public void resetTask(Task task, TaskGenerator generator) {
        allActiveTasks.remove(task);
        removeTaskFromDailyLists(task);

        processPendingTasks(generator);
    }

    public void deleteTaskCompletely(Task task, TaskGenerator generator) {
        allActiveTasks.remove(task);
        removeTaskFromDailyLists(task);

        removeTaskByParent(generator);
        allTaskGenerators.remove(generator);
    }

    public List<Task> removeTaskByParent(TaskGenerator parent) {
        List<Task> removed = new ArrayList<>();

        Iterator<Task> allTasksIterator = allActiveTasks.iterator();
        while(allTasksIterator.hasNext()) {
            Task task = allTasksIterator.next();

            if(task.getParent() == parent) {
                removed.add(task);
                allTasksIterator.remove();
                removeTaskFromDailyLists(task);
            }
        }

        return removed;
    }

    public int getEstimatedMinutesOfWorkForDate(LocalDate date) {
        int deltaDays = (int) ChronoUnit.DAYS.between(LocalDate.now(), date);

        if(deltaDays < 0 || deltaDays > tasksByDay.length + 1) {
            return -1;
        } else {
            int estimatedMinutes = 0;

            List<Task> dayList = getTasksByDay(deltaDays);
            for(Task task : dayList) {
                estimatedMinutes += task.getTodaysMinutesLeft();
            }

            return estimatedMinutes;
        }
    }

    private void removeTaskFromDailyLists(Task task) {
        for (int i = 0; i < tasksByDay.length; i++) {
            tasksByDay[i].remove(task);
        }
    }

    private void addTaskToDailyLists(Task task) {
        addTaskToDailyLists(task, LocalDate.now(), task.getDueDateTime().toLocalDate());
    }

    private void addTaskToDailyLists(Task task, LocalDate taskStartDate, LocalDate taskDueDate) {
        //place respectively in the daily task list
        for (int i = 0; i < tasksByDay.length; i++) {
            LocalDate date = LocalDate.now().plusDays(1 + i);

            if(taskStartDate.isAfter(date)) {
                continue;
            }

            if (date.equals(taskDueDate) || date.isBefore(taskDueDate)) {
                if(!tasksByDay[i].contains(task)) {
                    addTaskInOrder(tasksByDay[i], task);
                }
            }
        }
    }

    private void addTaskInOrder(List<Task> list, Task task) {
        boolean added = false;
        //add by due date order first
        for(int i = 0; i < list.size(); i++) {
            if(task.getDueDateTime().isBefore(list.get(i).getDueDateTime())) {
                list.add(i, task);
                added = true;
                break;
            }
        }

        //then, add by creation order
        if(!added) {
            LocalDateTime creationTime = task.getParent().getCreationTime();
            for (int i = 0; i < list.size(); i++) {
                if (creationTime.isBefore(list.get(i).getParent().getCreationTime())) {
                    list.add(i, task);
                    added = true;
                    break;
                }
            }
        }

        //if all are equal, then just add at the end
        if(!added) {
            list.add(task);
        }
    }

    /**
     * Adds tasks that are needed to the allTasks list, and also adds preview classes for a generator
     * @param generator
     * @return
     */
    public boolean processPendingTasks(TaskGenerator generator) {
        Task[] generatedTasks = generator.getPendingTasks();

        if(generatedTasks.length > 0) {
            for(Task task : generatedTasks) {
                addTaskInOrder(allActiveTasks, task);
                addTaskToDailyLists(task);
            }
        }

        //add an upcoming task, if applicable, to the weekly commitment list

        Task upcomingTask = generator.getNextUpcomingTask();
        if(upcomingTask != null) {
            LocalDate upcomingTaskDueDate = upcomingTask.getDueDateTime().toLocalDate();

            addTaskToDailyLists(upcomingTask, upcomingTask.getStartDate(), upcomingTaskDueDate);
        }

        return generatedTasks.length > 0;
    }

    public boolean doesGroupExist(String name) {
        return groups.containsKey(name);
    }

    public Group getGroupByName(String name) {
        return groups.getOrDefault(name, taskManager.getPersistentGroupByName(name));
    }

    public void addNewGroup(String name) {
        if(groups.containsKey(name)) {
            return;
        }

        groups.put(name, new Group(name));

        taskManager.saveData(true, this);
    }

    public boolean updateGroupName(Group group, String name) {
        if(groups.containsKey(name)) {
            return false;
        }

        groups.remove(group.getGroupName());
        group.setGroupName(name);
        groups.put(name, group);

        taskManager.saveData(true, this);

        return true;
    }

    public Collection<Group> getAllGroups() {
        return groups.values();
    }

    public HashMap<String, Group> getAllGroupsHashed() {
        return groups;
    }

    public String getName() {
        return timePeriodName;
    }

    public boolean isInTimePeriod(LocalDate date) {
        if(beginDate == null || endDate == null) {
            return false;
        }

        return !date.isBefore(beginDate) && !date.isAfter(endDate);
    }

    public boolean isInTimePeriod(LocalDate start, LocalDate end) {
        if(beginDate == null || endDate == null) {
            return false;
        }

        return !beginDate.isBefore(start) && !endDate.isAfter(end);
    }

    public LocalDate getBeginDate() {
        return beginDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<Task> getTasksByDay(int index) {
        if(index < 0 || index >= tasksByDay.length) {
            return null;
        }

        if(index == 0) {
            List<Task> filtered = new ArrayList<>();

            for(Task task : allActiveTasks) {
                if(task.isDoneWithTaskToday()) {
                    continue;
                }
                if(workPreferences.isBlackedOutDay(LocalDate.now())) {
                    //check if there exists at least one day before the due date where the user can work on this task
                    boolean isPossibleToComplete = false;

                    if(ChronoUnit.DAYS.between(LocalDate.now(), task.getDueDateTime().toLocalDate()) > 7) {
                        isPossibleToComplete = true;
                    } else {
                        LocalDate date = LocalDate.now();
                        for (int i = 0; i < ChronoUnit.DAYS.between(date, task.getDueDateTime().toLocalDate()); i++) {
                            date = date.plusDays(1);

                            if (!workPreferences.isBlackedOutDay(date)) {
                                isPossibleToComplete = true;
                                break;
                            }
                        }
                    }

                    if(isPossibleToComplete) {
                        continue;
                    }
                }

                filtered.add(task);
            }

            return filtered;
        } else {
            List<Task> filtered = new ArrayList<>();

            for(Task task : tasksByDay[index - 1]) {
                if(workPreferences.isBlackedOutDay(LocalDate.now().plusDays(index))) {
                    continue;
                }

                filtered.add(task);
            }

            return filtered;
        }
    }

    public int getTasksCountThisWeekByGroup(Group group) {
        int count = 0;

        Set<Task> countedTasks = new HashSet<>();
        for(List<Task> tasks : tasksByDay) {
            for(Task task : tasks) {
                if(!countedTasks.contains(task)) {
                    countedTasks.add(task);

                    if(task.getGroup().equals(group)) {
                        count++;
                    }
                }
            }
        }

        for(Task task : allActiveTasks) {
            if(!countedTasks.contains(task)) {
                countedTasks.add(task);

                if(task.getGroup().equals(group)) {
                    count++;
                }
            }
        }

        return count;
    }

    public String getTimePeriodID() {
        return timePeriodID;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public GlobalTaskWorkPreference getWorkPreferences() {
        return workPreferences;
    }
}
