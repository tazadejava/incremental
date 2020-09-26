package me.tazadejava.incremental.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.LogicalUtils;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.ui.dashboard.OnSwipeTouchListener;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class StatisticsFragment extends Fragment implements BackPressedInterface {

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.GONE);

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_statistics, container, false);

        BarChart workloadTrendsChart = root.findViewById(R.id.workloadTrendChart);

        defineWorkloadTrends(workloadTrendsChart);

        return root;
    }

    private void defineWorkloadTrends(BarChart chart) {
        //define the graph

        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setAutoScaleMinMaxEnabled(true);
        chart.setScaleEnabled(false);
        chart.setHighlightFullBarEnabled(false);
        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);

        //get the weeks to work with

        int weekSize = 5;

        List<LocalDate[]> weeks = new ArrayList<>();
        for(int i = 0; i < weekSize; i++) {
            weeks.add(LogicalUtils.getWorkWeekDates(LocalDate.now().plusDays(7 * (i - 3))));
        }

        String[] weeksFormatted = new String[weekSize];

        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd");

        for(int i = 0; i < weekSize; i++) {
            LocalDate[] week = weeks.get(i);
            weeksFormatted[i] = week[0].format(format) + "-" + week[6].format(format);
        }

        //format the graph

        Description description = new Description();
        description.setTextSize(12f);
        description.setTextColor(Color.LTGRAY);
        chart.setDescription(description);

        ValueFormatter xAxisFormatter = new IndexAxisValueFormatter(weeksFormatted);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(weekSize);

        xAxis.setValueFormatter(xAxisFormatter);

        ValueFormatter yAxisFormatter = new DefaultValueFormatter(1);

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setGranularity(1.0f);
        yAxis.setGranularityEnabled(true);

        yAxis.setAxisMinimum(0);

        yAxis.setValueFormatter(yAxisFormatter);
        yAxis.setDrawGridLines(true);

        int zeroLineColor = yAxis.getZeroLineColor();
        yAxis.setGridColor(Color.LTGRAY);
        yAxis.setZeroLineColor(zeroLineColor);

        //set colors

        int primaryTextColor = Utils.getAttrColor(getActivity(), android.R.attr.textColorPrimary);

        chart.getXAxis().setTextColor(primaryTextColor);
        chart.getAxisLeft().setTextColor(primaryTextColor);

        //remove unnecessary labels

        chart.getAxisRight().setDrawLabels(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getLegend().setEnabled(false);

        //set the data

        //refresh data

        StatsManager stats = ((IncrementalApplication) getActivity().getApplication()).getTaskManager().getCurrentTimePeriod().getStatsManager();

        int[] totalMinutes = new int[weekSize];
        int maxMinutes = 0;
        int totalNonzeroDays = 0;

        //mark trends for past two weeks and current week
        for(int i = 0; i < weekSize - 1; i++) {
            LocalDate[] dates = weeks.get(i);
            for(LocalDate date : dates) {
                int minutes = stats.getMinutesWorked(date);

                if(minutes > 0) {
                    totalMinutes[i] += minutes;
                    totalNonzeroDays++;
                }
            }

            maxMinutes = Math.max(maxMinutes, totalMinutes[i]);
        }

        //guess trends for next week
        int startIndex;
        boolean currentWeekIsProjection;

        if(totalMinutes[weekSize - 2] == 0) {
            currentWeekIsProjection = true;
            startIndex = weekSize - 2;
        } else {
            currentWeekIsProjection = false;
            startIndex = weekSize - 1;
        }

        TimePeriod currentTimePeriod = ((IncrementalApplication) getActivity().getApplication()).getTaskManager().getCurrentTimePeriod();
        for(int i = startIndex; i < weekSize; i++) {
            int projectedMinutes = 0;

            for(LocalDate date : weeks.get(i)) {
                System.out.println(date);
                projectedMinutes += currentTimePeriod.getEstimatedMinutesOfWorkForDate(date);
            }

            totalMinutes[i] = projectedMinutes;

            maxMinutes = Math.max(maxMinutes, projectedMinutes);
        }

        List<BarEntry> values = new ArrayList<>();
        if(totalNonzeroDays != 0 && maxMinutes > 60) {
            ((DefaultValueFormatter) chart.getAxisLeft().getValueFormatter()).setup(1);

            int index = 0;
            for(int min : totalMinutes) {
                float hours = min / 60f;
                values.add(new BarEntry(index, hours));
                index++;
            }

            //add offset to allow for top axis to appear
            chart.getAxisLeft().setAxisMaximum((int) Math.ceil(maxMinutes / 60f) + 5);
            description.setText("Average hours worked per week (transparent is projection)");
        } else {
            ((DefaultValueFormatter) chart.getAxisLeft().getValueFormatter()).setup(0);

            int index = 0;
            for(int min : totalMinutes) {
                values.add(new BarEntry(index, min));
                index++;
            }

            //add offset to allow for top axis to appear
            chart.getAxisLeft().setAxisMaximum(maxMinutes + 30);
            description.setText("Average minutes worked per week (transparent is projection)");
        }

        BarDataSet barDataSet = new BarDataSet(values, "");
        barDataSet.setDrawValues(false);

        int[] colors = new int[weekSize];
        for(int index = 0; index < weekSize; index++) {
            int relativeWeek;
            if(index < weekSize - 2) {
                relativeWeek = -1;
            } else if (index == weekSize - 2) {
                if(currentWeekIsProjection) {
                    relativeWeek = 2;
                } else {
                    relativeWeek = 0;
                }
            } else {
                relativeWeek = 1;
            }

            switch(relativeWeek) {
                case 0:
                    colors[index] = ContextCompat.getColor(getContext(), R.color.primaryColor);
                    break;
                case 2:
                    Color primaryColor = Color.valueOf(ContextCompat.getColor(getContext(), R.color.primaryColor));
                    colors[index] = Color.argb(0.5f, primaryColor.red(), primaryColor.green(), primaryColor.blue());
                    break;
                case 1:
                    colors[index] = Color.argb(0.5f, 0.7f, 0.7f, 0.7f);
                    break;
                case -1:
                    colors[index] = ContextCompat.getColor(getContext(), R.color.secondaryColor);
                    break;
            }
        }
        barDataSet.setColors(colors);

        BarData data = new BarData(barDataSet);

        chart.setData(data);
        chart.animateY(500, Easing.EaseOutCubic);
    }

    @Override
    public void onBackPressed() {
        ((MainActivity) getActivity()).getNavController().popBackStack();
    }
}