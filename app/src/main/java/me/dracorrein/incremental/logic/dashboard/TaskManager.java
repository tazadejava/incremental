package me.dracorrein.incremental.logic.dashboard;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TaskManager {

    //goal of this class: creates the days in which tasks are allocated; stores all the tasks

    private List<Task> allTasks;

    public TaskManager() {
        allTasks = new ArrayList<>();

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

    public List<Task> getTasks() {
        return allTasks;
    }

    public void load() {
        //TODO: get all tasks from the text files

        addNewTask(new Task("Pset 3", LocalDateTime.now().plusDays(1)));
        addNewTask(new Task("Finish Lab 1", LocalDateTime.now().plusDays(7)));
        addNewTask(new Task("Clean Room", LocalDateTime.now().plusDays(13)));
    }

    public void save() {

    }
}
