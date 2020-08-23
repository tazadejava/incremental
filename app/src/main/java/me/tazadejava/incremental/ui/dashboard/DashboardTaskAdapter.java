package me.tazadejava.incremental.ui.dashboard;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.ui.main.Utils;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

public class DashboardTaskAdapter extends RecyclerView.Adapter<DashboardTaskAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ConstraintLayout taskCardConstraintLayout;
        private TextView taskName, taskClass, taskSubtasksLeft, taskHoursRemaining, taskDueDate, actionTaskText;

        private View horizontalLine;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskName = itemView.findViewById(R.id.taskGroupName);
            taskClass = itemView.findViewById(R.id.task_class);
            taskSubtasksLeft = itemView.findViewById(R.id.task_subtasks_left);
            taskHoursRemaining = itemView.findViewById(R.id.task_hours_remaining);
            taskDueDate = itemView.findViewById(R.id.task_due_date);

            actionTaskText = itemView.findViewById(R.id.actionTaskText);

            horizontalLine = itemView.findViewById(R.id.horizontalLine);
        }
    }

    private Context context;
    private LocalDate date;
    private List<Task> tasks;
    private DashboardAdapter dashboardAdapter;

    private HashMap<Task, ConstraintLayout> taskLayout;

    public DashboardTaskAdapter(Context context, LocalDate date, List<Task> tasks, DashboardAdapter dashboardAdapter) {
        this.context = context;
        this.date = date;
        this.tasks = tasks;
        this.dashboardAdapter = dashboardAdapter;

        taskLayout = new HashMap<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Task task = tasks.get(position);

        updateTaskColor(task, holder.taskCardConstraintLayout);

        holder.taskCardConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                IncrementalApplication.taskManager.setActiveEditTask(task);

                Intent editTask = new Intent(context, CreateTaskActivity.class);
                context.startActivity(editTask);
                return true;
            }
        });

        taskLayout.put(task, holder.taskCardConstraintLayout);

        holder.taskName.setText(task.getName());
        holder.taskClass.setText(task.getGroupName());

        holder.taskHoursRemaining.setText("");

        float hoursLeft = task.getTotalHoursLeftOfWork();
        String hoursLeftFormatted = String.valueOf((hoursLeft == (int) hoursLeft) ? (int) hoursLeft : hoursLeft);

<<<<<<< Updated upstream:app/src/main/java/me/tazadejava/incremental/ui/dashboard/DashboardTaskAdapter.java
        float hoursLeftToday = task.getTodaysHoursOfWork();
        String hoursTodayFormatted = String.valueOf((hoursLeftToday == (int) hoursLeftToday) ? (int) hoursLeftToday : hoursLeftToday);

        holder.taskSubtasksLeft.setText("est. " + hoursLeftFormatted + " hour" + (task.getEstimatedCompletionTime() == 1 ? "" : "s") + " remaining" +
                "\n" + hoursTodayFormatted + " hour" + (task.getTodaysHoursOfWork() == 1 ? "" : "s") + " of work");
=======
        if(date.equals(LocalDate.now())) {
            float hoursLeftToday = task.getTodaysHoursLeft();

            if(hoursLeftToday < 0) {
                hoursLeftToday = 0;
            }

            String hoursTodayFormatted = hoursLeftToday % 1 == 0 ? String.valueOf((int) hoursLeftToday) : String.valueOf(hoursLeftToday);
            holder.dailyHoursRemaining.setText(hoursTodayFormatted + " hour" + (hoursLeftToday == 1 ? "" : "s") + " of work today");
        } else {
            float hoursLeft = task.getDayHoursOfWorkTotal(date);

            if(hoursLeft < 0) {
                hoursLeft = 0;
            }

            String hoursLeftThisDay = hoursLeft % 1 == 0 ? String.valueOf((int) hoursLeft) : String.valueOf(hoursLeft);
            holder.dailyHoursRemaining.setText(hoursLeftThisDay + " hour" + (hoursLeft == 1 ? "" : "s") + " of work");
        }

        if(task.isOverdue()) {
            int overdueDays = task.getOverdueDays();
            holder.dailyHoursRemaining.setText(holder.dailyHoursRemaining.getText() + "\nOVERDUE BY " + overdueDays + " DAY" + (overdueDays == 1 ? "" : "S"));
            holder.actionTaskText.setTextColor(Color.RED);
        } else {
            holder.actionTaskText.setTextColor(holder.actionTaskText.getTextColors());
        }

        holder.totalHoursRemaining.setText(hoursLeftFormatted + " total hour" + (totalHoursLeft == 1 ? "" : "s") + " remaining");
