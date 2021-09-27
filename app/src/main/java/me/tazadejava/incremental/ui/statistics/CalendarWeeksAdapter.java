package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
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

        public ConstraintLayout parent;

        public View[] dayButtons;
        public TextView hourDisplay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            parent = itemView.findViewById(R.id.calendarWeekParent);

            dayButtons = new View[] {itemView.findViewById(R.id.button1), itemView.findViewById(R.id.button2), itemView.findViewById(R.id.button3),
                    itemView.findViewById(R.id.button4), itemView.findViewById(R.id.button5), itemView.findViewById(R.id.button6), itemView.findViewById(R.id.button7)};
            hourDisplay = itemView.findViewById(R.id.hourDisplay);
        }
    }

    private static final int CALCULATED_DAYS_COUNT = 42;
    private static final int TODAY_HEATMAP_SIZE = 6;

    private Activity activity;
    private StatisticsCalendarHeatmapFragment fragment;
    private TaskManager taskManager;
    private StatsManager statsManager;

    private HashMap<DayOfWeek, Integer> dowIndices;
    private Group heatmapGroup;

    private YearMonth yearMonth;
    private float[] heatmap;
    private int[] minutesWorkedPerDay;
    private int firstDayIndex, daysOffsetFromTop, maxMinutesWorked;

    private int[] heatmapMaxColors;

    private boolean animate = false;

    private View dayOutlineView;

    public CalendarWeeksAdapter(Activity activity, StatisticsCalendarHeatmapFragment fragment, int daysOffsetFromTop, int maxMinutesWorked, @Nullable Group heatmapGroup, HashMap<DayOfWeek, Integer> dowIndices, YearMonth yearMonth) {
        this.activity = activity;
        this.fragment = fragment;
        this.daysOffsetFromTop = daysOffsetFromTop;
        this.yearMonth = yearMonth;
        this.maxMinutesWorked = maxMinutesWorked;

        taskManager = ((IncrementalApplication) activity.getApplication()).getTaskManager();
        statsManager = taskManager.getCurrentTimePeriod().getStatsManager();

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

        heatmap = new float[CALCULATED_DAYS_COUNT];
        minutesWorkedPerDay = new int[CALCULATED_DAYS_COUNT];
        //first, log the heatmap in regards to total minutes worked
        for(int i = 0; i < CALCULATED_DAYS_COUNT; i++) {
            //calculate minutes for all days of the week so that the weekly hour:min is correct
            int minutesWorked;
            LocalDate day = yearMonth.atDay(1).plusDays(i - firstDayIndex);
            if(heatmapGroup == null) {
                minutesWorked = statsManager.getMinutesWorked(day, fragment.shouldIncludeTimeInvariants());
            } else {
                minutesWorked = statsManager.getMinutesWorkedByGroup(heatmapGroup, day, fragment.shouldIncludeTimeInvariants());
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
        LocalDate now = LocalDate.now();
        for(int i = 0; i < 7; i++) {
            int heatmapPosition = i + (position * 7);
            if(heatmap[heatmapPosition] == -1) {
                holder.dayButtons[i].setVisibility(View.GONE);
                holder.dayButtons[i].setOnClickListener(null);
                holder.dayButtons[i].setOnLongClickListener(null);
            } else {
                holder.dayButtons[i].setVisibility(View.VISIBLE);
                holder.dayButtons[i].setBackgroundColor(Color.BLACK);

                //add indicator for where today is
                LocalDate date = yearMonth.atDay(heatmapPosition - firstDayIndex + 1);
                if(date.equals(now)) {
                    if(dayOutlineView != null) {
                        holder.parent.removeView(dayOutlineView);
                        dayOutlineView = null;
                    }

                    dayOutlineView = new View(activity);
                    dayOutlineView.setId(View.generateViewId());
                    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(holder.dayButtons[i].getLayoutParams());
                    params.width += TODAY_HEATMAP_SIZE;
                    params.height += TODAY_HEATMAP_SIZE;
                    dayOutlineView.setLayoutParams(params);
                    dayOutlineView.setBackgroundColor(Color.WHITE);
                    holder.parent.addView(dayOutlineView, holder.parent.indexOfChild(holder.dayButtons[i]));

                    ConstraintSet set = new ConstraintSet();
                    set.clone(holder.parent);

                    set.connect(dayOutlineView.getId(), ConstraintSet.LEFT, holder.dayButtons[i].getId(), ConstraintSet.LEFT, 0);
                    set.connect(dayOutlineView.getId(), ConstraintSet.TOP, holder.dayButtons[i].getId(), ConstraintSet.TOP, 0);

                    set.setTranslationX(dayOutlineView.getId(), -(TODAY_HEATMAP_SIZE / 2));
                    set.setTranslationY(dayOutlineView.getId(), -(TODAY_HEATMAP_SIZE / 2));

                    set.applyTo(holder.parent);
                }

                int buttonColor;
                if(animate && holder.dayButtons[i].getBackground() instanceof ColorDrawable &&
                        (buttonColor = ((ColorDrawable) holder.dayButtons[i].getBackground()).getColor()) != heatmapPositionToColor(heatmapMaxColors, heatmapPosition)) {
                    ColorAnimation colorAnimation = new ColorAnimation(holder.dayButtons[i], colorToIntArray(heatmapGroup == null ? Color.RED : heatmapGroup.getLightColor()), heatmapPositionToColorValues(heatmapMaxColors, heatmapPosition));
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

                holder.dayButtons[i].setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if(minutesWorkedPerDay[heatmapPosition] > 0) {
                            //open an interface to show what classes were worked on
                            NavController nav = Navigation.findNavController(activity, R.id.nav_host_fragment);
                            LocalDate date = yearMonth.atDay(heatmapPosition - firstDayIndex + 1);

                            Bundle bundle = new Bundle();

                            bundle.putBoolean("shouldIncludeTimeInvariants", fragment.shouldIncludeTimeInvariants());
                            bundle.putString("date", date.toString());

                            NavOptions navOptions = new NavOptions.Builder()
                                    .setEnterAnim(R.anim.slide_in_left)
                                    .setExitAnim(R.anim.slide_out_right)
                                    .setPopEnterAnim(R.anim.slide_in_right)
                                    .setPopExitAnim(R.anim.slide_out_left)
                                    .build();

                            nav.navigate(R.id.nav_specific_heatmap_point, bundle, navOptions);
                            return true;
                        }
                        return false;
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

            holder.hourDisplay.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    String[] dates = new String[7];
                    LocalDate day = yearMonth.atDay(1).plusDays(-firstDayIndex);
                    for(int i = 0; i < 7; i++) {
                        int heatmapPosition = i + (holder.getAdapterPosition() * 7);
                        dates[i] = day.plusDays(heatmapPosition).toString();
                    }

                    //open an interface to show what classes were worked on
                    NavController nav = Navigation.findNavController(activity, R.id.nav_host_fragment);

                    Bundle bundle = new Bundle();

                    bundle.putBoolean("shouldIncludeTimeInvariants", fragment.shouldIncludeTimeInvariants());
                    bundle.putStringArray("dates", dates);

                    NavOptions navOptions = new NavOptions.Builder()
                            .setEnterAnim(R.anim.slide_in_left)
                            .setExitAnim(R.anim.slide_out_right)
                            .setPopEnterAnim(R.anim.slide_in_right)
                            .setPopExitAnim(R.anim.slide_out_left)
                            .build();

                    nav.navigate(R.id.nav_specific_heatmap_point, bundle, navOptions);
                    return true;
                }
            });
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

        if(dayOutlineView != null) {
            ((ConstraintLayout) dayOutlineView.getParent()).removeView(dayOutlineView);
            dayOutlineView = null;
        }

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
