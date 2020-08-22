package me.tazadejava.incremental.ui.create;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
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
import java.util.List;

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
    private TextView dueDate, dueTime, startDateNonRepeating;
    private LocalDate dueDateObject, startDateNonrepeatingObject;
    private LocalTime dueTimeObject;
    private Spinner groupSpinner;

    private ArrayAdapter<String> groupSpinnerAdapter;

    private Button saveButton;

    private Switch repeatingEventSwitch;
    private ConstraintLayout repeatingEventDateLayout;
    private TextView startDate;
    private LocalDate startDateFormatted;

    private RecyclerView repeatingTaskNamesList;
    private RepeatingTaskAdapter repeatingTaskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        TaskManager taskManager = IncrementalApplication.taskManager;

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

        startDateNonRepeating = findViewById(R.id.startDateNonRepeating);

        startDateNonRepeating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateTaskActivity.this);

                if(startDateNonrepeatingObject != null) {
                    datePicker.updateDate(startDateNonrepeatingObject.getYear(), startDateNonrepeatingObject.getMonthValue() - 1, startDateNonrepeatingObject.getDayOfMonth());
                }

                datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        month++;
                        startDateNonrepeatingObject = LocalDate.of(year, month, dayOfMonth);

                        String dayOfWeek = startDateNonrepeatingObject.getDayOfWeek().toString();

                        startDateNonRepeating.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + startDateNonrepeatingObject.getMonthValue()
                                + "/" + startDateNonrepeatingObject.getDayOfMonth() + "/" + startDateNonrepeatingObject.getYear());

                        updateSaveButton();
                    }
                });

                datePicker.show();
            }
        });

        repeatingEventSwitch = findViewById(R.id.repeatingEventSwitch);
        repeatingEventDateLayout = findViewById(R.id.repeatingEventDateLayout);
        startDate = findViewById(R.id.startDate);

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateTaskActivity.this);

                if(startDateFormatted != null) {
                    datePicker.updateDate(startDateFormatted.getYear(), startDateFormatted.getMonthValue() - 1, startDateFormatted.getDayOfMonth());
                }

                datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        month++;
                        startDateFormatted = LocalDate.of(year, month, dayOfMonth);

                        String dayOfWeek = startDateFormatted.getDayOfWeek().toString();

                        startDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + startDateFormatted.getMonthValue()
                                + "/" + startDateFormatted.getDayOfMonth() + "/" + startDateFormatted.getYear());

                        repeatingTaskAdapter.setStartDate(startDateFormatted);
                        repeatingTaskAdapter.notifyDataSetChanged();
                    }
                });

                datePicker.show();
            }
        });

        repeatingEventSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    repeatingEventDateLayout.setVisibility(View.VISIBLE);
                    nameOfTaskEdit.setEnabled(false);

                    startDateFormatted = LocalDate.now();
                    String dayOfWeek = startDateFormatted.getDayOfWeek().toString();
                    startDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + startDateFormatted.getMonthValue()
                            + "/" + startDateFormatted.getDayOfMonth()+ "/" + startDateFormatted.getYear());

                    repeatingTaskAdapter.setStartDate(startDateFormatted);
                } else {
                    repeatingEventDateLayout.setVisibility(View.GONE);
                    nameOfTaskEdit.setEnabled(true);

                    startDateFormatted = null;
                }
            }
        });

        repeatingTaskNamesList = findViewById(R.id.repeatingTaskNamesList);

        repeatingTaskNamesList.setAdapter(repeatingTaskAdapter = new RepeatingTaskAdapter(repeatingTaskNamesList, startDateFormatted, this));
        repeatingTaskNamesList.setLayoutManager(new LinearLayoutManager(this));

        groupSpinner = findViewById(R.id.classSpinner);

        List<String> items = new ArrayList<>();

        items.add("Select class");

        items.addAll(taskManager.getAllTaskGroupNames());

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
                    builder.setTitle("Create new class");

                    EditText input = new EditText(CreateTaskActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(input);

                    builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!input.getText().toString().isEmpty()) {
                                lastPosition = position;

                                String className = input.getText().toString();

                                if(!items.contains(className)) {
                                    items.add(items.size() - 1, className);
                                    taskManager.addNewPersistentGroup(className);
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
                    //TODO: IMPLEMENT
                    System.out.println("TODO: i need to transfer all data needed to create a new generating task");
                } else {
                    if (startDateFormatted != null) {
                        taskManager.addNewGeneratedTask(new RepeatingTask(taskManager, repeatingTaskAdapter.getTaskNames(), startDateFormatted.getDayOfWeek(), dueDateAndTime.getDayOfWeek(), dueTimeObject, taskManager.getGroupByName(taskClass), timePeriod, estimatedHours));
                    } else {
                        //TODO: change to local date time
                        taskManager.addNewGeneratedTask(new NonrepeatingTask(taskManager, startDateNonrepeatingObject.atTime(0, 0), timePeriod, nameOfTaskEdit.getText().toString(), dueDateAndTime, taskManager.getGroupByName(taskClass), estimatedHours));
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
        } else {
            setDefaultValues();
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        nameOfTaskEdit.requestFocus();
    }

    public void updateSaveButton() {
        saveButton.setEnabled(areRequiredFieldsFilled());
    }

    private void setTaskValues(Task activeTask) {
        dueDateObject = activeTask.getDueDateTime().toLocalDate();
        dueDate.setText(formatLocalDate(dueDateObject));

        startDateNonrepeatingObject = LocalDate.now();
        startDateNonRepeating.setText(formatLocalDate(startDateNonrepeatingObject));

        dueTimeObject = activeTask.getDueDateTime().toLocalTime();
        dueTime.setText(Utils.formatLocalTime(dueTimeObject));

        groupSpinner.setSelection(groupSpinnerAdapter.getPosition(activeTask.getGroupName()));

        estimatedHoursEdit.setText(String.valueOf(activeTask.getEstimatedCompletionTime()));

        nameOfTaskEdit.setText(activeTask.getName());
    }

    private void setDefaultValues() {
        dueDateObject = LocalDate.now();
        dueDate.setText(formatLocalDate(dueDateObject));

        startDateNonrepeatingObject = LocalDate.now();
        startDateNonRepeating.setText(formatLocalDate(startDateNonrepeatingObject));

        dueTimeObject = LocalTime.of(23, 59);
        dueTime.setText(Utils.formatLocalTime(dueTimeObject));
    }

    private String formatLocalDate(LocalDate date) {
        String dayOfWeek = dueDateObject.getDayOfWeek().toString();

        return (dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + dueDateObject.getMonthValue()
                + "/" + dueDateObject.getDayOfMonth()+ "/" + dueDateObject.getYear();
    }

    public boolean areRequiredFieldsFilled() {
        if(repeatingEventSwitch.isChecked()) {
            if(!repeatingTaskAdapter.areAllTasksNamed()) {
                return false;
            }
        } else {
            if(nameOfTaskEdit.getText().length() == 0) {
                return false;
            }
            if(!startDateNonRepeating.getText().toString().isEmpty() && !dueDate.getText().toString().isEmpty() && startDateNonrepeatingObject.isAfter(dueDateObject)) {
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
}