>>>>>>> Stashed changes:app/src/main/java/me/tazadejava/incremental/ui/dashboard/TaskAdapter.java

        LocalDateTime dueDateTime = task.getDueDateTime();
        if(dueDateTime.toLocalDate().equals(LocalDate.now())) {
            holder.taskDueDate.setText("Due TODAY" + " @ " + Utils.formatLocalTime(dueDateTime.getHour(), dueDateTime.getMinute()));
        } else {
            holder.taskDueDate.setText("Due on " + dueDateTime.getMonthValue() + "/" + dueDateTime.getDayOfMonth() + " @ " + Utils.formatLocalTime(dueDateTime.getHour(), dueDateTime.getMinute()));
        }

        if(date.equals(LocalDate.now())) {
            holder.actionTaskText.setVisibility(View.VISIBLE);
            holder.horizontalLine.setVisibility(View.VISIBLE);
            if(task.isTaskCurrentlyWorkedOn()) {
                holder.actionTaskText.setText("Log Hours");

                holder.actionTaskText.setOnClickListener(getActionTaskListener(task, holder.actionTaskText, holder.taskCardConstraintLayout, true));
            } else {
                holder.actionTaskText.setText("Start Task");

                holder.actionTaskText.setOnClickListener(getActionTaskListener(task, holder.actionTaskText, holder.taskCardConstraintLayout, false));
            }
        } else {
            holder.actionTaskText.setVisibility(View.GONE);
            holder.horizontalLine.setVisibility(View.GONE);
        }
    }

    protected void updateTaskColor(Task task) {
        if(taskLayout.containsKey(task)) {
            updateTaskColor(task, taskLayout.get(task));
        }
    }

    private void updateTaskColor(Task task, ConstraintLayout taskCardConstraintLayout) {
        taskCardConstraintLayout.post(new Runnable() {
            @Override
            public void run() {
                LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(context, R.drawable.task_card_gradient).mutate();

                GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
                GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);

                darkColor.setColor(task.getGroup().getBeginColor());
                lightColor.setColor(task.getGroup().getEndColor());

<<<<<<< Updated upstream:app/src/main/java/me/tazadejava/incremental/ui/dashboard/DashboardTaskAdapter.java
                unwrapped.setLayerSize(1, (int) (taskCardConstraintLayout.getWidth() * task.getTaskCompletionPercentage()), unwrapped.getLayerHeight(1));
                taskCardConstraintLayout.setBackground(unwrapped);
=======
                if(date.equals(LocalDate.now())) {
                    unwrapped.setLayerSize(1, (int) (viewHolder.taskCardConstraintLayout.getWidth() * task.getTodaysTaskCompletionPercentage()), unwrapped.getLayerHeight(1));
                } else {
                    unwrapped.setLayerSize(1, (int) (viewHolder.taskCardConstraintLayout.getWidth() * task.getTaskCompletionPercentage()), unwrapped.getLayerHeight(1));
                }

                viewHolder.taskCardConstraintLayout.setBackground(unwrapped);
>>>>>>> Stashed changes:app/src/main/java/me/tazadejava/incremental/ui/dashboard/TaskAdapter.java
            }
        });
    }

    private View.OnClickListener getActionTaskListener(Task task, TextView taskText, ConstraintLayout taskCardConstraintLayout, boolean hasTaskStarted) {
        if(hasTaskStarted) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle("For how many hours did you work on this task?");

                    EditText input = new EditText(v.getContext());
                    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

                    String estimatedHours;
                    float hours = task.getCurrentWorkedHours();

                    if(hours == (int) hours) {
                        estimatedHours = "" + (int) hours;
                    } else {
                        estimatedHours = "" + hours;
                    }

                    input.setText(estimatedHours);
                    input.setSelectAllOnFocus(true);
                    builder.setView(input);

                    builder.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AlertDialog.Builder finishedTaskBuilder = new AlertDialog.Builder(v.getContext());
                            finishedTaskBuilder.setTitle("Did you finish the task?");

                            float hoursWorked = Float.parseFloat(input.getText().toString());

                            finishedTaskBuilder.setPositiveButton("Finished for today!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    task.incrementTaskHours(hoursWorked, false);
                                    taskText.setText("Start Task");
                                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, false));

                                    task.completeTaskForTheDay();

                                    dashboardAdapter.updateTaskColors(task);
                                    updateLayout(task);

                                    hideKeyboard(v);
                                }
                            });

                            finishedTaskBuilder.setNeutralButton("Finished the task!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    task.incrementTaskHours(hoursWorked, true);
                                    taskText.setText("Start Task");
                                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, false));

                                    dashboardAdapter.updateTaskColors(task);
                                    updateLayout(task);

                                    hideKeyboard(v);
                                }
                            });

                            finishedTaskBuilder.setNegativeButton("Not done yet!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    task.incrementTaskHours(hoursWorked, false);
                                    taskText.setText("Start Task");
                                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, false));

                                    dashboardAdapter.updateTaskColors(task);

                                    hideKeyboard(v);
                                }
                            });

                            finishedTaskBuilder.show();
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    builder.show();
                    input.requestFocus();

                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            };
        } else {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    task.startWorkingOnTask();

                    taskText.setText("Log Hours");
                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, true));
                }
            };
        }
    }

    private void hideKeyboard(View v) {
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }, 50);
    }

    private void updateLayout(Task task) {
        dashboardAdapter.updateDayLayouts(task);
    }

    public boolean hasTask(Task task) {
        return taskLayout.containsKey(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
