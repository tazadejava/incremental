package me.tazadejava.incremental.ui.dashboard;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.Utils;

public class MainDashboardDayAdapter extends RecyclerView.Adapter<MainDashboardDayAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView taskName, estimatedTime, tasksCount;
        private RecyclerView taskList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskList = itemView.findViewById(R.id.dashboard_tasks_list);
            taskName = itemView.findViewById(R.id.timePeriodName);
            estimatedTime = itemView.findViewById(R.id.dashoboard_estimated_time);
            tasksCount = itemView.findViewById(R.id.dashboard_tasks_count);
        }
    }

    private TaskManager taskManager;
    private DashboardFragment fragment;
    private RecyclerView recyclerView;
    private Activity context;

    private TimePeriod timePeriod;

    private HashMap<TaskAdapter, ViewHolder> taskAdapters;

    private Set<Task> tasksToday;

    public MainDashboardDayAdapter(TaskManager taskManager, DashboardFragment fragment, RecyclerView recyclerView, Activity context) {
        this.taskManager = taskManager;
        this.fragment = fragment;
        this.recyclerView = recyclerView;
        this.context = context;

        taskAdapters = new HashMap<>();

        timePeriod = taskManager.getCurrentTimePeriod();
        tasksToday = new HashSet<>(timePeriod.getTasksByDay(0));
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
        holder.taskList.setAdapter(adapter = new TaskAdapter(taskManager, context, timePeriod, position, date, tasksToday, dayTasks, this));
        holder.taskList.setLayoutManager(new LinearLayoutManager(context));

        taskAdapters.put(adapter, holder);

        holder.taskName.setText(dayToTitleFormat(date));

        refreshEstimatedTime(holder, dayTasks, date);
    }

    private void refreshEstimatedTime(ViewHolder holder, List<Task> dayTasks, LocalDate date) {
        if(dayTasks.isEmpty()) {
            if(timePeriod.getWorkPreferences().isBlackedOutDay(date)) {
                holder.estimatedTime.setText("No tasks here; it's your day off!");
            } else {
                holder.estimatedTime.setText("No tasks here!");
            }
            holder.tasksCount.setText("");
        } else {
            int tasksCount = timePeriod.getTasksByDay(date).size();
            holder.estimatedTime.setText("est. " + Utils.formatHourMinuteTime(timePeriod.getEstimatedMinutesOfWorkForDate(date)) + " of work remaining");
            holder.tasksCount.setText(tasksCount + " task" + (tasksCount == 1 ? "" : "s"));
        }
    }

    private void refreshEstimatedTime(TaskAdapter adapter) {
        List<Task> dayTasks = adapter.getUpdatedTasks();
        refreshEstimatedTime(taskAdapters.get(adapter), dayTasks, adapter.getDate());
    }

    private String dayToTitleFormat(LocalDate date) {
        if(date.equals(LocalDate.now())) {
            return "Today, " + date.getMonthValue() + "/" + date.getDayOfMonth();
        }

        String dayOfWeek = date.getDayOfWeek().toString();
        return dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase() + ", " + date.getMonthValue() + "/" + date.getDayOfMonth();
    }

    public void updateTaskCards(Task task) {
        for(TaskAdapter adapter : taskAdapters.keySet()) {
            if(adapter.hasTask(task)) {
                adapter.updateTaskCardsAndAnimation(task);
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

    public void smoothScrollToPosition(int position) {
        if(recyclerView.computeVerticalScrollOffset() == 0) {
            return;
        }

        recyclerView.smoothScrollToPosition(position);
    }

    @Override
    public int getItemCount() {
        return TimePeriod.DAILY_LOGS_AHEAD_COUNT_SHOW + 1;
    }
}
