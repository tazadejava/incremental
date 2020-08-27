package me.tazadejava.incremental.ui.archive;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.groups.TaskGroupListAdapter;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class PastTasksListAdapter extends RecyclerView.Adapter<PastTasksListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout;

        public TextView taskName, taskDetails, taskGroup, taskCompletionDate, actionTaskText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskName = itemView.findViewById(R.id.timePeriodName);
            taskDetails = itemView.findViewById(R.id.estimatedDailyTime);
            taskGroup = itemView.findViewById(R.id.task_class);
            taskCompletionDate = itemView.findViewById(R.id.task_due_date);

            actionTaskText = itemView.findViewById(R.id.actionTaskText);
        }
    }

    private Context context;

    private List<Task> tasks;

    public PastTasksListAdapter(Context context) {
        this.context = context;

        tasks = IncrementalApplication.taskManager.getCurrentTimePeriod().getCompletedTasksHistory();
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

        updateCardColor(task.getGroup(), holder);

        holder.taskName.setText(task.getName());
        holder.taskGroup.setText(task.getGroupName());

        holder.taskDetails.setText("Worked " + Utils.formatHourMinuteTime(task.getTotalLoggedMinutesOfWork()) + " total");

        holder.taskCompletionDate.setText("Completed on " + Utils.formatLocalDateWithDayOfWeek(task.getLastTaskWorkedTime().toLocalDate()) + "\n@ " + Utils.formatLocalTime(task.getLastTaskWorkedTime().toLocalTime()));


        holder.actionTaskText.setText("");
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

                unwrapped.setLayerSize(1, (int) (viewHolder.taskCardConstraintLayout.getWidth() * 1), unwrapped.getLayerHeight(1));

                viewHolder.taskCardConstraintLayout.setBackground(unwrapped);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
