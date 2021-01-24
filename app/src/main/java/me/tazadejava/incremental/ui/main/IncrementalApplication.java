package me.tazadejava.incremental.ui.main;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;

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

import java.util.concurrent.TimeUnit;

import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.notifications.ReminderNotificationWorker;

public class IncrementalApplication extends android.app.Application implements LifecycleObserver {

    public final static String NOTIFICATION_MAIN_CHANNEL = "Reminder Notifications";
    public final static String NOTIFICATION_ACTIVE_TASK_CHANNEL = "Active Task Notifications";
    public final static int REMINDER_NOTIFICATION_ID = 0;
    public final static int PERSISTENT_NOTIFICATION_ID = 1;

    private TaskManager taskManager;
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
        reset(true);
    }

    public void reset(boolean verifyDayChangeAndSave) {
        //first, reset taskManager and save changes
        if(verifyDayChangeAndSave) {
            taskManager.verifyDayChangeReset();
        }

        taskManager = new TaskManager(getFilesDir().getAbsolutePath());
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public void checkSettings() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //notifications

        if(prefs.getAll().containsKey("reminderNotification") && ((Boolean) prefs.getAll().get("reminderNotification"))) {
            ReminderNotificationWorker.annotateLogDoc(getApplicationContext(), "The reminder notification is on, and the request has been done.");
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ReminderNotificationWorker.class, 1, TimeUnit.HOURS)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(NOTIFICATION_MAIN_CHANNEL, ExistingPeriodicWorkPolicy.REPLACE, request);
        } else {
            ReminderNotificationWorker.annotateLogDoc(getApplicationContext(), "The reminder notification is off, so the request has been canceled.");
            WorkManager.getInstance(getApplicationContext()).cancelUniqueWork(NOTIFICATION_MAIN_CHANNEL);
        }

        //dark mode

        //enable dark mode by default if the system is dark mode
        if(!prefs.getAll().containsKey("darkModeOn")) {
            int nightModeFlags = getApplicationContext().getResources().getConfiguration().uiMode &
                            Configuration.UI_MODE_NIGHT_MASK;

            SharedPreferences.Editor edit = prefs.edit();
            if(nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                edit.putBoolean("darkModeOn", true);
            } else {
                edit.putBoolean("darkModeOn", false);
            }
            edit.apply();
        }

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
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        isInForeground = false;
    }

    private void createNotificationChannels() {
        NotificationManager nMan = getSystemService(NotificationManager.class);

        NotificationChannel reminderChannel = new NotificationChannel(NOTIFICATION_MAIN_CHANNEL, NOTIFICATION_MAIN_CHANNEL, NotificationManager.IMPORTANCE_DEFAULT);
        reminderChannel.setDescription("Sends daily reminders of tasks and hours of work.");
        nMan.createNotificationChannel(reminderChannel);


        NotificationChannel activeTaskChannel = new NotificationChannel(NOTIFICATION_ACTIVE_TASK_CHANNEL, NOTIFICATION_ACTIVE_TASK_CHANNEL, NotificationManager.IMPORTANCE_LOW);
        activeTaskChannel.setDescription("Displays currently active task for quick logging.");
        nMan.createNotificationChannel(activeTaskChannel);
    }

    public boolean isDarkModeOn() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs.getBoolean("darkModeOn", false);
    }

    public boolean isInForeground() {
        return isInForeground;
    }
}
