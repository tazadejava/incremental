package me.tazadejava.incremental.ui.timeperiods;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.create.CreateTimePeriodActivity;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class TimePeriodsListAdapter extends RecyclerView.Adapter<TimePeriodsListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout;
        public TextView timePeriodName, timePeriodDatesText, timePeriodActiveText, actionTaskText;

        public View sideCardAccent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            timePeriodName = itemView.findViewById(R.id.timePeriodName);

            timePeriodDatesText = itemView.findViewById(R.id.estimatedDailyTime);
            timePeriodActiveText = itemView.findViewById(R.id.task_due_date);

            actionTaskText = itemView.findViewById(R.id.actionTaskText);

            sideCardAccent = itemView.findViewById(R.id.sideCardAccent);
        }
    }

    private TaskManager taskManager;
    private MainActivity context;

    private List<TimePeriod> timePeriods;

    public TimePeriodsListAdapter(TaskManager taskManager, MainActivity context) {
        this.taskManager = taskManager;
        this.context = context;

        timePeriods = new ArrayList<>();

        for(TimePeriod period : taskManager.getTimePeriods()) {
            if(period.getBeginDate() != null && period.getEndDate() != null) {
                timePeriods.add(period);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new TimePeriodsListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimePeriod timePeriod = timePeriods.get(position);

        holder.actionTaskText.setText("Edit Start/End Dates");

        holder.timePeriodName.setText(timePeriod.getName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate beginDate = timePeriod.getBeginDate();
        LocalDate endDate = timePeriod.getEndDate();

        holder.timePeriodDatesText.setText(beginDate.format(formatter) + " to " + endDate.format(formatter));

        if(timePeriod.isInTimePeriod(LocalDate.now())) {
            holder.timePeriodActiveText.setText("Active");
        } else {
            holder.timePeriodActiveText.setText("");
        }

        if(taskManager.getCurrentTimePeriod() == timePeriod) {
            Utils.setViewGradient(ContextCompat.getColor(context, R.color.primaryColor), ContextCompat.getColor(context, R.color.secondaryColor), holder.sideCardAccent, 0);
            holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColorActive));
        } else {
            Utils.setViewGradient(ContextCompat.getColor(context, R.color.primaryColor), ContextCompat.getColor(context, R.color.secondaryColor), holder.sideCardAccent, 1);
            holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
        }

        holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent editTimePeriod = new Intent(context, CreateTimePeriodActivity.class);
                editTimePeriod.putExtra("timePeriod", timePeriod.getName());
                context.startActivity(editTimePeriod);
            }
        });

        holder.taskCardConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(taskManager.getCurrentTimePeriod() != timePeriod) {
                    Toast.makeText(context, "Long press the card to switch time periods.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        holder.taskCardConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(timePeriod != taskManager.getCurrentTimePeriod()) {
                    taskManager.setCurrentTimePeriod(timePeriod);
                    if (taskManager.isCurrentTimePeriodActive()) {
                        Toast.makeText(context, "Switched to the current time period.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Switched to an inactive time period. Note that inactive time periods are read-only.", Toast.LENGTH_SHORT).show();
                    }

                    context.updateNavBarTimePeriod();

                    notifyDataSetChanged();
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return timePeriods.size();
    }
}
