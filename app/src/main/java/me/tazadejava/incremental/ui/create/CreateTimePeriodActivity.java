package me.tazadejava.incremental.ui.create;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class CreateTimePeriodActivity extends AppCompatActivity {

    private TextView nameOfTimePeriod, startDate, endDate;
    private Button saveButton;

    private LocalDate startDateFormatted, endDateFormatted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_time_period);

        nameOfTimePeriod = findViewById(R.id.nameOfTimePeriod);
        startDate = findViewById(R.id.startDate);
        endDate = findViewById(R.id.endDate);
        saveButton = findViewById(R.id.saveButton);

        nameOfTimePeriod.addTextChangedListener(new TextWatcher() {
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

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateTimePeriodActivity.this);

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

                        updateSaveButton();
                    }
                });

                datePicker.show();
            }
        });

        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(CreateTimePeriodActivity.this);

                if(endDateFormatted != null) {
                    datePicker.updateDate(endDateFormatted.getYear(), endDateFormatted.getMonthValue() - 1, endDateFormatted.getDayOfMonth());
                }

                datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        month++;
                        endDateFormatted = LocalDate.of(year, month, dayOfMonth);

                        String dayOfWeek = endDateFormatted.getDayOfWeek().toString();

                        endDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + endDateFormatted.getMonthValue()
                                + "/" + endDateFormatted.getDayOfMonth() + "/" + endDateFormatted.getYear());

                        updateSaveButton();
                    }
                });

                datePicker.show();
            }
        });

        TaskManager taskManager = ((IncrementalApplication) getApplication()).getTaskManager();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(taskManager.addNewTimePeriod(nameOfTimePeriod.getText().toString(), startDateFormatted, endDateFormatted)) {
                    Intent returnToMain = new Intent(CreateTimePeriodActivity.this, MainActivity.class);
                    startActivity(returnToMain);
                } else {
                    AlertDialog.Builder failedToCreateGroup = new AlertDialog.Builder(CreateTimePeriodActivity.this);
                    failedToCreateGroup.setTitle("Failed to create time period!");

                    boolean foundReason = false;

                    for(TimePeriod timePeriod : taskManager.getTimePeriods()) {
                        if(timePeriod.getName().equals(nameOfTimePeriod.getText().toString())) {
                            failedToCreateGroup.setMessage("A time period with that name already exists!");
                            foundReason = true;
                            break;
                        }
                    }

                    if(!foundReason) {
                        failedToCreateGroup.setMessage("Your time period conflicts with another time period's dates!");
                    }

                    failedToCreateGroup.show();
                }
            }
        });

        setDefaultValues();

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        nameOfTimePeriod.requestFocus();
    }

    private void setDefaultValues() {
        startDateFormatted = LocalDate.now();
        endDateFormatted = LocalDate.now().plusDays(31);

        String dayOfWeek = startDateFormatted.getDayOfWeek().toString();

        startDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + startDateFormatted.getMonthValue()
                + "/" + startDateFormatted.getDayOfMonth()+ "/" + startDateFormatted.getYear());

        endDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + endDateFormatted.getMonthValue()
                + "/" + endDateFormatted.getDayOfMonth()+ "/" + endDateFormatted.getYear());
    }

    private void updateSaveButton() {
        if(nameOfTimePeriod.getText().length() == 0) {
            saveButton.setEnabled(false);
            return;
        }
        if(startDateFormatted.isAfter(endDateFormatted)) {
            saveButton.setEnabled(false);
            return;
        }
        if(((IncrementalApplication) getApplication()).getTaskManager().doesTimePeriodExist(nameOfTimePeriod.getText().toString())) {
            saveButton.setEnabled(false);
            return;
        }

        saveButton.setEnabled(true);
    }
}
