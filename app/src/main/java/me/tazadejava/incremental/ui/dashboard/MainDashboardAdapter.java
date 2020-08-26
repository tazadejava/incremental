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
import java.util.HashMap;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class MainDashboardAdapter extends RecyclerView.Adapter<MainDashboardAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView taskName, estimatedTime;
        private RecyclerView taskList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskList = itemView.findViewById(R.id.dashboard_tasks_list);
            taskName = itemView.findViewById(R.id.timePeriodName);
            estimatedTime = itemView.findViewById(R.id.dashoboard_estimated_time);
        }
    }

    private DashboardFragment fragment;
    private Context context;

    private TimePeriod timePeriod;

    private HashMap<TaskAdapter, ViewHolder> taskAdapters;

    public MainDashboardAdapter(DashboardFragment fragment, Context context) {
        this.fragment = fragment;
        this.context = context;

        taskAdapters = new HashMap<>();

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
        holder.taskList.setAdapter(adapter = new TaskAdapter(context, timePeriod, position, date, dayTasks, this));
        holder.taskList.setLayoutManager(new LinearLayoutManager(context));

        taskAdapters.put(adapter, holder);

        holder.taskName.setText(dayToTitleFormat(date));

        if(dayTasks.isEmpty()) {
            if(timePeriod.getWorkPreferences().isBlackedOutDay(date)) {
                holder.estimatedTime.setText("No tasks here; it's your day off!");
            } else {
                holder.estimatedTime.setText("No tasks here!");
            }
        } else {
            holder.estimatedTime.setText("est. " + Utils.formatHourMinuteTime(timePeriod.getEstimatedMinutesOfWorkForDate(date)) + " of work remaining");
        }
    }

    private void refreshEstimatedTime(TaskAdapter adapter) {
        ViewHolder holder = taskAdapters.get(adapter);

        List<Task> dayTasks = adapter.getUpdatedTasks();

        if(dayTasks.isEmpty()) {
            holder.estimatedTime.setText("No tasks here; it's your day off!");
        } else {
            holder.estimatedTime.setText("est. " + Utils.formatHourMinuteTime(timePeriod.getEstimatedMinutesOfWorkForDate(adapter.getDate())) + " of work remaining");
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
        for(TaskAdapter adapter : taskAdapters.keySet()) {
            if(adapter.hasTask(task)) {
                adapter.updateTaskCards(task);
            }
        }

        //also refresh the graph
        fragment.refreshChartData();
    }

    public void updateDayLayouts(Task task) {
        for(TaskAdapter adapter : taskAdapters.keySet()) {
            if(adapter.hasTask(task)) {
                adapter.refreshLayout();

                refreshEstimatedTime(adapter);
            }
        }
    }

    @Override
    public int getItemCount() {
        return TimePeriod.DAILY_LOGS_AHEAD_COUNT;
    }
}
