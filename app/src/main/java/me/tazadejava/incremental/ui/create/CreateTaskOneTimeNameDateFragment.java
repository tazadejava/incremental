package me.tazadejava.incremental.ui.create;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.fragment.app.Fragment;

import java.time.LocalDate;
import java.time.LocalTime;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class CreateTaskOneTimeNameDateFragment extends Fragment implements BackPressedInterface {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CreateTaskActivity act = (CreateTaskActivity) getActivity();
        TaskManager taskManager = IncrementalApplication.taskManager;

        if(taskManager.getActiveEditTask() != null) {
            act.setTitle("Edit one-time task");
        } else {
            act.setTitle("Create new one-time task");
        }
        act.setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_create_task_one_time_name_date, container, false);

        Button backButton = root.findViewById(R.id.backButton);
        Button nextButton = root.findViewById(R.id.nextButton);

        if(taskManager.getActiveEditTask() != null) {
            nextButton.setText("SAVE CHANGES");
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                act.createTask();
            }
        });

        EditText taskNameInput = root.findViewById(R.id.taskNameInput);
        TextView startDate = root.findViewById(R.id.startDate);
        TextView dueDate = root.findViewById(R.id.dueDate);
        TextView dueTime = root.findViewById(R.id.dueTime);

        taskNameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateNextButton(taskNameInput, nextButton);

                if(s.length() > 0) {
                    act.setTaskName(s.toString());
                }
            }
        });

        startDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(getContext());

                datePicker.getDatePicker().setMinDate(System.currentTimeMillis());

                LocalDate startDateObject = act.getStartDate();
                if(startDateObject != null) {
                    datePicker.updateDate(startDateObject.getYear(), startDateObject.getMonthValue() - 1, startDateObject.getDayOfMonth());
                }

                datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        LocalDate startDateObject = LocalDate.of(year, month + 1, dayOfMonth);
                        act.setStartDate(startDateObject);
                        startDate.setText(Utils.formatLocalDateWithDayOfWeek(startDateObject));

                        if(act.getDueDate().isBefore(startDateObject)) {
                            act.setDueDate(startDateObject);
                            dueDate.setText(Utils.formatLocalDateWithDayOfWeek(act.getDueDate()));
                        }

                        updateNextButton(taskNameInput, nextButton);
                    }
                });

                datePicker.show();
            }
        });

        dueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePicker = new DatePickerDialog(getContext());

                if(act.getStartDate().isAfter(LocalDate.now())) {
                    //convert days to milliseconds to set the min date
                    datePicker.getDatePicker().setMinDate((act.getStartDate().toEpochDay() + 1) * 24 * 60 * 60 * 1000);
                } else {
                    datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
                }

                LocalDate dueDateObject = act.getDueDate();
                if(dueDateObject != null) {
                    datePicker.updateDate(dueDateObject.getYear(), dueDateObject.getMonthValue() - 1, dueDateObject.getDayOfMonth());
                }

                datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        LocalDate dueDateObject = LocalDate.of(year, month + 1, dayOfMonth);
                        act.setDueDate(dueDateObject);
                        dueDate.setText(Utils.formatLocalDateWithDayOfWeek(dueDateObject));

                        updateNextButton(taskNameInput, nextButton);
                    }
                });

                datePicker.show();
            }
        });

        dueTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TimePickerDialog.OnTimeSetListener onTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        LocalTime dueTimeObject = LocalTime.of(hourOfDay, minute);
                        act.setDueTime(dueTimeObject);
                        dueTime.setText(Utils.formatLocalTime(dueTimeObject));
                    }
                };

                LocalTime dueTimeObject = act.getDueTime();
                TimePickerDialog timePicker = new TimePickerDialog(getContext(), onTimeSetListener, dueTimeObject.getHour(), dueTimeObject.getMinute(), false);
                timePicker.show();
            }
        });

        //check for previous data; put defaults if non-existent

        if(act.getTaskName() != null) {
            taskNameInput.setText(act.getTaskName());

            updateNextButton(taskNameInput, nextButton);
        }

        if(act.getStartDate() == null) {
            act.setStartDate(LocalDate.now());
        }

        startDate.setText(Utils.formatLocalDateWithDayOfWeek(act.getStartDate()));

        if(act.getDueDate() == null) {
            act.setDueDate(LocalDate.now());
        }

        dueDate.setText(Utils.formatLocalDateWithDayOfWeek(act.getDueDate()));

        if(act.getDueTime() == null) {
            act.setDueTime(LocalTime.of(23, 59));
        }

        dueTime.setText(Utils.formatLocalTime(act.getDueTime()));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) taskNameInput.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                taskNameInput.requestFocus();
            }
        }, 100);

        return root;
    }

    private void updateNextButton(EditText taskNameInput, Button nextButton) {
        if(taskNameInput.getText().length() == 0) {
            nextButton.setEnabled(false);
            return;
        }

        nextButton.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.createTaskFrame, new CreateTaskGroupTimeFragment())
                .commit();
    }
}