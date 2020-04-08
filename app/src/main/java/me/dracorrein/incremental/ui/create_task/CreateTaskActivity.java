package me.dracorrein.incremental.ui.create_task;

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

import me.dracorrein.incremental.R;
import me.dracorrein.incremental.Utils;
import me.dracorrein.incremental.logic.dashboard.Task;
import me.dracorrein.incremental.ui.main.IncrementalApplication;
import me.dracorrein.incremental.ui.main.MainActivity;

public class CreateTaskActivity extends AppCompatActivity {

    private EditText nameOfTaskEdit, estimatedHoursEdit;
    private TextView dueDate, dueTime;
    private LocalDate dueDateFormatted;
    private LocalTime dueTimeFormatted;

    private Button saveButton;

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
                saveButton.setEnabled(!nameOfTaskEdit.getText().toString().isEmpty());
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

        saveButton = findViewById(R.id.saveButton);

        saveButton.setEnabled(false);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalDateTime dueDateAndTime = dueDateFormatted.atStartOfDay().withHour(dueTimeFormatted.getHour()).withMinute(dueTimeFormatted.getMinute());

                Task task = new Task(nameOfTaskEdit.getText().toString(), dueDateAndTime);

                if(!estimatedHoursEdit.getText().toString().isEmpty()) {
                    task.setEstimatedCompletionTime(Float.parseFloat(estimatedHoursEdit.getText().toString()));
                }

                IncrementalApplication.taskManager.addNewTask(task);

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
}
