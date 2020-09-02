package me.tazadejava.incremental.ui.create;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.RepeatingTask;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class CreateTaskRepeatingNameDateFragment extends Fragment implements BackPressedInterface {

    private Button nextButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CreateTaskActivity act = (CreateTaskActivity) getActivity();
        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

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

        Spinner startDayOfWeek = root.findViewById(R.id.startDayOfWeek);

        RecyclerView repeatingTaskNamesList = root.findViewById(R.id.repeatingTaskNamesList);
        repeatingTaskNamesList.setLayoutManager(new LinearLayoutManager(getContext()));
        Button duplicateAllTaskEntries = root.findViewById(R.id.duplicateAllTaskEntries);
        RepeatingTaskNamesAdapter repeatingTaskNamesAdapter = new RepeatingTaskNamesAdapter(act, duplicateAllTaskEntries, this, act.getStartDate(), act.getDueDayOfWeek());

        duplicateAllTaskEntries.setEnabled(false);

        duplicateAllTaskEntries.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());

                dialog.setTitle("What type of duplication would you like to perform?");

                dialog.setMessage("You can either copy the first task word-for-word or append the subsequent tasks with numbers (starting with " + repeatingTaskNamesAdapter.getFirstNumberOfTask() + ", going up)");

                dialog.setPositiveButton("Copy word-for-word", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        repeatingTaskNamesAdapter.duplicateFirstTaskNameToAll();
                    }
                });

                dialog.setNegativeButton("Append tasks with numbers", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        repeatingTaskNamesAdapter.duplicateFirstTaskNameToAllWithNumbering();
                    }
                });

                dialog.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                dialog.show();
            }
        });

        TextView startDate = root.findViewById(R.id.startDate);
        TextView dueTime = root.findViewById(R.id.dueTime);

        if(taskManager.getActiveEditTask() != null) {
            startDayOfWeek.setVisibility(View.VISIBLE);
            startDate.setVisibility(View.GONE);

            TextView startDateTitleText = root.findViewById(R.id.startDateTitleText);

            startDateTitleText.setText("Start Day of Week");
        }

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

        Button minusWeek = root.findViewById(R.id.minusWeek);

        minusWeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentWeeks;
                if(Utils.isInteger(repeatingWeeksInput.getText().toString())) {
                    currentWeeks = Integer.parseInt(repeatingWeeksInput.getText().toString());
                } else {
                    currentWeeks = 1;
                }

                if(currentWeeks > 1) {
                    repeatingWeeksInput.setText(String.valueOf(currentWeeks - 1));
                }
            }
        });

        Button plusWeek = root.findViewById(R.id.plusWeek);

        plusWeek.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentWeeks;
                if(Utils.isInteger(repeatingWeeksInput.getText().toString())) {
                    currentWeeks = Integer.parseInt(repeatingWeeksInput.getText().toString());
                } else {
                    currentWeeks = 1;
                }

                repeatingWeeksInput.setText(String.valueOf(currentWeeks + 1));
            }
        });

        DayOfWeek[] dayOfWeekValues = DayOfWeek.values();

        ArrayAdapter<String> groupSpinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>());

        updateDueDayOfWeek(act, act.getStartDate(), groupSpinnerAdapter, taskManager.getActiveEditTask() == null);

        dueDayOfWeek.setAdapter(groupSpinnerAdapter);

        dueDayOfWeek.setSelection(Utils.getDaysBetweenDaysOfWeek(act.getStartDate().getDayOfWeek(), act.getDueDayOfWeek()));

        dueDayOfWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                DayOfWeek dayOfWeek = act.getStartDate().plusDays(i).getDayOfWeek();

                act.setDueDayOfWeek(dayOfWeek);

                repeatingTaskNamesAdapter.setDueDayOfWeek(dayOfWeek);

                updateNextButton(repeatingTaskNamesAdapter, act);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if(taskManager.getActiveEditTask() != null) {
            List<String> items = new ArrayList<>();

            DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd");

            for(DayOfWeek dayOfWeek : dayOfWeekValues) {
                String val = dayOfWeek.toString();

                LocalDate potentialStartDate = taskManager.getActiveEditTask().getParent().getStartDate().plusDays(
                        Utils.getDaysBetweenDaysOfWeek(taskManager.getActiveEditTask().getParent().getStartDate().getDayOfWeek(), dayOfWeek));
                if(potentialStartDate.equals(LocalDate.now())) {
                    items.add(val.charAt(0) + val.substring(1).toLowerCase() + " (starts today, " + potentialStartDate.format(format) + ")");
                } else {
                    items.add(val.charAt(0) + val.substring(1).toLowerCase() + " (starts " + potentialStartDate.format(format) + ")");
                }
            }

            startDayOfWeek.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, items));

            int index = 0;
            for(int i = 0; i < 7; i++) {
                if(dayOfWeekValues[i] == ((RepeatingTask) taskManager.getActiveEditTask().getParent()).getDayOfWeekTaskBegins()) {
                    index = i;
                    break;
                }
            }

            startDayOfWeek.setSelection(index);

            startDayOfWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    DayOfWeek dayOfWeek = dayOfWeekValues[i];

                    LocalDate date = act.getStartDate().plusDays(Utils.getDaysBetweenDaysOfWeek(act.getStartDate().getDayOfWeek(), dayOfWeek));
                    act.setStartDate(date);

                    updateDueDayOfWeek(act, date, groupSpinnerAdapter);

                    repeatingTaskNamesAdapter.setStartDate(date);
                    repeatingTaskNamesAdapter.setDueDayOfWeek(act.getDueDayOfWeek());
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        }

        //multiple day of weeks

        Button multipleDatesButton = root.findViewById(R.id.multipleDatesButton);

        RecyclerView multipleStartEndDatesList = root.findViewById(R.id.multipleStartEndDatesList);
        MultipleDaysOfWeekAdapter daysOfWeekAdapter = new MultipleDaysOfWeekAdapter(getActivity(), multipleStartEndDatesList, multipleDatesButton, act.getStartDate());
        multipleStartEndDatesList.setLayoutManager(new LinearLayoutManager(getContext()));
        multipleStartEndDatesList.setAdapter(daysOfWeekAdapter);

        multipleDatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(multipleStartEndDatesList.getVisibility() != View.VISIBLE) {
                    multipleStartEndDatesList.setVisibility(View.VISIBLE);
                }

                daysOfWeekAdapter.addAdditionalWeek();

                if(daysOfWeekAdapter.getItemCount() == 6) {
                    multipleDatesButton.setEnabled(false);
                }
            }
        });

        //submit button

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HashMap<LocalDate, DayOfWeek> additionalDaysOfWeek = daysOfWeekAdapter.getAllAdditionalDaysOfWeek();

                if(additionalDaysOfWeek.size() > 0) {
                    act.setAdditionalDueDatesRepeating(additionalDaysOfWeek);
                }

                act.createTask();
            }
        });

        //start date/due date

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

                        updateDueDayOfWeek(act, startDateObject, groupSpinnerAdapter);

                        repeatingTaskNamesAdapter.setStartDate(startDateObject);
                        repeatingTaskNamesAdapter.setDueDayOfWeek(act.getDueDayOfWeek());
                        daysOfWeekAdapter.setStartDate(startDateObject);
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

        //check for editing

        if(taskManager.getActiveEditTask() != null) {
            startDate.setEnabled(false);
        }

        //check for previous data; put defaults if non-existent

        if(act.getTaskNames() != null) {
            repeatingWeeksInput.setText(String.valueOf(act.getTaskNames().length));
            repeatingTaskNamesAdapter.setTaskNamesAndDisabled(act.getTaskNames(), act.getDisabledTasks());
        } else {
            repeatingWeeksInput.setText("1");
        }

        repeatingTaskNamesList.setAdapter(repeatingTaskNamesAdapter);

        useAverageRepeatingSwitch.setChecked(act.isUseAverageEstimateRepeating());

        startDate.setText(Utils.formatLocalDateWithDayOfWeek(act.getStartDate()));

        dueTime.setText(Utils.formatLocalTime(act.getDueTime()));

        backButton.bringToFront();
        nextButton.bringToFront();

        return root;
    }

    private void updateDueDayOfWeek(CreateTaskActivity act, LocalDate startDate, ArrayAdapter<String> dueDateAdapter) {
        updateDueDayOfWeek(act, startDate, dueDateAdapter, true);
    }

    private void updateDueDayOfWeek(CreateTaskActivity act, LocalDate startDate, ArrayAdapter<String> dueDateAdapter, boolean updateDueDate) {
        List<String> itemsAnnotated = new ArrayList<>();
        for(int index = 0; index < 7; index++) {
            DayOfWeek dow = startDate.getDayOfWeek();
            String dowFormatted = dow.toString().charAt(0) + dow.toString().substring(1).toLowerCase();
            String dueDateText = dowFormatted + " (+" + index + " day" + (index == 1 ? "" : "s") + ")";
            itemsAnnotated.add(dueDateText);

            startDate = startDate.plusDays(1);
        }

        dueDateAdapter.clear();
        dueDateAdapter.addAll(itemsAnnotated);
        dueDateAdapter.notifyDataSetChanged();

        if(updateDueDate) {
            act.setDueDayOfWeek(startDate.getDayOfWeek());
        }
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