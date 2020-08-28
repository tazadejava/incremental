package me.tazadejava.incremental.ui.main;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.reminder.NotificationWorker;

public class IncrementalApplication extends android.app.Application implements LifecycleObserver {

    private static class LastAccessFileUpdate extends AsyncTask<Void, Void, Void> {

        private File dataFolder;

        public LastAccessFileUpdate(File dataFolder) {
            this.dataFolder = dataFolder;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                if(!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }

                File lastAccessFile = new File(dataFolder.getAbsolutePath() + "/lastAccess.json");

                if(!lastAccessFile.exists()) {
                    lastAccessFile.createNewFile();
                }

                FileWriter writer = new FileWriter(lastAccessFile);

                JsonObject data = new JsonObject();

                data.addProperty("lastAccessTimestamp", LocalDateTime.now().toString());

                new Gson().toJson(data, writer);

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            dataFolder = null;

            return null;
        }
    }

    public final static String NOTIFICATION_MAIN_CHANNEL = "Reminder Notifications";
    public final static int PERSISTENT_NOTIFICATION_ID = 0;

    public TaskManager taskManager;
    private boolean isInForeground;

    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        isInForeground = true;

        createNotificationChannels();

        taskManager = new TaskManager(getFilesDir().getAbsolutePath());

        //schedule work

        checkSettings();
    }

    public void reset() {
        taskManager = new TaskManager(getFilesDir().getAbsolutePath());
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public void checkSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //notifications

        if(prefs.getAll().containsKey("persistentNotification") && ((Boolean) prefs.getAll().get("persistentNotification"))) {
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(NotificationWorker.class, 1, TimeUnit.HOURS)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(NOTIFICATION_MAIN_CHANNEL, ExistingPeriodicWorkPolicy.KEEP, request);
        } else {
            WorkManager.getInstance(getApplicationContext()).cancelUniqueWork(NOTIFICATION_MAIN_CHANNEL);
        }

        //dark mode

        if(prefs.getAll().containsKey("darkModeOn")) {
            boolean darkModeOn = ((Boolean) prefs.getAll().get("darkModeOn"));
            AppCompatDelegate.setDefaultNightMode(darkModeOn ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        isInForeground = true;

        //mark access time
        new LastAccessFileUpdate(new File(getFilesDir().getAbsolutePath() + "/data/")).execute();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        isInForeground = false;
    }

    private void createNotificationChannels() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_MAIN_CHANNEL, NOTIFICATION_MAIN_CHANNEL, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Sends daily reminders of tasks and hours of work.");

            NotificationManager nMan = getSystemService(NotificationManager.class);
            nMan.createNotificationChannel(channel);
        }
    }

    public boolean isInForeground() {
        return isInForeground;
    }
}
