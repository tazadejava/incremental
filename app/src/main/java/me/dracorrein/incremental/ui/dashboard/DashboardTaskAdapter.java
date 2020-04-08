package me.dracorrein.incremental.ui.dashboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import me.dracorrein.incremental.R;
import me.dracorrein.incremental.logic.dashboard.Day;
import me.dracorrein.incremental.logic.dashboard.Task;
import me.dracorrein.incremental.ui.create_task.CreateTaskActivity;

public class DashboardTaskAdapter extends RecyclerView.Adapter<DashboardTaskAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ConstraintLayout taskCardConstraintLayout;
        private TextView taskName, taskClass, taskSubtasksLeft, taskHoursRemaining, taskDueDate, actionTaskText;

        private View horizontalLine;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskName = itemView.findViewById(R.id.task_name);
            taskClass = itemView.findViewById(R.id.task_class);
            taskSubtasksLeft = itemView.findViewById(R.id.task_subtasks_left);
            taskHoursRemaining = itemView.findViewById(R.id.task_hours_remaining);
            taskDueDate = itemView.findViewById(R.id.task_due_date);

            actionTaskText = itemView.findViewById(R.id.actionTaskText);

            horizontalLine = itemView.findViewById(R.id.horizontalLine);
        }
    }

    private Context context;
    private Day day;

    public DashboardTaskAdapter(Context context, Day day) {
        this.context = context;
        this.day = day;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Task task = day.getTasks().get(position);

        updateTaskColor(task, holder.taskCardConstraintLayout);

        holder.taskName.setText(task.getName());
        holder.taskClass.setText(task.getClassName());

        holder.taskSubtasksLeft.setText("est. " + task.getEstimatedCompletionTimeFormatted() + " hour" + (task.getEstimatedCompletionTime() == 1 ? "" : "s") + " remaining - do " + (task.getDailyHoursOfWork()) + " hours today!");
        holder.taskHoursRemaining.setText("");

        holder.taskDueDate.setText(task.getDueDateFormatted());

        if(day.isToday()) {
            holder.actionTaskText.setVisibility(View.VISIBLE);
            holder.horizontalLine.setVisibility(View.VISIBLE);
            if(task.isTaskInProgress()) {
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

    private void updateTaskColor(Task task, ConstraintLayout taskCardConstraintLayout) {
        taskCardConstraintLayout.post(new Runnable() {
            @Override
            public void run() {
                LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(context, R.drawable.task_card_gradient).mutate();

                GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
                GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);

                darkColor.setColor(task.getCardBeginColor());
                lightColor.setColor(task.getCardEndColor());

                unwrapped.setLayerSize(1, (int) (taskCardConstraintLayout.getWidth() * task.getTaskCompletionPercentage()), unwrapped.getLayerHeight(1));
                taskCardConstraintLayout.setBackground(unwrapped);
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
                    float hours = task.getCurrentTaskWorkHours();

                    if(hours == (int) hours) {
                        estimatedHours = "" + (int) hours;
                    } else {
                        estimatedHours = "" + hours;
                    }

                    input.setText(estimatedHours);
                    builder.setView(input);

                    builder.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AlertDialog.Builder finishedTaskBuilder = new AlertDialog.Builder(v.getContext());
                            finishedTaskBuilder.setTitle("Did you finish the task?");

                            finishedTaskBuilder.setPositiveButton("Finished for today!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    task.incrementTask(Float.parseFloat(input.getText().toString()));
                                    taskText.setText("Start Task");
                                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, false));

                                    task.completeTaskForTheDay();

                                    day.completeTask(task);
                                    updateTaskColor(task, taskCardConstraintLayout);
                                    updateLayout();
                                }
                            });

                            finishedTaskBuilder.setNeutralButton("Finished the task!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    task.completeTask(Float.parseFloat(input.getText().toString()));
                                    taskText.setText("Start Task");
                                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, false));

                                    day.completeTask(task);
                                    updateTaskColor(task, taskCardConstraintLayout);
                                    updateLayout();
                                }
                            });

                            finishedTaskBuilder.setNegativeButton("Not done yet!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    task.incrementTask(Float.parseFloat(input.getText().toString()));
                                    taskText.setText("Start Task");
                                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, false));

                                    updateTaskColor(task, taskCardConstraintLayout);
                                }
                            });

                            finishedTaskBuilder.show();
                            InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
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
                    task.startTask();

                    taskText.setText("Log Hours");
                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, true));
                }
            };
        }
    }

    private void updateLayout() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return day.getTasks().size();
    }
}
