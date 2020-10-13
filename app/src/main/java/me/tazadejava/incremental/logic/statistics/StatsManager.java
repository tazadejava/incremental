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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TimePeriod;

public class StatsManager {

    //things to store:
    //total hours worked by day (count by looking at the start date, not the end date)
    //  track by looking at incremented and completed hours
    //total hours worked by group by day

    /**
     * Used to save stats for a particular group before changing a group's hashcode. MUST call restore to recover data.
     */
    public static class StatsGroupPacket {

        private StatsManager statsManager;

        private Group group;
        private HashMap<LocalDate, Integer> totalMinutes = new HashMap<>();
        private HashMap<LocalDate, Integer> totalTasks = new HashMap<>();

        public StatsGroupPacket(StatsManager statsManager, Group group) {
            this.statsManager = statsManager;
            this.group = group;

            //add to list and remove from stats

            for(LocalDate date : statsManager.totalMinutesWorkedByGroup.keySet()) {
                if(statsManager.totalMinutesWorkedByGroup.get(date).containsKey(group)) {
                    totalMinutes.put(date, statsManager.totalMinutesWorkedByGroup.get(date).get(group));
                    statsManager.totalMinutesWorkedByGroup.get(date).remove(group);
                }
            }

            for(LocalDate date : statsManager.totalTasksCompletedByGroup.keySet()) {
                if(statsManager.totalTasksCompletedByGroup.get(date).containsKey(group)) {
                    totalTasks.put(date, statsManager.totalTasksCompletedByGroup.get(date).get(group));
                    statsManager.totalTasksCompletedByGroup.get(date).remove(group);
                }
            }
        }

        /**
         * Restores data and saves the new group data name.
         */
        public void restore() {
            if(statsManager == null) {
                return;
            }

            for(LocalDate date : totalMinutes.keySet()) {
                statsManager.totalMinutesWorkedByGroup.get(date).put(group, totalMinutes.get(date));
            }

            for(LocalDate date : totalTasks.keySet()) {
                statsManager.totalTasksCompletedByGroup.get(date).put(group, totalTasks.get(date));
            }

            statsManager.saveData();

            statsManager = null;
        }
    }

    private static class SaveDataAsyncTask extends AsyncTask<String, Void, Void> {

        private StatsManager statsManager;

        public SaveDataAsyncTask(StatsManager statsManager) {
            this.statsManager = statsManager;
        }

        @Override
        protected Void doInBackground(String... fileDir) {
            File timePeriodFolder = new File(fileDir[0] + "/data/" + statsManager.timePeriod.getTimePeriodID() + "/");

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

                JsonObject totalMinutesByGroup = new JsonObject();

                for(LocalDate key : statsManager.totalMinutesWorkedByGroup.keySet()) {
                    JsonObject groupMinutes = new JsonObject();

                    for(Group group : statsManager.totalMinutesWorkedByGroup.get(key).keySet()) {
                        groupMinutes.addProperty(group.getGroupName(), statsManager.totalMinutesWorkedByGroup.get(key).get(group));
                    }

                    totalMinutesByGroup.add(key.toString(), groupMinutes);
                }

                data.add("totalMinutesWorkedByGroup", totalMinutesByGroup);

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
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private Gson gson;
    private String fileDir;
    private TimePeriod timePeriod;

    private SaveDataAsyncTask asyncTask;

    private HashMap<LocalDate, HashMap<Group, Integer>> totalMinutesWorkedByGroup = new HashMap<>();
    private HashMap<LocalDate, HashMap<Group, Integer>> totalTasksCompletedByGroup = new HashMap<>();

    public StatsManager(Gson gson, String fileDir, TimePeriod timePeriod) {
        this.gson = gson;
        this.fileDir = fileDir;
        this.timePeriod = timePeriod;
    }

    //load data
    public StatsManager(Gson gson, String fileDir, TimePeriod timePeriod, HashMap<String, Group> groups) {
        this(gson, fileDir, timePeriod);

        try {
            File timePeriodFolder = new File(fileDir + "/data/" + timePeriod.getTimePeriodID() + "/");
            File statsFile = new File(timePeriodFolder.getAbsolutePath() + "/statistics.json");

            if(statsFile.exists()) {
                FileReader reader = new FileReader(statsFile);
                JsonObject data = gson.fromJson(reader, JsonObject.class);
                reader.close();

                if(data == null) {
                    return;
                }

                JsonObject totalMinutesByGroup = data.getAsJsonObject("totalMinutesWorkedByGroup");

                for(Map.Entry<String, JsonElement> dates : totalMinutesByGroup.entrySet()) {
                    HashMap<Group, Integer> map = new HashMap<>();

                    JsonObject groupInfo = dates.getValue().getAsJsonObject();
                    for(Map.Entry<String, JsonElement> groupMinutes : groupInfo.entrySet()) {
                        map.put(groups.get(groupMinutes.getKey()), groupMinutes.getValue().getAsInt());
                    }

                    totalMinutesWorkedByGroup.put(LocalDate.parse(dates.getKey()), map);
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
        asyncTask.execute(fileDir);
    }

    public void appendMinutes(Group group, LocalDate date, int minutes, boolean finishedTask) {
        if(!totalMinutesWorkedByGroup.containsKey(date)) {
            totalMinutesWorkedByGroup.put(date, new HashMap<>());
        }

        if(!totalMinutesWorkedByGroup.get(date).containsKey(group)) {
            totalMinutesWorkedByGroup.get(date).put(group, 0);
        }

        totalMinutesWorkedByGroup.get(date).put(group, totalMinutesWorkedByGroup.get(date).get(group) + minutes);

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

    public int getMinutesWorked(LocalDate date) {
        int minutes = 0;

        if(totalMinutesWorkedByGroup.containsKey(date)) {
            for(Group group : totalMinutesWorkedByGroup.get(date).keySet()) {
                minutes += totalMinutesWorkedByGroup.get(date).get(group);
            }
        }

        return minutes;
    }

    public int getMinutesWorkedByGroup(Group group, LocalDate date) {
        if(totalMinutesWorkedByGroup.containsKey(date)) {
            if(totalMinutesWorkedByGroup.get(date).containsKey(group)) {
                return totalMinutesWorkedByGroup.get(date).get(group);
            }
        }

        return 0;
    }
}
