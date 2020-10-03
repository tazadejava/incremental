package me.tazadejava.incremental.ui.groups;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
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
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.Utils;

public class TaskGroupListAdapter extends RecyclerView.Adapter<TaskGroupListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout;
        public TextView taskGroupName, tasksCount, actionTaskText, timePeriodText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskGroupName = itemView.findViewById(R.id.timePeriodName);
            tasksCount = itemView.findViewById(R.id.estimatedDailyTime);
            actionTaskText = itemView.findViewById(R.id.actionTaskText);

            timePeriodText = itemView.findViewById(R.id.task_due_date);
        }
    }

    private AppCompatActivity context;
    private TaskManager taskManager;

    private List<Group> groups;

    public TaskGroupListAdapter(TaskManager taskManager, AppCompatActivity context) {
        this.context = context;
        this.taskManager = taskManager;

        groups = new ArrayList<>(taskManager.getAllCurrentGroups());

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

        updateCardColor(group, holder);

        holder.taskGroupName.setText(group.getGroupName());

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
        int tasksTotal = taskManager.getCurrentTimePeriod().getAllTasksByGroup(group).size();

        int[] averageWorkload = getAverageMinutesWorkedPerWeek(group);

        holder.tasksCount.setText(tasksCount + " task" + (tasksCount == 1 ? "" : "s") + " this week"
                + "\n" + tasksTotal + " task" + (tasksTotal == 1 ? "" : "s") + " total (active and pending)"
                + "\n\n" + Utils.formatHourMinuteTime(getMinutesWorkedThisWeek(group)) + " worked this week"
                + "\n\nAverage workload per week (" + averageWorkload[1] + "): " + Utils.formatHourMinuteTime(averageWorkload[0]));

        holder.actionTaskText.setText("Randomize Color");
        holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StatsManager.StatsGroupPacket packet = new StatsManager.StatsGroupPacket(taskManager.getCurrentTimePeriod().getStatsManager(), group);

                group.randomizeColor();
                updateCardColor(group, holder);
                taskManager.saveData(true);

                packet.restore();
            }
        });

        holder.taskCardConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(tasksTotal > 0) {
                    NavController nav = Navigation.findNavController(context, R.id.nav_host_fragment);

                    Bundle bundle = new Bundle();

                    bundle.putString("group", group.getGroupName());

                    TimePeriod scope = taskManager.getGroupScope(group);
                    if (scope == null) {
                        bundle.putString("scope", null);
                    } else {
                        bundle.putString("scope", scope.getName());
                    }

                    nav.navigate(R.id.nav_specific_group, bundle);
                    return true;
                }

                return false;
            }
        });

        TimePeriod timePeriodScope = taskManager.getGroupScope(group);

        if(timePeriodScope == null) {
            holder.timePeriodText.setText("Global Scope");
        } else {
            holder.timePeriodText.setText(timePeriodScope.getName());
        }
    }

    /**
     *
     * @param group
     * @return array of two ints: the first value is the average minutes worked, and the second value is the total weeks counted
     */
    private int[] getAverageMinutesWorkedPerWeek(Group group) {
        int averageMinutesWorked = 0;
        int totalWeeksWorked = 0;

        LocalDate weekDate = taskManager.getCurrentTimePeriod().getBeginDate();
        LocalDate now = LocalDate.now();
        WeekFields week = WeekFields.of(Locale.getDefault());

        //make the week start on a MONDAY not sunday, offset by one day
        long nowWeek = now.plusDays(-1).get(week.weekOfWeekBasedYear());

        while(weekDate.plusDays(-1).get(week.weekOfWeekBasedYear()) < nowWeek || weekDate.getYear() < now.getYear()) {
            boolean workedThisWeek = false;
            for(LocalDate date : LogicalUtils.getWorkWeekDates(weekDate)) {
                if(!workedThisWeek) {
                    workedThisWeek = true;
                }

                averageMinutesWorked += taskManager.getCurrentTimePeriod().getStatsManager().getMinutesWorkedByGroup(group, date);
            }

            if(workedThisWeek) {
                totalWeeksWorked++;
            }

            weekDate = weekDate.plusDays(7);
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

    private void updateCardColor(Group group, ViewHolder viewHolder) {
        viewHolder.taskCardConstraintLayout.post(new Runnable() {
            @Override
            public void run() {
                LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(context, R.drawable.task_card_gradient).mutate();

                GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
                GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);

                darkColor.setColor(group.getBeginColor());
                lightColor.setColor(group.getEndColor());

                unwrapped.setLayerSize(1, (int) (viewHolder.taskCardConstraintLayout.getWidth() * 0.8), unwrapped.getLayerHeight(1));

                viewHolder.taskCardConstraintLayout.setBackground(unwrapped);
            }
        });
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
