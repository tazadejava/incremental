package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.HashMap;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;

public class CalendarMonthAdapter extends RecyclerView.Adapter<CalendarMonthAdapter.ViewHolder> {

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
    private HashMap<DayOfWeek, Integer> dowIndices;

    private YearMonth[] yearMonths;

    //TODO: this is not very good if the time period lasts for large numbers, but this is the temporary solution in order to create animations
    private CalendarWeekAdapter[] weekAdapters;

    public CalendarMonthAdapter(Activity activity, YearMonth[] yearMonths) {
        this.activity = activity;
        this.yearMonths = yearMonths;

        dowIndices = new HashMap<>();
        dowIndices.put(DayOfWeek.MONDAY, 0);
        dowIndices.put(DayOfWeek.TUESDAY, 1);
        dowIndices.put(DayOfWeek.WEDNESDAY, 2);
        dowIndices.put(DayOfWeek.THURSDAY, 3);
        dowIndices.put(DayOfWeek.FRIDAY, 4);
        dowIndices.put(DayOfWeek.SATURDAY, 5);
        dowIndices.put(DayOfWeek.SUNDAY, 6);

        weekAdapters = new CalendarWeekAdapter[yearMonths.length];
        int daysTotal = 0;
        for(int i = 0; i < yearMonths.length; i++) {
            if(i > 0) {
                daysTotal += yearMonths[i - 1].lengthOfMonth();
            }

            weekAdapters[i] = new CalendarWeekAdapter(activity, daysTotal, null, dowIndices, yearMonths[i]);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_month, parent, false);
        ViewHolder vh = new CalendarMonthAdapter.ViewHolder(view);

        vh.monthCalendar.setLayoutManager(new LinearLayoutManager(activity));

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YearMonth yearMonth = yearMonths[position];

        holder.monthName.setText(yearMonth.getMonth().toString());
        holder.monthCalendar.setAdapter(weekAdapters[position]);
    }

    public void setHeatmapGroup(Group group) {
        for(CalendarWeekAdapter adapter : weekAdapters) {
            adapter.setHeatmapGroup(group);
        }
    }

    @Override
    public int getItemCount() {
        return yearMonths.length;
    }
}
