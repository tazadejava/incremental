package me.tazadejava.incremental.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.adapters.LargeHeightArrayAdapter;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

public class StatisticsCalendarHeatmapFragment extends StatisticsFragment {

    private CalendarMonthsAdapter monthAdapter;

    private Spinner heatmapGroupSpinner;
    private Switch toggleTimeInvariant;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ConstraintLayout root = (ConstraintLayout) inflater.inflate(R.layout.fragment_statistics_calendar_heatmap, container, false);

        toggleTimeInvariant = root.findViewById(R.id.toggleTimeInvariant);

        toggleTimeInvariant.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

                monthAdapter.setHeatmapGroup(heatmapGroupSpinner.getSelectedItemPosition() == 0 ? null :
                        taskManager.getCurrentGroupByName(heatmapGroupSpinner.getSelectedItem().toString())); //use to refresh the layout
            }
        });

        setupNav(root, container, StatisticsWorkloadTrendsFragment.class, StatisticsWorkloadTrendsFragment.class);

        setupCalendar(root);

        return root;
    }

    private void setupCalendar(ConstraintLayout root) {
        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

        heatmapGroupSpinner = root.findViewById(R.id.heatmapGroupSpinner);

        List<String> items = new ArrayList<>();
        items.add("All groups");
        items.addAll(taskManager.getAllCurrentGroupNames());

        ArrayAdapter<String> groupSpinnerAdapter = new LargeHeightArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, items);

        heatmapGroupSpinner.setAdapter(groupSpinnerAdapter);

        heatmapGroupSpinner.setSelection(0, false);
        heatmapGroupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0) {
                    monthAdapter.setHeatmapGroup(null);
                } else {
                    Group group = taskManager.getCurrentGroupByName(heatmapGroupSpinner.getSelectedItem().toString());
                    monthAdapter.setHeatmapGroup(group);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        RecyclerView calendarLayout = root.findViewById(R.id.calendarLayout);
        calendarLayout.setLayoutManager(new LinearLayoutManager(getContext()));

        List<YearMonth> yearMonths = new ArrayList<>();
        YearMonth endDate;

        if(taskManager.isCurrentTimePeriodActive()) {
            endDate = YearMonth.now();
        } else {
            endDate = YearMonth.from(taskManager.getCurrentTimePeriod().getEndDate());
        }

        YearMonth currentYearMonth = YearMonth.from(taskManager.getCurrentTimePeriod().getBeginDate());
        do {
            yearMonths.add(currentYearMonth);
            currentYearMonth = YearMonth.from(currentYearMonth.atEndOfMonth().plusDays(1));
        } while(currentYearMonth.isBefore(endDate) || currentYearMonth.equals(endDate));

        calendarLayout.setAdapter(monthAdapter = new CalendarMonthsAdapter(getActivity(), this, yearMonths.toArray(new YearMonth[0])));
    }

    public boolean shouldIncludeTimeInvariants() {
        return toggleTimeInvariant.isChecked();
    }
}