package me.tazadejava.incremental.ui.create;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.NonrepeatingTask;
import me.tazadejava.incremental.logic.tasks.RepeatingTask;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class CreateTaskActivity extends AppCompatActivity {

    private TaskManager taskManager;
    private BackPressedInterface backPressedInterface;

    private FrameLayout frame;

    private Boolean isRepeatingTask;
    private Group selectedGroup;
    private int minutesToCompletion = -1;

    private String taskName;
    private LocalDate startDate, dueDate;
    private LocalTime dueTime;

    private String[] taskNames;
    private boolean useAverageEstimateRepeating = true;
    private Set<Integer> disabledTasks;
    private DayOfWeek dueDayOfWeek;
    private HashMap<LocalDate, DayOfWeek> additionalDueDatesRepeating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);
        taskManager = ((IncrementalApplication) getApplication()).getTaskManager();

        frame = findViewById(R.id.createTaskFrame);

        if(taskManager.getActiveEditTask() != null) {
            //fill in the data
            Task activeTask = taskManager.getActiveEditTask();

            isRepeatingTask = activeTask.getParent() instanceof RepeatingTask;
            selectedGroup = activeTask.getGroup();
            minutesToCompletion = activeTask.getEstimatedCompletionTime();

            startDate = activeTask.getParent().getStartDate();
            dueTime = activeTask.getDueDateTime().toLocalTime();

            if(isRepeatingTask) {
                RepeatingTask generator = (RepeatingTask) activeTask.getParent();
                taskNames = generator.getTaskNames();

                useAverageEstimateRepeating = generator.getUseAverageInWorktimeEstimate();

                disabledTasks = new HashSet<>();

                for(int i = 0; i < taskNames.length; i++) {
                    if(taskNames[i].isEmpty()) {
                        disabledTasks.add(i);
                    }
                }

                dueDayOfWeek = activeTask.getDueDateTime().getDayOfWeek();
            } else {
                taskName = activeTask.getName();

                dueDate = activeTask.getDueDateTime().toLocalDate();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.createTaskFrame, new CreateTaskGroupTimeFragment())
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.createTaskFrame, new CreateTaskTaskTypeFragment())
                    .commit();
        }
    }

    public void createTask() {
        TimePeriod timePeriod = taskManager.getCurrentTimePeriod();

        //to avoid having errors with timing (particularly being overdue on the minute of), we will make the seconds marker at :59 for due times
        dueTime = dueTime.withSecond(59);

        if(taskManager.getActiveEditTask() != null) {
            Task editTask = taskManager.getActiveEditTask();
            taskManager.setActiveEditTask(null);

            if(editTask.isRepeatingTask()) {
                ((RepeatingTask) editTask.getParent()).updateAndSaveTask(taskNames,
                        startDate.getDayOfWeek(), dueDayOfWeek, dueTime, selectedGroup, minutesToCompletion, useAverageEstimateRepeating);
            } else {
                LocalDateTime dueDateAndTime = dueDate.atStartOfDay().withHour(dueTime.getHour()).withMinute(dueTime.getMinute());
                ((NonrepeatingTask) editTask.getParent()).updateAndSaveTask(startDate, taskName, dueDateAndTime, selectedGroup, minutesToCompletion);
            }
        } else {
            if (isRepeatingTask) {
                taskManager.addNewGeneratedTask(new RepeatingTask(taskManager, taskNames,
                        startDate, startDate.getDayOfWeek(), dueDayOfWeek, dueTime, selectedGroup, timePeriod, minutesToCompletion, useAverageEstimateRepeating));
            } else {
                LocalDateTime dueDateAndTime = dueDate.atStartOfDay().withHour(dueTime.getHour()).withMinute(dueTime.getMinute());
                taskManager.addNewGeneratedTask(new NonrepeatingTask(taskManager, startDate, timePeriod, taskName, dueDateAndTime, selectedGroup, minutesToCompletion));
            }
        }

        //add additional days of week, if applicable
        if(isRepeatingTask && additionalDueDatesRepeating != null && !additionalDueDatesRepeating.isEmpty()) {
            for(LocalDate startDate : additionalDueDatesRepeating.keySet()) {
                //the repeating tasks are identified by creation date, oops. so to alleviate this, forcibly sleep for a bit of time to ensure creation dates are different
                SystemClock.sleep(50);

                DayOfWeek dueDayOfWeek = additionalDueDatesRepeating.get(startDate);

                taskManager.addNewGeneratedTask(new RepeatingTask(taskManager, taskNames,
                        startDate, startDate.getDayOfWeek(), dueDayOfWeek, dueTime, selectedGroup, timePeriod, minutesToCompletion, useAverageEstimateRepeating));
            }
        }

        Intent returnToMain = new Intent(this, MainActivity.class);
        startActivity(returnToMain);

        Utils.hideKeyboard(frame);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(taskManager.getActiveEditTask() == null) {
            return super.onCreateOptionsMenu(menu);
        }

        getMenuInflater().inflate(R.menu.menu_edit_task, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_delete_task:
                AlertDialog.Builder confirmation = new AlertDialog.Builder(this);

                confirmation.setTitle("Are you sure you want to delete this task?");

                if(taskManager.getActiveEditTask().getParent() instanceof RepeatingTask) {
                    confirmation.setMessage("All tasks in this chain will be deleted. This cannot be undone!");
                } else {
                    confirmation.setMessage("This task will be deleted. This cannot be undone!");
                }

                confirmation.setPositiveButton("DELETE TASK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        taskManager.getCurrentTimePeriod().deleteTaskCompletely(taskManager.getActiveEditTask(), taskManager.getActiveEditTask().getParent());

                        taskManager.saveData(true, taskManager.getCurrentTimePeriod());

                        Intent returnToMain = new Intent(CreateTaskActivity.this, MainActivity.class);
                        startActivity(returnToMain);
                    }
                });

                confirmation.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setBackPressedInterface(BackPressedInterface backPressedInterface) {
        this.backPressedInterface = backPressedInterface;
    }

    @Override
    public void onBackPressed() {
        if(backPressedInterface != null) {
            backPressedInterface.onBackPressed();
        }
    }

    public Boolean isRepeatingTask() {
        return isRepeatingTask;
    }

    public void setRepeatingTask(boolean repeatingTask) {
        isRepeatingTask = repeatingTask;
    }

    public Group getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(Group selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    public int getMinutesToCompletion() {
        return minutesToCompletion;
    }

    public void setMinutesToCompletion(int minutesToCompletion) {
        this.minutesToCompletion = minutesToCompletion;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }

    public String[] getTaskNames() {
        return taskNames;
    }

    public void setTaskNames(String[] taskNames) {
        this.taskNames = taskNames;
    }

    public boolean isUseAverageEstimateRepeating() {
        return useAverageEstimateRepeating;
    }

    public void setUseAverageEstimateRepeating(boolean useAverageEstimateRepeating) {
        this.useAverageEstimateRepeating = useAverageEstimateRepeating;
    }

    public Set<Integer> getDisabledTasks() {
        return disabledTasks;
    }

    public void setDisabledTasks(Set<Integer> disabledTasks) {
        this.disabledTasks = disabledTasks;
    }

    public DayOfWeek getDueDayOfWeek() {
        return dueDayOfWeek;
    }

    public void setDueDayOfWeek(DayOfWeek dueDayOfWeek) {
        this.dueDayOfWeek = dueDayOfWeek;
    }

    public HashMap<LocalDate, DayOfWeek> getAdditionalDueDatesRepeating() {
        return additionalDueDatesRepeating;
    }

    public void setAdditionalDueDatesRepeating(HashMap<LocalDate, DayOfWeek> additionalDueDatesRepeating) {
        this.additionalDueDatesRepeating = additionalDueDatesRepeating;
    }
}
