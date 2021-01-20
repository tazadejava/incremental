package me.tazadejava.incremental.ui.archive;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class ArchiveFragment extends Fragment implements BackPressedInterface {

    private RecyclerView groupView;
    private PastTasksListAdapter adapter;

    private List<Task> currentTasks;
    private int lastSearchLength = -1;

    private LocalDate showDate;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.GONE);

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_archive, container, false);

        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

        List<Task> allTasks = new ArrayList<>(taskManager.getCurrentTimePeriod().getCompletedTasksHistory());

        groupView = root.findViewById(R.id.dashboard_day_list);
        groupView.setAdapter(adapter = new PastTasksListAdapter(allTasks));
        groupView.setLayoutManager(new LinearLayoutManager(getContext()));

        EditText searchTaskEdit = root.findViewById(R.id.searchTaskEdit);

        TextView resultsText = root.findViewById(R.id.resultsText);
        resultsText.setText("");

        searchTaskEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String text = editable.toString().toLowerCase();

                if(text.length() > 0) {
                    //if the text increased, then we are specifying more
                    //else, we are specifying less and must reload all
                    //can be improved in the future by trie data implementation
                    List<Task> tasksList;
                    if(lastSearchLength != -1 && text.length() > lastSearchLength) {
                        tasksList = currentTasks;
                    } else {
                        tasksList = allTasks;
                    }

                    List<Task> newTasks = new ArrayList<>();
                    HashMap<Task, Integer> occurrencePosition = new HashMap<>();

                    for(Task task : tasksList) {
                        if(showDate != null) {
                            if (!task.getLastTaskWorkedTime().toLocalDate().equals(showDate)) {
                                continue;
                            }
                        }

                        String searchString = task.getName().toLowerCase() + " " + task.getGroupName().toLowerCase();
                        int index = searchString.indexOf(text);
                        if(index != -1) {
                            occurrencePosition.put(task, index);

                            if(newTasks.isEmpty()) {
                                newTasks.add(task);
                            } else {
                                boolean added = false;
                                for (int i = 0; i < newTasks.size(); i++) {
                                    if (occurrencePosition.get(newTasks.get(i)) >= index) {
                                        newTasks.add(i, task);
                                        added = true;
                                        break;
                                    }
                                }
                                
                                if(!added) {
                                    newTasks.add(task);
                                }
                            }
                        }
                    }

                    adapter.setTasks(newTasks);

                    lastSearchLength = text.length();
                    currentTasks = newTasks;

                    resultsText.setText(currentTasks.size() + " result" + (currentTasks.size() == 1 ? "" : "s"));
                } else {
                    List<Task> newTasks = new ArrayList<>();
                    if(showDate != null) {
                        newTasks = new ArrayList<>();

                        for (Task task : allTasks) {
                            if (task.getLastTaskWorkedTime().toLocalDate().equals(showDate)) {
                                newTasks.add(task);
                            }
                        }
                    } else {
                        newTasks = allTasks;
                    }

                    adapter.setTasks(newTasks);
                    resultsText.setText("");
                }
            }
        });

        TextView dateSelector = root.findViewById(R.id.dateSelector);
        Button showAllDatesButton = root.findViewById(R.id.showAllDatesButton);

        showAllDatesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(showDate != null) {
                    adapter.setTasks(allTasks);
                    showDate = null;

                    dateSelector.setText("Showing: all dates");
                    showAllDatesButton.setText("SET DATE");

                    searchTaskEdit.setText(searchTaskEdit.getText());
                } else {
                    DatePickerDialog datePicker = new DatePickerDialog(getContext());

                    datePicker.getDatePicker().setMinDate(Utils.localDateToMilliseconds(taskManager.getCurrentTimePeriod().getBeginDate()));

                    if(taskManager.isCurrentTimePeriodActive()) {
                        datePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
                    } else {
                        datePicker.getDatePicker().setMaxDate(Utils.localDateToMilliseconds(taskManager.getCurrentTimePeriod().getEndDate()));
                    }

                    if(showDate != null) {
                        datePicker.updateDate(showDate.getYear(), showDate.getMonthValue() - 1, showDate.getDayOfMonth());
                    }

                    datePicker.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            showDate = LocalDate.of(year, month + 1, dayOfMonth);
                            dateSelector.setText("Showing: " + Utils.formatLocalDateWithDayOfWeek(showDate));
                            showAllDatesButton.setText("ALL DATES");

                            searchTaskEdit.setText(searchTaskEdit.getText());
                        }
                    });

                    datePicker.show();
                }
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        //refresh contents
        groupView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        ((MainActivity) getActivity()).getNavController().popBackStack();
    }
}