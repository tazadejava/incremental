package me.dracorrein.incremental.ui.create_task;

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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.dracorrein.incremental.R;
import me.dracorrein.incremental.Utils;
import me.dracorrein.incremental.logic.dashboard.RepeatingTask;
import me.dracorrein.incremental.logic.dashboard.Task;
import me.dracorrein.incremental.logic.dashboard.TaskManager;
import me.dracorrein.incremental.ui.main.IncrementalApplication;
import me.dracorrein.incremental.ui.main.MainActivity;

public class CreateTaskActivity extends AppCompatActivity {

    private EditText nameOfTaskEdit, estimatedHoursEdit;
    private TextView dueDate, dueTime;
    private LocalDate dueDateFormatted;
    private LocalTime dueTimeFormatted;
    private Spinner classSpinner;

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
                saveButton.setEnabled(areRequiredFieldsFilled());
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
                saveButton.setEnabled(areRequiredFieldsFilled());
            }
        });

        dueDate = findViewById(R.id.dueDate);

        dueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateTaskActivity.this);

                datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        month++;
                        dueDateFormatted = LocalDate.of(year, month, dayOfMonth);

                        String dayOfWeek = dueDateFormatted.getDayOfWeek().toString();

                        dueDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + dueDateFormatted.getMonthValue()
                                + "/" + dueDateFormatted.getDayOfMonth() + "/" + dueDateFormatted.getYear());
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
                        dueTimeFormatted = LocalTime.of(hourOfDay, minute);
                        dueTime.setText(Utils.getDueDateTimeFormatted(hourOfDay, minute));
                    }
                };

                TimePickerDialog timePicker = new TimePickerDialog(CreateTaskActivity.this, onTimeSetListener, dueTimeFormatted.getHour(), dueTimeFormatted.getMinute(), false);
                timePicker.show();
            }
        });

        repeatingEventSwitch = findViewById(R.id.repeatingEventSwitch);
        repeatingEventDateLayout = findViewById(R.id.repeatingEventDateLayout);
        startDate = findViewById(R.id.startDate);

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateTaskActivity.this);

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

        repeatingTaskNamesList.setAdapter(repeatingTaskAdapter = new RepeatingTaskAdapter(repeatingTaskNamesList, startDateFormatted));
        repeatingTaskNamesList.setLayoutManager(new LinearLayoutManager(this));

        classSpinner = findViewById(R.id.classSpinner);

        List<String> items = new ArrayList<>();

        items.add("Select class");

        items.addAll(taskManager.getAllClasses());

        items.add("Add new class...");

        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        classSpinner.setAdapter(classAdapter);

        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                                String className = input.getText().toString();

                                if(!items.contains(className)) {
                                    items.add(items.size() - 1, className);
                                    taskManager.getAllClasses().add(className);
                                    classSpinner.setSelection(items.size() - 2);

                                    classAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    builder.show();
                    input.requestFocus();
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
                LocalDateTime dueDateAndTime = dueDateFormatted.atStartOfDay().withHour(dueTimeFormatted.getHour()).withMinute(dueTimeFormatted.getMinute());

                String taskClass = classSpinner.getSelectedItem().toString();
                float estimatedHours = Float.parseFloat(estimatedHoursEdit.getText().toString());

                String timePeriod = taskManager.getCurrentTimePeriod();

                if(startDateFormatted != null) {
                    RepeatingTask repeatingTask = new RepeatingTask(repeatingTaskAdapter.getTaskNames(), startDateFormatted.getDayOfWeek(), dueDateAndTime.getDayOfWeek(), dueTimeFormatted, taskClass, timePeriod, estimatedHours);

                    taskManager.addNewRepeatingTask(repeatingTask);
                } else {
                    Task task = new Task(nameOfTaskEdit.getText().toString(), dueDateAndTime, taskClass, timePeriod, estimatedHours);

                    taskManager.addNewTask(task);
                }

                Intent returnToMain = new Intent(CreateTaskActivity.this, MainActivity.class);
                startActivity(returnToMain);
            }
        });

        setDefaultValues();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        nameOfTaskEdit.requestFocus();
    }

    private void setDefaultValues() {
        dueDateFormatted = LocalDate.now();

        String dayOfWeek = dueDateFormatted.getDayOfWeek().toString();
        
        dueDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + dueDateFormatted.getMonthValue()
                + "/" + dueDateFormatted.getDayOfMonth()+ "/" + dueDateFormatted.getYear());

        dueTimeFormatted = LocalTime.of(23, 59);
        dueTime.setText(Utils.getDueDateTimeFormatted(23, 59));
    }

    public boolean areRequiredFieldsFilled() {
        if(nameOfTaskEdit.getText().toString().isEmpty()) {
            return false;
        }
        if(estimatedHoursEdit.getText().toString().isEmpty()) {
            return false;
        }
        if(classSpinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION || classSpinner.getSelectedItemPosition() == 0) {
            return false;
        }

        return true;
    }
}
