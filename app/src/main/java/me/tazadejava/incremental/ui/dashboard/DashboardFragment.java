package me.tazadejava.incremental.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.LogicalUtils;
import me.tazadejava.incremental.logic.statistics.StatsManager;
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

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //change the FAB to create a new task
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent createTask = new Intent(getContext(), CreateTaskActivity.class);
                startActivity(createTask);
            }
        });

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        workBarChart = root.findViewById(R.id.workBarChart);

        createWorkChart();

        dashboardView = root.findViewById(R.id.dashboard_day_list);
        dashboardView.setAdapter(adapter = new MainDashboardAdapter(this, getContext()));
        dashboardView.setLayoutManager(new LinearLayoutManager(getContext()));

        return root;
    }

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
        yAxis.setLabelCount(3, true);
        yAxis.setAxisMinimum(0);
        yAxis.setValueFormatter(yAxisFormatter);
        yAxis.setDrawGridLines(true);
        yAxis.setMinWidth(20);
        yAxis.setMaxWidth(20);

        int zeroLineColor = yAxis.getZeroLineColor();
        yAxis.setGridColor(Color.LTGRAY);
        yAxis.setZeroLineColor(zeroLineColor);

        //remove unnecessary labels

        workBarChart.getAxisRight().setDrawLabels(false);
        workBarChart.getAxisRight().setDrawAxisLine(false);
        workBarChart.getAxisRight().setDrawGridLines(false);
        workBarChart.getLegend().setEnabled(false);

        //set data

        refreshChartData();
    }

    public void refreshChartData() {
        //refresh axes

        ((IndexAxisValueFormatter) workBarChart.getXAxis().getValueFormatter()).setValues(formatWorkDates());

        if(currentDateOffset == 0) {
            workBarChart.getXAxis().setTextColor(Color.DKGRAY);
        } else {
            workBarChart.getXAxis().setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        }

        //refresh data

        StatsManager stats = IncrementalApplication.taskManager.getCurrentTimePeriod().getStatsManager();

        int totalMinutes = 0;
        int totalNonzeroDays = 0;
        for(LocalDate date : currentDates) {
            int minutes = stats.getMinutesWorked(date);

            if(minutes > 0) {
                totalMinutes += minutes;
                totalNonzeroDays++;
            }
        }

        List<BarEntry> values = new ArrayList<>();
        if(totalNonzeroDays != 0 && totalMinutes / totalNonzeroDays > 60) {
            isShowingHours = true;
            description.setText("Hours worked weekly");
            ((DefaultValueFormatter) workBarChart.getAxisLeft().getValueFormatter()).setup(1);

            int index = 0;
            for(LocalDate date : currentDates) {
                float hours = stats.getMinutesWorked(date) / 60f;
                values.add(new BarEntry(index, hours));
                index++;
            }
        } else {
            isShowingHours = false;
            description.setText("Minutes worked weekly");
            ((DefaultValueFormatter) workBarChart.getAxisLeft().getValueFormatter()).setup(0);

            int index = 0;
            for(LocalDate date : currentDates) {
                int minutes = stats.getMinutesWorked(date);
                values.add(new BarEntry(index, minutes));
                index++;
            }
        }

        BarDataSet barDataSet = new BarDataSet(values, "");
        barDataSet.setDrawValues(false);
        BarData data = new BarData(barDataSet);

        workBarChart.setData(data);
        workBarChart.animateY(500, Easing.EaseOutCubic);
    }

    @Override
    public void onResume() {
        super.onResume();

        //refresh contents
        dashboardView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {

    }
}