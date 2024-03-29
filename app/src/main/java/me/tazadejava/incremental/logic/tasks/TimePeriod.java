package me.tazadejava.incremental.logic.tasks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.taskmodifiers.GlobalTaskWorkPreference;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.SubGroup;
import me.tazadejava.incremental.ui.main.Utils;

public class TimePeriod {

    //need to account for the possible next week plus 6 days if on a Monday
    public static final int DAILY_LOGS_AHEAD_COUNT_LOAD = Utils.getDaysUntilEndOfWeek(2);
    public static final int DAILY_LOGS_AHEAD_COUNT_SHOW_UI = 6; //not including today

    private TaskManager taskManager;
    private Gson gson;
    private File dataFolder;

    private boolean hasLoadedTaskData;

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

    private HashMap<Group, WeeklyTimeInvariant> timeInvariantsPerGroup = new HashMap<>();

    private TimePeriod(TaskManager taskManager) {
        this.taskManager = taskManager;

        tasksByDay = new List[DAILY_LOGS_AHEAD_COUNT_LOAD];

        for (int i = 0; i < tasksByDay.length; i++) {
            tasksByDay[i] = new ArrayList<>();
        }
    }

    public TimePeriod(TaskManager taskManager, String fileDir, Gson gson, String name, LocalDate beginDate, LocalDate endDate) {
        this(taskManager);
        this.timePeriodName = name;
        this.beginDate = beginDate;
        this.endDate = endDate;

        statsManager = new StatsManager(gson, fileDir, this);

        workPreferences = new GlobalTaskWorkPreference();

        timePeriodID = UUID.randomUUID().toString().replace("-", "") + beginDate.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    //load from file
    public TimePeriod(TaskManager taskManager, JsonObject data, Gson gson, File dataFolder, boolean loadTaskData) {
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

        if(data.has("timeInvariantData")) {
            JsonObject timeInvariantData = data.getAsJsonObject("timeInvariantData");

            for(String groupName : timeInvariantData.keySet()) {
                timeInvariantsPerGroup.put(getGroupByName(groupName), new WeeklyTimeInvariant(timeInvariantData.get(groupName).getAsJsonObject()));
            }
        }

        if(data.has("workPreferences")) {
            workPreferences = gson.fromJson(data.get("workPreferences"), GlobalTaskWorkPreference.class);
        } else {
            workPreferences = new GlobalTaskWorkPreference();
        }

        if(loadTaskData) {
            loadTaskData(taskManager, gson, dataFolder);
        } else {
            this.gson = gson;
            this.dataFolder = dataFolder;
        }
    }

    public boolean setBeginDate(LocalDate date) {
        if(date.isAfter(endDate)) {
            return false;
        }

        beginDate = date;
        taskManager.saveData(true, this);
        return true;
    }

    public boolean setEndDate(LocalDate date) {
        if(date.isBefore(beginDate)) {
            return false;
        }

        endDate = date;
        taskManager.saveData(true, this);
        return true;
    }

    private LocalDate getStartDate(Task task) {
        LocalDate startDate;
        if(task.getStartDate() != null) {
            startDate = task.getStartDate();
        } else {
            startDate = task.getParent().getStartDate();
        }

        return startDate;
    }

    /**
     * Reset task minutes for all active tasks for today
     */
    public void verifyDayChangeReset() {
        for(Task task : allActiveTasks) {
            task.verifyDayChangeReset();
        }
    }

    /**
     * Run after all time periods have been defined
     * @param gson
     */
    public void initializeStatsManager(Gson gson, String fileDir) {
        statsManager = new StatsManager(gson, fileDir, this, taskManager.getAllGroupsHashed(this));
    }

    /**
     * The hashes in the stats manager get messed up, so this has to be refreshed every time the group data changes
     */
    public void refreshAfterGroupAttributeChange() {
        //TODO: this is going to crash unless we fix it; hashmap gets messed up...
//        statsManager = new StatsManager(taskManager.getGson(), taskManager.getFileDir(), this, taskManager.getAllCurrentGroupsHashed());
//        statsManager.saveData();
    }

    public void loadInactiveTimePeriodTaskData() {
        if(hasLoadedTaskData) {
            return;
        }

        loadTaskData(taskManager, gson, dataFolder);

        gson = null;
        dataFolder = null;
    }

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

                hasLoadedTaskData = true;
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

        if(!timeInvariantsPerGroup.isEmpty()) {
            JsonObject timeInvariantData = new JsonObject();

            //add time invariants
            for(Group group : timeInvariantsPerGroup.keySet()) {
                timeInvariantData.add(group.getGroupName(), timeInvariantsPerGroup.get(group).save());
            }

            data.add("timeInvariantData", timeInvariantData);
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

            TaskGenerator generatedTask = NonrepeatingTask.createInstance(gson, taskManager, this, generatorObject);

            generators.add(generatedTask);

            generatedTask.assignTasksToLoadingHashMap(tasksList);
        }

        if(tasks != null) {
            JsonArray tasksObject = data.getAsJsonArray("activeTasks");

            for (JsonElement taskID : tasksObject) {
                //assert: this call will never fail if implemented correctly
                addTaskInOrder(tasks, tasksList.get(taskID.getAsString()));
            }
        }
    }

