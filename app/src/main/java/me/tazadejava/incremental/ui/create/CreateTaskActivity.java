package me.tazadejava.incremental.ui.create;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.tazadejava.incremental.R;
import me.tazadejava.incremental.ui.main.Utils;
import me.tazadejava.incremental.logic.tasks.NonrepeatingTask;
import me.tazadejava.incremental.logic.tasks.RepeatingTask;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.logic.dashboard.TimePeriod;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class CreateTaskActivity extends AppCompatActivity {

    private EditText nameOfTaskEdit, estimatedHoursEdit;
    private TextView dueDate, dueTime, startDate;
    private LocalDate dueDateObject, startDateObject;
    private LocalTime dueTimeObject;
    private Spinner groupSpinner;

    private ArrayAdapter<String> groupSpinnerAdapter;

    private Button saveButton;

    private Switch repeatingEventSwitch;
    private ConstraintLayout repeatingEventDateLayout;
    private EditText repeatingEventNumber;

    private RecyclerView repeatingTaskNamesList;
    private RepeatingTaskNamesAdapter repeatingTaskNamesAdapter;

    private TaskManager taskManager = IncrementalApplication.taskManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        estimatedHoursEdit = findViewById(R.id.estimatedCompletionTime);

        nameOfTaskEdit = findViewById(R.id.nameOfTask);

        nameOfTaskEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSaveButton();
            }
        });

        estimatedHoursEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSaveButton();
            }
        });

        dueDate = findViewById(R.id.dueDate);

        dueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateTaskActivity.this);

                datePicker.getDatePicker().setMinDate(System.currentTimeMillis());

                if(dueDateObject != null) {
                    datePicker.updateDate(dueDateObject.getYear(), dueDateObject.getMonthValue() - 1, dueDateObject.getDayOfMonth());
                }

                datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        dueDateObject = LocalDate.of(year, month + 1, dayOfMonth);

                        String dayOfWeek = dueDateObject.getDayOfWeek().toString();

                        dueDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + dueDateObject.getMonthValue()
                                + "/" + dueDateObject.getDayOfMonth() + "/" + dueDateObject.getYear());

                        if(repeatingEventSwitch.isChecked()) {
                            repeatingTaskNamesAdapter.setDueDate(dueDateObject);
                            repeatingTaskNamesAdapter.notifyDataSetChanged();
                        }

                        updateSaveButton();
                    }
                });

                datePicker.show();
            }
        });

        dueTime = findViewById(R.id.dueTime);

        dueTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        dueTimeObject = LocalTime.of(hourOfDay, minute);
                        dueTime.setText(Utils.formatLocalTime(hourOfDay, minute));
                    }
                };

                TimePickerDialog timePicker = new TimePickerDialog(CreateTaskActivity.this, onTimeSetListener, dueTimeObject.getHour(), dueTimeObject.getMinute(), false);
                timePicker.show();
            }
        });

        startDate = findViewById(R.id.startDateNonRepeating);

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateTaskActivity.this);

                datePicker.getDatePicker().setMinDate(System.currentTimeMillis());

                if(startDateObject != null) {
                    datePicker.updateDate(startDateObject.getYear(), startDateObject.getMonthValue() - 1, startDateObject.getDayOfMonth());
                }

                datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        month++;
                        startDateObject = LocalDate.of(year, month, dayOfMonth);

                        String dayOfWeek = startDateObject.getDayOfWeek().toString();

                        startDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + startDateObject.getMonthValue()
                                + "/" + startDateObject.getDayOfMonth() + "/" + startDateObject.getYear());

                        if(repeatingEventSwitch.isChecked()) {
                            repeatingTaskNamesAdapter.setStartDate(startDateObject);
                            repeatingTaskNamesAdapter.notifyDataSetChanged();
                        }

                        updateSaveButton();
                    }
                });

                datePicker.show();
            }
        });

        repeatingEventSwitch = findViewById(R.id.repeatingEventSwitch);
        repeatingEventDateLayout = findViewById(R.id.repeatingEventDateLayout);
        repeatingEventSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    repeatingEventDateLayout.setVisibility(View.VISIBLE);
                    nameOfTaskEdit.setEnabled(false);
                } else {
                    repeatingEventDateLayout.setVisibility(View.GONE);
                    nameOfTaskEdit.setEnabled(true);
                }

                updateSaveButton();
            }
        });

        repeatingTaskNamesList = findViewById(R.id.repeatingTaskNamesList);
        repeatingTaskNamesList.setLayoutManager(new LinearLayoutManager(this));

        repeatingEventNumber = findViewById(R.id.repeatingEventNumber);
        repeatingEventNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.length() > 0) {
                    int amount = Integer.parseInt(repeatingEventNumber.getText().toString());

                    if(amount > 0) {
                        repeatingTaskNamesAdapter.setRepeatSize(amount);
                        repeatingTaskNamesAdapter.notifyDataSetChanged();
                    }
                }
            }
        });

        groupSpinner = findViewById(R.id.classSpinner);

        List<String> items = new ArrayList<>();

        items.add("Select class");

        items.addAll(taskManager.getAllCurrentGroupNames());

        items.add("Add new class...");

        groupSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        groupSpinner.setAdapter(groupSpinnerAdapter);

        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            private int lastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //If the user has no classes or needs a new one, open a dialog to create a new one!
                if(position == items.size() - 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CreateTaskActivity.this);
                    builder.setTitle("Create new group");

                    View dialogView = View.inflate(CreateTaskActivity.this, R.layout.dialog_new_group, null);

                    List<String> groupScope = new ArrayList<>();

                    groupScope.add("Global Group (no expiration date)");
                    for(TimePeriod period : taskManager.getTimePeriods()) {
                        String beginDateFormatted = period.getBeginDate() != null ? period.getBeginDate().getMonthValue() + "/" + period.getBeginDate().getDayOfMonth() : "";
                        String endDateFormatted = period.getEndDate() != null ? period.getEndDate().getMonthValue() + "/" + period.getEndDate().getDayOfMonth() : "";
                        groupScope.add(period.getName() + " (" + beginDateFormatted + " - " + endDateFormatted + ")");
                    }

                    Spinner groupScopeSpinner = dialogView.findViewById(R.id.groupScopeSpinner);
                    groupScopeSpinner.setAdapter(new ArrayAdapter<>(CreateTaskActivity.this, android.R.layout.simple_spinner_item, groupScope));

                    groupScopeSpinner.setSelection(groupScope.size() - 1);

                    EditText input = dialogView.findViewById(R.id.groupNameText);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);

                    builder.setView(dialogView);

                    builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!input.getText().toString().isEmpty()) {
                                lastPosition = position;

                                String className = input.getText().toString();

                                if(!items.contains(className)) {
                                    items.add(items.size() - 1, className);

                                    if(groupScopeSpinner.getSelectedItemPosition() == 0) {
                                        taskManager.addNewPersistentGroup(className);
                                    } else {
                                        taskManager.getTimePeriods().get(groupScopeSpinner.getSelectedItemPosition() - 1).addNewGroup(className);
                                    }

                                    groupSpinner.setSelection(items.size() - 2);
                                    groupSpinnerAdapter.notifyDataSetChanged();
                                }

                                updateSaveButton();
                            }
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            groupSpinner.setSelection(lastPosition);
                        }
                    });

                    builder.show();
                    input.requestFocus();
                } else {
                    lastPosition = position;

                    updateSaveButton();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        saveButton = findViewById(R.id.saveButton);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDateTime dueDateAndTime = dueDateObject.atStartOfDay().withHour(dueTimeObject.getHour()).withMinute(dueTimeObject.getMinute());
                String taskClass = groupSpinner.getSelectedItem().toString();
                float estimatedHours = Float.parseFloat(estimatedHoursEdit.getText().toString());
                TimePeriod timePeriod = taskManager.getCurrentTimePeriod();

                if(taskManager.getActiveEditTask() != null) {
                    Task editTask = taskManager.getActiveEditTask();
                    taskManager.setActiveEditTask(null);

                    if(editTask.isRepeatingTask()) {
                        //if did not yet start the first repeating task, no need to adjust the current task(s)
                        if(editTask.getParent().getStartDate().isAfter(LocalDate.now())) {
                            ((RepeatingTask) editTask.getParent()).updateAndSaveTask(repeatingTaskNamesAdapter.getTaskNames(),
                                    startDateObject.getDayOfWeek(), dueDateAndTime.getDayOfWeek(), dueTimeObject, taskManager.getCurrentGroupByName(taskClass), estimatedHours);
                        } else {
                            editTask.editTask(nameOfTaskEdit.getText().toString(), dueDateAndTime, taskManager.getCurrentGroupByName(taskClass), estimatedHours);
                            ((RepeatingTask) editTask.getParent()).updateAndSaveTask(repeatingTaskNamesAdapter.getTaskNames(),
                                    startDateObject.getDayOfWeek(), dueDateAndTime.getDayOfWeek(), dueTimeObject, taskManager.getCurrentGroupByName(taskClass), estimatedHours);
                        }
                    } else {
                        editTask.editTask(nameOfTaskEdit.getText().toString(), dueDateAndTime, taskManager.getCurrentGroupByName(taskClass), estimatedHours);
                        ((NonrepeatingTask) editTask.getParent()).updateAndSaveTask(startDateObject);
                    }
                } else {
                    if (repeatingEventSwitch.isChecked()) {
                        taskManager.addNewGeneratedTask(new RepeatingTask(taskManager, repeatingTaskNamesAdapter.getTaskNames(),
                                startDateObject, startDateObject.getDayOfWeek(), dueDateAndTime.getDayOfWeek(), dueTimeObject, taskManager.getCurrentGroupByName(taskClass), timePeriod, estimatedHours));
                    } else {
                        //TODO: change to local date time
                        taskManager.addNewGeneratedTask(new NonrepeatingTask(taskManager, startDateObject, timePeriod, nameOfTaskEdit.getText().toString(), dueDateAndTime, taskManager.getCurrentGroupByName(taskClass), estimatedHours));
                    }
                }

                Intent returnToMain = new Intent(CreateTaskActivity.this, MainActivity.class);
                startActivity(returnToMain);

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(saveButton.getWindowToken(), 0);
            }
        });

        if(taskManager.getActiveEditTask() != null) {
            setTaskValues(taskManager.getActiveEditTask());

            if(taskManager.getActiveEditTask().isRepeatingTask()) {
                setTitle("Edit repeating task...");
            } else {
                setTitle("Edit one-time task...");

                repeatingTaskNamesList.setAdapter(repeatingTaskNamesAdapter = new RepeatingTaskNamesAdapter(repeatingTaskNamesList, startDateObject, dueDateObject, this));
            }
        } else {
            setDefaultValues();

            repeatingTaskNamesList.setAdapter(repeatingTaskNamesAdapter = new RepeatingTaskNamesAdapter(repeatingTaskNamesList, startDateObject, dueDateObject, this));
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        nameOfTaskEdit.requestFocus();
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

    public void updateSaveButton() {
        saveButton.setEnabled(areRequiredFieldsFilled());
    }

    //editing an existing task
    private void setTaskValues(Task activeTask) {
        dueDateObject = activeTask.getDueDateTime().toLocalDate();
        dueDate.setText(formatLocalDate(dueDateObject));

        startDateObject = activeTask.getParent().getStartDate();
        startDate.setText(formatLocalDate(startDateObject));

        dueTimeObject = activeTask.getDueDateTime().toLocalTime();
        dueTime.setText(Utils.formatLocalTime(dueTimeObject));

        groupSpinner.setSelection(groupSpinnerAdapter.getPosition(activeTask.getGroupName()));

        estimatedHoursEdit.setText(String.valueOf(activeTask.getEstimatedCompletionTime()));

        if(activeTask.isRepeatingTask()) {
            repeatingTaskNamesList.setAdapter(repeatingTaskNamesAdapter = new RepeatingTaskNamesAdapter(repeatingTaskNamesList, startDateObject, dueDateObject, this));

            repeatingEventSwitch.setChecked(true);

            RepeatingTask generator = (RepeatingTask) activeTask.getParent();

            String[] taskNames = generator.getTaskNames();

            repeatingTaskNamesAdapter.setRepeatSize(taskNames.length);

            Set<Integer> disabled = new HashSet<>();

            for(int i = 0; i < taskNames.length; i++) {
                if(taskNames[i].isEmpty()) {
                    disabled.add(i);
                }
            }

            repeatingTaskNamesAdapter.setTaskNamesAndDisabled(taskNames, disabled);
        } else {
            nameOfTaskEdit.setText(activeTask.getName());
        }

        repeatingEventSwitch.setEnabled(false);
    }

    private void setDefaultValues() {
        dueDateObject = LocalDate.now();
        dueDate.setText(formatLocalDate(dueDateObject));

        startDateObject = LocalDate.now();
        startDate.setText(formatLocalDate(startDateObject));

        dueTimeObject = LocalTime.of(23, 59);
        dueTime.setText(Utils.formatLocalTime(dueTimeObject));
    }

    private String formatLocalDate(LocalDate date) {
        String dayOfWeek = date.getDayOfWeek().toString();

        return (dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + date.getMonthValue()
                + "/" + date.getDayOfMonth()+ "/" + date.getYear();
    }

    public boolean areRequiredFieldsFilled() {
        if(repeatingEventSwitch.isChecked()) {
            if(repeatingTaskNamesAdapter == null || !repeatingTaskNamesAdapter.areAllTasksNamed()) {
                return false;
            }
        } else {
            if(nameOfTaskEdit.getText().length() == 0) {
                return false;
            }
            if(!startDate.getText().toString().isEmpty() && !dueDate.getText().toString().isEmpty() && startDateObject.isAfter(dueDateObject)) {
                return false;
            }
        }

        if(estimatedHoursEdit.getText().length() == 0) {
            return false;
        }

        if(groupSpinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION || groupSpinner.getSelectedItemPosition() == 0) {
            return false;
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if(IncrementalApplication.taskManager.getActiveEditTask() != null) {
            IncrementalApplication.taskManager.setActiveEditTask(null);
        }
    }
}
