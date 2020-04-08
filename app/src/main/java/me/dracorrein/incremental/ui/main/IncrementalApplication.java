package me.dracorrein.incremental.ui.main;

import me.dracorrein.incremental.logic.dashboard.TaskManager;

public class IncrementalApplication extends android.app.Application {

    public static TaskManager taskManager;

    @Override
    public void onCreate() {
        super.onCreate();

        taskManager = new TaskManager();
    }
}