    private void saveTasksToFile(Gson gson, File destFile, List<TaskGenerator> generators, List<Task> tasks) throws IOException {
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

        //create temp file just in case something fails
        File tempFile = new File(destFile.getParentFile().getAbsolutePath() + "/" + destFile.getName() + ".TMP");

        FileWriter writer = new FileWriter(tempFile);
        gson.toJson(data, writer);
        writer.close();

        Files.copy(tempFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        tempFile.delete();
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

    public int getNumberOfActiveTasks() {
        int count = 0;

        Set<Task> countedTasks = new HashSet<>();

        for(List<Task> tasks : tasksByDay) {
            for(Task task : tasks) {
                if(countedTasks.contains(task)) {
                    continue;
                }
                countedTasks.add(task);

                if(task.isTaskCurrentlyWorkedOn()) {
                    count++;
                }
            }
        }

        return count;
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

        //update future tasks, if applicable
        updateSubGroupEstimatedTime(task);
    }

    /**
     * When a task that exists in a subgroup is completed, we will update the future tasks that are also in this subgroup to the average of this time.
     * Call after the subgroup is updated.
     */
    public void updateSubGroupEstimatedTime(Task task) {
        if(!task.isInSubGroup()) {
            return;
        }

        SubGroup subGroup = task.getSubgroup();
        int revisedMinutes = subGroup.getAveragedEstimatedMinutes();

        //assumption: all active tasks still exist in the allTasksGenerators
        for(TaskGenerator futureTaskGenerator : allTaskGenerators) {
            for(Task futureTask : futureTaskGenerator.getAllTasks()) {
                if(futureTask.getGroup().equals(task.getGroup()) && futureTask.isInSubGroup() && futureTask.getSubgroup().equals(subGroup)) {
                    futureTask.setEstimatedTotalMinutesToCompletion(revisedMinutes);
                }
            }
        }
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

    public void deleteTaskCompletely(Task task) {
        TaskGenerator generator = task.getParent();
        allActiveTasks.remove(task);
        removeTaskFromDailyLists(task);

        removeActiveTasksByParent(generator);
        allTaskGenerators.remove(generator);

        allCompletedTaskGenerators.remove(generator);
    }

    public List<Task> removeActiveTasksByParent(TaskGenerator parent) {
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

    public void removeAllDailyTasksByParent(TaskGenerator parent) {
        for(List<Task> tasks : tasksByDay) {
            Iterator<Task> allTasksIterator = tasks.iterator();
            while (allTasksIterator.hasNext()) {
                Task task = allTasksIterator.next();

                if (task.getParent() == parent) {
                    allTasksIterator.remove();
                }
            }
        }
    }

    public int getEstimatedMinutesOfWorkForDate(LocalDate date) {
        int deltaDays = (int) ChronoUnit.DAYS.between(LocalDate.now(), date);

        if(deltaDays < 0 || deltaDays > tasksByDay.length + 1) {
            return -1;
        } else {
            int estimatedMinutes = 0;

            List<Task> dayList = getTasksByDay(deltaDays);

            if(deltaDays == 0) {
                for (Task task : dayList) {
                    estimatedMinutes += task.getTodaysMinutesLeft();
                }
            } else {
                for (Task task : dayList) {
                    estimatedMinutes += task.getDayMinutesOfWorkTotal(date, true);
                }
            }

            if(estimatedMinutes < 0) {
                return 0;
            }

            return estimatedMinutes;
        }
    }

    private void removeTaskFromDailyLists(Task task) {
        for (int i = 0; i < tasksByDay.length; i++) {
            tasksByDay[i].remove(task);
        }
    }

    /**
     * REQUIRES CALL TO OLDTASKS
     * @param group
     * @return length 2 array, representing min and max task estimated minute size
     */
    public int[] getMinMaxEstimatedMinutesByGroup(Group group) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for(TaskGenerator taskGenerator : allTaskGenerators) {
            for(Task task : taskGenerator.getAllTasks()) {
                if(task == null) {
                    continue;
                }

                if(task.getGroup() == group) {
                    min = Math.min(task.getEstimatedCompletionTime(), min);
                    max = Math.max(task.getEstimatedCompletionTime(), max);
                }
            }
        }

        for(TaskGenerator taskGenerator : allCompletedTaskGenerators) {
            for(Task task : taskGenerator.getAllTasks()) {
                if(task == null) {
                    continue;
                }

                if(task.getGroup() == group) {
                    min = Math.min(task.getEstimatedCompletionTime(), min);
                    max = Math.max(task.getEstimatedCompletionTime(), max);
                }
            }
        }

        return new int[] {min, max};
    }

    private void addTaskToDailyLists(Task task) {
        addTaskToDailyLists(task, task.getStartDate() != null ? task.getStartDate() : LocalDate.now(), task.getDueDateTime().toLocalDate());
    }

    private void addTaskToDailyLists(Task task, LocalDate taskStartDate, LocalDate taskDueDate) {
        //place respectively in the daily task list
        for (int i = 0; i < tasksByDay.length; i++) {
            LocalDate dailyDate = LocalDate.now().plusDays(1 + i);

            if(taskStartDate.isAfter(dailyDate)) {
                continue;
            }

            if (dailyDate.equals(taskDueDate) || dailyDate.isBefore(taskDueDate)) {
                if(!tasksByDay[i].contains(task)) {
                    addTaskInOrder(tasksByDay[i], task);
                }
            }
        }
    }

    private void addTaskInOrder(List<Task> tasks, Task task) {
        boolean added = false;
        //add by due date order first
        for(int i = 0; i < tasks.size(); i++) {
            Task compareTask = tasks.get(i);
            if(task.getDueDateTime().isBefore(compareTask.getDueDateTime())) {
                tasks.add(i, task);
                added = true;
                break;
            } else {
                if(task.getDueDateTime().equals(compareTask.getDueDateTime())) {
                    //the due date is the same

                    //sort by creation date; older gets put first
                    for(int j = i; j < tasks.size(); j++) {
                        Task sameDateTask = tasks.get(j);

                        if(!sameDateTask.getDueDateTime().equals(task.getDueDateTime())) {
                            tasks.add(j, task);
                            added = true;
                            break;
                        }

                        if(task.getParent().getCreationTime().compareTo(sameDateTask.getParent().getCreationTime()) < 0) {
                            tasks.add(j, task);
                            added = true;
                            break;
                        }
                    }

                    if(added) {
                        break;
                    }
                }
            }
        }

        //if all are equal, then just add at the end
        if(!added) {
            tasks.add(task);
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

    public boolean addNewGroup(String name) {
        if(groups.containsKey(name)) {
            return false;
        }

        groups.put(name, new Group(name));

        taskManager.saveData(true, this);

        return true;
    }

    public boolean deleteGroup(Group group) {
        if(!groups.containsKey(group.getGroupName())) {
            return false;
        }

        for(Task task : getAllTasksByGroup(group)) {
            deleteTaskCompletely(task);
        }

        statsManager.deleteGroupStats(group);

        groups.remove(group.getGroupName());
        taskManager.saveData(true, this);

        return true;
    }

    public boolean updateGroupName(Group group, String name) {
        if(groups.containsKey(name)) {
            return taskManager.updateGroupName(group, name);
        }

        groups.remove(group.getGroupName());
        group.setGroupName(name);
        groups.put(name, group);

        refreshAfterGroupAttributeChange();

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

    public List<Task> getTasksByDay(LocalDate date) {
        return getTasksByDay((int) ChronoUnit.DAYS.between(LocalDate.now(), date));
    }

    public List<Task> getTasksByDay(int index) {
        if(index < 0 || index > tasksByDay.length) { //allow tasksByDay length since it is subtracted by one
            return new ArrayList<>();
        }

        if(index == 0) {
            List<Task> filtered = new ArrayList<>();

            for(Task task : allActiveTasks) {
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

                if(workPreferences.isBlackedOutDay(LocalDate.now())) {
                    if(isPossibleToComplete) {
                        continue;
                    }
                }
                if(task.isDoneWithTaskToday()) {
                    if(!isPossibleToComplete) {
                        task.overrideDoneWithTaskMarker();
                        taskManager.saveData(true, this);
                    } else {
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

    public Set<Task> getAllCurrentAndUpcomingTasksByGroup(Group group) {
        Set<Task> countedTasks = new HashSet<>();

        for(TaskGenerator generator : allTaskGenerators) {
            for(Task task : generator.getAllTasks()) {
                if(task == null) {
                    continue;
                }
                if(!task.getGroup().equals(group)) {
                    continue;
                }

                countedTasks.add(task);
            }
        }

        return countedTasks;
    }

    public Set<Task> getAllTasksByGroup(Group group) {
        Set<Task> countedTasks = new HashSet<>();

        List<TaskGenerator> generators = new ArrayList<>();

        generators.addAll(allTaskGenerators);
        generators.addAll(allCompletedTaskGenerators);

        for(TaskGenerator generator : generators) {
            for(Task task : generator.getAllTasks()) {
                if(task == null) {
                    continue;
                }
                if(!task.getGroup().equals(group)) {
                    continue;
                }

                countedTasks.add(task);
            }
        }

        return countedTasks;
    }

    public List<Task> getCompletedTasksHistory() {
        List<Task> tasks = new ArrayList<>();

        for(TaskGenerator generator : allTaskGenerators) {
            for(Task task : generator.getAllTasks()) {
                if(task == null) {
                    continue;
                }

                if(task.isTaskComplete()) {
                    tasks.add(task);
                }
            }
        }

        for(TaskGenerator generator : allCompletedTaskGenerators) {
            for(Task task : generator.getAllTasks()) {
                if(task == null) {
                    continue;
                }

                if(task.isTaskComplete()) {
                    tasks.add(task);
                }
            }
        }

        tasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task task, Task t1) {
                return t1.getLastTaskWorkedTime().compareTo(task.getLastTaskWorkedTime());
            }
        });

        return tasks;
    }

    public int getTimeInvariantMinutes(LocalDate date) {
        int minutes = 0;
        for(Group group : groups.values()) {
            if(timeInvariantsPerGroup.containsKey(group)) {
                WeeklyTimeInvariant invariant = timeInvariantsPerGroup.get(group);
                minutes += invariant.getMinutes(this, date);
            }
        }

        return minutes;
    }

    public int getTimeInvariantMinutes(Group group, LocalDate date) {
        if(timeInvariantsPerGroup.containsKey(group)) {
            WeeklyTimeInvariant invariant = timeInvariantsPerGroup.get(group);
            return invariant.getMinutes(this, date);
        }

        return 0;
    }

    public WeeklyTimeInvariant getTimeInvariant(Group group) {
        return timeInvariantsPerGroup.getOrDefault(group, null);
    }

    public void setTimeInvariant(Group group, WeeklyTimeInvariant invariant) {
        timeInvariantsPerGroup.put(group, invariant);
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

    public boolean hasLoadedTaskData() {
        return hasLoadedTaskData;
    }
}
