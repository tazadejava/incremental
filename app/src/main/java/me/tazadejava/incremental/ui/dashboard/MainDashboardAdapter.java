package me.tazadejava.incremental.ui.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.dashboard.TimePeriod;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

public class MainDashboardAdapter extends RecyclerView.Adapter<MainDashboardAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView taskName, estimatedTime;
        private RecyclerView taskList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskList = itemView.findViewById(R.id.dashboard_tasks_list);
            taskName = itemView.findViewById(R.id.taskGroupName);
            estimatedTime = itemView.findViewById(R.id.dashoboard_estimated_time);
        }
    }

    private Context context;

    private TimePeriod timePeriod;

    private List<TaskAdapter> taskAdapters;

    public MainDashboardAdapter(Context context) {
        this.context = context;

        taskAdapters = new ArrayList<>();

        timePeriod = IncrementalApplication.taskManager.getCurrentTimePeriod();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalDate date = LocalDate.now().plusDays(position);
        List<Task> dayTasks = timePeriod.getTasksByDay(position);

        TaskAdapter adapter;
        holder.taskList.setAdapter(adapter = new TaskAdapter(context, date, dayTasks, this));
        holder.taskList.setLayoutManager(new LinearLayoutManager(context));

        taskAdapters.add(adapter);

        holder.taskName.setText(dayToTitleFormat(date));

        if(dayTasks.isEmpty()) {
            holder.estimatedTime.setText("No tasks here!");
        } else {
            float estimatedHoursOfWork = timePeriod.getEstimatedHoursOfWorkForDate(date);
            String estimatedHoursOfWorkFormatted = estimatedHoursOfWork % 1 == 0 ? String.valueOf((int) estimatedHoursOfWork) : String.valueOf(estimatedHoursOfWork);
            holder.estimatedTime.setText("est. " + estimatedHoursOfWorkFormatted + " hour" + (estimatedHoursOfWork == 1 ? "" : "s") + " of work remaining");
        }
    }

    private String dayToTitleFormat(LocalDate date) {
        if(date.equals(LocalDate.now())) {
            return "Today, " + date.getMonthValue() + "/" + date.getDayOfMonth();
        } else if(date.equals(LocalDate.now().plusDays(1))) {
            return "Tomorrow, " + date.getMonthValue() + "/" + date.getDayOfMonth();
        }

        String dayOfWeek = date.getDayOfWeek().toString();
        return dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase() + ", " + date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    public void updateTaskCards(Task task) {
        for(TaskAdapter adapter : taskAdapters) {
            if(adapter.hasTask(task)) {
                adapter.updateTaskCards(task);
            }
        }
    }

    public void updateDayLayouts(Task task) {
        for(TaskAdapter adapter : taskAdapters) {
            if(adapter.hasTask(task)) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public int getItemCount() {
        return TimePeriod.DAILY_LOGS_AHEAD_COUNT;
    }
}
