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
import java.util.Calendar;
import java.util.TimeZone;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class CreateTimePeriodActivity extends AppCompatActivity {

    private TextView nameOfTimePeriod, startDate, endDate;
    private Button saveButton;

    private LocalDate startDateFormatted, endDateFormatted;

    private TimePeriod editingTimePeriod;

    private long minDate = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_time_period);

        TaskManager taskManager = ((IncrementalApplication) getApplication()).getTaskManager();

        //check for extra: if so, edit activity instead of creating one
        String timePeriodName;
        if((timePeriodName = getIntent().getStringExtra("timePeriod")) != null) {
            for(TimePeriod timePeriod : taskManager.getTimePeriods()) {
                if(timePeriod.getName().equals(timePeriodName)) {
                    editingTimePeriod = timePeriod;
                    break;
                }
            }
        }

        for(TimePeriod timePeriod : taskManager.getTimePeriods()) {
            if(timePeriod == editingTimePeriod) {
                continue;
            }
            if(timePeriod.getBeginDate() == null || timePeriod.getEndDate() == null) {
                continue;
            }

            System.out.println(timePeriod.getName() + " " + timePeriod.getBeginDate() + " AND " + timePeriod.getEndDate());
            minDate = Math.max(minDate, dateToMilliseconds(timePeriod.getEndDate().plusDays(1)));
        }

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

                datePicker.getDatePicker().setMinDate(minDate);

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

                        if(startDateFormatted.isAfter(endDateFormatted)) {
                            endDateFormatted = startDateFormatted.plusDays(0);

                            endDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + endDateFormatted.getMonthValue()
                                    + "/" + endDateFormatted.getDayOfMonth() + "/" + endDateFormatted.getYear());
                        }

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

                datePicker.getDatePicker().setMinDate(Math.max(minDate, dateToMilliseconds(startDateFormatted)));

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

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editingTimePeriod != null) {
                    editingTimePeriod.setBeginDate(startDateFormatted);
                    editingTimePeriod.setEndDate(endDateFormatted);
                } else {
                    if(taskManager.addNewTimePeriod(nameOfTimePeriod.getText().toString(), startDateFormatted, endDateFormatted)) {
                        Intent returnToMain = new Intent(CreateTimePeriodActivity.this, MainActivity.class);
                        startActivity(returnToMain);
                    } else {
                        AlertDialog.Builder failedToCreateGroup = new AlertDialog.Builder(CreateTimePeriodActivity.this);
                        failedToCreateGroup.setTitle("Failed to create time period!");
                        failedToCreateGroup.setMessage("A time period with that name already exists!");
                        failedToCreateGroup.show();
                    }
                }
            }
        });

        if(editingTimePeriod != null) {
            startDateFormatted = editingTimePeriod.getBeginDate();
            endDateFormatted = editingTimePeriod.getEndDate();

            setTitle("Edit time period...");
            nameOfTimePeriod.setText(editingTimePeriod.getName());
            nameOfTimePeriod.setEnabled(false);
        } else {
            startDateFormatted = LocalDate.now();
            endDateFormatted = LocalDate.now().plusDays(31);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

            nameOfTimePeriod.requestFocus();
        }

        String dayOfWeek = startDateFormatted.getDayOfWeek().toString();

        startDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + startDateFormatted.getMonthValue()
                + "/" + startDateFormatted.getDayOfMonth()+ "/" + startDateFormatted.getYear());

        endDate.setText((dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + endDateFormatted.getMonthValue()
                + "/" + endDateFormatted.getDayOfMonth()+ "/" + endDateFormatted.getYear());

        updateSaveButton();
    }

    private long dateToMilliseconds(LocalDate date) {
        Calendar c = Calendar.getInstance();
        c.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());

        return c.getTimeInMillis();
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
        if(editingTimePeriod == null && ((IncrementalApplication) getApplication()).getTaskManager().doesTimePeriodExist(nameOfTimePeriod.getText().toString())) {
            saveButton.setEnabled(false);
            return;
        }

        saveButton.setEnabled(true);
    }
}
