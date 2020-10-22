package me.tazadejava.incremental.ui.dashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.InputType;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ConstraintLayout taskCardConstraintLayout, expandedOptionsLayout;
        private TextView taskName, taskClass, totalTimeRemaining, dailyTimeRemaining, taskDueDate, actionTaskText,
                secondaryActionTaskText, taskNotes, thirdActionTaskText, bottomLeftIndicator;

        private View horizontalLine;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);
            expandedOptionsLayout = itemView.findViewById(R.id.expandedOptionsLayout);

            taskName = itemView.findViewById(R.id.timePeriodName);
            taskClass = itemView.findViewById(R.id.task_class);
            totalTimeRemaining = itemView.findViewById(R.id.estimatedTotalTimeLeft);
            dailyTimeRemaining = itemView.findViewById(R.id.estimatedDailyTime);
            taskDueDate = itemView.findViewById(R.id.task_due_date);

            actionTaskText = itemView.findViewById(R.id.actionTaskText);
            secondaryActionTaskText = itemView.findViewById(R.id.secondaryActionTaskText);
            thirdActionTaskText = itemView.findViewById(R.id.thirdActionTaskText);

            bottomLeftIndicator = itemView.findViewById(R.id.bottomLeftIndicator);

            taskNotes = itemView.findViewById(R.id.taskNotes);

            horizontalLine = itemView.findViewById(R.id.horizontalLine);
        }
    }

    private TaskManager taskManager;
    private Activity context;

    private LocalDate date;
    private Set<Task> tasksToday;
    private List<Task> tasks;
    private MainDashboardAdapter mainDashboardAdapter;

    private TimePeriod timePeriod;
    private int dayPosition;

    private HashMap<Task, ViewHolder> taskLayout;

    private boolean animateCardChanges = true;

    public TaskAdapter(TaskManager taskManager, Activity context, TimePeriod timePeriod, int dayPosition, LocalDate date, Set<Task> tasksToday, List<Task> tasks, MainDashboardAdapter mainDashboardAdapter) {
        this.taskManager = taskManager;
        this.context = context;
        this.date = date;
        this.tasksToday = tasksToday;
        this.tasks = tasks;
        this.mainDashboardAdapter = mainDashboardAdapter;

        this.timePeriod = timePeriod;
        this.dayPosition = dayPosition;

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

        updateTaskCards(task, holder);

        holder.secondaryActionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskManager.setActiveEditTask(task);

                Intent editTask = new Intent(context, CreateTaskActivity.class);
                context.startActivity(editTask);
            }
        });

        holder.thirdActionTaskText.setOnClickListener(getDelayTaskClickListener(task, holder.actionTaskText, holder.taskCardConstraintLayout, holder.actionTaskText, holder.expandedOptionsLayout));
        holder.thirdActionTaskText.setVisibility((dayPosition != 0 || task.getDueDateTime().toLocalDate().equals(LocalDate.now()) || task.isOverdue()) ?
                View.GONE : View.VISIBLE);

        holder.taskCardConstraintLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                taskManager.setActiveEditTask(task);

                Intent editTask = new Intent(context, CreateTaskActivity.class);
                context.startActivity(editTask);
                return true;
            }
        });


        holder.taskCardConstraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TransitionManager.beginDelayedTransition(holder.expandedOptionsLayout);
                if(holder.expandedOptionsLayout.getVisibility() == View.VISIBLE) {
                    holder.expandedOptionsLayout.setVisibility(View.GONE);
                } else {
                    holder.expandedOptionsLayout.setVisibility(View.VISIBLE);
                    //TODO: how to expand animation
//                    holder.expandedOptionsLayout.setLayoutParams(new ConstraintLayout.LayoutParams(0, 0));
//                    for(int i = 0; i < 5; i++) {
//                        int finalI = i;
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                holder.expandedOptionsLayout.setMaxHeight((int) (maxHeight / 5d * (1 + finalI)));
//                            }
//                        }, i * 200);
//                    }
                }
            }
        });

        updateNotesView(task, holder);
        holder.taskNotes.setOnClickListener(getAddTaskNotesListener(task, holder));

        if(task.getTaskNotes() == null || task.getTaskNotes().isEmpty()) {
            holder.bottomLeftIndicator.setText("");
        } else {
            holder.bottomLeftIndicator.setText("*");
        }

        taskLayout.put(task, holder);

        holder.taskName.setText(task.getName());
        holder.taskClass.setText(task.getGroupName());

        int totalMinutesLeft = task.getTotalMinutesLeftOfWork();
        holder.totalTimeRemaining.setText(Utils.formatHourMinuteTime(totalMinutesLeft) + " of total work remaining");

        if(date.equals(LocalDate.now())) {
            int minutesLeftToday = task.getTodaysMinutesLeftIncludingCurrentWork();

            holder.dailyTimeRemaining.setText(Utils.formatHourMinuteTime(minutesLeftToday) + " of work left today");
        } else {
            int minutesLeft = task.getDayMinutesOfWorkTotal(date);

            if(minutesLeft < 0) {
                minutesLeft = 0;
            }

            holder.dailyTimeRemaining.setText(Utils.formatHourMinuteTime(minutesLeft) + " of work left");
        }

        if(task.isOverdue()) {
            int overdueDays = task.getOverdueDays();
            if(overdueDays > 0) {
                holder.dailyTimeRemaining.setText(holder.dailyTimeRemaining.getText() + "\nOVERDUE BY " + overdueDays + " DAY" + (overdueDays == 1 ? "" : "S"));
            } else {
                long overdueHours = ChronoUnit.HOURS.between(task.getDueDateTime(), LocalDateTime.now());
                if(overdueHours < 1) {
                    long overdueMin = ChronoUnit.MINUTES.between(task.getDueDateTime(), LocalDateTime.now());
                    holder.dailyTimeRemaining.setText(holder.dailyTimeRemaining.getText() + "\nOVERDUE BY " + overdueMin + " minute" + (overdueMin == 1 ? "" : "s"));
                } else {
                    holder.dailyTimeRemaining.setText(holder.dailyTimeRemaining.getText() + "\nOVERDUE BY " + overdueHours + " hour" + (overdueHours == 1 ? "" : "s"));
                }
            }
            holder.actionTaskText.setTextColor(ContextCompat.getColor(context, R.color.darkRed));
        } else {
            holder.actionTaskText.setTextColor(holder.actionTaskText.getTextColors());
        }

        LocalDateTime dueDateTime = task.getDueDateTime();
        if(dueDateTime.toLocalDate().equals(LocalDate.now())) {
            holder.taskDueDate.setText("Due TODAY" + " @ " + Utils.formatLocalTime(dueDateTime));
        } else {
            holder.taskDueDate.setText("Due " + dueDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US) + ", " + dueDateTime.getMonthValue() + "/" + dueDateTime.getDayOfMonth() + " @ " + Utils.formatLocalTime(dueDateTime));
        }

        boolean containsTaskAndIsNotDoneToday = false;

        for(Task taskToday : tasksToday) {
            if(taskToday.equals(task)) {
                if(!task.isDoneWithTaskToday()) {
                    containsTaskAndIsNotDoneToday = true;
                }
                break;
            }
        }

        if(dayPosition == 0 || !containsTaskAndIsNotDoneToday) {
            holder.actionTaskText.setVisibility(View.VISIBLE);
            holder.horizontalLine.setVisibility(View.VISIBLE);
            if (task.isTaskCurrentlyWorkedOn()) {
                holder.actionTaskText.setText("Log Work");

                holder.actionTaskText.setOnClickListener(getActionTaskListener(task, holder.actionTaskText, holder.taskCardConstraintLayout, holder.actionTaskText, holder.expandedOptionsLayout, true));
            } else {
                if(dayPosition == 0) {
                    holder.actionTaskText.setText("Start Task");
                } else {
                    holder.actionTaskText.setText("Start Task Early");
                }

                holder.actionTaskText.setOnClickListener(getActionTaskListener(task, holder.actionTaskText, holder.taskCardConstraintLayout, holder.actionTaskText, holder.expandedOptionsLayout, false));
            }
        } else {
            holder.actionTaskText.setVisibility(View.GONE);
            holder.horizontalLine.setVisibility(View.GONE);
        }
    }

    protected void updateTaskCards(Task task) {
        if(taskLayout.containsKey(task)) {
            updateTaskCards(task, taskLayout.get(task));
        }
    }

    private void updateTaskCards(Task task, ViewHolder holder) {
        updateNotesView(task, holder);

        LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(context, R.drawable.task_card_gradient).mutate();
        GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
        GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);
        darkColor.setColor(task.getGroup().getBeginColor());
        lightColor.setColor(task.getGroup().getEndColor());

        holder.taskCardConstraintLayout.setBackground(lightColor);

        //the width needs to be set, so we have to wait until the constraint layout is set
        holder.taskCardConstraintLayout.post(new Runnable() {
            @Override
            public void run() {
                unwrapped.setLayerSize(1, (int) ((double) holder.taskCardConstraintLayout.getWidth() *
                        (date.equals(LocalDate.now()) ? task.getTodaysTaskCompletionPercentage() : task.getTaskCompletionPercentage())),
                        unwrapped.getLayerHeight(1));

                if(animateCardChanges) {
                    TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{lightColor, unwrapped});
                    holder.taskCardConstraintLayout.setBackground(transitionDrawable);

                    transitionDrawable.startTransition(300);
                } else {
                    holder.taskCardConstraintLayout.setBackground(unwrapped);
                }
            }
        });
    }

    private View.OnClickListener getDelayTaskClickListener(Task task, TextView taskText, ConstraintLayout taskCardConstraintLayout, TextView actionTaskText, ConstraintLayout expandedOptionsLayout) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder finishedTaskBuilder = new AlertDialog.Builder(v.getContext());
                finishedTaskBuilder.setCancelable(false);
                finishedTaskBuilder.setTitle("Delay the task until tomorrow?");

                finishedTaskBuilder.setPositiveButton("Delay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
                        ConstraintLayout mainView = ((ConstraintLayout) taskCardConstraintLayout.getParent().getParent());
                        mainView.animate()
                                .translationXBy(width).setDuration(800).setInterpolator(new OvershootInterpolator()).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                task.startWorkingOnTask();
                                task.incrementTaskMinutes(0, false);
                                task.completeTaskForTheDay();
                                mainDashboardAdapter.updateTaskCards(task);
                                mainDashboardAdapter.updateDayLayouts(task);

                                notifyDataSetChanged();

                                mainView.setTranslationX(0);
                            }
                        }).start();

                        if(dayPosition == 0) {
                            taskText.setText("Start Task");
                        } else {
                            taskText.setText("Start Task Early");
                        }
                        taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, false));
                    }
                });

                finishedTaskBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

                finishedTaskBuilder.show();
            }
        };
    }

    private View.OnClickListener getAddTaskNotesListener(Task task, ViewHolder holder) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setTitle("Notes:");

                EditText input = new EditText(v.getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                input.setSelectAllOnFocus(false);
                builder.setView(input);

                if(task.getTaskNotes() != null) {
                    input.setText(task.getTaskNotes());
                }

                builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        task.setTaskNotes(input.getText().toString());
                        mainDashboardAdapter.updateTaskCards(task);

                        hideKeyboard(v);
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hideKeyboard(v);
                    }
                });

                builder.show();
                input.requestFocus();

                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        };
    }

    private void updateNotesView(Task task, ViewHolder holder) {
        if(task.getTaskNotes() == null) {
            holder.taskNotes.setText("Notes:\nTap to add notes");
        } else {
            holder.taskNotes.setText("Notes:\n" + task.getTaskNotes());
        }
    }

    private View.OnClickListener getActionTaskListener(Task task, TextView taskText, ConstraintLayout taskCardConstraintLayout, TextView actionTaskText, ConstraintLayout expandedOptionsLayout, boolean hasTaskStarted) {
        if(hasTaskStarted) {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.vibrate(context, 20);

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle("For how many minutes did you work on this task?");

                    EditText input = new EditText(v.getContext());
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);

                    input.setText(String.valueOf(task.getCurrentWorkedMinutes()));
                    input.setSelectAllOnFocus(true);
                    builder.setView(input);

                    builder.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AlertDialog.Builder finishedTaskBuilder = new AlertDialog.Builder(v.getContext());
                            finishedTaskBuilder.setCancelable(false);
                            finishedTaskBuilder.setTitle("Did you finish the task?");

                            int minutesWorked;
                            if(input.getText().length() == 0) {
                                minutesWorked = 0;
                            } else {
                                minutesWorked = Integer.parseInt(input.getText().toString());
                            }

                            if(LocalDate.now().isBefore(task.getDueDateTime().toLocalDate()) && dayPosition == 0) {
                                finishedTaskBuilder.setPositiveButton("Finished for today!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
                                        ConstraintLayout mainView = ((ConstraintLayout) taskCardConstraintLayout.getParent().getParent());

                                        //reset the task card to defaults
                                        actionTaskText.setTextColor(actionTaskText.getTextColors());
                                        expandedOptionsLayout.setVisibility(View.GONE);

                                        mainView.animate()
                                                .translationXBy(width).setDuration(800).setInterpolator(new OvershootInterpolator()).withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                task.completeTaskForTheDay();
                                                task.incrementTaskMinutes(minutesWorked, false);
                                                mainDashboardAdapter.updateTaskCards(task);
                                                mainDashboardAdapter.updateDayLayouts(task);

                                                notifyDataSetChanged();

                                                mainView.setTranslationX(0);
                                            }
                                        }).start();

                                        if(dayPosition == 0) {
                                            taskText.setText("Start Task");
                                        } else {
                                            taskText.setText("Start Task Early");
                                        }
                                        taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, false));

                                        hideKeyboard(taskText);
                                    }
                                });
                            }

                            finishedTaskBuilder.setNeutralButton("Finished the task!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    int width = Resources.getSystem().getDisplayMetrics().widthPixels;
                                    ConstraintLayout mainView = ((ConstraintLayout) taskCardConstraintLayout.getParent().getParent());

                                    //reset the task card to defaults
                                    actionTaskText.setTextColor(actionTaskText.getTextColors());
                                    expandedOptionsLayout.setVisibility(View.GONE);

                                    mainView.animate()
                                            .translationXBy(width).setStartDelay(200).setDuration(800).setInterpolator(new AnticipateOvershootInterpolator()).withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            task.incrementTaskMinutes(minutesWorked, true);
                                            mainDashboardAdapter.updateTaskCards(task);
                                            mainDashboardAdapter.updateDayLayouts(task);

                                            notifyDataSetChanged();

                                            mainView.setTranslationX(0);
                                        }
                                    }).start();

                                    if(dayPosition == 0) {
                                        taskText.setText("Start Task");
                                    } else {
                                        taskText.setText("Start Task Early");
                                    }
                                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, false));

                                    hideKeyboard(taskCardConstraintLayout);
                                }
                            });

                            finishedTaskBuilder.setNegativeButton("Not done yet!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    task.incrementTaskMinutes(minutesWorked, false);

                                    if(dayPosition == 0) {
                                        taskText.setText("Start Task");
                                    } else {
                                        taskText.setText("Start Task Early");
                                    }
                                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, false));
                                    mainDashboardAdapter.updateTaskCards(task);
                                    mainDashboardAdapter.updateDayLayouts(task);

                                    hideKeyboard(taskCardConstraintLayout);
                                }
                            });

                            finishedTaskBuilder.show();
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            hideKeyboard(taskText);
                        }
                    });

                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            hideKeyboard(taskText);
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
                    Utils.vibrate(context, 80);

                    task.startWorkingOnTask();

                    taskText.setText("Log Work");
                    taskText.setOnClickListener(getActionTaskListener(task, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, true));

                    task.getParent().saveTaskToFile();
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

    public void refreshLayout() {
        tasks = getUpdatedTasks();
        notifyDataSetChanged();
    }

    public List<Task> getUpdatedTasks() {
        return timePeriod.getTasksByDay(dayPosition);
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean hasTask(Task task) {
        return taskLayout.containsKey(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
