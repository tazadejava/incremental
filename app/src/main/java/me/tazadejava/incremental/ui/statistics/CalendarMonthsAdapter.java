package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.graphics.Color;
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
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

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
    private HashMap<DayOfWeek, Integer> dowIndices;

    private YearMonth[] yearMonths;

    public CalendarMonthsAdapter(Activity activity, YearMonth[] yearMonths) {
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
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_month, parent, false);
        ViewHolder vh = new CalendarMonthsAdapter.ViewHolder(view);

        vh.monthCalendar.setLayoutManager(new LinearLayoutManager(activity));

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YearMonth yearMonth = yearMonths[position];

        holder.monthName.setText(yearMonth.getMonth().toString());
        holder.monthCalendar.setAdapter(new CalendarWeekAdapter(activity, dowIndices, yearMonth));
    }

    @Override
    public int getItemCount() {
        return yearMonths.length;
    }
}
