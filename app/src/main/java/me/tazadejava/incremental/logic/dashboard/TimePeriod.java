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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import me.tazadejava.incremental.logic.tasks.NonrepeatingTask;
import me.tazadejava.incremental.logic.tasks.RepeatingTask;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskGenerator;
import me.tazadejava.incremental.logic.tasks.TaskManager;

public class TimePeriod {

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

        tasksByDay = new List[7];

        for (int i = 0; i < 7; i++) {
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
                generators.add(NonrepeatingTask.createInstance(gson, taskManager, generatorObject));
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
        while(iterator.hasNext()) {
            TaskGenerator generator = iterator.next();

            if(generator.hasGeneratorCompletedAllTasks()) {
                allCompletedTaskGenerators.add(generator);
                iterator.remove();
                continue;
            }

            processPendingTasks(generator);
        }
    }

    public void addNewTaskGenerator(TaskGenerator taskGenerator) {
        processPendingTasks(taskGenerator);

        allTaskGenerators.add(taskGenerator);
    }

    public void completeTask(Task task) {
        allTasks.remove(task);
        allCompletedTasks.add(task);
    }

    public float getEstimatedHoursOfWorkForDate(LocalDate date) {
        int deltaDays = (int) ChronoUnit.DAYS.between(LocalDate.now(), date);

        if(deltaDays < 0 || deltaDays > 7) {
            return -1;
        } else {
            int estimatedHours = 0;

            for(Task task : tasksByDay[deltaDays]) {
                estimatedHours += task.getTodaysHoursOfWork();
            }

            return Math.round(estimatedHours * 2) / 2.0f;
        }
    }

    private void addTaskToDailyLists(Task task, boolean addToAllTasks) {
        if(addToAllTasks) {
            allTasks.add(task);
        }

        //sort task list
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            LocalDate taskDueDate = task.getDueDateTime().toLocalDate();

            if (taskDueDate.equals(date) || date.isBefore(taskDueDate)) {
                tasksByDay[i].add(task);
            }
        }
    }

    private void processPendingTasks(TaskGenerator generator) {
        Task[] generatedTasks = generator.getPendingTasks();

        if(generatedTasks.length > 0) {
            for(Task task : generatedTasks) {
                addTaskToDailyLists(task, true);
            }
        }
    }

    public Group getGroupByName(String name) {
        return groups.getOrDefault(name, taskManager.getGroupByName(name));
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

    public void extendEndDate(int days) {
        endDate = endDate.plusDays(days);
    }

    public List<Task> getTasksByDay(int index) {
        if(index < 0 || index >= 7) {
            return null;
        }

        return tasksByDay[index];
    }
}
