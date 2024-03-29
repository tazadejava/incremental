package me.tazadejava.incremental.ui.groups;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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

    private static class LocalDateMinutes {

        public final LocalDate date;
        public final int minutes;

        public LocalDateMinutes(LocalDate date, int minutes) {
            this.date = date;
            this.minutes = minutes;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout, expandedOptionsLayout;
        public TextView taskGroupName, tasksCount, actionTaskText, timePeriodText, secondaryActionTaskText, thirdActionTaskText, taskNotes, fourthActionTaskText;

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

            fourthActionTaskText = itemView.findViewById(R.id.fourthActionTaskText);

            timePeriodText = itemView.findViewById(R.id.task_due_date);

            sideCardAccent = itemView.findViewById(R.id.sideCardAccent);
        }
    }

    private AppCompatActivity context;
    private TaskManager taskManager;

    private List<Group> groups;
    private HashMap<Group, Integer[]> groupStats = new HashMap<>();
    private HashMap<Group, LocalDateMinutes[]> groupMinMax = new HashMap<>();
    private HashMap<Group, Integer> groupCurrentWeekMinutes = new HashMap<>();

    public TaskGroupListAdapter(TaskManager taskManager, AppCompatActivity context) {
        this.context = context;
        this.taskManager = taskManager;

        groups = new ArrayList<>(taskManager.getAllCurrentGroups(taskManager.getCurrentTimePeriod()));

        for(Group group : groups) {
            calculateAverageMedianSTDWorkedPerWeek(group);
            groupCurrentWeekMinutes.put(group, getMinutesWorkedThisWeek(group));
        }

        if(taskManager.isCurrentTimePeriodActive()) {
            sortByCurrentWeekHours();
        } else {
            sortByAverageWeekHours();
        }
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
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                builder.show();
                input.requestFocus();

                input.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        InputMethodManager imm = (InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(input, 0);
                    }
                }, 50);
            }
        });

        int tasksCount = taskManager.getCurrentTimePeriod().getTasksCountThisWeekByGroup(group);

        Integer[] stats = groupStats.get(group);
        LocalDateMinutes[] minMaxStats = groupMinMax.get(group);

        if(taskManager.isCurrentTimePeriodActive()) {
            int minutesThisWeek = groupCurrentWeekMinutes.get(group);
            holder.tasksCount.setText(tasksCount + " task" + (tasksCount == 1 ? "" : "s") + " this week"
                    + "\n" + Utils.formatHourMinuteTime(minutesThisWeek) + " worked this week");
        } else {
            holder.tasksCount.setText("");
        }

        if(minMaxStats[0].date != null && minMaxStats[1].date != null) {
            holder.taskNotes.setLines(10);
            //align numbers with each other using tabs
            holder.taskNotes.setText("Detailed weekly statistics:\n" +
                    "Logged weeks: \t\t\t\t\t" + stats[3] + "\n" +
                    "Average workload: \t\t" + Utils.formatHourMinuteTime(stats[0]) + "\n" +
                    "Median workload: \t\t" + Utils.formatHourMinuteTime(stats[1]) + "\n" +
                    "Standard deviation: \t" + Utils.formatHourMinuteTime(stats[2]) + "\n\n" +
                    "Min workload: " + "\t\t\t\t\t" + Utils.formatHourMinuteTime(minMaxStats[0].minutes) + "\n" +
                    "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" + Utils.formatLocalDate(minMaxStats[0].date) + " - " + Utils.formatLocalDate(minMaxStats[0].date.plusDays(7)) + "\n" +
                    "Max workload: " + "\t\t\t\t\t" + Utils.formatHourMinuteTime(minMaxStats[1].minutes) + "\n" +
                    "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" + Utils.formatLocalDate(minMaxStats[1].date) + " - " + Utils.formatLocalDate(minMaxStats[1].date.plusDays(7)));
        } else {
            holder.taskNotes.setLines(5);
            holder.taskNotes.setText("Detailed weekly statistics:\n" +
                    "Logged weeks: \t\t\t\t\t" + stats[3] + "\n" +
                    "Average workload: \t\t" + Utils.formatHourMinuteTime(stats[0]) + "\n" +
                    "Median workload: \t\t" + Utils.formatHourMinuteTime(stats[1]) + "\n" +
                    "Standard deviation: \t" + Utils.formatHourMinuteTime(stats[2]));
        }

        holder.actionTaskText.setText("View Tasks");
        holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToGroupTasks(group);
            }
        });

        holder.secondaryActionTaskText.setText("Set Time Invariants");
        holder.secondaryActionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToGroupTimeInvariant(group);
            }
        });

        holder.thirdActionTaskText.setText("Change Color");
        holder.thirdActionTaskText.setOnClickListener(new View.OnClickListener() {
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

        holder.fourthActionTaskText.setVisibility(View.VISIBLE);
        holder.fourthActionTaskText.setText("Delete Group");
        holder.fourthActionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder deleteDialog = new AlertDialog.Builder(context);

                boolean doTasksExist = !taskManager.getCurrentTimePeriod().getAllTasksByGroup(group).isEmpty();
                if(doTasksExist) {
                    int taskSize = taskManager.getCurrentTimePeriod().getAllTasksByGroup(group).size();
                    deleteDialog.setTitle("Confirm group and tasks deletion");
                    deleteDialog.setMessage("Are you sure you want to delete the group " + group.getGroupName() +
                            " along with its " + taskSize + " total task" + (taskSize == 1 ? "" : "s") + "? THIS CANNOT BE UNDONE!");
                } else {
                    deleteDialog.setTitle("Confirm group deletion");
                    deleteDialog.setMessage("Are you sure you want to delete the group " + group.getGroupName() + "?");
                }

                deleteDialog.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(doTasksExist) {
                            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(context);

                            deleteDialog.setTitle("Confirm group and tasks deletion");
                            deleteDialog.setMessage("Press CONFIRM DELETE if you are sure. You will lose all tasks, completed and upcoming, associated with this group. THIS CANNOT BE UNDONE!");

                            deleteDialog.setPositiveButton("CONFIRM DELETE", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteGroup(group);
                                }
                            });

                            deleteDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });

                            deleteDialog.show();
                        } else {
                            deleteGroup(group);
                        }
                    }
                });

                deleteDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                deleteDialog.show();
            }
        });

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

    private void deleteGroup(Group group) {
        if(taskManager.deleteGroup(group)) {
            Toast.makeText(context, group.getGroupName() + " was deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Something went wrong, and the group could not be deleted...", Toast.LENGTH_SHORT).show();
        }

        groups = new ArrayList<>(taskManager.getAllCurrentGroups(taskManager.getCurrentTimePeriod()));
        notifyDataSetChanged();
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

    private void navigateToGroupTimeInvariant(Group group) {
        NavController nav = Navigation.findNavController(context, R.id.nav_host_fragment);

        Bundle bundle = new Bundle();

        bundle.putString("group", group.getGroupName());

        NavOptions navOptions = new NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_left)
                .setExitAnim(R.anim.slide_out_right)
                .setPopEnterAnim(R.anim.slide_in_right)
                .setPopExitAnim(R.anim.slide_out_left)
                .build();

        nav.navigate(R.id.nav_time_invariant, bundle, navOptions);
    }

    /**
     *
     * into groupStats -> array of four ints: average minutes, median minutes, standard deviation, then total weeks counted
     * into groupMinMax -> min minutes and date, max minutes and date (monday)
     * TODO: only calculates WORKED, does not calculate worked and lectures; option to view?
     * @param group
     *
     */
    private void calculateAverageMedianSTDWorkedPerWeek(Group group) {
        int averageMinutesWorked = 0;
        int totalWeeksWorked = 0;

        LocalDate currentLoopDate = taskManager.getCurrentTimePeriod().getBeginDate();

        LocalDate endLoopDate;
        if(taskManager.isCurrentTimePeriodActive()) {
            endLoopDate = LocalDate.now();
        } else {
            endLoopDate = taskManager.getCurrentTimePeriod().getEndDate();
        }

        //start on the week when the group started, not on the beginning date
        main:
        while(currentLoopDate.plusDays(-1).isBefore(endLoopDate)) {
            for(LocalDate date : LogicalUtils.getWorkWeekDates(currentLoopDate)) {
                if(taskManager.getCurrentTimePeriod().getStatsManager().getMinutesWorkedByGroup(group, date, false) > 0) {
                    break main;
                }
            }

            currentLoopDate = currentLoopDate.plusDays(7);
        }

        //make the week start on a MONDAY not sunday, offset by one day
        long currentWeekValue = currentLoopDate.plusDays(-1).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + (currentLoopDate.plusDays(-1).getYear() * 52);
        long endWeekValue = endLoopDate.plusDays(-1).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + (endLoopDate.plusDays(-1).getYear() * 52);

        List<Integer> minutesPerWeek = new ArrayList<>();

        int minMinutes = Integer.MAX_VALUE;
        LocalDate minDate = null;
        int maxMinutes = Integer.MIN_VALUE;
        LocalDate maxDate = null;

        StatsManager statsManager = taskManager.getCurrentTimePeriod().getStatsManager();

        //calculate per week
        while(currentWeekValue <= endWeekValue) {
            boolean workedThisWeek = false;
            int totalMinutes = 0;
            for(LocalDate date : LogicalUtils.getWorkWeekDates(currentLoopDate)) {
                if(!workedThisWeek) {
                    workedThisWeek = true;
                }

                int minutes = statsManager.getMinutesWorkedByGroup(group, date, false);
                totalMinutes += minutes;
                averageMinutesWorked += minutes;
            }

            if(workedThisWeek) {
                totalWeeksWorked++;
            }

            minutesPerWeek.add(totalMinutes);

            if(totalMinutes < minMinutes) {
                minMinutes = totalMinutes;
                minDate = currentLoopDate;
            }

            if(totalMinutes > maxMinutes) {
                maxMinutes = totalMinutes;
                maxDate = currentLoopDate;
            }

            currentLoopDate = currentLoopDate.plusDays(7);
            currentWeekValue = currentLoopDate.plusDays(-1).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + (currentLoopDate.plusDays(-1).getYear() * 52);
        }

        if(totalWeeksWorked == 0) {
            groupStats.put(group, new Integer[] {0, 0, 0, 0});
            groupMinMax.put(group, new LocalDateMinutes[] {new LocalDateMinutes(null, 0), new LocalDateMinutes(null, 0)});
        }

        double average = (double) averageMinutesWorked / totalWeeksWorked;

        minutesPerWeek.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Integer.compare(o1, o2);
            }
        });
        int median = minutesPerWeek.isEmpty() ? 0 : minutesPerWeek.get(minutesPerWeek.size() / 2);

        double standardDeviation = 0;
        for(int weeklyMinutes : minutesPerWeek) {
            standardDeviation += Math.pow(average - weeklyMinutes, 2);
        }

        standardDeviation = Math.sqrt(standardDeviation / totalWeeksWorked);

        //find the average
        groupStats.put(group, new Integer[] {(int) average, median, (int) standardDeviation, totalWeeksWorked});
        groupMinMax.put(group, new LocalDateMinutes[] {new LocalDateMinutes(minDate, minMinutes), new LocalDateMinutes(maxDate, maxMinutes)});
    }

    private int getMinutesWorkedThisWeek(Group group) {
        int minutesWorked = 0;

        for(LocalDate date : LogicalUtils.getWorkWeekDates()) { //TODO: GIVES WORKED THIS WEEK, NOT NECESSARILY WITH LECTURES
            minutesWorked += taskManager.getCurrentTimePeriod().getStatsManager().getMinutesWorkedByGroup(group, date, false);
        }

        return minutesWorked;
    }

    public void sortByAverageWeekHours() {
        HashMap<Group, Integer> averageWorkHours = new HashMap<>();

        for(Group group : groups) {
            averageWorkHours.put(group, groupStats.get(group)[0]);
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
            currentWeekHours.put(group, groupCurrentWeekMinutes.get(group));
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
