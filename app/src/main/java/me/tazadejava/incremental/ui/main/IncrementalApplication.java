package me.tazadejava.incremental.ui.main;

import me.tazadejava.incremental.logic.tasks.TaskManager;

public class IncrementalApplication extends android.app.Application {

    public static TaskManager taskManager;

    public static String filesDir;

    @Override
    public void onCreate() {
        super.onCreate();

        filesDir = getFilesDir().getAbsolutePath();

        taskManager = new TaskManager();
    }
}
