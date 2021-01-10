package me.tazadejava.incremental.logic.tasks;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import me.tazadejava.incremental.logic.customserializers.LocalDateAdapter;
import me.tazadejava.incremental.logic.customserializers.LocalDateTimeAdapter;
import me.tazadejava.incremental.logic.taskmodifiers.Group;

public class TaskManager {

    //if the day changes while on this app, maybe do something?

    private static class TaskManagerSaveFileTask extends AsyncTask<String, Void, Void> {

        private TaskManager taskManager;
        private boolean savePersistentData;
        private TimePeriod[] saveTimePeriods;

        public TaskManagerSaveFileTask(TaskManager taskManager, boolean savePersistentData, TimePeriod[] saveTimePeriods) {
            this.taskManager = taskManager;
            this.savePersistentData = savePersistentData;
            this.saveTimePeriods = saveTimePeriods;
        }

        @Override
        protected Void doInBackground(String... fileDir) {
            try {
                File dataFolder = new File(fileDir[0] + "/data/");

                if(!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }

                if(savePersistentData) {
                    File file = new File(dataFolder.getAbsolutePath() + "/" + "persistentData.json.TMP");

                    if(!file.exists()) {
                        file.createNewFile();
                    }

                    FileWriter writer = new FileWriter(file);

                    JsonObject main = new JsonObject();

                    JsonArray groupData = new JsonArray();
                    for(Group group : taskManager.allPersistentGroups.values()) {
                        groupData.add(group.save());
                    }
                    main.add("groupData", groupData);

                    JsonArray timePeriodData = new JsonArray();

                    for(TimePeriod period : taskManager.timePeriods) {
                        JsonObject data = period.saveTimePeriodInfo(taskManager.gson);

                        if(period.isInTimePeriod(LocalDate.now())) {
                            data.addProperty("current", true);
                        }

                        timePeriodData.add(data);
                    }

                    main.add("timePeriodData", timePeriodData);

                    taskManager.gson.toJson(main, writer);

                    writer.close();

                    //create temp file just in case something fails
                    File permFile = new File(dataFolder.getAbsolutePath() + "/" + "persistentData.json");
                    Files.copy(file.toPath(), permFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    file.delete();
                }

                for(TimePeriod saveTimePeriod : saveTimePeriods) {
                    saveTimePeriod.saveTaskData(taskManager.gson, dataFolder);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            taskManager = null;
            saveTimePeriods = null;

            return null;
        }
    }

    private TaskManagerSaveFileTask saveTasksAsync;

    private Gson gson;
    private String fileDir;

    private List<TimePeriod> timePeriods = new ArrayList<>();
    private TimePeriod currentTimePeriod;

    private HashMap<String, Group> allPersistentGroups = new HashMap<>();

    private Task currentlyEditingTask;

    public TaskManager(String fileDir) {
        gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .create();
        this.fileDir = fileDir;

        loadData();

        if(timePeriods.isEmpty()) {
            timePeriods.add(new TimePeriod(this, fileDir, gson, "", LocalDate.now(), null));
            currentTimePeriod = timePeriods.get(0);
        }
    }

    /**
     * Adds generated task and processes it for activation, if relevant
     * @param taskGenerator
     */
    public void addNewGeneratedTask(TaskGenerator taskGenerator) {
        currentTimePeriod.addNewTaskGenerator(taskGenerator);

        saveData(false, currentTimePeriod);
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

        TimePeriod newTimePeriod = new TimePeriod(this, fileDir, gson, timePeriod, startDate, endDate);
        timePeriods.add(newTimePeriod);
        currentTimePeriod = newTimePeriod;

        saveData(true, newTimePeriod);
        return true;
    }

    public boolean doesPersistentGroupExist(String name) {
        return allPersistentGroups.containsKey(name);
    }

    public boolean addNewPersistentGroup(String name) {
        if(allPersistentGroups.containsKey(name)) {
            return false;
        }

        allPersistentGroups.put(name, new Group(name));

        saveData(true);

        return true;
    }

    public boolean hasTimePeriodExpired() {
        return !currentTimePeriod.isInTimePeriod(LocalDate.now());
    }

    public List<String> getAllCurrentGroupNames() {
        List<String> names = new ArrayList<>();

        for(Group group : currentTimePeriod.getAllGroups()) {
            names.add(group.getGroupName());
        }

        for(Group group : allPersistentGroups.values()) {
            names.add(group.getGroupName());
        }

        return names;
    }

    public Collection<Group> getAllCurrentGroups(TimePeriod timePeriod) {
        return getAllGroupsHashed(timePeriod).values();
    }

    public HashMap<String, Group> getAllGroupsHashed(TimePeriod timePeriod) {
        HashMap<String, Group> groups = new HashMap<>();

        groups.putAll(timePeriod.getAllGroupsHashed());
        groups.putAll(allPersistentGroups);

        return groups;
    }

    /**
     * Returns null if global. Time period otherwise.
     *
     * Order of scoping checks:
     *
     * 1) Current time period
     * 2) Global scope
     * 3) Past time periods
     * @param group
     * @return
     */
    public TimePeriod getGroupScope(Group group) {
        if(currentTimePeriod.doesGroupExist(group.getGroupName())) {
            return currentTimePeriod;
        } else {
            if(allPersistentGroups.containsValue(group)) {
                return null;
            }

            for (TimePeriod period : timePeriods) {
                if(period == currentTimePeriod) {
                    continue;
                }

                if (period.doesGroupExist(group.getGroupName())) {
                    return period;
                }
            }

            return null;
        }
    }

    /**
     * first checks the time period, then checks the persistent
     * @param name
     * @return null if not exists
     */
    public Group getCurrentGroupByName(String name) {
        return currentTimePeriod.getGroupByName(name);
    }

    public Group getPersistentGroupByName(String name) {
        return allPersistentGroups.getOrDefault(name, null);
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
        currentTimePeriod.completeTask(task);

        saveData(false, currentTimePeriod);
    }

    public void setActiveEditTask(Task task) {
        currentlyEditingTask = task;
    }

    public Task getActiveEditTask() {
        return currentlyEditingTask;
    }

    public void loadData() {
        try {
            File dataFolder = new File(fileDir + "/data/");

            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dataFile = new File(dataFolder.getAbsolutePath() + "/" + "persistentData.json");
            if(dataFile.exists()) {
                FileReader reader = new FileReader(dataFile);
                JsonObject data = gson.fromJson(reader, JsonObject.class);
                reader.close();

                JsonArray groupData = data.getAsJsonArray("groupData");

                for(JsonElement group : groupData) {
                    Group groupObject = new Group(group.getAsJsonObject());
                    allPersistentGroups.put(groupObject.getGroupName(), groupObject);
                }

                JsonArray timePeriodData = data.getAsJsonArray("timePeriodData");

                for(JsonElement timePeriod : timePeriodData) {
                    JsonObject timePeriodObject = timePeriod.getAsJsonObject();

                    TimePeriod period = new TimePeriod(this, timePeriodObject, gson, dataFolder, timePeriodObject.has("current"));

                    if(timePeriodObject.has("current")) {
                        currentTimePeriod = period;
                    }

                    timePeriods.add(period);
                }

                if(currentTimePeriod == null) {
                    currentTimePeriod = timePeriods.get(timePeriods.size() - 1);
                }

                for(TimePeriod timePeriod : timePeriods) {
                    timePeriod.initializeStatsManager(gson, fileDir);
                }

                if(currentTimePeriod.getWorkPreferences().updateData()) {
                    saveData(true);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean updateGroupName(Group group, String name) {
        if(allPersistentGroups.containsKey(name)) {
            return false;
        }

        allPersistentGroups.remove(group.getGroupName());
        group.setGroupName(name);
        allPersistentGroups.put(name, group);

        currentTimePeriod.refreshAfterGroupAttributeChange();

        saveAllData();

        return true;
    }

    public boolean deleteGroup(Group group) {
        if(allPersistentGroups.containsKey(group.getGroupName())) {

            for(Task task : currentTimePeriod.getAllTasksByGroup(group)) {
                currentTimePeriod.deleteTaskCompletely(task);
            }

            currentTimePeriod.getStatsManager().deleteGroupStats(group);

            allPersistentGroups.remove(group.getGroupName());
            saveAllData();
            return true;
        } else {
            return currentTimePeriod.deleteGroup(group);
        }
    }

    public void verifyDayChangeReset() {
        currentTimePeriod.verifyDayChangeReset();
        saveData(true, currentTimePeriod);
    }

    public void setCurrentTimePeriod(TimePeriod timePeriod) {
        if(!timePeriod.hasLoadedTaskData()) {
            timePeriod.loadInactiveTimePeriodTaskData();
        }

        currentTimePeriod = timePeriod;
    }

    public boolean isCurrentTimePeriodActive() {
        return currentTimePeriod.isInTimePeriod(LocalDate.now());
    }

    public List<TimePeriod> getTimePeriods() {
        List<TimePeriod> timePeriodsRevised = new ArrayList<>();

        for(TimePeriod timePeriod : timePeriods) {
            if(timePeriod.getEndDate() != null) {
                timePeriodsRevised.add(timePeriod);
            }
        }

        return timePeriodsRevised;
    }

    /**
     * Returns whether an active time period exists at all
     * @return
     */
    public boolean existsActiveTimePeriod() {
        LocalDate now = LocalDate.now();
        for(int i = timePeriods.size() - 1; i >= 0; i--) {
            if(timePeriods.get(i).isInTimePeriod(now)) {
                return true;
            }
        }

        return false;
    }

    public void saveAllData() {
        saveData(true, timePeriods.toArray(new TimePeriod[0]));
    }

    public void saveData(boolean savePersistentData, TimePeriod... saveTimePeriods) {
        if(saveTasksAsync != null) {
            saveTasksAsync.cancel(true);
        }

        saveTasksAsync = new TaskManagerSaveFileTask(this, savePersistentData, saveTimePeriods);
        saveTasksAsync.execute(fileDir);
    }

    public Gson getGson() {
        return gson;
    }

    public String getFileDir() {
        return fileDir;
    }
}
