package me.tazadejava.incremental.ui.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

    private TaskManager taskManager;

    private BarChart workBarChart;

    private RecyclerView dashboardView;
    private LinearLayoutManager llm;
    private MainDashboardDayAdapter adapter;

    private Description description;
    private boolean isShowingHours;

    private int currentDateOffset;
    private LocalDate[] currentDates;
    private boolean workBarChartLongPressed;

    private LocalDate lastRefreshDate;

    private ImageView leftNav, rightNav;

    private long lastResume;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //change the FAB to create a new task
        taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        if(taskManager.isCurrentTimePeriodActive()) {
            addTaskButton.setVisibility(View.VISIBLE);
            addTaskButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    taskManager.setActiveEditTask(null);

                    Intent createTask = new Intent(getContext(), CreateTaskActivity.class);
                    startActivity(createTask);
                }
            });
        } else {
            addTaskButton.setVisibility(View.GONE);
        }

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        leftNav = root.findViewById(R.id.leftNavigation);
        rightNav = root.findViewById(R.id.rightNavigation);

        workBarChart = root.findViewById(R.id.workBarChart);

        createWorkChart();

        //theme the work chart

        int primaryTextColor = Utils.getAndroidAttrColor(getActivity(), android.R.attr.textColorPrimary);

        workBarChart.getXAxis().setTextColor(primaryTextColor);
        workBarChart.getAxisLeft().setTextColor(primaryTextColor);

        dashboardView = root.findViewById(R.id.dashboard_day_list);
        dashboardView.setLayoutManager(llm = new LinearLayoutManager(getContext()));
        dashboardView.setAdapter(adapter = new MainDashboardDayAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(),
                this, dashboardView, llm, getActivity()));

        dashboardView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if(!dashboardView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    //expand list by 7 days
                    taskManager.getCurrentTimePeriod().extendLookaheadTasksViewCount(TimePeriod.DAILY_LOGS_AHEAD_COUNT_SHOW_UI + 7);
                    adapter.notifyDataSetChanged();
                }
            }
        });

        lastRefreshDate = LocalDate.now();

        if(((IncrementalApplication) getActivity().getApplication()).isDarkModeOn()) {
            leftNav.setImageResource(R.drawable.ic_navigate_before_white_24dp);
            rightNav.setImageResource(R.drawable.ic_navigate_next_white_24dp);
        } else {
            leftNav.setImageResource(R.drawable.ic_navigate_before_black_24dp);
            rightNav.setImageResource(R.drawable.ic_navigate_next_black_24dp);
        }

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

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.onTouchEvent(event);

                if(event.getAction() == MotionEvent.ACTION_UP && workBarChartLongPressed) {
                    workBarChartLongPressed = false;

                    while(currentDateOffset != 0) {
                        int dir = currentDateOffset > 0 ? -1 : 1;
                        for(int i = 0; i < currentDates.length; i++) {
                            currentDates[i] = currentDates[i].plusDays(dir * 7);
                        }

                        currentDateOffset += dir;
                    }
                    refreshChartData();
                }

                return super.onTouch(v, event);
            }
        });

        workBarChart.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                workBarChartLongPressed = true;
                return true;
            }
        });

        leftNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i = 0; i < currentDates.length; i++) {
                    currentDates[i] = currentDates[i].minusDays(7);
                }

                currentDateOffset--;
                refreshChartData();
            }
        });


        rightNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i = 0; i < currentDates.length; i++) {
                    currentDates[i] = currentDates[i].plusDays(7);
                }

                currentDateOffset++;
                refreshChartData();
            }
        });

        LocalDate[] dates;
        if(taskManager.isCurrentTimePeriodActive() || taskManager.getCurrentTimePeriod().getEndDate() == null) {
            dates = LogicalUtils.getWorkWeekDates();
        } else {
            dates = LogicalUtils.getWorkWeekDates(taskManager.getCurrentTimePeriod().getEndDate().minusDays(7));
        }
        currentDates = dates;

        //format the graph

        description = new Description();
        description.setTextSize(12f);
        description.setTextColor(Utils.getThemeAttrColor(getContext(), R.attr.subtextColor));
        workBarChart.setDescription(description);

        ValueFormatter xAxisFormatter = new IndexAxisValueFormatter(formatWorkDates());

        XAxis xAxis = workBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        xAxis.setValueFormatter(xAxisFormatter);

        ValueFormatter yAxisFormatter = new DefaultValueFormatter(0);

        YAxis yAxis = workBarChart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yAxis.setGranularity(1.0f);
        yAxis.setGranularityEnabled(true);
        yAxis.setLabelCount(5, true);

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
        LocalDate now = LocalDate.now();

        //refresh axes

        ((IndexAxisValueFormatter) workBarChart.getXAxis().getValueFormatter()).setValues(formatWorkDates());

        if(currentDateOffset == 0) {
            int primaryTextColor = Utils.getAndroidAttrColor(getActivity(), android.R.attr.textColorPrimary);
            workBarChart.getXAxis().setTextColor(primaryTextColor);
        } else {
            workBarChart.getXAxis().setTextColor(ContextCompat.getColor(getContext(), R.color.primaryColor));
        }

        TimePeriod timePeriod = ((IncrementalApplication) getActivity().getApplication()).getTaskManager().getCurrentTimePeriod();

        //extend time period lookahead dynamically

        if(currentDateOffset > 0) {
            timePeriod.extendLookaheadWeekCount(currentDateOffset);
        }

        //refresh data

        StatsManager stats = timePeriod.getStatsManager();

        int maxMinutes = 0;
        int maxProjectedMinutes = 0;
        int totalMinutes = 0;
        int totalNonzeroDays = 0;
        //calculate maxes and determine whether to show hours or minutes as y axis
        for(LocalDate date : currentDates) {
            int minutes = stats.getMinutesWorked(date, false);

            maxMinutes = Math.max(maxMinutes, minutes);

            if(minutes > 0) {
                totalMinutes += minutes;
                totalNonzeroDays++;
            } else {
                maxProjectedMinutes = Math.max(maxProjectedMinutes, taskManager.getCurrentTimePeriod().getEstimatedMinutesOfWorkForDate(date));
            }
        }

        List<BarEntry> values = new ArrayList<>();
        if(totalNonzeroDays != 0 && totalMinutes / totalNonzeroDays > 60 || maxProjectedMinutes >= 120) {
            isShowingHours = true;
            description.setText("Hours worked this week (" + Utils.formatHourMinuteTime(totalMinutes) + " total)");
            ((DefaultValueFormatter) workBarChart.getAxisLeft().getValueFormatter()).setup(0);

            int index = 0;
            for(LocalDate date : currentDates) {
                float hours;
                if(date.isAfter(now) || (date.isEqual(now) && stats.getMinutesWorked(date, false) == 0)) {
                    //projection
                    maxMinutes = Math.max(taskManager.getCurrentTimePeriod().getEstimatedMinutesOfWorkForDate(date), maxMinutes);
                    hours = taskManager.getCurrentTimePeriod().getEstimatedMinutesOfWorkForDate(date) / 60f;
                } else {
                    hours = stats.getMinutesWorked(date, false) / 60f;
                }
                values.add(new BarEntry(index, hours));

                index++;
            }

            //rounded to the nearest 4, ceil
            workBarChart.getAxisLeft().setAxisMaximum((int) Math.ceil(maxMinutes / 60f / 4f) * 4);
        } else {
            isShowingHours = false;
            description.setText("Minutes worked this week (" + Utils.formatHourMinuteTime(totalMinutes) + " total)");
            ((DefaultValueFormatter) workBarChart.getAxisLeft().getValueFormatter()).setup(0);

            int index = 0;
            for(LocalDate date : currentDates) {
                int minutes;
                if(date.isAfter(now) || (date.isEqual(now) && stats.getMinutesWorked(date, false) == 0)) {
                    //projection
                    maxMinutes = Math.max(taskManager.getCurrentTimePeriod().getEstimatedMinutesOfWorkForDate(date), maxMinutes);
                    minutes = taskManager.getCurrentTimePeriod().getEstimatedMinutesOfWorkForDate(date);
                } else {
                    minutes = stats.getMinutesWorked(date, false);
                }
                values.add(new BarEntry(index, minutes));

                index++;
            }

            //rounded to the nearest 40, ceil
            workBarChart.getAxisLeft().setAxisMaximum((int) Math.ceil(maxMinutes / 40f) * 40);
        }

        BarDataSet barDataSet = new BarDataSet(values, "");
        barDataSet.setDrawValues(false);

        int[] colors = new int[currentDates.length];
        int index = 0;
        for(LocalDate date : currentDates) {
            if(date.equals(now) && stats.getMinutesWorked(now, false) > 0) {
                colors[index] = ContextCompat.getColor(getContext(), R.color.primaryColor);
            } else if(date.isBefore(now)) {
                colors[index] = ContextCompat.getColor(getContext(), R.color.secondaryColor);
            } else {
                colors[index] = Color.argb(0.5f, 0.7f, 0.7f, 0.7f);
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

        //don't refresh too often; for some reason, when pendingintent is run for logTask, this is called twice, which invalidates the log
        if(System.currentTimeMillis() - lastResume < 500) {
            return;
        }
        lastResume = System.currentTimeMillis();

        //if new day, then reset the task manager
        if(!lastRefreshDate.equals(LocalDate.now())) {
            lastRefreshDate = LocalDate.now();
            ((IncrementalApplication) getActivity().getApplication()).reset();

            dashboardView.setAdapter(adapter = new MainDashboardDayAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(),
                    this, dashboardView, llm, getActivity()));

            if(lastRefreshDate.getDayOfWeek() == DayOfWeek.MONDAY) {
                currentDateOffset++;
            }
            refreshChartData();
        } else {
            adapter.unmarkAllTasksAsAnimated();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if(llm.findFirstVisibleItemPosition() >= 7) { //if deeper than 7 days, then do not scroll smoothly
            dashboardView.scrollToPosition(0);
        } else {
            dashboardView.smoothScrollToPosition(0);
        }
    }
}