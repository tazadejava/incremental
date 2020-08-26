package me.tazadejava.incremental.logic.tasks;

import android.os.AsyncTask;
import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import me.tazadejava.incremental.logic.customserializers.LocalDateAdapter;
import me.tazadejava.incremental.logic.customserializers.LocalDateTimeAdapter;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

public class TaskManager {

    //if the day changes while on this app, maybe do something?

    private static class TaskManagerSaveFileTask extends AsyncTask<Void, Void, Void> {

        private TaskManager taskManager;
        private boolean savePersistentData;
        private TimePeriod[] saveTimePeriods;

        public TaskManagerSaveFileTask(TaskManager taskManager, boolean savePersistentData, TimePeriod[] saveTimePeriods) {
            this.taskManager = taskManager;
            this.savePersistentData = savePersistentData;
            this.saveTimePeriods = saveTimePeriods;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                File dataFolder = new File(IncrementalApplication.filesDir + "/data/");

                if(!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }

                if(savePersistentData) {
                    File file = new File(dataFolder.getAbsolutePath() + "/" + "persistentData.json");

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

                        if(period == taskManager.currentTimePeriod) {
                            data.addProperty("current", true);
                        }

                        timePeriodData.add(data);
                    }

                    main.add("timePeriodData", timePeriodData);

                    taskManager.gson.toJson(main, writer);

                    writer.close();
                }

                for(TimePeriod saveTimePeriod : saveTimePeriods) {
                    saveTimePeriod.saveTaskData(taskManager.gson, dataFolder);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private TaskManagerSaveFileTask saveTasksAsync;

    private Gson gson;

    private List<TimePeriod> timePeriods = new ArrayList<>();
    private TimePeriod currentTimePeriod;

    private HashMap<String, Group> allPersistentGroups = new HashMap<>();

    private Task currentlyEditingTask;

    public TaskManager() {
        gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .create();

        loadData();

        if(timePeriods.isEmpty()) {
            timePeriods.add(new TimePeriod(this, gson, "", LocalDate.now(), null));
            currentTimePeriod = timePeriods.get(0);
        }
    }

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

        TimePeriod newTimePeriod = new TimePeriod(this, gson, timePeriod, startDate, endDate);
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

    public Collection<Group> getAllCurrentGroups() {
        return getAllCurrentGroupsHashed().values();
    }

    public HashMap<String, Group> getAllCurrentGroupsHashed() {
        HashMap<String, Group> groups = new HashMap<>();

        groups.putAll(currentTimePeriod.getAllGroupsHashed());
        groups.putAll(allPersistentGroups);

        return groups;
    }

    /**
     * Returns null if global. Time period otherwise.
     * @param group
     * @return
     */
    public TimePeriod getGroupScope(Group group) {
        if(currentTimePeriod.doesGroupExist(group.getGroupName())) {
            return currentTimePeriod;
        } else {
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
            File dataFolder = new File(IncrementalApplication.filesDir + "/data/");

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

                    TimePeriod period = new TimePeriod(this, timePeriodObject, gson, dataFolder);

                    if(timePeriodObject.has("current")) {
                        currentTimePeriod = period;
                    }

                    timePeriods.add(period);
                }

                for(TimePeriod timePeriod : timePeriods) {
                    timePeriod.initializeStatsManager(gson);
                }

                if(currentTimePeriod.getWorkPreferences().checkForWeekChanges()) {
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

        saveAllData();

        return true;
    }

    public List<TimePeriod> getTimePeriods() {
        return timePeriods;
    }

    public void saveAllData() {
        saveData(true, timePeriods.toArray(new TimePeriod[0]));
    }

    public void saveData(boolean savePersistentData, TimePeriod... saveTimePeriods) {
        if(saveTasksAsync != null) {
            saveTasksAsync.cancel(true);
        }

        saveTasksAsync = new TaskManagerSaveFileTask(this, savePersistentData, saveTimePeriods);
        saveTasksAsync.execute();
    }
}
