package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.HashMap;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class CalendarWeekAdapter extends RecyclerView.Adapter<CalendarWeekAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public View[] dayButtons;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            dayButtons = new View[] {itemView.findViewById(R.id.button1), itemView.findViewById(R.id.button2), itemView.findViewById(R.id.button3),
                    itemView.findViewById(R.id.button4), itemView.findViewById(R.id.button5), itemView.findViewById(R.id.button6), itemView.findViewById(R.id.button7)};
        }
    }

    private Activity activity;

    private YearMonth yearMonth;
    private float[] heatmap;
    private int[] minutesWorkedPerDay;

    public CalendarWeekAdapter(Activity activity, HashMap<DayOfWeek, Integer> dowIndices, YearMonth yearMonth) {
        this.activity = activity;
        this.yearMonth = yearMonth;

        DayOfWeek firstDay = yearMonth.atDay(1).getDayOfWeek();
        int firstDayIndex = dowIndices.get(firstDay);

        int monthLength = yearMonth.lengthOfMonth() + firstDayIndex;

        StatsManager stats = ((IncrementalApplication) activity.getApplication()).getTaskManager().getCurrentTimePeriod().getStatsManager();

        int maxMinutesWorked = 0;
        for(int i = 1; i <= yearMonth.lengthOfMonth(); i++) {
            maxMinutesWorked = Math.max(maxMinutesWorked, stats.getMinutesWorked(yearMonth.atDay(i)));
        }
//        int totalMinutesWorked = 0;
//        for(int i = 1; i <= yearMonth.lengthOfMonth(); i++) {
//            totalMinutesWorked += stats.getMinutesWorked(yearMonth.atDay(i));
//        }

        heatmap = new float[35];
        minutesWorkedPerDay = new int[35];
        //first, log the heatmap in regards to total minutes worked
        for(int i = 0; i < 35; i++) {
            if(i < firstDayIndex || i >= monthLength) {
                heatmap[i] = -1;
            } else {
                int minutesWorked = stats.getMinutesWorked(yearMonth.atDay(i - firstDayIndex + 1));
                heatmap[i] = (float) minutesWorked / maxMinutesWorked;
                minutesWorkedPerDay[i] = minutesWorked;
            }
        }

//        //then, renormalize the heatmap in regards to which was the most worked day; this ends up being the same thing if mathed out lol
//        float maxHeatmap = 0;
//        for(float val : heatmap) {
//            maxHeatmap = Math.max(maxHeatmap, val);
//        }
//
//        for(int i = 0; i < heatmap.length; i++) {
//            if(heatmap[i] == -1) {
//                continue;
//            }
//
//            heatmap[i] = heatmap[i] / maxHeatmap;
//        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_week, parent, false);
        return new CalendarWeekAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        for(int i = 0; i < 7; i++) {
            int heatmapPosition = i + (position * 7);

            if(heatmap[heatmapPosition] == -1) {
                holder.dayButtons[i].setEnabled(false);
                holder.dayButtons[i].setAlpha(0);
                holder.dayButtons[i].setOnClickListener(null);
            } else {
                holder.dayButtons[i].setEnabled(true);
                holder.dayButtons[i].setAlpha(1);
                holder.dayButtons[i].setBackgroundColor(Color.rgb((int) (heatmap[heatmapPosition] * 255f), 0, 0));
                holder.dayButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(activity, "Worked " + Utils.formatHourMinuteTime(minutesWorkedPerDay[heatmapPosition]), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
