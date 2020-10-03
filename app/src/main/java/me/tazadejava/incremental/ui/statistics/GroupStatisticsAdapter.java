package me.tazadejava.incremental.ui.statistics;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.LogicalUtils;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.Utils;

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

    private HashMap<Group, Integer> maxIndexCurrentWeek = new HashMap<>();
    private HashMap<Group, Integer> maxMinutesCurrentWeek = new HashMap<>();
    private HashMap<Group, List<BarEntry>> barEntryValuesCurrentWeek = new HashMap<>();

    private HashMap<Group, Boolean> isViewingWeeklyAverage = new HashMap<>();

    public GroupStatisticsAdapter(Activity activity, TaskManager taskManager) {
        this.activity = activity;
        timePeriod = taskManager.getCurrentTimePeriod();

        groups = new ArrayList<>(timePeriod.getAllGroups());
        statsManager = timePeriod.getStatsManager();

        calculateDailyTrends();

        for(Group group : groups) {
            isViewingWeeklyAverage.put(group, true);
        }

        //sort them by maxIndex
        groups.sort(new Comparator<Group>() {
            @Override
            public int compare(Group group, Group t1) {
                return Integer.compare(maxMinutes.get(t1), maxMinutes.get(group));
            }
        });
    }

    /**
     * If viewing weekly average, switch to current week, and vice versa. Takes the value of the first group in the list only.
     */
    public void reverseAllWeeklyCharts() {
        boolean weeklyAverage = !isViewingWeeklyAverage.get(groups.get(0));

        for(Group group : groups) {
            isViewingWeeklyAverage.put(group, weeklyAverage);
        }

        if(weeklyAverage) {
            groups.sort(new Comparator<Group>() {
                @Override
                public int compare(Group group, Group t1) {
                    return Integer.compare(maxMinutes.get(t1), maxMinutes.get(group));
                }
            });
        } else {
            groups.sort(new Comparator<Group>() {
                @Override
                public int compare(Group group, Group t1) {
                    return Integer.compare(maxMinutesCurrentWeek.get(t1), maxMinutesCurrentWeek.get(group));
                }
            });
        }

        notifyDataSetChanged();
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

            //also, calculate the current week's numbers and data

            maxIndex = 0;
            maxMinutes = 0;
            List<BarEntry> entriesByDay = new ArrayList<>();

            int index = 0;
            for(LocalDate date : LogicalUtils.getWorkWeekDates()) {
                int minutesWorked = statsManager.getMinutesWorkedByGroup(group, date);
                entriesByDay.add(new BarEntry(index, minutesWorked));

                if(minutesWorked > maxMinutes) {
                    maxMinutes = minutesWorked;
                    maxIndex = index;
                }

                index++;
            }

            BarDataSet barDataSet = new BarDataSet(entriesByDay, "");
            barDataSet.setDrawValues(false);

            this.maxIndexCurrentWeek.put(group, maxIndex);
            this.maxMinutesCurrentWeek.put(group, maxMinutes);
            this.barEntryValuesCurrentWeek.put(group, entriesByDay);
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

        //remove unnecessary labels

        chart.getAxisRight().setDrawLabels(false);
        chart.getAxisRight().setDrawGridLines(false);
        chart.getLegend().setEnabled(false);

        //format the graph

        Description description = new Description();
        description.setTextSize(12f);
        description.setTextColor(Color.LTGRAY);
        chart.setDescription(description);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

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

        //set text colors

        int primaryTextColor = Utils.getAttrColor(activity, android.R.attr.textColorPrimary);

        chart.getXAxis().setTextColor(primaryTextColor);
        chart.getAxisLeft().setTextColor(primaryTextColor);

        //set to respective chart

        if(isViewingWeeklyAverage.get(group)) {
            setChartToWeeklyAverage(group, chart, description);
        } else {
            setChartToCurrentWeek(group, chart, description);
        }

        chart.animateY(500, Easing.EaseOutCubic);

        //attach touch listener

        chart.setOnTouchListener((view, motionEvent) -> {
            if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if(isViewingWeeklyAverage.get(group)) {
                    setChartToCurrentWeek(group, chart, description);
                } else {
                    setChartToWeeklyAverage(group, chart, description);
                }
                isViewingWeeklyAverage.put(group, !isViewingWeeklyAverage.get(group));
                return true;
            }

            return false;
        });
    }

    private void setChartToWeeklyAverage(Group group, BarChart chart, Description description) {
        //get the weeks to work with

        List<String> weeks = new ArrayList<>(Arrays.asList("Mon", "Tue", "Wed", "Thur", "Fri", "Sat", "Sun"));
        int weekSize = weeks.size();

        ValueFormatter xAxisFormatter = new IndexAxisValueFormatter(weeks);

        XAxis xAxis = chart.getXAxis();
        xAxis.setLabelCount(weekSize);
        xAxis.setValueFormatter(xAxisFormatter);

        //setup y axis

        ((DefaultValueFormatter) chart.getAxisLeft().getValueFormatter()).setup(0);

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
    }

    private void setChartToCurrentWeek(Group group, BarChart chart, Description description) {
        //instead of calculating average values, show the current week's minutes

        //get the weeks to work with

        List<String> weeks = new ArrayList<>(Arrays.asList("Mon", "Tue", "Wed", "Thur", "Fri", "Sat", "Sun"));
        int weekSize = weeks.size();

        ValueFormatter xAxisFormatter = new IndexAxisValueFormatter(weeks);

        XAxis xAxis = chart.getXAxis();
        xAxis.setLabelCount(weekSize);
        xAxis.setValueFormatter(xAxisFormatter);

        //setup y axis

        ((DefaultValueFormatter) chart.getAxisLeft().getValueFormatter()).setup(0);

        description.setText(group.getGroupName() + " - min daily for this week");

        BarDataSet barDataSet = new BarDataSet(barEntryValuesCurrentWeek.get(group), "");
        barDataSet.setDrawValues(false);

        int[] colors = new int[weekSize];
        for(int index = 0; index < weekSize; index++) {
            int relativeWeek = index == maxIndexCurrentWeek.get(group) ? 0 : 1;

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
