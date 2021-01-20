package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.animation.ColorAnimation;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class CalendarWeeksAdapter extends RecyclerView.Adapter<CalendarWeeksAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public View[] dayButtons;
        public TextView hourDisplay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            dayButtons = new View[] {itemView.findViewById(R.id.button1), itemView.findViewById(R.id.button2), itemView.findViewById(R.id.button3),
                    itemView.findViewById(R.id.button4), itemView.findViewById(R.id.button5), itemView.findViewById(R.id.button6), itemView.findViewById(R.id.button7)};
            hourDisplay = itemView.findViewById(R.id.hourDisplay);
        }
    }

    private static final int CALCULATED_DAYS_COUNT = 42;

    private Activity activity;
    private TaskManager taskManager;

    private HashMap<DayOfWeek, Integer> dowIndices;
    private Group heatmapGroup;

    private YearMonth yearMonth;
    private float[] heatmap;
    private int[] minutesWorkedPerDay;
    private int firstDayIndex, daysOffsetFromTop, maxMinutesWorked;

    private int[] heatmapMaxColors;

    private boolean animate = false;

    public CalendarWeeksAdapter(Activity activity, int daysOffsetFromTop, int maxMinutesWorked, @Nullable Group heatmapGroup, HashMap<DayOfWeek, Integer> dowIndices, YearMonth yearMonth) {
        this.activity = activity;
        this.daysOffsetFromTop = daysOffsetFromTop;
        this.yearMonth = yearMonth;
        this.maxMinutesWorked = maxMinutesWorked;

        taskManager = ((IncrementalApplication) activity.getApplication()).getTaskManager();

        this.heatmapGroup = heatmapGroup;
        this.dowIndices = dowIndices;

        calculateGroupHeatmap();
    }

    private void calculateGroupHeatmap() {
        if(heatmapGroup == null) {
            heatmapMaxColors = new int[] {255, 0, 0};
        } else {
            Color color = Color.valueOf(heatmapGroup.getLightColor());
            heatmapMaxColors = new int[] {(int) (color.red() * 255f), (int) (color.green() * 255f), (int) (color.blue() * 255f)};
        }

        DayOfWeek firstDay = yearMonth.atDay(1).getDayOfWeek();
        firstDayIndex = dowIndices.get(firstDay);

        int monthLength = yearMonth.lengthOfMonth() + firstDayIndex;

        StatsManager stats = taskManager.getCurrentTimePeriod().getStatsManager();

        heatmap = new float[CALCULATED_DAYS_COUNT];
        minutesWorkedPerDay = new int[CALCULATED_DAYS_COUNT];
        //first, log the heatmap in regards to total minutes worked
        for(int i = 0; i < CALCULATED_DAYS_COUNT; i++) {
            //calculate minutes for all days of the week so that the weekly hour:min is correct
            int minutesWorked;
            LocalDate day = yearMonth.atDay(1).plusDays(i - firstDayIndex);
            if(heatmapGroup == null) {
                minutesWorked = stats.getMinutesWorked(day);
            } else {
                minutesWorked = stats.getMinutesWorkedByGroup(heatmapGroup, day);
            }
            minutesWorkedPerDay[i] = minutesWorked;

            if(i < firstDayIndex || i >= monthLength) {
                heatmap[i] = -1;
            } else {
                heatmap[i] = (float) minutesWorked / maxMinutesWorked;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_week, parent, false);
        return new CalendarWeeksAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int totalMinutes = 0;
        for(int i = 0; i < 7; i++) {
            int heatmapPosition = i + (position * 7);

            if(heatmap[heatmapPosition] == -1) {
                holder.dayButtons[i].setVisibility(View.GONE);
                holder.dayButtons[i].setOnClickListener(null);
            } else {
                holder.dayButtons[i].setVisibility(View.VISIBLE);
                holder.dayButtons[i].setBackgroundColor(Color.BLACK);

                int buttonColor;
                if(animate && holder.dayButtons[i].getBackground() instanceof ColorDrawable &&
                        (buttonColor = ((ColorDrawable) holder.dayButtons[i].getBackground()).getColor()) != heatmapPositionToColor(heatmapMaxColors, heatmapPosition)) {
                    ColorAnimation colorAnimation = new ColorAnimation(holder.dayButtons[i], colorToIntArray(Color.WHITE), heatmapPositionToColorValues(heatmapMaxColors, heatmapPosition));
                    colorAnimation.setDuration(500);
                    colorAnimation.setInterpolator(new AccelerateInterpolator());
                    colorAnimation.setStartOffset((daysOffsetFromTop + heatmapPosition - firstDayIndex) * 5);
                    holder.dayButtons[i].startAnimation(colorAnimation);
                } else {
                    holder.dayButtons[i].setBackgroundColor(heatmapPositionToColor(heatmapMaxColors, heatmapPosition));
                }

                holder.dayButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LocalDate date = yearMonth.atDay(heatmapPosition - firstDayIndex + 1);
                        Toast.makeText(activity, date.getMonth().toString() + " " + date.getDayOfMonth() + ": worked " + Utils.formatHourMinuteTime(minutesWorkedPerDay[heatmapPosition]), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            totalMinutes += minutesWorkedPerDay[heatmapPosition];
        }

        //if the current week is not full AND there exists a next week, then don't render the time
        if(heatmap[6 + (position * 7)] == -1 && taskManager.getCurrentTimePeriod().isInTimePeriod(yearMonth.atEndOfMonth().plusDays(1))) {
            holder.hourDisplay.setText("");
        } else {
            String text = Utils.formatHourMinuteTimeColonShorthand(totalMinutes);
            if (animate) {
                AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
                alphaAnimation.setDuration(500);
                alphaAnimation.setInterpolator(new AccelerateInterpolator());
                alphaAnimation.setStartOffset((daysOffsetFromTop - firstDayIndex) * 5);
                holder.hourDisplay.startAnimation(alphaAnimation);
            } else {
                holder.hourDisplay.setAlpha(1f);
            }
            holder.hourDisplay.setText(text);
        }
    }

    private int[] colorToIntArray(int colorVal) {
        Color color = Color.valueOf(colorVal);
        return new int[] {(int) (color.red() * 255f), (int) (color.green() * 255f), (int) (color.blue() * 255f)};
    }

    private int heatmapPositionToColor(int[] colorMap, int heatmapPosition) {
        int[] revisedColors = heatmapPositionToColorValues(colorMap, heatmapPosition);
        return Color.rgb(revisedColors[0], revisedColors[1], revisedColors[2]);
    }

    private int[] heatmapPositionToColorValues(int[] colorMap, int heatmapPosition) {
        return new int[] {(int) ((float) heatmap[heatmapPosition] * colorMap[0]), (int) ((float) heatmap[heatmapPosition] * colorMap[1]),
                (int) ((float) heatmap[heatmapPosition] * colorMap[2])};
    }

    public void setHeatmapGroup(Group group, int maxMinutesWorked) {
        heatmapGroup = group;
        this.maxMinutesWorked = maxMinutesWorked;
        calculateGroupHeatmap();

        animate = true;
        notifyDataSetChanged();

        //disable animations for heatmaps below or above that aren't on screen
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animate = false;
            }
        }, 200);
    }

    @Override
    public int getItemCount() {
        return CALCULATED_DAYS_COUNT / 7;
    }
}
