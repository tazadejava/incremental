package me.tazadejava.incremental.ui.archive;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.dashboard.TaskAdapter;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.Utils;

public class PastTasksListAdapter extends RecyclerView.Adapter<PastTasksListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout;

        public TextView taskName, taskDetails, taskGroup, taskCompletionDate, actionTaskText;

        public View sideCardAccent;

        public ConstraintLayout expandedOptionsLayout;
        public TextView taskNotes, secondaryActionTaskText, thirdActionTaskText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskName = itemView.findViewById(R.id.timePeriodName);
            taskDetails = itemView.findViewById(R.id.estimatedDailyTime);
            taskGroup = itemView.findViewById(R.id.task_class);
            taskCompletionDate = itemView.findViewById(R.id.task_due_date);

            actionTaskText = itemView.findViewById(R.id.actionTaskText);

            sideCardAccent = itemView.findViewById(R.id.sideCardAccent);

            expandedOptionsLayout = itemView.findViewById(R.id.expandedOptionsLayout);
            secondaryActionTaskText = itemView.findViewById(R.id.secondaryActionTaskText);
            thirdActionTaskText = itemView.findViewById(R.id.thirdActionTaskText);
            taskNotes = itemView.findViewById(R.id.taskNotes);
        }
    }

    private List<Task> tasks;

    public PastTasksListAdapter(List<Task> tasks) {
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
        ViewHolder holder = new PastTasksListAdapter.ViewHolder(view);

        holder.actionTaskText.setEnabled(false);
        holder.thirdActionTaskText.setEnabled(false);
        holder.secondaryActionTaskText.setVisibility(View.GONE);
        holder.thirdActionTaskText.setVisibility(View.GONE);

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

        holder.taskCardConstraintLayout.post(new Runnable() {
            @Override
            public void run() {
                updateMinutesNotesView(task, holder);
            }
        });
        holder.taskCardConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.animateTaskCardOptionsLayout(holder.expandedOptionsLayout, task.getGroup(), holder.sideCardAccent, 1);
            }
        });
    }

    private void updateMinutesNotesView(Task task, ViewHolder holder) {
        List<LocalDateTime> timestamps = task.getMinutesNotesTimestamps();
        if(timestamps.isEmpty()) {
            holder.taskNotes.setLines(2);
            holder.taskNotes.setText(Html.fromHtml("<b>Minutes:</b><br>Nothing here!"));
        } else {
            StringBuilder minutesNotes = new StringBuilder();

            int lines = 1;
//            for(int i = timestamps.size() - 1; i >= (timestamps.size() >= 3 ? timestamps.size() - 3 : 0); i--) {
            for(int i = timestamps.size() - 1; i >= 0; i--) {
                LocalDateTime dateTime = timestamps.get(i);
                int minutes = task.getMinutesFromTimestamp(dateTime);
                String notes = task.getNotesFromTimestamp(dateTime);

                if(notes.isEmpty()) {
                    minutesNotes.append("<b>" + dateTime.getMonthValue() + "/" + dateTime.getDayOfMonth() + " @ " + Utils.formatLocalTime(dateTime) + ", worked " + minutes + " min.</b> <br>");
                    lines += 1;
                } else {
                    minutesNotes.append("<b>" + dateTime.getMonthValue() + "/" + dateTime.getDayOfMonth() + " @ " + Utils.formatLocalTime(dateTime) + ", worked " + minutes + " min:</b> <br>");
                    minutesNotes.append("<font color='lightgray'>" + notes + "</font><br>");

                    Paint paint = new Paint();
                    paint.setTextSize(holder.taskNotes.getTextSize());
                    Rect bounds = new Rect();
                    paint.getTextBounds(notes, 0, notes.length(), bounds);

                    lines += 1 + Math.ceil((double) bounds.width() / holder.taskDetails.getWidth());
                }
            }

            holder.taskNotes.setText(Html.fromHtml("<b>Minutes (" + timestamps.size() + "):</b><br>" + minutesNotes.toString()));
            holder.taskNotes.setLines(lines);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
