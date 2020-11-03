package me.tazadejava.incremental.ui.archive;

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

import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class PastTasksListAdapter extends RecyclerView.Adapter<PastTasksListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout;

        public TextView taskName, taskDetails, taskGroup, taskCompletionDate, actionTaskText;

        public View sideCardAccent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskName = itemView.findViewById(R.id.timePeriodName);
            taskDetails = itemView.findViewById(R.id.estimatedDailyTime);
            taskGroup = itemView.findViewById(R.id.task_class);
            taskCompletionDate = itemView.findViewById(R.id.task_due_date);

            actionTaskText = itemView.findViewById(R.id.actionTaskText);

            sideCardAccent = itemView.findViewById(R.id.sideCardAccent);
        }
    }

    private Context context;

    private List<Task> tasks;

    public PastTasksListAdapter(Context context, List<Task> tasks) {
        this.context = context;

        this.tasks = tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new PastTasksListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);

        Utils.setViewGradient(task.getGroup(), holder.sideCardAccent, 1);

        holder.taskName.setText(task.getName());
        holder.taskGroup.setText(task.getGroupName());

        holder.taskGroup.setTextColor(task.getGroup().getLightColor());

        holder.taskDetails.setText("Worked " + Utils.formatHourMinuteTime(task.getTotalLoggedMinutesOfWork()) + " total");

        holder.taskCompletionDate.setText("Completed on " + Utils.formatLocalDateWithDayOfWeek(task.getLastTaskWorkedTime().toLocalDate()) + "\n@ " + Utils.formatLocalTime(task.getLastTaskWorkedTime().toLocalTime()));


        holder.actionTaskText.setText("");
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
