package me.tazadejava.incremental.ui.create;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.SubGroup;
import me.tazadejava.incremental.logic.tasks.NonrepeatingTask;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class CreateTaskActivity extends AppCompatActivity {

    private TaskManager taskManager;
    private BackPressedInterface backPressedInterface;

    private FrameLayout frame;

    private Boolean isBatchCreation;
    private Group selectedGroup;
    private SubGroup selectedSubgroup;
    private int minutesToCompletion = -1;

    private String taskName;
    private LocalDate startDate, dueDate;
    private LocalTime dueTime;

    private String[] taskNames;
    private boolean useAverageEstimateRepeating = true;
    private Set<Integer> disabledTasks = new HashSet<>();
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

            selectedGroup = activeTask.getGroup();
            selectedSubgroup = activeTask.getSubgroup();
            minutesToCompletion = activeTask.getEstimatedCompletionTime();

            startDate = activeTask.getParent().getStartDate();
            dueTime = activeTask.getDueDateTime().toLocalTime();

            taskName = activeTask.getName();
            dueDate = activeTask.getDueDateTime().toLocalDate();

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

        //update subgroup original time estimation ONLY IF is the first task being created with it
        if(selectedSubgroup != null && !selectedSubgroup.haveMinutesBeenSet()) {
            selectedSubgroup.setOriginalEstimatedMinutesCompletion(minutesToCompletion);
        }

        if(taskManager.getActiveEditTask() != null) {
            Task editTask = taskManager.getActiveEditTask();
            taskManager.setActiveEditTask(null);

            LocalDateTime dueDateAndTime = dueDate.atStartOfDay().withHour(dueTime.getHour()).withMinute(dueTime.getMinute());
                ((NonrepeatingTask) editTask.getParent()).updateAndSaveTask(startDate, taskName, dueDateAndTime, selectedGroup, selectedSubgroup, minutesToCompletion);
        } else {
            if (isBatchCreation) {
                List<LocalDate> startDates = new ArrayList<>();
                List<DayOfWeek> dueDayOfWeeks = new ArrayList<>();

                startDates.add(startDate);
                dueDayOfWeeks.add(dueDayOfWeek);

                if(additionalDueDatesRepeating != null && !additionalDueDatesRepeating.isEmpty()) {
                    for(Map.Entry<LocalDate, DayOfWeek> entry : additionalDueDatesRepeating.entrySet()) {
                        startDates.add(entry.getKey());
                        dueDayOfWeeks.add(entry.getValue());
                    }
                }

                int startDateIndex = 0;
                for(LocalDate startDate : startDates) {
                    DayOfWeek dueDayOfWeek = dueDayOfWeeks.get(startDateIndex);
                    for (int i = 0; i < taskNames.length; i++) {
                        if (!taskNames[i].isEmpty()) {
                            LocalDate taskStartDate = startDate.plusDays(7 * i);
                            LocalDateTime taskDueDateTime = taskStartDate.plusDays(Utils.getDaysBetweenDaysOfWeek(startDate.getDayOfWeek(), dueDayOfWeek)).atTime(dueTime);
                            taskManager.addNewGeneratedTask(new NonrepeatingTask(taskManager, taskStartDate, timePeriod, taskNames[i], taskDueDateTime, selectedGroup, selectedSubgroup, minutesToCompletion));
                        }
                    }

                    startDateIndex++;
                }
            } else {
                LocalDateTime dueDateAndTime = dueDate.atStartOfDay().withHour(dueTime.getHour()).withMinute(dueTime.getMinute());
                taskManager.addNewGeneratedTask(new NonrepeatingTask(taskManager, startDate, timePeriod, taskName, dueDateAndTime, selectedGroup, selectedSubgroup, minutesToCompletion));
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

        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);

            SpannableString span = new SpannableString(item.getTitle());

            span.setSpan(new ForegroundColorSpan(Utils.getAndroidAttrColor(this, android.R.attr.textColorPrimary)), 0, span.length(), 0);

            item.setTitle(span);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_delete_task:
                AlertDialog.Builder confirmation = new AlertDialog.Builder(this);

                confirmation.setTitle("Are you sure you want to delete this task?");
                confirmation.setMessage("This task will be deleted. This cannot be undone!");

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

    public Boolean getIsBatchCreation() {
        return isBatchCreation;
    }

    public void setIsBatchCreation(boolean isBatchCreation) {
        this.isBatchCreation = isBatchCreation;
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

    public SubGroup getSelectedSubgroup() {
        return selectedSubgroup;
    }

    public void setSelectedSubgroup(SubGroup subgroup) {
        selectedSubgroup = subgroup;
    }
}
