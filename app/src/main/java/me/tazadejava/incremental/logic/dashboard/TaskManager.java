package me.tazadejava.incremental.logic.dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import me.tazadejava.incremental.logic.gson_adapters.LocalDateAdapter;
import me.tazadejava.incremental.logic.gson_adapters.LocalDateTimeAdapter;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class TaskManager {

    public enum SaveType {
        CURRENT_TASKS, PAST_TASKS, MISC_DATA
    }

    private static final int DATA_FILES_VERSION = 1;

    //goal of this class: creates the days in which tasks are allocated; stores all the tasks

    private Gson gson;

    private List<Task> allTasks;
    private List<RepeatingTask> allRepeatingTasks;

    private HashMap<TimePeriod, List<Task>> completedTasksByTimePeriod;
    private HashMap<TimePeriod, List<RepeatingTask>> completedRepeatingTasksByTimePeriod;

    private TimePeriod currentTimePeriod;
    private List<TimePeriod> timePeriods = new ArrayList<>();

    private List<Group> allGroups = new ArrayList<>();

    public TaskManager() {
        allTasks = new ArrayList<>();
        allRepeatingTasks = new ArrayList<>();

        completedTasksByTimePeriod = new HashMap<>();
        completedRepeatingTasksByTimePeriod = new HashMap<>();

        timePeriods.add(new TimePeriod("", LocalDate.now(), null));
        currentTimePeriod = timePeriods.get(0);

        //TODO: WHEN TIME PERIODS IS "", ask user to define a time period w/ dialog

        gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .create();

        load();
    }

    public void addNewTask(Task task) {
        allTasks.add(task);

        allTasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2) {
                return o1.getDueDate().compareTo(o2.getDueDate());
            }
        });

        save(SaveType.CURRENT_TASKS);
    }

    public boolean isTimePeriodExpired() {
        return !currentTimePeriod.isInTimePeriod(LocalDate.now());
    }

    //renews the default time period to extend for an additional 7 days, until then it will notify again to create a new time period
    public void renewDefaultTimePeriod() {
        if(currentTimePeriod.getName().isEmpty()) {
            currentTimePeriod.extendEndDate(7);
        }
    }

    //return false if not allowed in some way: cannot be duplicate name, cannot end before it starts, cannot overlap with another date range
    //sets as current time period
    public boolean addNewTimePeriod(String timePeriod, LocalDate startDate, LocalDate endDate) {
        if(endDate.isBefore(startDate)) {
            return false;
        }

        for(TimePeriod period : timePeriods) {
            if(period.getName().equals(timePeriod)) {
                return false;
            }
            if(period.isInTimePeriod(startDate, endDate)) {
                return false;
            }
        }

        TimePeriod newTimePeriod = new TimePeriod(timePeriod, startDate, endDate);
        timePeriods.add(newTimePeriod);
        currentTimePeriod = newTimePeriod;

        save(SaveType.MISC_DATA);
        return true;
    }

    public void addNewRepeatingTask(RepeatingTask repeatingTask) {
        allRepeatingTasks.add(repeatingTask);

        if(repeatingTask.hasPendingTasks()) {
            for(Task task : repeatingTask.popAllPendingTasks()) {
                addNewTask(task);
            }
        }

        save(SaveType.CURRENT_TASKS);
    }

    public List<Task> getTasks() {
        return allTasks;
    }

    public List<String> getAllGroupNames() {
        List<String> names = new ArrayList<>();

        for(Group group : allGroups) {
            names.add(group.getGroupName());
        }

        return names;
    }

    public Group getGroupByName(String name) {
        for(Group group : allGroups) {
            if(group.getGroupName().equals(name)) {
                return group;
            }
        }

        return null;
    }

    public void addNewGroup(String name) {
        allGroups.add(new Group(name));

        save(SaveType.MISC_DATA);
    }

    private void addPendingRepeatingTasks() {
        for(RepeatingTask repeatingTask : allRepeatingTasks) {
            if(repeatingTask.hasPendingTasks()) {
                for(Task task : repeatingTask.popAllPendingTasks()) {
                    addNewTask(task);
                }
            }
        }
    }

    public TimePeriod getCurrentTimePeriod() {
        return currentTimePeriod;
    }

    public boolean doesTimePeriodExist(String name) {
        for(TimePeriod period : timePeriods) {
            if(period.getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    public void completeTask(Task task) {
        allTasks.remove(task);

        completedTasksByTimePeriod.putIfAbsent(currentTimePeriod, new ArrayList<>());
        completedTasksByTimePeriod.get(task.getTimePeriod()).add(task);

        save(SaveType.CURRENT_TASKS, SaveType.PAST_TASKS);
    }

    public void completeRepeatingTask(RepeatingTask task) {
        allRepeatingTasks.remove(task);

        completedRepeatingTasksByTimePeriod.putIfAbsent(currentTimePeriod, new ArrayList<>());
        completedRepeatingTasksByTimePeriod.get(task.getTimePeriod()).add(task);

        save(SaveType.CURRENT_TASKS, SaveType.PAST_TASKS);
    }

    public void load() {
        //TODO: get all tasks from the text files

//        addNewTask(new Task("Pset 3", LocalDateTime.now().plusDays(1), "", "Default", (int) (Math.random() * 5), getClassColor("")));
//        addNewTask(new Task("Finish Lab 1", LocalDateTime.now().plusDays(7), "", "Default", (int) (Math.random() * 5), getClassColor("")));
////        addNewTask(new Task("Clean Room", LocalDateTime.now().plusDays(13), "", "Default", (int) (Math.random() * 5), getClassColor("")));
//        addNewRepeatingTask(new RepeatingTask(new String[] {"A", "B"}, DayOfWeek.MONDAY, DayOfWeek.FRIDAY, LocalTime.now(), "", "", 0, 0));

        try {
            File dataFolder = new File(IncrementalApplication.filesDir + "/data/");

            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File miscDataFile = new File(dataFolder.getAbsolutePath() + "/data.json");
            if(miscDataFile.exists()) {
                FileReader reader = new FileReader(miscDataFile);
                JsonObject dataTree = gson.fromJson(reader, JsonObject.class);
                reader.close();

                JsonObject timePeriodsTree = (JsonObject) dataTree.get("timePeriods");

                currentTimePeriod = gson.fromJson(timePeriodsTree.get("currentTimePeriod"), TimePeriod.class);

                Type groupList = new TypeToken<List<Group>>() {}.getType();
                allGroups = gson.fromJson(dataTree.get("allGroups"), groupList);
            }

            File tasksFile = new File(dataFolder.getAbsolutePath() + "/tasks.json");
            if(tasksFile.exists()) {
                FileReader reader = new FileReader(tasksFile);
                JsonObject currentTaskTree = gson.fromJson(reader, JsonObject.class);
                reader.close();

                Type allTasksType = new TypeToken<List<Task>>() {}.getType();
                Type allRepeatingTasksType = new TypeToken<List<RepeatingTask>>() {}.getType();

                allTasks = gson.fromJson(currentTaskTree.get("allTasks"), allTasksType);
                allRepeatingTasks = gson.fromJson(currentTaskTree.get("allRepeatingTasks"), allRepeatingTasksType);
            }

            File pastTasksFile = new File(dataFolder.getAbsolutePath() + "/completed_tasks.json");
            if(pastTasksFile.exists()) {
                FileReader reader = new FileReader(pastTasksFile);
                JsonObject completedTasksMainTree = gson.fromJson(reader, JsonObject.class);
                reader.close();

                JsonObject completedTasks = (JsonObject) completedTasksMainTree.get("allTasks");
                JsonObject completedRepeatedTasks = (JsonObject) completedTasksMainTree.get("allRepeatingTasks");

                Type allTasksType = new TypeToken<HashMap<String, List<Task>>>() {}.getType();
                Type allRepeatingTasksType = new TypeToken<HashMap<String, List<RepeatingTask>>>() {}.getType();

                completedTasksByTimePeriod = gson.fromJson(completedTasks, allTasksType);
                completedRepeatingTasksByTimePeriod = gson.fromJson(completedRepeatedTasks, allRepeatingTasksType);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        addPendingRepeatingTasks();
    }

    private void updateFiles(int lastVersion) {
        if(lastVersion == -1) {

        }
    }

    private FileWriter getJsonFileWriter(File dataFolder, String name) throws IOException {
        File file = new File(dataFolder.getAbsolutePath() + "/" + name + ".json");

        if(!file.exists()) {
            file.createNewFile();
        }

        return new FileWriter(file);
    }

    public void save(SaveType... saveTypes) {
        List<SaveType> saveTypesList;
        if(saveTypes.length == 0) {
            saveTypesList = Arrays.asList(SaveType.values());
        } else {
            saveTypesList = Arrays.asList(saveTypes);
        }

        try {
            File dataFolder = new File(IncrementalApplication.filesDir + "/data/");

            if(!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            if(saveTypesList.contains(SaveType.CURRENT_TASKS)) {
                FileWriter writer = getJsonFileWriter(dataFolder, "tasks");

                JsonObject currentTaskTree = new JsonObject();
                currentTaskTree.add("allTasks", gson.toJsonTree(allTasks));
                currentTaskTree.add("allRepeatingTasks", gson.toJsonTree(allRepeatingTasks));
                gson.toJson(currentTaskTree, writer);

                writer.close();
            }

            if(saveTypesList.contains(SaveType.PAST_TASKS)) {
                FileWriter writer = getJsonFileWriter(dataFolder, "completed_tasks");

                JsonObject completedTasksMainTree = new JsonObject();
                completedTasksMainTree.add("allTasks", gson.toJsonTree(completedTasksByTimePeriod));
                completedTasksMainTree.add("allRepeatingTasks", gson.toJsonTree(completedRepeatingTasksByTimePeriod));
                gson.toJson(completedTasksMainTree, writer);

                writer.close();
            }

            if(saveTypesList.contains(SaveType.MISC_DATA)) {
                FileWriter writer = getJsonFileWriter(dataFolder, "data");

                JsonObject dataTree = new JsonObject();
                dataTree.add("version", gson.toJsonTree(DATA_FILES_VERSION));
                JsonObject timePeriodsTree = new JsonObject();

                timePeriodsTree.add("currentTimePeriod", gson.toJsonTree(currentTimePeriod));
                timePeriodsTree.add("timePeriods", gson.toJsonTree(timePeriods));

                dataTree.add("timePeriods", timePeriodsTree);
                dataTree.add("allGroups", gson.toJsonTree(allGroups));

                gson.toJson(dataTree, writer);

                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
