package me.tazadejava.incremental.ui.dashboard;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

        private TextView dashboardDate, estimatedTime, tasksCount;
        private RecyclerView taskList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskList = itemView.findViewById(R.id.dashboard_tasks_list);
            dashboardDate = itemView.findViewById(R.id.timePeriodName);
            estimatedTime = itemView.findViewById(R.id.dashoboard_estimated_time);
            tasksCount = itemView.findViewById(R.id.dashboard_tasks_count);
        }
    }

    private TaskManager taskManager;
    private DashboardFragment fragment;
    private RecyclerView recyclerView;
    private LinearLayoutManager llm;
    private Activity context;

    private TimePeriod timePeriod;

    private HashMap<TaskAdapter, ViewHolder> taskAdapters;

    private Set<Task> tasksToday, alreadyAnimatedTasks;

    public MainDashboardDayAdapter(TaskManager taskManager, DashboardFragment fragment, RecyclerView recyclerView, LinearLayoutManager llm, Activity context) {
        this.taskManager = taskManager;
        this.fragment = fragment;
        this.recyclerView = recyclerView;
        this.llm = llm;
        this.context = context;

        taskAdapters = new HashMap<>();
        alreadyAnimatedTasks = new HashSet<>();

        timePeriod = taskManager.getCurrentTimePeriod();
        tasksToday = new HashSet<>(timePeriod.getTasksByDay(0));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard, parent, false);
        ViewHolder holder = new ViewHolder(view);

        holder.taskList.setLayoutManager(new LinearLayoutManager(context));

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocalDate date = LocalDate.now().plusDays(position);

        List<Task> dayTasks = timePeriod.getTasksByDay(position);

        TaskAdapter adapter;
        holder.taskList.setAdapter(adapter = new TaskAdapter(taskManager, context, timePeriod, position, date, tasksToday, dayTasks, this));
        taskAdapters.put(adapter, holder);

        holder.dashboardDate.setText(dayToTitleFormat(date));
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

    protected void refreshEstimatedTime(TaskAdapter adapter) {
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

    public void updateTaskCards(Task task, @Nullable TaskAdapter skipAdapter) {
        for(TaskAdapter adapter : taskAdapters.keySet()) {
            if(skipAdapter == adapter) {
                continue;
            }

            if(adapter.hasTask(task)) {
                adapter.updateTaskCardsAndAnimation(task);
            }
        }

        //also refresh the graph
        fragment.refreshChartData();
    }

    public void updateDayLayouts(Task task, @Nullable TaskAdapter skipAdapter) {
        for(TaskAdapter adapter : taskAdapters.keySet()) {
            if(skipAdapter == adapter) {
                continue;
            }

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

        int firstVisible = llm.findFirstVisibleItemPosition();
        int lastVisible = llm.findLastVisibleItemPosition();

        //when first < last, the day should already be visible and scrolling becomes choppy, so don't scroll there
        if(firstVisible >= lastVisible) {
            recyclerView.smoothScrollToPosition(position);
        }
    }

    public boolean hasTaskBeenAnimated(Task task) {
        return alreadyAnimatedTasks.contains(task);
    }

    public void markTaskAsAnimated(Task task) {
        alreadyAnimatedTasks.add(task);
    }

    public void unmarkTaskAsAnimated(Task task) {
        alreadyAnimatedTasks.remove(task);
    }

    public void unmarkAllTasksAsAnimated() {
        alreadyAnimatedTasks.clear();
    }

    @Override
    public int getItemCount() {
        return TimePeriod.DAILY_LOGS_AHEAD_COUNT_SHOW_UI + 1;
    }
}
