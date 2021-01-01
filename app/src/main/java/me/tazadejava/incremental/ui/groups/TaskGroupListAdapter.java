package me.tazadejava.incremental.ui.groups;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
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

public class TaskGroupListAdapter extends RecyclerView.Adapter<TaskGroupListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout, expandedOptionsLayout;
        public TextView taskGroupName, tasksCount, actionTaskText, timePeriodText, secondaryActionTaskText, thirdActionTaskText, taskNotes;

        public View sideCardAccent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);
            expandedOptionsLayout = itemView.findViewById(R.id.expandedOptionsLayout);

            taskGroupName = itemView.findViewById(R.id.timePeriodName);
            tasksCount = itemView.findViewById(R.id.estimatedDailyTime);
            actionTaskText = itemView.findViewById(R.id.actionTaskText);
            secondaryActionTaskText = itemView.findViewById(R.id.secondaryActionTaskText);
            thirdActionTaskText = itemView.findViewById(R.id.thirdActionTaskText);
            taskNotes = itemView.findViewById(R.id.taskNotes);

            timePeriodText = itemView.findViewById(R.id.task_due_date);

            sideCardAccent = itemView.findViewById(R.id.sideCardAccent);
        }
    }

    private AppCompatActivity context;
    private TaskManager taskManager;

    private List<Group> groups;

    public TaskGroupListAdapter(TaskManager taskManager, AppCompatActivity context) {
        this.context = context;
        this.taskManager = taskManager;

        groups = new ArrayList<>(taskManager.getAllCurrentGroups(taskManager.getCurrentTimePeriod()));

        sortByCurrentWeekHours();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new TaskGroupListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group group = groups.get(position);

        Utils.setViewGradient(group, holder.sideCardAccent, 0.5);

        holder.taskGroupName.setText(group.getGroupName());
        holder.taskGroupName.setTextColor(group.getLightColor());

        holder.taskGroupName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Set name");

                EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                input.setSelectAllOnFocus(false);
                builder.setView(input);

                input.setText(group.getGroupName());

                builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(input.getText().length() > 0 && !input.getText().toString().equals(group.getGroupName())) {
                            StatsManager.StatsGroupPacket packet = new StatsManager.StatsGroupPacket(taskManager.getCurrentTimePeriod().getStatsManager(), group);
                            if(taskManager.getCurrentTimePeriod().updateGroupName(group, input.getText().toString())) {
                                holder.taskGroupName.setText(input.getText().toString());

                                packet.restore();
                            } else {
                                packet.restore();

                                AlertDialog.Builder error = new AlertDialog.Builder(context);
                                error.setTitle("Something went wrong! Does the group name already exist?");
                                error.show();
                            }
                        }

                        input.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(holder.taskGroupName.getWindowToken(), 0);
                            }
                        }, 50);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                builder.show();
                input.requestFocus();

                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });

        int tasksCount = taskManager.getCurrentTimePeriod().getTasksCountThisWeekByGroup(group);
        int tasksTotal = taskManager.getCurrentTimePeriod().getAllCurrentAndUpcomingTasksByGroup(group).size();

        int[] averageWorkload = getAverageMinutesWorkedPerWeek(group);

        holder.tasksCount.setText(tasksCount + " task" + (tasksCount == 1 ? "" : "s") + " this week"
                + "\n\n" + Utils.formatHourMinuteTime(getMinutesWorkedThisWeek(group)) + " worked this week"
                + "\nAverage workload per week (" + averageWorkload[1] + "): " + Utils.formatHourMinuteTime(averageWorkload[0]));

        holder.actionTaskText.setText("View Tasks");
        holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToGroupTasks(group);
            }
        });

        holder.secondaryActionTaskText.setText("Change Color");
        holder.secondaryActionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);

                dialog.setTitle("Pick a color:");

                LayoutInflater inflater = context.getLayoutInflater();
                View root = inflater.inflate(R.layout.menu_group_color_picker, null);

                View colorLeft = root.findViewById(R.id.colorShowerLeft);
                View colorRight = root.findViewById(R.id.colorShowerRight);
                SeekBar colorSlider = root.findViewById(R.id.colorSlider);

                View[] colorGradients = new View[] {root.findViewById(R.id.gradient1), root.findViewById(R.id.gradient2), root.findViewById(R.id.gradient3),
                        root.findViewById(R.id.gradient4), root.findViewById(R.id.gradient5), root.findViewById(R.id.gradient6)};

                colorSlider.setMin(0);
                colorSlider.setMax(359);

                Group dummyGroup = new Group("DUMMY");

                int val = 0;
                for(View gradient : colorGradients) {
                    dummyGroup.setColor(val);
                    int beginColor = dummyGroup.getLightColor();
                    dummyGroup.setColor(val + 59);
                    int endColor = dummyGroup.getLightColor();

                    GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {beginColor, endColor});
                    gradient.setBackground(drawable);

                    int finalVal = val;
                    gradient.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            colorSlider.setProgress(finalVal);
                        }
                    });

                    val += 60;
                }

                colorSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        dummyGroup.setColor(progress);

                        colorLeft.setBackgroundColor(dummyGroup.getDarkColor());
                        colorRight.setBackgroundColor(dummyGroup.getLightColor());
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });

                colorSlider.setProgress((int) (group.getColorValue()), true);

                dialog.setView(root);

                dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                dialog.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StatsManager.StatsGroupPacket packet = new StatsManager.StatsGroupPacket(taskManager.getCurrentTimePeriod().getStatsManager(), group);

                        group.setColor(colorSlider.getProgress());
                        Utils.setViewGradient(group, holder.sideCardAccent, 0.5);
                        holder.taskGroupName.setTextColor(group.getLightColor());
                        taskManager.saveData(true);

                        packet.restore();
                    }
                });

                dialog.show();
            }
        });

        holder.taskNotes.setVisibility(View.GONE);
        holder.thirdActionTaskText.setVisibility(View.GONE);

        holder.taskCardConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.animateTaskCardOptionsLayout(holder.expandedOptionsLayout, group, holder.sideCardAccent, 0.5);
            }
        });

        holder.taskCardConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                navigateToGroupTasks(group);
                return true;
            }
        });

        TimePeriod timePeriodScope = taskManager.getGroupScope(group);

        if(timePeriodScope == null) {
            holder.timePeriodText.setText("Global Scope");
        } else {
            holder.timePeriodText.setText(timePeriodScope.getName());
        }
    }

    private void navigateToGroupTasks(Group group) {
        NavController nav = Navigation.findNavController(context, R.id.nav_host_fragment);

        Bundle bundle = new Bundle();

        bundle.putString("group", group.getGroupName());

        TimePeriod scope = taskManager.getGroupScope(group);
        if (scope == null) {
            bundle.putString("scope", null);
        } else {
            bundle.putString("scope", scope.getName());
        }

        NavOptions navOptions = new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_left)
                .setExitAnim(R.anim.slide_out_right)
                .setPopEnterAnim(R.anim.slide_in_right)
                .setPopExitAnim(R.anim.slide_out_left)
                .build();

        nav.navigate(R.id.nav_specific_group, bundle, navOptions);
    }

    /**
     *
     * @param group
     * @return array of two ints: the first value is the average minutes worked, and the second value is the total weeks counted
     */
    private int[] getAverageMinutesWorkedPerWeek(Group group) {
        int averageMinutesWorked = 0;
        int totalWeeksWorked = 0;

        LocalDate currentLoopDate = taskManager.getCurrentTimePeriod().getBeginDate();

        LocalDate endLoopDate;
        if(taskManager.isCurrentTimePeriodActive()) {
            endLoopDate = LocalDate.now();
        } else {
            endLoopDate = taskManager.getCurrentTimePeriod().getEndDate();
        }

        //make the week start on a MONDAY not sunday, offset by one day
        long currentWeekValue = currentLoopDate.plusDays(-1).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        long endWeekValue = endLoopDate.plusDays(-1).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        while(currentWeekValue <= endWeekValue && currentLoopDate.plusDays(-1).isBefore(endLoopDate)) {
            boolean workedThisWeek = false;
            for(LocalDate date : LogicalUtils.getWorkWeekDates(currentLoopDate)) {
                if(!workedThisWeek) {
                    workedThisWeek = true;
                }

                averageMinutesWorked += taskManager.getCurrentTimePeriod().getStatsManager().getMinutesWorkedByGroup(group, date);
            }

            if(workedThisWeek) {
                totalWeeksWorked++;
            }

            currentLoopDate = currentLoopDate.plusDays(7);
            currentWeekValue = currentLoopDate.plusDays(-1).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        }

        if(totalWeeksWorked == 0) {
            return new int[] {0, 0};
        }

        //find the average
        return new int[] {averageMinutesWorked / totalWeeksWorked, totalWeeksWorked};
    }

    private int getMinutesWorkedThisWeek(Group group) {
        int minutesWorked = 0;

        for(LocalDate date : LogicalUtils.getWorkWeekDates()) {
            minutesWorked += taskManager.getCurrentTimePeriod().getStatsManager().getMinutesWorkedByGroup(group, date);
        }

        return minutesWorked;
    }

    public void sortByAverageWeekHours() {
        HashMap<Group, Integer> averageWorkHours = new HashMap<>();

        for(Group group : groups) {
            averageWorkHours.put(group, getAverageMinutesWorkedPerWeek(group)[0]);
        }

        groups.sort(new Comparator<Group>() {
            @Override
            public int compare(Group group, Group t1) {
                return Integer.compare(averageWorkHours.get(t1), averageWorkHours.get(group));
            }
        });

        notifyDataSetChanged();
    }

    public void sortByCurrentWeekHours() {
        HashMap<Group, Integer> currentWeekHours = new HashMap<>();

        for(Group group : groups) {
            currentWeekHours.put(group, getMinutesWorkedThisWeek(group));
        }

        groups.sort(new Comparator<Group>() {
            @Override
            public int compare(Group group, Group t1) {
                return Integer.compare(currentWeekHours.get(t1), currentWeekHours.get(group));
            }
        });

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }
}
