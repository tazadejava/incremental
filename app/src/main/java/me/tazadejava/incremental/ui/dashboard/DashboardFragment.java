package me.tazadejava.incremental.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.StackedValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.DayOfWeek;
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

public class DashboardFragment extends Fragment implements BackPressedInterface {

    private BarChart workBarChart;

    private RecyclerView dashboardView;
    private MainDashboardAdapter adapter;

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

    private String[] formatWorkDates(LocalDate[] dates) {
        String[] datesFormatted = new String[dates.length];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");

        LocalDate firstDate = dates[0];

        for(int i = 0; i < dates.length; i++) {
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

        workBarChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if(e.getY() > 0) {
                    Toast.makeText(getContext(), e.getY() + " hours worked", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {

            }
        });

        LocalDate[] dates = LogicalUtils.getWorkWeekDates();

        //format the graph

        Description description = new Description();

        description.setTextSize(12f);
        description.setText("Hours worked this week");

        workBarChart.setDescription(description);

        ValueFormatter xAxisFormatter = new IndexAxisValueFormatter(formatWorkDates(dates));

        XAxis xAxis = workBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        xAxis.setValueFormatter(xAxisFormatter);

        ValueFormatter yAxisFormatter = new DefaultValueFormatter(0);

        YAxis yAxis = workBarChart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setGranularity(1.0f);
        yAxis.setGranularityEnabled(true);
        yAxis.setLabelCount(3, true);
        yAxis.setAxisMinimum(0);
        yAxis.setValueFormatter(yAxisFormatter);
        yAxis.setDrawGridLines(true);

        int zeroLineColor = yAxis.getZeroLineColor();
        yAxis.setGridColor(Color.LTGRAY);
        yAxis.setZeroLineColor(zeroLineColor);

        //remove unnecessary labels

        workBarChart.getAxisRight().setDrawLabels(false);
        workBarChart.getAxisRight().setDrawAxisLine(false);
        workBarChart.getAxisRight().setDrawGridLines(false);
        workBarChart.getLegend().setEnabled(false);

        //set data

        refreshChartData(dates);
    }

    public void refreshChartData() {
        refreshChartData(LogicalUtils.getWorkWeekDates());
    }

    private void refreshChartData(LocalDate[] dates) {
        List<BarEntry> values = new ArrayList<>();

        StatsManager stats = IncrementalApplication.taskManager.getCurrentTimePeriod().getStatsManager();

        int index = 0;
        for(LocalDate date : dates) {
            float hours = stats.getHoursWorked(date);
            values.add(new BarEntry(index, hours));
            index++;
        }

        BarDataSet barDataSet = new BarDataSet(values, "Hours worked this week");

        barDataSet.setDrawValues(false);

        BarData data = new BarData(barDataSet);

        workBarChart.setData(data);
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