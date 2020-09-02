package me.tazadejava.incremental.ui.groups;

import android.content.Context;
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
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.RepeatingTask;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class SpecificGroupTaskAdapter extends RecyclerView.Adapter<SpecificGroupTaskAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout;
        public TextView taskGroupName, startDate, actionTaskText, workRemaining, dueDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskGroupName = itemView.findViewById(R.id.timePeriodName);
            startDate = itemView.findViewById(R.id.estimatedDailyTime);
            actionTaskText = itemView.findViewById(R.id.actionTaskText);

            workRemaining = itemView.findViewById(R.id.estimatedTotalTimeLeft);
            dueDate = itemView.findViewById(R.id.task_due_date);
        }
    }

    private Context context;
    private TaskManager taskManager;

    private List<Task> tasks;

    public SpecificGroupTaskAdapter(TaskManager taskManager, Context context, Group group, TimePeriod timePeriod) {
        this.context = context;
        this.taskManager = taskManager;

        tasks = new ArrayList<>(timePeriod.getAllTasksByGroup(group));

        tasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task task, Task t1) {
                if(task.getDueDateTime().equals(t1.getDueDateTime())) {
                    return task.getParent().getStartDate().compareTo(t1.getParent().getStartDate());
                } else {
                    return task.getDueDateTime().compareTo(t1.getDueDateTime());
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);

        updateCardColor(task.getGroup(), holder);

        holder.taskCardConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                taskManager.setActiveEditTask(task);

                Intent editTask = new Intent(context, CreateTaskActivity.class);
                context.startActivity(editTask);
                return true;
            }
        });

        holder.taskGroupName.setText(task.getName());

        LocalDate startDate;
        if(task.getStartDate() != null) {
            startDate = task.getStartDate();
        } else {
            if(task.getParent() instanceof RepeatingTask) {
                startDate = ((RepeatingTask) task.getParent()).getTaskStartDate(task);
            } else {
                startDate = task.getParent().getStartDate();
            }
        }

        holder.startDate.setText("Task starts " + Utils.formatLocalDateWithDayOfWeek(startDate));

        int totalMinutesLeft = task.getTotalMinutesLeftOfWork();
        holder.workRemaining.setText(Utils.formatHourMinuteTime(totalMinutesLeft) + " of total work remaining");

        LocalDateTime dueDateTime = task.getDueDateTime();
        if(dueDateTime.toLocalDate().equals(LocalDate.now())) {
            holder.dueDate.setText("Due TODAY" + " @ " + Utils.formatLocalTime(dueDateTime));
        } else {
            holder.dueDate.setText("Due " + dueDateTime.getMonthValue() + "/" + dueDateTime.getDayOfMonth() + " @ " + Utils.formatLocalTime(dueDateTime));
        }

        holder.actionTaskText.setText("Edit Task");
        holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskManager.setActiveEditTask(task);

                Intent editTask = new Intent(context, CreateTaskActivity.class);
                context.startActivity(editTask);
            }
        });
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

                unwrapped.setLayerSize(1, (int) (viewHolder.taskCardConstraintLayout.getWidth() * 0), unwrapped.getLayerHeight(1));

                viewHolder.taskCardConstraintLayout.setBackground(unwrapped);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
