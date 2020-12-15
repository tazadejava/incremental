package me.tazadejava.incremental.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class StatisticsCalendarHeatmapFragment extends StatisticsFragment {

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        ConstraintLayout root = (ConstraintLayout) inflater.inflate(R.layout.fragment_statistics_calendar_heatmap, container, false);

        setupNav(root, container, StatisticsWorkloadTrendsFragment.class, StatisticsWorkloadTrendsFragment.class);

        setupCalendar(root);

        return root;
    }

    private void setupCalendar(ConstraintLayout root) {
        RecyclerView calendarLayout = root.findViewById(R.id.calendarLayout);

        calendarLayout.setLayoutManager(new LinearLayoutManager(getContext()));

        List<YearMonth> yearMonths = new ArrayList<>();
        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();
        YearMonth now = YearMonth.now();

        YearMonth currentYearMonth = YearMonth.from(taskManager.getCurrentTimePeriod().getBeginDate());
        do {
            yearMonths.add(currentYearMonth);
            currentYearMonth = YearMonth.from(currentYearMonth.atEndOfMonth().plusDays(1));
        } while(currentYearMonth.isBefore(now) || currentYearMonth.equals(now));

        calendarLayout.setAdapter(new CalendarMonthsAdapter(getActivity(), yearMonths.toArray(new YearMonth[0])));
    }
}