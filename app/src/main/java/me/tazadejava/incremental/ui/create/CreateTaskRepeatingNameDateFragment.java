package me.tazadejava.incremental.ui.create;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class CreateTaskRepeatingNameDateFragment extends Fragment implements BackPressedInterface {

    private Button nextButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CreateTaskActivity act = (CreateTaskActivity) getActivity();
        TaskManager taskManager = IncrementalApplication.taskManager;

        if(taskManager.getActiveEditTask() != null) {
            act.setTitle("Edit routine task");
        } else {
            act.setTitle("Create new routine task");
        }
        act.setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_create_task_repeating_name_date, container, false);

        Button backButton = root.findViewById(R.id.backButton);
        nextButton = root.findViewById(R.id.nextButton);

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

        //activate some data early on for the adapter

        if(act.getStartDate() == null) {
            act.setStartDate(LocalDate.now());
        }

        if(act.getDueDayOfWeek() == null) {
            act.setDueDayOfWeek(LocalDate.now().getDayOfWeek());
        }

        if(act.getDueTime() == null) {
            act.setDueTime(LocalTime.of(23, 59));
        }

        EditText repeatingWeeksInput = root.findViewById(R.id.repeatingWeeksInput);
        Switch useAverageRepeatingSwitch = root.findViewById(R.id.useAverageRepeatingSwitch);
        Spinner dueDayOfWeek = root.findViewById(R.id.dueDayOfWeek);

        RecyclerView repeatingTaskNamesList = root.findViewById(R.id.repeatingTaskNamesList);
        repeatingTaskNamesList.setLayoutManager(new LinearLayoutManager(getContext()));
        RepeatingTaskNamesAdapter repeatingTaskNamesAdapter = new RepeatingTaskNamesAdapter(act, this, act.getStartDate(), act.getDueDayOfWeek());

        TextView startDate = root.findViewById(R.id.startDate);
        TextView dueTime = root.findViewById(R.id.dueTime);

        repeatingWeeksInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.length() > 0) {
                    int amount = Integer.parseInt(repeatingWeeksInput.getText().toString());

                    if(amount > 0) {
                        repeatingTaskNamesAdapter.setRepeatSize(amount);
                        repeatingTaskNamesAdapter.notifyDataSetChanged();
                    }

                    updateNextButton(repeatingTaskNamesAdapter, act);
                }
            }
        });

        List<String> items = new ArrayList<>();

        for(DayOfWeek dayOfWeek : DayOfWeek.values()) {
            String val = dayOfWeek.toString();

            items.add(val.charAt(0) + val.substring(1).toLowerCase());
        }

        ArrayAdapter<String> groupSpinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, items);
        dueDayOfWeek.setAdapter(groupSpinnerAdapter);

        int index = 0;

        DayOfWeek[] dayOfWeekValues = DayOfWeek.values();
        for(int i = 0; i < 7; i++) {
            if(dayOfWeekValues[i] == act.getDueDayOfWeek()) {
                index = i;
                break;
            }
        }

        dueDayOfWeek.setSelection(index);

        dueDayOfWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                DayOfWeek dayOfWeek = dayOfWeekValues[i];

                act.setDueDayOfWeek(dayOfWeek);

                repeatingTaskNamesAdapter.setDueDayOfWeek(dayOfWeek);
                repeatingTaskNamesAdapter.notifyDataSetChanged();

                updateNextButton(repeatingTaskNamesAdapter, act);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

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
                        dueTime.setText(Utils.formatLocalTime(hourOfDay, minute));
                    }
                };

                LocalTime dueTimeObject = act.getDueTime();
                TimePickerDialog timePicker = new TimePickerDialog(getContext(), onTimeSetListener, dueTimeObject.getHour(), dueTimeObject.getMinute(), false);
                timePicker.show();
            }
        });

        //check for previous data; put defaults if non-existent

        repeatingTaskNamesList.setAdapter(repeatingTaskNamesAdapter);

        if(act.getTaskNames() != null) {
            repeatingWeeksInput.setText(String.valueOf(act.getTaskNames().length));
            repeatingTaskNamesAdapter.setTaskNamesAndDisabled(act.getTaskNames(), act.getDisabledTasks());
        }

        useAverageRepeatingSwitch.setChecked(act.isUseAverageEstimateRepeating());

        startDate.setText(Utils.formatLocalDateWithDayOfWeek(act.getStartDate()));

        dueTime.setText(Utils.formatLocalTime(act.getDueTime()));

        return root;
    }

    public void updateNextButton(RepeatingTaskNamesAdapter repeatingTaskNamesAdapter, CreateTaskActivity act) {
        if(!repeatingTaskNamesAdapter.areAllTasksNamed()) {
            nextButton.setEnabled(false);
            return;
        }

        if(act.getDueDayOfWeek() == null) {
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