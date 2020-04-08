package me.dracorrein.incremental.ui.dashboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import me.dracorrein.incremental.R;
import me.dracorrein.incremental.logic.dashboard.Day;
import me.dracorrein.incremental.ui.main.IncrementalApplication;

public class DashboardAdapter extends RecyclerView.Adapter<DashboardAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView taskName, estimatedTime;
        private RecyclerView taskList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskList = itemView.findViewById(R.id.dashboard_tasks_list);
            taskName = itemView.findViewById(R.id.task_name);
            estimatedTime = itemView.findViewById(R.id.dashoboard_estimated_time);
        }
    }

    private Context context;

    private List<Day> daysList;

    public DashboardAdapter(Context context) {
        this.context = context;

        resetDays();
    }

    private void resetDays() {
        daysList = new ArrayList<>();

        for(int i = 0; i < 7; i++) {
            daysList.add(new Day(LocalDateTime.now().plusDays(i), IncrementalApplication.taskManager.getTasks()));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Day day = daysList.get(position);

        holder.taskList.setAdapter(new DashboardTaskAdapter(context, day));
        holder.taskList.setLayoutManager(new LinearLayoutManager(context));

        holder.taskName.setText(day.getDayFormatted());

        if(day.getTasks().isEmpty()) {
            holder.estimatedTime.setText("No tasks here!");
        } else {
            holder.estimatedTime.setText("est. " + day.getEstimatedHoursOfWorkFormatted() + " hour" + (day.getEstimatedHoursOfWork() == 1 ? "" : "s") + " of work remaining");
        }
    }

    @Override
    public int getItemCount() {
        return daysList.size();
    }

    public void update() {
        resetDays();
    }
}
