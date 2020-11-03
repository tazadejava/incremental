package me.tazadejava.incremental.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.LogicalUtils;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class DashboardFragment extends Fragment implements BackPressedInterface {

    private BarChart workBarChart;

    private RecyclerView dashboardView;
    private MainDashboardAdapter adapter;

    private Description description;
    private boolean isShowingHours;

    private int currentDateOffset;
    private LocalDate[] currentDates;

    private LocalDate lastRefreshDate;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //change the FAB to create a new task
        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.VISIBLE);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskManager.setActiveEditTask(null);

                Intent createTask = new Intent(getContext(), CreateTaskActivity.class);
                startActivity(createTask);
            }
        });

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        workBarChart = root.findViewById(R.id.workBarChart);

        createWorkChart();

        //theme the work chart

        int primaryTextColor = Utils.getAttrColor(getActivity(), android.R.attr.textColorPrimary);

        workBarChart.getXAxis().setTextColor(primaryTextColor);
        workBarChart.getAxisLeft().setTextColor(primaryTextColor);

        dashboardView = root.findViewById(R.id.dashboard_day_list);
        dashboardView.setAdapter(adapter = new MainDashboardAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(), this, getActivity()));
        dashboardView.setLayoutManager(new LinearLayoutManager(getContext()));

        lastRefreshDate = LocalDate.now();

        return root;
    }
