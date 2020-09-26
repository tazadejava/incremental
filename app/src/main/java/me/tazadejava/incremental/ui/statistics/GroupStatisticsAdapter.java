package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.LogicalUtils;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;
import me.tazadejava.incremental.ui.timeperiods.TimePeriodsListAdapter;

public class GroupStatisticsAdapter extends RecyclerView.Adapter<GroupStatisticsAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public BarChart chart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            chart = itemView.findViewById(R.id.chart);
        }
    }

    private Activity activity;

    private TimePeriod timePeriod;
    private List<Group> groups;
    private StatsManager statsManager;

    private int dateWeeksSize;
    private HashMap<Group, Integer> maxIndex = new HashMap<>();
    private HashMap<Group, Integer> maxMinutes = new HashMap<>();
    private HashMap<Group, List<BarEntry>> barEntryValues = new HashMap<>();

    public GroupStatisticsAdapter(Activity activity, TaskManager taskManager) {
        this.activity = activity;
        timePeriod = taskManager.getCurrentTimePeriod();

        groups = new ArrayList<>(timePeriod.getAllGroups());
        statsManager = timePeriod.getStatsManager();

        calculateDailyTrends();

        //sort them by maxIndex
        groups.sort(new Comparator<Group>() {
            @Override
            public int compare(Group group, Group t1) {
                return Integer.compare(maxMinutes.get(t1), maxMinutes.get(group));
            }
        });
    }

    private void calculateDailyTrends() {
        List<LocalDate[]> dateWeeks = new ArrayList<>();
        LocalDate currentDate = timePeriod.getBeginDate();

        int currentWeekNumber = LogicalUtils.getWeekNumber(LocalDate.now());
        while (LogicalUtils.getWeekNumber(currentDate) <= currentWeekNumber) {
            dateWeeks.add(LogicalUtils.getWorkWeekDates(currentDate));
            currentDate = currentDate.plusDays(7);
        }

        dateWeeksSize = dateWeeks.size();

        int weekSize = 7;

        for(Group group : groups) {
            //refresh data

            int[] totalMinutes = new int[weekSize];

            //calculate average minutes per day

            for (LocalDate[] dates : dateWeeks) {
                for (int i = 0; i < 7; i++) {
                    totalMinutes[i] += statsManager.getMinutesWorkedByGroup(group, dates[i]);
                }
            }

            List<BarEntry> values = new ArrayList<>();
            int maxMinutes = 0;

            int i = 0;
            for (int mins : totalMinutes) {
                maxMinutes = Math.max(maxMinutes, mins);

                values.add(new BarEntry(i, mins / dateWeeks.size()));
                i++;
            }

            //get max minutes index

            int maxIndex = 0;
            i = 0;
            for (int mins : totalMinutes) {
                if (mins == maxMinutes) {
                    maxIndex = i;
                    break;
                }
                i++;
            }

            this.maxIndex.put(group, maxIndex);
            this.maxMinutes.put(group, maxMinutes);
            this.barEntryValues.put(group, values);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group group = groups.get(position);

        BarChart chart = holder.chart;

        //define the graph

        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setAutoScaleMinMaxEnabled(true);
        chart.setScaleEnabled(false);
        chart.setHighlightFullBarEnabled(false);
        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);

        //get the weeks to work with

        List<String> weeks = new ArrayList<>(Arrays.asList("Mon", "Tue", "Wed", "Thur", "Fri", "Sat", "Sun"));

        int weekSize = weeks.size();

        //format the graph

        Description description = new Description();
        description.setTextSize(12f);
        description.setTextColor(Color.LTGRAY);
        chart.setDescription(description);

        ValueFormatter xAxisFormatter = new IndexAxisValueFormatter(weeks);

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

        int primaryTextColor = Utils.getAttrColor(activity, android.R.attr.textColorPrimary);

        chart.getXAxis().setTextColor(primaryTextColor);
        chart.getAxisLeft().setTextColor(primaryTextColor);

        //remove unnecessary labels

        chart.getAxisRight().setDrawLabels(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getLegend().setEnabled(false);

        ((DefaultValueFormatter) chart.getAxisLeft().getValueFormatter()).setup(0);
//        chart.getAxisLeft().setAxisMaximum((int) Math.ceil(totalAverageMinutes / 60f) + 5);
        description.setText(group.getGroupName() + " - average min per day (" + dateWeeksSize + ")");

        BarDataSet barDataSet = new BarDataSet(barEntryValues.get(group), "");
        barDataSet.setDrawValues(false);

        int maxIndex = this.maxIndex.get(group);

        int[] colors = new int[weekSize];
        for(int index = 0; index < weekSize; index++) {
            int relativeWeek = index == maxIndex ? 0 : 1;

            switch(relativeWeek) {
                case 0:
                    colors[index] = ContextCompat.getColor(activity, R.color.primaryColor);
                    break;
                case 1:
                    colors[index] = ContextCompat.getColor(activity, R.color.secondaryColor);
                    break;
            }
        }
        barDataSet.setColors(colors);

        BarData data = new BarData(barDataSet);

        chart.setData(data);
        chart.animateY(500, Easing.EaseOutCubic);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_graph_card, parent, false);
        return new GroupStatisticsAdapter.ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }
}
