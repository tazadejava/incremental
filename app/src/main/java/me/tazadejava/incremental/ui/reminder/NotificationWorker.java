package me.tazadejava.incremental.ui.reminder;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void annotateLogDoc(Context context, String message) {
        try {
            File log = new File(context.getFilesDir().getAbsolutePath() + "/log.txt");

            if(!log.exists()) {
                log.createNewFile();
            }

            FileWriter writer = new FileWriter(log, true);

            DateTimeFormatter format = DateTimeFormatter.ISO_DATE_TIME;

            writer.append(String.format("[" + LocalDateTime.now(), format) + "] " + message);
            writer.append("\n");

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void annotateLogDoc(String message) {
        annotateLogDoc(getApplicationContext(), message);
    }

    @NonNull
    @Override
    public Result doWork() {
        IncrementalApplication app = (IncrementalApplication) getApplicationContext();
        if(app.isInForeground()) {
            annotateLogDoc("The app is currently in the foreground, so the service will retry later.");
            return Result.retry();
        }

        LocalDateTime nowTime = LocalDateTime.now();

        long minutesBetweenLastAccess = ChronoUnit.MINUTES.between(getLastBumpOrApplicationOpenTime(), nowTime);
        //don't send notifications for 1.5 hours after the app was opened
        if(minutesBetweenLastAccess < 90) {
            annotateLogDoc("The app was opened last " + minutesBetweenLastAccess + " minute(s) last. It is too soon (threshold 90 minutes).");
            return Result.success();
        } else {
            //don't send notifications between 11PM-9AM
            if(nowTime.getHour() < 9 || nowTime.getHour() > 22) {
                annotateLogDoc("The time is not right. " + nowTime.getHour() + " needs to be between 9-21.");
                return Result.success();
            }
        }

        annotateLogDoc("Service is ready to run!");

        TaskManager taskManager = new TaskManager(app.getFilesDir().getAbsolutePath());

        TimePeriod timePeriod = taskManager.getCurrentTimePeriod();

        List<Task> tasks = timePeriod.getTasksByDay(0);
        Set<Group> uniqueGroups = new HashSet<>();

        int tasksLimit = 5;
        StringBuilder tasksList = new StringBuilder();

        LocalDate now = LocalDate.now();

        for(Task task : tasks) {
            uniqueGroups.add(task.getGroup());

            if(tasksLimit > 0) {
                tasksLimit--;

                LocalDateTime dueDateTime = task.getDueDateTime();
                String dueDateFormatted;

                if(dueDateTime.toLocalDate().isBefore(now)) {
                    tasksList.append("<u>" + task.getName() + "</u> - <i>" + Utils.formatHourMinuteTime(task.getTodaysMinutesLeft()) + " left - OVERDUE</i>");
                } else {
                    if (dueDateTime.toLocalDate().equals(now)) {
                        long hoursBetween = ChronoUnit.HOURS.between(nowTime, dueDateTime);
                        if (hoursBetween < 1) {
                            long minutesBetween = ChronoUnit.MINUTES.between(nowTime, dueDateTime);

                            dueDateFormatted = "in " + minutesBetween + " minute" + (minutesBetween == 1 ? "" : "s!!!");
                        } else {
                            dueDateFormatted = "in " + hoursBetween + " hour" + (hoursBetween == 1 ? "" : "s");
                        }
                    } else {
                        long daysBetween = ChronoUnit.DAYS.between(nowTime, dueDateTime);

                        dueDateFormatted = "in " + daysBetween + " day" + (daysBetween == 1 ? "" : "s");
                    }

                    tasksList.append("<u>" + task.getName() + "</u> - <i>" + Utils.formatHourMinuteTime(task.getTodaysMinutesLeft()) + " left - due " + dueDateFormatted + "</i>");
                }

                if(tasksLimit > 0) {
                    tasksList.append("<br>");
                }
            }
        }

        int activeTasks = tasks.size();
        if(activeTasks > 0) {
            String contentText = "You have " + Utils.formatHourMinuteTimeFull(timePeriod.getEstimatedMinutesOfWorkForDate(now)) + " of work today (" + activeTasks + " task" + (activeTasks == 1 ? "" : "s") + ")";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), IncrementalApplication.NOTIFICATION_MAIN_CHANNEL)
                    .setSmallIcon(R.drawable.icon)
                    .setContentTitle(getTimeRelatedWelcomeMessage(nowTime))
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(Html.fromHtml("<b>" + contentText + "</b> <br>" + tasksList, 0)))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setAutoCancel(true);

            NotificationManagerCompat nMan = NotificationManagerCompat.from(getApplicationContext());
            nMan.notify(IncrementalApplication.PERSISTENT_NOTIFICATION_ID, builder.build());
        }

        annotateLogDoc("Service finished running successfully!");

        return Result.success();
    }

    private String getTimeRelatedWelcomeMessage(LocalDateTime time) {
        String[] messages;
        if(time.getHour() >= 5 && time.getHour() <= 11) {
            //morning
            messages = new String[] {"Good morning!", "Rise and shine!", "Sunshine time!", "Here comes the sun!", "Wake up!", "Top of the morning!"};
        } else if(time.getHour() >= 12 && time.getHour() <= 15) {
            //afternoon
            messages = new String[] {"Good afternoon!", "Good day!", "Howdy.", "Greetings!", "How's your day been so far?", "Hello!"};
        } else if(time.getHour() >= 16 && time.getHour() <= 18) {
            //evening
            //evening
            messages = new String[] {"Good evening!", "Good day!", "Howdy!", "Had time to chill today?"};
        } else {
            //night
            messages = new String[] {"Happy late hours!", "Late night grind time!", "Crackhead hours time!", "It's a lovely night."};
        }

        return messages[(int) (Math.random() * messages.length)];
    }

    public LocalDateTime getLastBumpOrApplicationOpenTime() {
        try {
            File dataFolder = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/data/");

            if(!dataFolder.exists()) {
                return LocalDateTime.MIN;
            }

            File lastAccessFile = new File(dataFolder.getAbsolutePath() + "/lastAccess.json");

            if(!lastAccessFile.exists()) {
                return LocalDateTime.MIN;
            }

            FileReader reader = new FileReader(lastAccessFile);
            JsonObject data = new Gson().fromJson(reader, JsonObject.class);
            reader.close();

            return LocalDateTime.parse(data.get("lastAccessTimestamp").getAsString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return LocalDateTime.MIN;
    }
}
