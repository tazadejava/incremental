package me.dracorrein.incremental.logic.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class TaskManager {

    //goal of this class: creates the days in which tasks are allocated; stores all the tasks

    private List<Task> allTasks, completedTasks;
    private List<RepeatingTask> allRepeatingTasks;

    private HashMap<String, List<Task>> completedTasksByTimePeriod;
    private HashMap<String, List<RepeatingTask>> completedRepeatingTasksByTimePeriod;

    private String currentTimePeriod;
    private List<String> timePeriods;
    private HashMap<String, LocalDate[]> timePeriodSpans;

    private List<String> allClasses;

    public TaskManager() {
        allTasks = new ArrayList<>();
        completedTasks = new ArrayList<>();
        allRepeatingTasks = new ArrayList<>();
        allClasses = new ArrayList<>();

        completedTasksByTimePeriod = new HashMap<>();
        timePeriods = new ArrayList<>();
        timePeriodSpans = new HashMap<>();

        timePeriods.add("Default");
        currentTimePeriod = timePeriods.get(0);

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

        save();
    }

    //return false if not allowed in some way: cannot be duplicate name, cannot end before it starts, cannot overlap with another date range
    //sets as current time period
    public boolean addNewTimePeriod(String timePeriod, LocalDate startDate, LocalDate endDate) {
        if(timePeriods.contains(timePeriod)) {
            return false;
        }
        if(endDate.isBefore(startDate)) {
            return false;
        }

        for(LocalDate[] span : timePeriodSpans.values()) {
            if(span[0].isBefore(startDate) || span[1].isAfter(endDate)) {
                return false;
            }
        }

        timePeriods.add(timePeriod);
        currentTimePeriod = timePeriod;
        timePeriodSpans.put(timePeriod, new LocalDate[] {startDate, endDate});

        return true;
    }

    public void addNewRepeatingTask(RepeatingTask repeatingTask) {
        allRepeatingTasks.add(repeatingTask);

        if(repeatingTask.hasPendingTasks()) {
            for(Task task : repeatingTask.popAllPendingTasks()) {
                addNewTask(task);
            }
        }
    }

    public List<Task> getTasks() {
        return allTasks;
    }

    public List<String> getAllClasses() {
        return allClasses;
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

    public String getCurrentTimePeriod() {
        return currentTimePeriod;
    }

    public void completeTask(Task task) {
        allTasks.remove(task);

        completedTasksByTimePeriod.putIfAbsent(currentTimePeriod, new ArrayList<>());
        completedTasksByTimePeriod.get(task.getTimePeriod()).add(task);
    }

    public void completeRepeatingTask(RepeatingTask task) {
        allRepeatingTasks.remove(task);

        completedRepeatingTasksByTimePeriod.putIfAbsent(currentTimePeriod, new ArrayList<>());
        completedRepeatingTasksByTimePeriod.get(task.getTimePeriod()).add(task);
    }

    public void load() {
        //TODO: get all tasks from the text files

        addNewTask(new Task("Pset 3", LocalDateTime.now().plusDays(1), "", "Default", (int) (Math.random() * 5)));
        addNewTask(new Task("Finish Lab 1", LocalDateTime.now().plusDays(7), "", "Default", (int) (Math.random() * 5)));
        addNewTask(new Task("Clean Room", LocalDateTime.now().plusDays(13), "", "Default", (int) (Math.random() * 5)));

        addPendingRepeatingTasks();
    }

    public void save() {

    }
}
