package me.tazadejava.incremental.ui.statistics;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.statistics.StatsManager;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
import me.tazadejava.incremental.ui.groups.GroupTasksViewFragment;
import me.tazadejava.incremental.ui.main.Utils;

public class CalendarHeatmapPointAdapter extends RecyclerView.Adapter<CalendarHeatmapPointAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout;
        public TextView taskGroupName, startDate, actionTaskText, workRemaining, dueDate;

        public View sideCardAccent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskGroupName = itemView.findViewById(R.id.timePeriodName);
            startDate = itemView.findViewById(R.id.estimatedTotalTimeLeft);
            actionTaskText = itemView.findViewById(R.id.actionTaskText);

            workRemaining = itemView.findViewById(R.id.estimatedDailyTime);
            dueDate = itemView.findViewById(R.id.task_due_date);

            sideCardAccent = itemView.findViewById(R.id.sideCardAccent);
        }
    }

    private List<Group> groups;
    private HashMap<Group, Integer> groupTimes;
    private HashMap<Group, Float> groupPercentages;

    public CalendarHeatmapPointAdapter(boolean shouldIncludeTimeInvariants, TaskManager taskManager, LocalDate date) {
        StatsManager statsManager = taskManager.getCurrentTimePeriod().getStatsManager();

        groupTimes = new HashMap<>(statsManager.getMinutesWorkedSplitByGroup(date, shouldIncludeTimeInvariants));
        groups = new ArrayList<>(groupTimes.keySet());

        int totalMinutes = 0;
        for(Group group : groups) {
            totalMinutes += groupTimes.get(group);
        }

        groupPercentages = new HashMap<>();
        for(Group group : groups) {
            groupPercentages.put(group, (float) groupTimes.get(group) / totalMinutes);
        }

        groups.sort(new Comparator<Group>() {
            @Override
            public int compare(Group o1, Group o2) {
                return Integer.compare(groupTimes.get(o2), groupTimes.get(o1));
            }
        });
    }

    public CalendarHeatmapPointAdapter(boolean shouldIncludeTimeInvariants, TaskManager taskManager, LocalDate[] dates) {
        StatsManager statsManager = taskManager.getCurrentTimePeriod().getStatsManager();

        groupTimes = new HashMap<>();

        for(LocalDate date : dates) {
            HashMap<Group, Integer> times = statsManager.getMinutesWorkedSplitByGroup(date, shouldIncludeTimeInvariants);

            for(Group group : times.keySet()) {
                groupTimes.putIfAbsent(group, 0);
                groupTimes.put(group, groupTimes.get(group) + times.get(group));
            }
        }

        groups = new ArrayList<>(groupTimes.keySet());

        int totalMinutes = 0;
        for(Group group : groups) {
            totalMinutes += groupTimes.get(group);
        }

        groupPercentages = new HashMap<>();
        for(Group group : groups) {
            groupPercentages.put(group, (float) groupTimes.get(group) / totalMinutes);
        }

        groups.sort(new Comparator<Group>() {
            @Override
            public int compare(Group o1, Group o2) {
                return Integer.compare(groupTimes.get(o2), groupTimes.get(o1));
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new CalendarHeatmapPointAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group group = groups.get(position);

        holder.taskGroupName.setTextColor(group.getLightColor());
        holder.taskGroupName.setText(group.getGroupName());
        holder.workRemaining.setText("Worked " + Utils.formatHourMinuteTime(groupTimes.get(group)));
        Utils.setViewGradient(group, holder.sideCardAccent, groupPercentages.get(group));

        holder.dueDate.setText("");
        holder.actionTaskText.setText("");
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }
}
