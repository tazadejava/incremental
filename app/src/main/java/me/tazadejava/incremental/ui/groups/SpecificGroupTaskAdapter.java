package me.tazadejava.incremental.ui.groups;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.SubGroup;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
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

    private Activity context;
    private GroupTasksViewFragment groupFragment;
    private TaskManager taskManager;

    private List<Task> tasks;
    private HashMap<Task, List<Task>> taskGroups = new HashMap<>();
    private HashMap<Task, Integer> taskGroupMinutes = new HashMap<>();
    private HashMap<Task, Task> taskGroupHeads = new HashMap<>(); //store the top task pointer for all task groups
    private Set<Task> taskGroupsVisible = new HashSet<>();

    //used to batch delete tasks; the array is used to batch delete a batch-created task
    private Set<Task> selectedTasks = new HashSet<>();
    private boolean isSelectingTasks;

    public SpecificGroupTaskAdapter(TaskManager taskManager, GroupTasksViewFragment groupFragment, Activity context, Group group, TimePeriod timePeriod) {
        this.context = context;
        this.groupFragment = groupFragment;
        this.taskManager = taskManager;

        tasks = new ArrayList<>(timePeriod.getAllTasksByGroup(group));

        //sort by subgroup name, if applicable

        tasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task task, Task t1) {
                if(task.getSubgroup() == null) {
                    return 1;
                }
                if(t1.getSubgroup() == null) {
                    return -1;
                }

                int subgroup = task.getSubgroup().getName().compareTo(t1.getSubgroup().getName());

                if(subgroup == 0) {
                    return task.getDueDateTime().compareTo(t1.getDueDateTime());
                } else {
                    return subgroup;
                }
            }
        });

        //first off, group any subgroups together
        Set<Task> subgroupedTaskHeads = new HashSet<>();
        List<Task> tasksGrouped = new ArrayList<>();
        for(int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);

            if(task.getSubgroup() == null) {
                tasksGrouped.add(task);
                continue;
            }

            //if created at the same time, then they are batched together
            if(i + 1 < tasks.size() && tasks.get(i + 1).getSubgroup() != null && tasks.get(i + 1).getSubgroup().equals(task.getSubgroup())) {
                int totalMinutes = task.getTotalLoggedMinutesOfWork();
                List<Task> batchTasks = new ArrayList<>();
                batchTasks.add(task);
                taskGroupHeads.put(task, task);

                do {
                    totalMinutes += tasks.get(i + 1).getTotalLoggedMinutesOfWork();
                    batchTasks.add(tasks.get(i + 1));
                    taskGroupHeads.put(tasks.get(i + 1), task);
                    i++;
                } while(i + 1 < tasks.size() && tasks.get(i + 1).getSubgroup() != null && tasks.get(i + 1).getSubgroup().equals(task.getSubgroup()));

                tasksGrouped.add(task);
                subgroupedTaskHeads.add(task);

                sortTasksListByStartDate(batchTasks);

                taskGroups.put(task, batchTasks);
                taskGroupMinutes.put(task, totalMinutes);
            } else {
                tasksGrouped.add(task);
            }
        }

        tasks = tasksGrouped;

        //sort by name

        tasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task task, Task t1) {
                int name = task.getName().compareTo(t1.getName());

                if(name == 0) {
                    //guarantee that if an existing head matches with another name that is NOT in the subgroup, then it will be chosen first
                    if(subgroupedTaskHeads.contains(task)) {
                        return -1;
                    } else if(subgroupedTaskHeads.contains(t1)) {
                        return 1;
                    } else {
                        return task.getDueDateTime().compareTo(t1.getDueDateTime());
                    }
                } else {
                    return name;
                }
            }
        });

        //next, group tasks if they have the same name (minus a suffix of a number)
        tasksGrouped = new ArrayList<>();
        for(int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);

            boolean hasSameName = i + 1 < tasks.size() && areTaskNamesEffectivelyIdentical(task, tasks.get(i + 1));
            if(hasSameName) {
                int totalMinutes = task.getTotalLoggedMinutesOfWork();
                List<Task> batchTasks = new ArrayList<>();

                //if already grouped via subgroup, then re-add the previous tasks first
                if(subgroupedTaskHeads.contains(task)) {
                    batchTasks.addAll(taskGroups.get(task));
                }

                batchTasks.add(0, task);
                taskGroupHeads.put(task, task);

                do {
                    totalMinutes += tasks.get(i + 1).getTotalLoggedMinutesOfWork();
                    batchTasks.add(tasks.get(i + 1));
                    taskGroupHeads.put(tasks.get(i + 1), task);
                    i++;

                    hasSameName = i + 1 < tasks.size() && areTaskNamesEffectivelyIdentical(task, tasks.get(i + 1));
                } while (hasSameName);

                tasksGrouped.add(task);

                sortTasksListByStartDate(batchTasks);

                taskGroups.put(task, batchTasks);
                taskGroupMinutes.put(task, totalMinutes);
            } else {
                tasksGrouped.add(task);
            }
        }

        tasks = tasksGrouped;

        //sort by start date
        sortTasksListByStartDate(tasks);
    }

    /**
     * Will return true if the two task names are identical, ignoring the appended number at the end, if it exists (from batch-creation)
     * @param task1
     * @param task2
     * @return
     */
    private boolean areTaskNamesEffectivelyIdentical(Task task1, Task task2) {
        if(task1.getName().equals(task2.getName())) {
            return true;
        }

        String task1Name = task1.getName();
        String task2Name = task2.getName();

        //remove appended number, if it exists
        int lastIndex;
        if((lastIndex = task1Name.lastIndexOf(" ")) != -1) {
            String[] split = task1Name.split(" ");
            if(Utils.isInteger(split[split.length - 1])) {
                task1Name = task1Name.substring(0, lastIndex);
            }
        }

        if((lastIndex = task2Name.lastIndexOf(" ")) != -1) {
            String[] split = task2Name.split(" ");
            if(Utils.isInteger(split[split.length - 1])) {
                task2Name = task2Name.substring(0, lastIndex);
            }
        }

        return task1Name.equals(task2Name);
    }

    private void sortTasksListByStartDate(List<Task> tasks) {
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

    private boolean isTaskGroupHead(int position, Task task) {
        return (position == 0 || taskGroupHeads.getOrDefault(tasks.get(position - 1), null) != taskGroupHeads.getOrDefault(task, task));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);

        if(taskGroups.containsKey(task) && isTaskGroupHead(position, task)) {
            List<Task> groupTasks = taskGroups.get(task);
            Task lastTask = groupTasks.get(groupTasks.size() - 1);
            holder.workRemaining.setText("Worked " + Utils.formatHourMinuteTime(taskGroupMinutes.get(task)) + " total\n" +
                    groupTasks.size() + " grouped task" + (groupTasks.size() == 1 ? "" : "s"));

            if(task.getSubgroup() != null) {
                //if all tasks are the same subgroup, label them
                boolean allSameSubgroup = true;
                for (Task groupTask : groupTasks) {
                    if (groupTask.getSubgroup() == null || !groupTask.getSubgroup().equals(task.getSubgroup())) {
                        allSameSubgroup = false;
                        break;
                    }
                }

                if (allSameSubgroup) {
                    holder.startDate.setText(task.getSubgroup().getName());
                } else {
                    holder.startDate.setText("");
                }
            }

            //calculate percent done
            int completedGroupTasks = 0;
            for(Task groupTask : groupTasks) {
                if(groupTask.isTaskComplete()) {
                    completedGroupTasks++;
                }
            }

            LocalDate startDateFirst = getStartDate(task);
            LocalDate startDateLast = getStartDate(lastTask);
            holder.dueDate.setText("Spans from " +
                    Utils.formatLocalDate(startDateFirst) +
                    " to " +
                    Utils.formatLocalDate(startDateLast));

            updateTaskGroupState(holder, task, taskGroupsVisible.contains(task));

            Utils.setViewGradient(task.getGroup(), holder.sideCardAccent, (double) completedGroupTasks / groupTasks.size());

            holder.taskGroupName.setTextColor(task.getGroup().getLightColor());
            holder.taskGroupName.setText(task.getName() + " +" + (groupTasks.size() - 1));
        } else {
            if (task.isTaskComplete()) {
                Utils.setViewGradient(task.getGroup(), holder.sideCardAccent, 1);

                holder.taskCardConstraintLayout.setOnLongClickListener(null);

                if (task.getSubgroup() != null) {
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
                        editTask.putExtra("isViewingGroupTasks", true);
                        context.startActivity(editTask);
                    }
                });

                LocalDateTime dueDateTime = task.getDueDateTime();
                if (dueDateTime.toLocalDate().equals(LocalDate.now())) {
                    holder.dueDate.setText("Due TODAY" + " @ " + Utils.formatLocalTime(dueDateTime));
                } else {
                    holder.dueDate.setText("Due " + dueDateTime.getMonthValue() + "/" + dueDateTime.getDayOfMonth() + " @ " + Utils.formatLocalTime(dueDateTime));
                }
            }

            holder.taskGroupName.setTextColor(Utils.getThemeAttrColor(context, R.attr.cardTextColor));
            holder.taskGroupName.setText(task.getName());
        }

        //task selection for deletion logic

        holder.taskCardConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(isSelectingTasks) {
                    groupFragment.setBatchDeleteIconAndOptionsActive(false);
                    isSelectingTasks = false;
                    selectedTasks.clear();

                    notifyDataSetChanged();
                } else {
                    groupFragment.setBatchDeleteIconAndOptionsActive(true);
                    isSelectingTasks = true;
                    holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColorActive));

                    if(taskGroups.containsKey(task) && isTaskGroupHead(position, task)) {
                        selectedTasks.addAll(taskGroups.get(task));
                        notifyItemRangeChanged(position + 1, taskGroups.get(task).size());
                    } else {
                        selectedTasks.add(task);

                        //update active status of the head task node
                        if(taskGroupHeads.containsKey(task)) {
                            notifyItemChanged(tasks.indexOf(taskGroupHeads.get(task)));
                        }
                    }
                }
                return true;
            }
        });

        holder.taskCardConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isSelectingTasks) {
                    boolean isGroupTaskSelected = false;
                    boolean isHead = isTaskGroupHead(position, task);
                    if(taskGroups.containsKey(task) && isHead) {
                        for(Task groupTask : taskGroups.get(task)) {
                            if(selectedTasks.contains(groupTask)) {
                                isGroupTaskSelected = true;
                                break;
                            }
                        }
                    }

                    if (isGroupTaskSelected || selectedTasks.contains(task)) {
                        if(taskGroups.containsKey(task) && isHead) {
                            selectedTasks.removeAll(taskGroups.get(task));
                            notifyItemRangeChanged(position + 1, taskGroups.get(task).size());
                        } else {
                            selectedTasks.remove(task);

                            //update active status of the head task node
                            if(taskGroupHeads.containsKey(task)) {
                                notifyItemChanged(tasks.indexOf(taskGroupHeads.get(task)));
                            }
                        }

                        if (selectedTasks.isEmpty()) {
                            isSelectingTasks = false;
                            groupFragment.setBatchDeleteIconAndOptionsActive(false);

                            notifyDataSetChanged();
                        }

                        holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                    } else {
                        holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColorActive));

                        if(taskGroups.containsKey(task) && isHead) {
                            selectedTasks.addAll(taskGroups.get(task));
                            notifyItemRangeChanged(position + 1, taskGroups.get(task).size());
                        } else {
                            selectedTasks.add(task);

                            //update active status of the head task node
                            if(taskGroupHeads.containsKey(task)) {
                                notifyItemChanged(tasks.indexOf(taskGroupHeads.get(task)));
                            }
                        }
                    }
                }
            }
        });

        if(isSelectingTasks) {
            if(taskGroups.containsKey(task) && isTaskGroupHead(position, task)) {
                boolean isGroupTaskSelected = false;
                for(Task groupTask : taskGroups.get(task)) {
                    if(selectedTasks.contains(groupTask)) {
                        isGroupTaskSelected = true;
                        break;
                    }
                }

                holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, isGroupTaskSelected ? R.attr.cardColorActive : R.attr.cardColor));
            } else {
                holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, selectedTasks.contains(task) ? R.attr.cardColorActive : R.attr.cardColor));
            }
        } else {
            holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
        }
    }

    private void updateTaskGroupState(ViewHolder holder, Task task, boolean shouldExpandGroup) {
        if(shouldExpandGroup) {
            holder.actionTaskText.setText("Collapse Task List");
            holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    taskGroupsVisible.remove(task);

                    List<Task> groupTasks = taskGroups.get(task);

                    int index = tasks.indexOf(task);
                    for(int i = index + groupTasks.size(); i > index; i--) {
                        tasks.remove(i);
                    }

                    notifyItemRangeRemoved(index + 1, groupTasks.size());
                    notifyItemChanged(index);
                }
            });
        } else {
            holder.actionTaskText.setText("Expand Task List");
            holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    taskGroupsVisible.add(task);

                    int position = tasks.indexOf(task);
                    List<Task> groupTasks = taskGroups.get(task);

                    tasks.addAll(position + 1, groupTasks);
                    notifyItemRangeInserted(position + 1, groupTasks.size());
                    notifyItemChanged(position);
                }
            });
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
                    for(Task task : selectedTasks) {
                        taskManager.getCurrentTimePeriod().deleteTaskCompletely(task);
                        tasks.remove(task);
                    }

                    taskManager.saveData(true, taskManager.getCurrentTimePeriod());

                    groupFragment.setBatchDeleteIconAndOptionsActive(false);
                    isSelectingTasks = false;
                    selectedTasks.clear();

                    notifyDataSetChanged();
                }
            });

            confirmation.show();
        }
    }

    public void selectAllIncompleteTasks() {
        for(Task task : tasks) {
            if(!task.isTaskComplete()) {
                selectedTasks.add(task);
            }

            if(taskGroups.containsKey(task) && !taskGroupsVisible.contains(task)) {
                for(Task groupTask : taskGroups.get(task)) {
                    if (!groupTask.isTaskComplete()) {
                        selectedTasks.add(groupTask);
                    }
                }
            }
        }

        if(!selectedTasks.isEmpty()) {
            isSelectingTasks = true;
            groupFragment.setBatchDeleteIconAndOptionsActive(true);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