//
//    @Override
//    public void onConfigurationChanged(@NonNull Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//
//        int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
//        applyThemeConfig(currentNightMode == Configuration.UI_MODE_NIGHT_YES);
//    }
//
//    private void applyThemeConfig(boolean nightMode) {
//        if(nightMode) {
//            TypedValue tv = new TypedValue();
//            getActivity().getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
//            workBarChart.getXAxis().setTextColor(ContextCompat.getColor(getContext(), tv.resourceId));
//        } else {
//            workBarChart.setBackgroundColor(Color.RED);
//        }
//    }

    private String[] formatWorkDates() {
        String[] datesFormatted = new String[currentDates.length];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");

        LocalDate firstDate = currentDates[0];

        for(int i = 0; i < currentDates.length; i++) {
            datesFormatted[i] = firstDate.format(formatter);
            firstDate = firstDate.plusDays(1);
        }

        return datesFormatted;
    }

    private void createWorkChart() {
        workBarChart.setPinchZoom(false);
        workBarChart.setDrawGridBackground(false);
        workBarChart.setAutoScaleMinMaxEnabled(true);
        workBarChart.setScaleEnabled(false);
        workBarChart.setHighlightFullBarEnabled(false);
        workBarChart.setHighlightPerTapEnabled(false);
        workBarChart.setHighlightPerDragEnabled(false);

        workBarChart.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeLeft() {
                for(int i = 0; i < currentDates.length; i++) {
                    currentDates[i] = currentDates[i].plusDays(7);
                }

                currentDateOffset++;
                refreshChartData();
            }

            @Override
            public void onSwipeRight() {
                for(int i = 0; i < currentDates.length; i++) {
                    currentDates[i] = currentDates[i].minusDays(7);
                }

                currentDateOffset--;
                refreshChartData();
            }
        });

        LocalDate[] dates = LogicalUtils.getWorkWeekDates();
        currentDates = dates;

        //format the graph

        description = new Description();
        description.setTextSize(12f);
        description.setTextColor(Color.LTGRAY);
        workBarChart.setDescription(description);

        ValueFormatter xAxisFormatter = new IndexAxisValueFormatter(formatWorkDates());

        XAxis xAxis = workBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        xAxis.setValueFormatter(xAxisFormatter);

        ValueFormatter yAxisFormatter = new DefaultValueFormatter(1);

        YAxis yAxis = workBarChart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setGranularity(1.0f);
        yAxis.setGranularityEnabled(true);
        yAxis.setLabelCount(4, true);

        yAxis.setAxisMinimum(0);

        yAxis.setValueFormatter(yAxisFormatter);
        yAxis.setDrawGridLines(true);
        yAxis.setMinWidth(30);
        yAxis.setMaxWidth(30);

        int zeroLineColor = yAxis.getZeroLineColor();
        yAxis.setGridColor(Color.LTGRAY);
        yAxis.setZeroLineColor(zeroLineColor);

        //remove unnecessary labels

        workBarChart.getAxisRight().setDrawLabels(false);
        workBarChart.getAxisRight().setDrawGridLines(false);
        workBarChart.getLegend().setEnabled(false);

        //set data

        refreshChartData();
    }

    public void refreshChartData() {
        //refresh axes

        ((IndexAxisValueFormatter) workBarChart.getXAxis().getValueFormatter()).setValues(formatWorkDates());

        if(currentDateOffset == 0) {
            int primaryTextColor = Utils.getAttrColor(getActivity(), android.R.attr.textColorPrimary);
            workBarChart.getXAxis().setTextColor(primaryTextColor);
        } else {
            workBarChart.getXAxis().setTextColor(ContextCompat.getColor(getContext(), R.color.primaryColor));
        }

        //refresh data

        StatsManager stats = ((IncrementalApplication) getActivity().getApplication()).getTaskManager().getCurrentTimePeriod().getStatsManager();

        int maxMinutes = 0;
        int totalMinutes = 0;
        int totalNonzeroDays = 0;
        for(LocalDate date : currentDates) {
            int minutes = stats.getMinutesWorked(date);

            maxMinutes = Math.max(maxMinutes, minutes);

            if(minutes > 0) {
                totalMinutes += minutes;
                totalNonzeroDays++;
            }
        }

        List<BarEntry> values = new ArrayList<>();
        if(totalNonzeroDays != 0 && totalMinutes / totalNonzeroDays > 60) {
            isShowingHours = true;
            float totalHours = totalMinutes / 60f;
            totalHours = Math.round(totalHours * 10f) / 10f;
            description.setText("Hours worked weekly (" + totalHours + " total)");
            ((DefaultValueFormatter) workBarChart.getAxisLeft().getValueFormatter()).setup(1);

            int index = 0;
            for(LocalDate date : currentDates) {
                float hours = stats.getMinutesWorked(date) / 60f;
                values.add(new BarEntry(index, hours));
                index++;
            }

            workBarChart.getAxisLeft().setAxisMaximum((int) Math.ceil(maxMinutes / 60f));
        } else {
            isShowingHours = false;
            description.setText("Minutes worked weekly (" + totalMinutes + " total)");
            ((DefaultValueFormatter) workBarChart.getAxisLeft().getValueFormatter()).setup(0);

            int index = 0;
            for(LocalDate date : currentDates) {
                int minutes = stats.getMinutesWorked(date);
                values.add(new BarEntry(index, minutes));
                index++;
            }

            workBarChart.getAxisLeft().setAxisMaximum(maxMinutes);
        }

        BarDataSet barDataSet = new BarDataSet(values, "");
        barDataSet.setDrawValues(false);

        int[] colors = new int[currentDates.length];
        int index = 0;
        for(LocalDate date : currentDates) {
            if(date.equals(LocalDate.now())) {
                colors[index] = ContextCompat.getColor(getContext(), R.color.primaryColor);
            } else {
                colors[index] = ContextCompat.getColor(getContext(), R.color.secondaryColor);
            }
            index++;
        }

        barDataSet.setColors(colors);

        BarData data = new BarData(barDataSet);

        workBarChart.setData(data);
        workBarChart.animateY(500, Easing.EaseOutCubic);
    }

    @Override
    public void onResume() {
        super.onResume();

        //refresh contents

        //if new day, then reset the task manager
        if(!lastRefreshDate.equals(LocalDate.now())) {
            lastRefreshDate = LocalDate.now();
            ((IncrementalApplication) getActivity().getApplication()).reset();

            dashboardView.setAdapter(adapter = new MainDashboardAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(), this, getActivity()));

            if(lastRefreshDate.getDayOfWeek() == DayOfWeek.MONDAY) {
                currentDateOffset++;
            }
            refreshChartData();
        } else {
            dashboardView.setAdapter(adapter);
        }
    }

    @Override
    public void onBackPressed() {
        dashboardView.smoothScrollToPosition(0);
    }
}