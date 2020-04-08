package me.dracorrein.incremental.ui.dashboard;

import android.content.Context;
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

import me.dracorrein.incremental.R;
import me.dracorrein.incremental.logic.dashboard.Day;
import me.dracorrein.incremental.logic.dashboard.Task;

public class DashboardTaskAdapter extends RecyclerView.Adapter<DashboardTaskAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ConstraintLayout taskCardConstraintLayout;
        private TextView taskName, taskClass, taskSubtasksLeft, taskHoursRemaining, taskDueDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskName = itemView.findViewById(R.id.task_name);
            taskClass = itemView.findViewById(R.id.task_class);
            taskSubtasksLeft = itemView.findViewById(R.id.task_subtasks_left);
            taskHoursRemaining = itemView.findViewById(R.id.task_hours_remaining);
            taskDueDate = itemView.findViewById(R.id.task_due_date);
        }
    }

    private Context context;
    private Day day;

    public DashboardTaskAdapter(Context context, Day day) {
        this.context = context;
        this.day = day;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Task task = day.getTasks().get(position);

        holder.taskCardConstraintLayout.post(new Runnable() {
            @Override
            public void run() {
                LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(context, R.drawable.task_card_gradient).mutate();

                GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
                GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);

                darkColor.setColor(task.getCardBeginColor());
                lightColor.setColor(task.getCardEndColor());

                unwrapped.setLayerSize(1, (int) (holder.taskCardConstraintLayout.getWidth() * task.getTaskCompletionPercentage()), unwrapped.getLayerHeight(1));
                holder.taskCardConstraintLayout.setBackground(unwrapped);
            }
        });

        holder.taskName.setText(task.getName());
        holder.taskClass.setText(task.getClassName());

        if(task.getSubTasks() == null) {
            holder.taskSubtasksLeft.setText("est. " + task.getEstimatedCompletionTimeFormatted() + " hour" + (task.getEstimatedCompletionTime() == 1 ? "" : "s") + " remaining");
            holder.taskHoursRemaining.setText("");
        } else {
            holder.taskSubtasksLeft.setText(task.getCompletedSubtasks() + "/" + task.getSubTasks().size() + " subtasks completed");
            holder.taskHoursRemaining.setText("est. " + task.getEstimatedCompletionTimeFormatted() + " hour" + (task.getEstimatedCompletionTime() == 1 ? "" : "s") + " remaining");
        }

        holder.taskDueDate.setText(task.getDueDateFormatted());
    }

    @Override
    public int getItemCount() {
        return day.getTasks().size();
    }
}
