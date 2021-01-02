package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;
import com.github.mikephil.charting.formatter.DefaultValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.LogicalUtils;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.Utils;

public class GroupWorkloadTrendsStatisticsAdapter extends RecyclerView.Adapter<GroupWorkloadTrendsStatisticsAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public BarChart chart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            chart = itemView.findViewById(R.id.chart);
        }
    }

    private Activity activity;

    private TaskManager taskManager;

    private List<DayOfWeek> daysOfWeek;
    private int currentDayOfWeek;

    private TimePeriod timePeriod;
    private List<Group> groups;
    private StatsManager statsManager;

    private int dateWeeksSize;
    private HashMap<Group, Integer> maxIndex = new HashMap<>();
    private HashMap<Group, Integer> maxMinutes = new HashMap<>();
    private HashMap<Group, List<Integer>> weeklyAverageMinutesByGroupAndDay = new HashMap<>();

    private HashMap<Group, Integer> maxIndexCurrentWeek = new HashMap<>();
    private HashMap<Group, Integer> maxMinutesCurrentWeek = new HashMap<>();
    private HashMap<Group, List<Integer>> currentWeekAveragesByGroupAndDay = new HashMap<>();

    private boolean isViewingWeeklyAverage = true;

    public GroupWorkloadTrendsStatisticsAdapter(Activity activity, TaskManager taskManager) {
        this.activity = activity;
        this.taskManager = taskManager;
        timePeriod = taskManager.getCurrentTimePeriod();

        daysOfWeek = new ArrayList<>(Arrays.asList(DayOfWeek.values()));

        currentDayOfWeek = daysOfWeek.indexOf(LocalDate.now().getDayOfWeek());

        groups = new ArrayList<>(timePeriod.getAllGroups());
        statsManager = timePeriod.getStatsManager();

        calculateDailyTrends();
    }

    /**
     * If viewing weekly average, switch to current week, and vice versa. Takes the value of the first group in the list only.
     */
    public boolean reverseAllWeeklyCharts() {
        isViewingWeeklyAverage = !isViewingWeeklyAverage;

        notifyDataSetChanged();

        return isViewingWeeklyAverage;
    }

    private void calculateDailyTrends() {
        List<LocalDate[]> dateWeeks = new ArrayList<>();
        LocalDate currentDate = timePeriod.getBeginDate();

        LocalDate finalDate;
        if(taskManager.isCurrentTimePeriodActive()) {
            finalDate = LocalDate.now();
        } else {
            finalDate = taskManager.getCurrentTimePeriod().getEndDate();
        }

        while (currentDate.plusDays(-1).isBefore(finalDate)) {
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

            List<Integer> values = new ArrayList<>();
            int maxMinutes = 0;

            int i = 0;
            for (int mins : totalMinutes) {
                maxMinutes = Math.max(maxMinutes, mins);

                values.add(mins / dateWeeks.size());
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
            this.weeklyAverageMinutesByGroupAndDay.put(group, values);

            //also, calculate the current week's numbers and data

            maxIndex = 0;
            maxMinutes = 0;
            List<Integer> entriesByDay = new ArrayList<>();

            int index = 0;
            for(LocalDate date : LogicalUtils.getWorkWeekDates()) {
                int minutesWorked = statsManager.getMinutesWorkedByGroup(group, date);
                entriesByDay.add(minutesWorked);

                if(minutesWorked > maxMinutes) {
                    maxMinutes = minutesWorked;
                    maxIndex = index;
                }

                index++;
            }

            this.maxIndexCurrentWeek.put(group, maxIndex);
            this.maxMinutesCurrentWeek.put(group, maxMinutes);
            this.currentWeekAveragesByGroupAndDay.put(group, entriesByDay);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayOfWeek dow = daysOfWeek.get(position);

        BarChart chart = holder.chart;

        //define the graph

        chart.setDrawGridBackground(false);
        chart.setAutoScaleMinMaxEnabled(true);
        chart.setPinchZoom(false);
        chart.setScaleXEnabled(false);
        chart.setScaleYEnabled(false);
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
        description.setTextColor(Utils.getThemeAttrColor(activity, R.attr.subtextColor));
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

        int primaryTextColor = Utils.getAndroidAttrColor(activity, android.R.attr.textColorPrimary);

        chart.getXAxis().setTextColor(primaryTextColor);
        chart.getAxisLeft().setTextColor(primaryTextColor);

        //set to respective chart

        if(isViewingWeeklyAverage) {
            setChartToWeeklyAverage(dow, chart, description);
        } else {
            setChartToCurrentWeek(dow, chart, description);
        }

        chart.animateY(300, Easing.EaseOutCubic);

        //attach touch listener

//        chart.setOnTouchListener((view, motionEvent) -> {
//            if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                if(isViewingWeeklyAverage) {
//                    setChartToCurrentWeek(dow, chart, description);
//                } else {
//                    setChartToWeeklyAverage(dow, chart, description);
//                }
//                isViewingWeeklyAverage = !isViewingWeeklyAverage;
//                return true;
//            }
//
//            return false;
//        });
    }

    private void setChartToWeeklyAverage(DayOfWeek dow, BarChart chart, Description description) {
        setCustomDataChart(dow, chart, description, weeklyAverageMinutesByGroupAndDay);
    }

    private void setChartToCurrentWeek(DayOfWeek dow, BarChart chart, Description description) {
        setCustomDataChart(dow, chart, description, currentWeekAveragesByGroupAndDay);
    }

    private void setCustomDataChart(DayOfWeek dow, BarChart chart, Description description, HashMap<Group, List<Integer>> minuteValues) {
        int dowIndex = daysOfWeek.indexOf(dow);

        //calculate the minutes by group

        List<BarEntry> barEntries = new ArrayList<>();

        HashMap<Group, Integer> minutesByGroupForDOW = new HashMap<>();

        for(Group group : groups) {
            int minutes = minuteValues.get(group).get(dowIndex);
            if(minutes > 0) {
                minutesByGroupForDOW.put(group, minutes);
            }
        }

        List<Group> groups = new ArrayList<>(minutesByGroupForDOW.keySet());

        groups.sort(new Comparator<Group>() {
            @Override
            public int compare(Group group, Group t1) {
                return minutesByGroupForDOW.get(t1).compareTo(minutesByGroupForDOW.get(group));
            }
        });

        //format group names

        String[] groupNames = new String[groups.size()];

        int maxMinutes = 0;
        int i = 0;
        for(Group group : groups) {
            maxMinutes = Math.max(maxMinutes, minutesByGroupForDOW.get(group));
            barEntries.add(new BarEntry(i, minutesByGroupForDOW.get(group)));

            groupNames[i] = group.getGroupName();

            i++;
        }

        ValueFormatter xAxisFormatter = new IndexAxisValueFormatter(groupNames);

        XAxis xAxis = chart.getXAxis();
        xAxis.setLabelCount(groups.size());
        xAxis.setValueFormatter(xAxisFormatter);
        xAxis.setGranularity(1);

        if(groups.size() > 4) {
            xAxis.setLabelRotationAngle(-25);
        }

        //setup y axis

        ((DefaultValueFormatter) chart.getAxisLeft().getValueFormatter()).setup(0);
        chart.getAxisLeft().setAxisMaximum(maxMinutes * 1.1f);

        //set other chart values

        description.setText(dow.getDisplayName(TextStyle.FULL, Locale.US) + " (" + dateWeeksSize + ")");

        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        barDataSet.setDrawValues(false);

        int[] colors = new int[groups.size()];
        for(int index = 0; index < colors.length; index++) {
            colors[index] = groups.get(index).getDarkColor();
        }
        barDataSet.setColors(colors);

        BarData data = new BarData(barDataSet);
        chart.setData(data);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_graph_card, parent, false);
        return new GroupWorkloadTrendsStatisticsAdapter.ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return isViewingWeeklyAverage ? daysOfWeek.size() : currentDayOfWeek + 1;
    }
}
