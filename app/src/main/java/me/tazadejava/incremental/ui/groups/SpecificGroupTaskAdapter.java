package me.tazadejava.incremental.ui.groups;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class SpecificGroupTaskAdapter extends RecyclerView.Adapter<SpecificGroupTaskAdapter.ViewHolder> {

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

    private Context context;
    private GroupTasksViewFragment groupFragment;
    private TaskManager taskManager;

    private List<Task> tasks;

    //used to batch delete tasks
    private HashMap<Integer, Task> selectedTasks = new HashMap<>();
    private HashMap<Integer, ConstraintLayout> selectedTaskLayouts = new HashMap<>();
    private boolean isSelectingTasks;

    public SpecificGroupTaskAdapter(TaskManager taskManager, GroupTasksViewFragment groupFragment, Context context, Group group, TimePeriod timePeriod) {
        this.context = context;
        this.groupFragment = groupFragment;
        this.taskManager = taskManager;

        tasks = new ArrayList<>(timePeriod.getAllTasksByGroup(group));

        //sort by start date

        tasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task task, Task t1) {
                //sort by subgroup
                String subgroup1 = task.getSubgroup() == null ? "zzz" : task.getSubgroup().getName();
                String subgroup2 = t1.getSubgroup() == null ? "zzz" : t1.getSubgroup().getName();

                if(Objects.equals(task.getSubgroup(), t1.getSubgroup())) {
                    LocalDate startTask1 = getStartDate(task);
                    LocalDate startTask2 = getStartDate(t1);
                    if (startTask1.equals(startTask2)) {
                        return task.getDueDateTime().compareTo(t1.getDueDateTime());
                    } else {
                        return startTask1.compareTo(startTask2);
                    }
                } else {
                    return subgroup1.compareTo(subgroup2);
                }
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new SpecificGroupTaskAdapter.ViewHolder(view);
    }

    private LocalDate getStartDate(Task task) {
        LocalDate startDate;
        if(task.getStartDate() != null) {
            startDate = task.getStartDate();
        } else {
            startDate = task.getParent().getStartDate();
        }

        return startDate;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);

        if(task.isTaskComplete()) {
            Utils.setViewGradient(task.getGroup(), holder.sideCardAccent, 1);

            holder.taskCardConstraintLayout.setOnLongClickListener(null);

            if(task.getSubgroup() != null) {
                holder.startDate.setText(task.getSubgroup().getName());
            } else {
                holder.startDate.setText("");
            }

            holder.dueDate.setText("Completed on " + Utils.formatLocalDateWithDayOfWeek(task.getLastTaskWorkedTime().toLocalDate()) + "\n@ " + Utils.formatLocalTime(task.getLastTaskWorkedTime().toLocalTime()));

            holder.workRemaining.setText("Worked " + Utils.formatHourMinuteTime(task.getTotalLoggedMinutesOfWork()) + " total");

            holder.actionTaskText.setText("");
            holder.actionTaskText.setOnClickListener(null);
        } else {
            Utils.setViewGradient(task.getGroup(), holder.sideCardAccent, task.getTaskCompletionPercentage());

            holder.taskCardConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    taskManager.setActiveEditTask(task);

                    Intent editTask = new Intent(context, CreateTaskActivity.class);
                    context.startActivity(editTask);
                    return true;
                }
            });

            LocalDate startDate = getStartDate(task);
            holder.startDate.setText("Task starts " + Utils.formatLocalDateWithDayOfWeek(startDate));

            int totalMinutesLeft = task.getTotalMinutesLeftOfWork();
            holder.workRemaining.setText(Utils.formatHourMinuteTime(totalMinutesLeft) + " of total work remaining");

            holder.actionTaskText.setText("Edit Task");
            holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    taskManager.setActiveEditTask(task);

                    Intent editTask = new Intent(context, CreateTaskActivity.class);
                    context.startActivity(editTask);
                }
            });

            LocalDateTime dueDateTime = task.getDueDateTime();
            if(dueDateTime.toLocalDate().equals(LocalDate.now())) {
                holder.dueDate.setText("Due TODAY" + " @ " + Utils.formatLocalTime(dueDateTime));
            } else {
                holder.dueDate.setText("Due " + dueDateTime.getMonthValue() + "/" + dueDateTime.getDayOfMonth() + " @ " + Utils.formatLocalTime(dueDateTime));
            }
        }

        holder.taskGroupName.setText(task.getName());

        holder.taskCardConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(isSelectingTasks) {
                    for(int taskPosition : selectedTasks.keySet()) {
                        selectedTaskLayouts.get(taskPosition).setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                    }

                    groupFragment.setBatchDeleteIconActive(false);
                    isSelectingTasks = false;
                    selectedTasks.clear();
                    selectedTaskLayouts.clear();
                } else {
                    groupFragment.setBatchDeleteIconActive(true);
                    isSelectingTasks = true;
                    holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColorActive));
                    selectedTasks.put(position, task);
                    selectedTaskLayouts.put(position, holder.taskCardConstraintLayout);
                }
                return true;
            }
        });

        holder.taskCardConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isSelectingTasks) {
                    if (selectedTasks.containsKey(position)) {
                        selectedTasks.remove(position);
                        selectedTaskLayouts.remove(position);
                        if (selectedTasks.isEmpty()) {
                            isSelectingTasks = false;
                            groupFragment.setBatchDeleteIconActive(false);
                        }

                        holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                    } else {
                        holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColorActive));
                        selectedTasks.put(position, task);
                        selectedTaskLayouts.put(position, holder.taskCardConstraintLayout);
                    }
                }
            }
        });

        if(isSelectingTasks) {
            holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, selectedTasks.containsKey(position) ? R.attr.cardColorActive : R.attr.cardColor));
        }
    }

    public void batchDeleteTasks() {
        if(isSelectingTasks) {
            AlertDialog.Builder confirmation = new AlertDialog.Builder(context);

            confirmation.setTitle("Are you sure you want to delete " + selectedTasks.size() + " task" + (selectedTasks.size() == 1 ? "" : "s") + "?");
            confirmation.setMessage("This cannot be undone!");

            confirmation.setPositiveButton("DELETE TASK" + (selectedTasks.size() == 1 ? "" : "S"), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    for(Task task : selectedTasks.values()) {
                        taskManager.getCurrentTimePeriod().deleteTaskCompletely(task, task.getParent());
                        tasks.remove(task);
                    }

                    taskManager.saveData(true, taskManager.getCurrentTimePeriod());

                    for(int taskPosition : selectedTasks.keySet()) {
                        selectedTaskLayouts.get(taskPosition).setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                    }

                    groupFragment.setBatchDeleteIconActive(true);
                    isSelectingTasks = false;
                    selectedTasks.clear();
                    selectedTaskLayouts.clear();

                    notifyDataSetChanged();
                }
            });

            confirmation.show();
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
