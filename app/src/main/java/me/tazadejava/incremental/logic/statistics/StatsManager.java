package me.tazadejava.incremental.logic.statistics;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

public class StatsManager {

    //things to store:
    //total hours worked by day (count by looking at the start date, not the end date)
    //  track by looking at incremented and completed hours
    //total hours worked by group by day

    private static class SaveDataAsyncTask extends AsyncTask<Void, Void, Void> {

        private StatsManager statsManager;

        public SaveDataAsyncTask(StatsManager statsManager) {
            this.statsManager = statsManager;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            File timePeriodFolder = new File(IncrementalApplication.filesDir + "/data/" + statsManager.timePeriod.getTimePeriodID() + "/");

            if(!timePeriodFolder.exists()) {
                timePeriodFolder.mkdirs();
            }

            File statsFile = new File(timePeriodFolder.getAbsolutePath() + "/statistics.json");

            try {
                if(!statsFile.exists()) {
                    statsFile.createNewFile();
                }

                FileWriter writer = new FileWriter(statsFile);

                JsonObject data = new JsonObject();

                JsonObject totalHoursByGroup = new JsonObject();

                for(LocalDate key : statsManager.totalHoursWorkedByGroup.keySet()) {
                    JsonObject groupHours = new JsonObject();

                    for(Group group : statsManager.totalHoursWorkedByGroup.get(key).keySet()) {
                        groupHours.addProperty(group.getGroupName(), statsManager.totalHoursWorkedByGroup.get(key).get(group));
                    }

                    totalHoursByGroup.add(key.toString(), groupHours);
                }

                data.add("totalHoursWorkedByGroup", totalHoursByGroup);

                JsonObject totalTasksByGroup = new JsonObject();

                for(LocalDate key : statsManager.totalTasksCompletedByGroup.keySet()) {
                    JsonObject groupCount = new JsonObject();

                    for(Group group : statsManager.totalTasksCompletedByGroup.get(key).keySet()) {
                        groupCount.addProperty(group.getGroupName(), statsManager.totalTasksCompletedByGroup.get(key).get(group));
                    }

                    totalTasksByGroup.add(key.toString(), groupCount);
                }

                data.add("totalTasksCompletedByGroup", totalTasksByGroup);

                statsManager.gson.toJson(data, writer);

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private Gson gson;
    private TimePeriod timePeriod;

    private SaveDataAsyncTask asyncTask;

    private HashMap<LocalDate, HashMap<Group, Float>> totalHoursWorkedByGroup = new HashMap<>();
    private HashMap<LocalDate, HashMap<Group, Integer>> totalTasksCompletedByGroup = new HashMap<>();

    public StatsManager(Gson gson, TimePeriod timePeriod) {
        this.gson = gson;
        this.timePeriod = timePeriod;
    }

    //load data
    public StatsManager(Gson gson, TimePeriod timePeriod, HashMap<String, Group> groups) {
        this(gson, timePeriod);

        try {
            File timePeriodFolder = new File(IncrementalApplication.filesDir + "/data/" + timePeriod.getTimePeriodID() + "/");
            File statsFile = new File(timePeriodFolder.getAbsolutePath() + "/statistics.json");

            if(statsFile.exists()) {
                FileReader reader = new FileReader(statsFile);
                JsonObject data = gson.fromJson(reader, JsonObject.class);
                reader.close();

                JsonObject totalHoursByGroup = data.getAsJsonObject("totalHoursWorkedByGroup");

                for(Map.Entry<String, JsonElement> dates : totalHoursByGroup.entrySet()) {
                    HashMap<Group, Float> map = new HashMap<>();

                    JsonObject groupInfo = dates.getValue().getAsJsonObject();
                    for(Map.Entry<String, JsonElement> groupHours : groupInfo.entrySet()) {
                        map.put(groups.get(groupHours.getKey()), groupHours.getValue().getAsFloat());
                    }

                    totalHoursWorkedByGroup.put(LocalDate.parse(dates.getKey()), map);
                }

                JsonObject totalTasksByGroup = data.getAsJsonObject("totalTasksCompletedByGroup");

                for(Map.Entry<String, JsonElement> dates : totalTasksByGroup.entrySet()) {
                    HashMap<Group, Integer> map = new HashMap<>();

                    JsonObject groupInfo = dates.getValue().getAsJsonObject();
                    for(Map.Entry<String, JsonElement> groupCount : groupInfo.entrySet()) {
                        map.put(groups.get(groupCount.getKey()), groupCount.getValue().getAsInt());
                    }

                    totalTasksCompletedByGroup.put(LocalDate.parse(dates.getKey()), map);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //save to a file
    public void saveData() {
        if(asyncTask != null) {
            asyncTask.cancel(true);
        }

        asyncTask = new SaveDataAsyncTask(this);
        asyncTask.execute();
    }

    public void appendHours(Group group, LocalDate date, float hours, boolean finishedTask) {
        if(!totalHoursWorkedByGroup.containsKey(date)) {
            totalHoursWorkedByGroup.put(date, new HashMap<>());
        }

        if(!totalHoursWorkedByGroup.get(date).containsKey(group)) {
            totalHoursWorkedByGroup.get(date).put(group, 0F);
        }

        totalHoursWorkedByGroup.get(date).put(group, totalHoursWorkedByGroup.get(date).get(group) + hours);

        if(finishedTask) {
            if(!totalTasksCompletedByGroup.containsKey(date)) {
                totalTasksCompletedByGroup.put(date, new HashMap<>());
            }

            if(!totalTasksCompletedByGroup.get(date).containsKey(group)) {
                totalTasksCompletedByGroup.get(date).put(group, 0);
            }

            totalTasksCompletedByGroup.get(date).put(group, totalTasksCompletedByGroup.get(date).get(group) + 1);
        }

        saveData();
    }

    public float getHoursWorked(LocalDate date) {
        float hours = 0;

        if(totalHoursWorkedByGroup.containsKey(date)) {
            for(Group group : totalHoursWorkedByGroup.get(date).keySet()) {
                hours += totalHoursWorkedByGroup.get(date).get(group);
            }
        }

        return hours;
    }

    public float getHoursWorkedByGroup(Group group, LocalDate date) {
        if(totalHoursWorkedByGroup.containsKey(date)) {
            if(totalHoursWorkedByGroup.get(date).containsKey(group)) {
                return totalHoursWorkedByGroup.get(date).get(group);
            }
        }

        return 0;
    }
}
