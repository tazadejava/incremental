package me.tazadejava.incremental.logic.dashboard;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
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

import me.tazadejava.incremental.logic.tasks.NonrepeatingTask;
import me.tazadejava.incremental.logic.tasks.RepeatingTask;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskGenerator;
import me.tazadejava.incremental.logic.tasks.TaskManager;

public class TimePeriod {

    public static final int DAILY_LOGS_AHEAD_COUNT = 7;

    private TaskManager taskManager;

    private String timePeriodName, timePeriodID;
    private LocalDate beginDate, endDate;

    private List<TaskGenerator> allTaskGenerators = new ArrayList<>();
    private List<TaskGenerator> allCompletedTaskGenerators = new ArrayList<>();

    private List<Task> allTasks = new ArrayList<>();
    private List<Task> allCompletedTasks = new ArrayList<>();

    private HashMap<String, Group> groups = new HashMap<>();

    //size of 7, representing index in allTasks for each day; value of -1 means the day is nonexistent
    private List<Task>[] tasksByDay;

    private TimePeriod(TaskManager taskManager) {
        this.taskManager = taskManager;

        tasksByDay = new List[DAILY_LOGS_AHEAD_COUNT];

        for (int i = 0; i < tasksByDay.length; i++) {
            tasksByDay[i] = new ArrayList<>();
        }
    }

    public TimePeriod(TaskManager taskManager, String name, LocalDate beginDate, LocalDate endDate) {
        this(taskManager);
        this.timePeriodName = name;
        this.beginDate = beginDate;
        this.endDate = endDate;

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

        loadTaskData(taskManager, gson, dataFolder);
    }

    //TODO: DON"T ALWAYS LOAD THE OLD TASKS
    private void loadTaskData(TaskManager taskManager, Gson gson, File dataFolder) {
        try {
            File timePeriodFolder = new File(dataFolder.getAbsolutePath() + "/" + timePeriodID + "/");

            if(timePeriodFolder.exists()) {
                File currentTasksFile = new File(timePeriodFolder + "/currentTasks.json");
                loadTasksFromFile(taskManager, gson, currentTasksFile, allTaskGenerators, allTasks);

                for(Task task : allTasks) {
                    addTaskToDailyLists(task, false);
                }

                File completedTasksFile = new File(timePeriodFolder + "/completedTasks.json");
                loadTasksFromFile(taskManager, gson, completedTasksFile, allCompletedTaskGenerators, allCompletedTasks);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JsonObject saveTimePeriodInfo() {
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

        return data;
    }

    public void saveTaskData(Gson gson, File dataFolder) {
        try {
            File timePeriodFolder = new File(dataFolder.getAbsolutePath() + "/" + timePeriodID + "/");

            if(!timePeriodFolder.exists()) {
                timePeriodFolder.mkdirs();
            }

            File currentTasksFile = new File(timePeriodFolder + "/currentTasks.json");
            saveTasksToFile(gson, currentTasksFile, allTaskGenerators, allTasks);

            File completedTasksFile = new File(timePeriodFolder + "/completedTasks.json");
            saveTasksToFile(gson, completedTasksFile, allCompletedTaskGenerators, allCompletedTasks);
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

        JsonArray generatorsObject = data.getAsJsonArray("generators");

        for(JsonElement generator : generatorsObject) {
            JsonObject generatorObject = generator.getAsJsonObject();

            if(generatorObject.has("generatorType") && generatorObject.get("generatorType").getAsString().equals("repeating")) {
                generators.add(RepeatingTask.createInstance(gson, taskManager, this, generatorObject));
            } else {
                generators.add(NonrepeatingTask.createInstance(gson, taskManager, this, generatorObject));
            }
        }

        JsonArray tasksObject = data.getAsJsonArray("tasks");

        for(JsonElement task : tasksObject) {
            JsonObject taskObject = task.getAsJsonObject();
            TaskGenerator parent = null;

            String id = taskObject.get("parent").getAsString();
            for(TaskGenerator generator : generators) {
                if(generator.getInstanceReference().equals(id)) {
                    parent = generator;
                    break;
                }
            }

            if(parent != null) {
                Task loadedTask = Task.createInstance(gson, taskObject, parent, this);
                tasks.add(loadedTask);
                parent.loadLatestTaskFromFile(loadedTask);
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

        JsonArray tasksObject = new JsonArray();

        for(Task task : tasks) {
            tasksObject.add(task.save(gson));
        }

        data.add("tasks", tasksObject);

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
        allTasks.remove(task);
        allCompletedTasks.add(task);

        removeTaskFromDailyLists(task);
    }

    public void removeTask(Task task) {
        allTasks.remove(task);
        removeTaskFromDailyLists(task);
    }

    public int removeTaskByParent(TaskGenerator parent) {
        int removed = 0;
        Iterator<Task> allTasksIterator = allTasks.iterator();
        while(allTasksIterator.hasNext()) {
            Task task = allTasksIterator.next();

            if(task.getParent() == parent) {
                removed++;
                allTasksIterator.remove();
                removeTaskFromDailyLists(task);
            }
        }

        return removed;
    }

    public float getEstimatedHoursOfWorkForDate(LocalDate date) {
        int deltaDays = (int) ChronoUnit.DAYS.between(LocalDate.now(), date);

        if(deltaDays < 0 || deltaDays > tasksByDay.length) {
            return -1;
        } else {
            float estimatedHours = 0;

            for(Task task : tasksByDay[deltaDays]) {
                estimatedHours += task.getDayHoursOfWorkTotal(date, false);
            }

            return Math.round(estimatedHours * 2.0f) / 2.0f;
        }
    }

    private void removeTaskFromDailyLists(Task task) {
        for (int i = 0; i < tasksByDay.length; i++) {
            tasksByDay[i].remove(task);
        }
    }

    private void addTaskToDailyLists(Task task, boolean addToTasksList) {
        addTaskToDailyLists(task, addToTasksList, LocalDate.now(), task.getDueDateTime().toLocalDate());
    }

    private void addTaskToDailyLists(Task task, boolean addToTasksList, LocalDate taskStartDate, LocalDate taskDueDate) {
        if(addToTasksList) {
            allTasks.add(task);
        }

        //place respectively in the daily task list
        for (int i = 0; i < tasksByDay.length; i++) {
            LocalDate date = LocalDate.now().plusDays(i);

            if(taskStartDate.isAfter(date)) {
                continue;
            }

            if (date.equals(taskDueDate) || date.isBefore(taskDueDate) || (i == 0 && taskDueDate.isBefore(date))) {
                tasksByDay[i].add(task);
            }
        }
    }

    public boolean processPendingTasks(TaskGenerator generator) {
        Task[] generatedTasks = generator.getPendingTasks();

        if(generatedTasks.length > 0) {
            for(Task task : generatedTasks) {
                addTaskToDailyLists(task, true);
            }
        }

        //add an upcoming task, if applicable, to the weekly commitment list

        Task upcomingTask = generator.getNextUpcomingTask();
        if(upcomingTask != null) {
            LocalDate upcomingTaskStartDate = generator.getNextUpcomingTaskStartDate();
            LocalDate upcomingTaskDueDate = upcomingTask.getDueDateTime().toLocalDate();

            upcomingTask.setStartDate(upcomingTaskStartDate);

            addTaskToDailyLists(upcomingTask, false, upcomingTaskStartDate, upcomingTaskDueDate);
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

        return tasksByDay[index];
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

        return count;
    }
}
