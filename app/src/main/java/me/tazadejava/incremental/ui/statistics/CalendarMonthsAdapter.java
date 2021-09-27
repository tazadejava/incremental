package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.HashMap;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class CalendarMonthsAdapter extends RecyclerView.Adapter<CalendarMonthsAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView monthName;
        public RecyclerView monthCalendar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            monthName = itemView.findViewById(R.id.monthName);
            monthCalendar = itemView.findViewById(R.id.monthCalendar);
        }
    }

    private Activity activity;
    private StatisticsCalendarHeatmapFragment fragment;
    private HashMap<DayOfWeek, Integer> dowIndices;

    private YearMonth[] yearMonths;

    //TODO: this is not very good if the time period lasts for large numbers, but this is the temporary solution in order to create animations
    private CalendarWeeksAdapter[] weekAdapters;

    public CalendarMonthsAdapter(Activity activity, StatisticsCalendarHeatmapFragment fragment, YearMonth[] yearMonths) {
        this.activity = activity;
        this.fragment = fragment;
        this.yearMonths = yearMonths;

        dowIndices = new HashMap<>();
        dowIndices.put(DayOfWeek.MONDAY, 0);
        dowIndices.put(DayOfWeek.TUESDAY, 1);
        dowIndices.put(DayOfWeek.WEDNESDAY, 2);
        dowIndices.put(DayOfWeek.THURSDAY, 3);
        dowIndices.put(DayOfWeek.FRIDAY, 4);
        dowIndices.put(DayOfWeek.SATURDAY, 5);
        dowIndices.put(DayOfWeek.SUNDAY, 6);

        weekAdapters = new CalendarWeeksAdapter[yearMonths.length];
        int daysTotal = 0;

        int maxMinutesWorked = getMaxMinutesWorked(null);

        //now, set week adapters
        for(int i = 0; i < yearMonths.length; i++) {
            if(i > 0) {
                daysTotal += yearMonths[i - 1].lengthOfMonth();
            }

            weekAdapters[i] = new CalendarWeeksAdapter(activity, fragment, daysTotal, maxMinutesWorked, null, dowIndices, yearMonths[i]);
        }
    }

    private int getMaxMinutesWorked(@Nullable Group group) {
        TaskManager taskManager = ((IncrementalApplication) activity.getApplication()).getTaskManager();
        StatsManager stats = taskManager.getCurrentTimePeriod().getStatsManager();

        int maxMinutesWorked = 0;
        for(int i = 0; i < yearMonths.length; i++) { //for all weeks
            for (int j = 1; j <= yearMonths[i].lengthOfMonth(); j++) { //for all days of the month
                int worked;
                if(group == null) {
                    worked = stats.getMinutesWorked(yearMonths[i].atDay(j), fragment.shouldIncludeTimeInvariants());
                } else {
                    worked = stats.getMinutesWorkedByGroup(group, yearMonths[i].atDay(j), fragment.shouldIncludeTimeInvariants());
                }

                maxMinutesWorked = Math.max(worked, maxMinutesWorked);
            }
        }

        return maxMinutesWorked;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_month, parent, false);
        ViewHolder vh = new CalendarMonthsAdapter.ViewHolder(view);

        vh.monthCalendar.setLayoutManager(new LinearLayoutManager(activity));

        if(!((IncrementalApplication) activity.getApplication()).isDarkModeOn()) {
            vh.monthCalendar.setBackgroundColor(Utils.getThemeAttrColor(activity, R.attr.cardTextColor));
        }

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YearMonth yearMonth = yearMonths[position];

        holder.monthName.setText(yearMonth.getMonth().toString() + " " + yearMonth.getYear());
        holder.monthCalendar.setAdapter(weekAdapters[position]);
    }

    public void setHeatmapGroup(Group group) {
        for(CalendarWeeksAdapter adapter : weekAdapters) {
            adapter.setHeatmapGroup(group, getMaxMinutesWorked(group));
        }
    }

    @Override
    public int getItemCount() {
        return yearMonths.length;
    }
}
