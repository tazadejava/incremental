package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
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

    public class ColorAnimation extends Animation {

        private View view;
        private int[] fromColor, toColor;

        public ColorAnimation(View view, int[] fromColor, int[] toColor) {
            this.view = view;
            this.fromColor = fromColor;
            this.toColor = toColor;

            this.view.setHasTransientState(true);
            setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setHasTransientState(false);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            view.setBackgroundColor(Color.rgb(fromColor[0] + (int) ((toColor[0] - fromColor[0]) * interpolatedTime), fromColor[1] + (int) ((toColor[1] - fromColor[1]) * interpolatedTime),
                    fromColor[2] + (int) ((toColor[2] - fromColor[2]) * interpolatedTime)));
        }
    }

    private Activity activity;

    private HashMap<DayOfWeek, Integer> dowIndices;
    private Group heatmapGroup;

    private YearMonth yearMonth;
    private float[] heatmap;
    private int[] minutesWorkedPerDay;
    private int firstDayIndex, daysOffsetFromTop;

    private int[] heatmapMaxColors;

    private boolean animate = false;
    private HashMap<View, Integer> animatingViews = new HashMap<>();

    public CalendarWeekAdapter(Activity activity, int daysOffsetFromTop, @Nullable Group heatmapGroup, HashMap<DayOfWeek, Integer> dowIndices, YearMonth yearMonth) {
        this.activity = activity;
        this.daysOffsetFromTop = daysOffsetFromTop;
        this.yearMonth = yearMonth;

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

        StatsManager stats = ((IncrementalApplication) activity.getApplication()).getTaskManager().getCurrentTimePeriod().getStatsManager();

        int maxMinutesWorked = 0;
        for(int i = 1; i <= yearMonth.lengthOfMonth(); i++) {
            if(heatmapGroup == null) {
                maxMinutesWorked = Math.max(maxMinutesWorked, stats.getMinutesWorked(yearMonth.atDay(i)));
            } else {
                maxMinutesWorked = Math.max(maxMinutesWorked, stats.getMinutesWorkedByGroup(heatmapGroup, yearMonth.atDay(i)));
            }
        }

        heatmap = new float[35];
        minutesWorkedPerDay = new int[35];
        //first, log the heatmap in regards to total minutes worked
        for(int i = 0; i < 35; i++) {
            if(i < firstDayIndex || i >= monthLength) {
                heatmap[i] = -1;
            } else {
                int minutesWorked;
                if(heatmapGroup == null) {
                    minutesWorked = stats.getMinutesWorked(yearMonth.atDay(i - firstDayIndex + 1));
                } else {
                    minutesWorked = stats.getMinutesWorkedByGroup(heatmapGroup, yearMonth.atDay(i - firstDayIndex + 1));
                }
                heatmap[i] = (float) minutesWorked / maxMinutesWorked;
                minutesWorkedPerDay[i] = minutesWorked;
            }
        }
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
                holder.dayButtons[i].setVisibility(View.GONE);
                holder.dayButtons[i].setOnClickListener(null);
            } else {
                holder.dayButtons[i].setVisibility(View.VISIBLE);

                int buttonColor;
                if(animate && holder.dayButtons[i].getBackground() instanceof ColorDrawable &&
                        (buttonColor = ((ColorDrawable) holder.dayButtons[i].getBackground()).getColor()) != heatmapPositionToColor(heatmapMaxColors, heatmapPosition)) {
                    ColorAnimation colorAnimation = new ColorAnimation(holder.dayButtons[i], colorToIntArray(buttonColor), heatmapPositionToColorValues(heatmapMaxColors, heatmapPosition));
                    colorAnimation.setDuration(500);
                    colorAnimation.setInterpolator(new AccelerateInterpolator());
                    colorAnimation.setStartOffset((daysOffsetFromTop + heatmapPosition - firstDayIndex) * 5);
                    holder.dayButtons[i].startAnimation(colorAnimation);
                    animatingViews.put(holder.dayButtons[i], heatmapPositionToColor(heatmapMaxColors, heatmapPosition));
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

    public void setHeatmapGroup(Group group) {
        heatmapGroup = group;
        calculateGroupHeatmap();

        animate = true;
        notifyDataSetChanged();

        //disable animations for heatmaps below or above that aren't on screen
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animate = false;
            }
        }, 100);
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
