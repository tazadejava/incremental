package me.tazadejava.incremental.ui.dashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.text.Html;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
                secondaryActionTaskText, taskNotes, thirdActionTaskText;

        private View sideCardAccent, horizontalLine;

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

            taskNotes = itemView.findViewById(R.id.taskNotes);

            horizontalLine = itemView.findViewById(R.id.horizontalLine);
            sideCardAccent = itemView.findViewById(R.id.sideCardAccent);
        }
    }

    private TaskManager taskManager;
    private Activity context;

    private LocalDate date;
    private Set<Task> tasksToday;
    private List<Task> tasks;
    private MainDashboardDayAdapter mainDashboardDayAdapter;

    private TimePeriod timePeriod;
    private int dayPosition;

    private HashMap<Task, ViewHolder> taskLayout;

    private boolean animateCardChanges = true;

    public TaskAdapter(TaskManager taskManager, Activity context, TimePeriod timePeriod, int dayPosition, LocalDate date, Set<Task> tasksToday, List<Task> tasks, MainDashboardDayAdapter mainDashboardDayAdapter) {
        this.taskManager = taskManager;
        this.context = context;
        this.date = date;
        this.tasksToday = tasksToday;
        this.tasks = new ArrayList<>(tasks);
        this.mainDashboardDayAdapter = mainDashboardDayAdapter;

        this.timePeriod = timePeriod;
        this.dayPosition = dayPosition;

        taskLayout = new HashMap<>();

        //sort if active
        this.tasks.sort(new Comparator<Task>() {
            @Override
            public int compare(Task task, Task t1) {
                if(task.isTaskCurrentlyWorkedOn()) {
                    return -1;
                } else if(t1.isTaskCurrentlyWorkedOn()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
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

        //update card color if active
        if(task.isTaskCurrentlyWorkedOn()) {
            holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColorActive));
        } else {
            holder.taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
        }

        holder.secondaryActionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                taskManager.setActiveEditTask(task);

                Intent editTask = new Intent(context, CreateTaskActivity.class);
                context.startActivity(editTask);
            }
        });

        holder.thirdActionTaskText.setOnClickListener(getDelayTaskClickListener(task, position, holder.actionTaskText, holder.taskCardConstraintLayout, holder.actionTaskText, holder.expandedOptionsLayout));
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
                Utils.animateTaskCardOptionsLayout(holder.expandedOptionsLayout, task.getGroup(), holder.sideCardAccent);
            }
        });

        holder.dailyTimeRemaining.post(new Runnable() {
            @Override
            public void run() {
                updateMinutesNotesView(task, holder);
            }
        });

        //todo: temp disabled because unsure if the design benefits from the addition of a star
//        if(task.getMinutesNotesTimestamps().isEmpty()) {
//            holder.bottomLeftIndicator.setText("");
//        } else {
//            holder.bottomLeftIndicator.setText("*");
//        }

        taskLayout.put(task, holder);

        holder.taskName.setText(task.getName());
        holder.taskClass.setText(task.getGroupName());

        holder.taskClass.setTextColor(task.getGroup().getLightColor());

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
                holder.actionTaskText.setText("Log\nWork");

                holder.actionTaskText.setOnClickListener(getActionTaskListener(task, position, holder.actionTaskText, holder.taskCardConstraintLayout, holder.actionTaskText, holder.expandedOptionsLayout, true));
            } else {
                if(dayPosition == 0) {
                    holder.actionTaskText.setText("Start\nTask");
                } else {
                    holder.actionTaskText.setText("Start\nTask\nEarly");
                }

                holder.actionTaskText.setOnClickListener(getActionTaskListener(task, position, holder.actionTaskText, holder.taskCardConstraintLayout, holder.actionTaskText, holder.expandedOptionsLayout, false));
            }
        } else {
            holder.actionTaskText.setVisibility(View.GONE);
            holder.horizontalLine.setVisibility(View.GONE);
        }
    }

    protected void updateTaskCardsAndAnimation(Task task) {
        if(taskLayout.containsKey(task)) {
            updateTaskCards(task, taskLayout.get(task), true);
        }
    }

    private void updateTaskCards(Task task, ViewHolder holder) {
        updateTaskCards(task, holder, false);
    }

    private void updateTaskCards(Task task, ViewHolder holder, boolean updateAnimation) {
        updateMinutesNotesView(task, holder, updateAnimation);

        LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(context, R.drawable.task_card_gradient).mutate();
        GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
        GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);
        lightColor.setColor(task.getGroup().getDarkColor());
        darkColor.setColor(task.getGroup().getLightColor());

        holder.sideCardAccent.setBackground(lightColor);

        //the width needs to be set, so we have to wait until the constraint layout is set
        holder.sideCardAccent.post(new Runnable() {
            @Override
            public void run() {
                double completionPercentage = date.equals(LocalDate.now()) ? task.getTodaysTaskCompletionPercentage() : task.getTaskCompletionPercentage();
                unwrapped.setLayerSize(1, unwrapped.getLayerWidth(1), (int) ((double) holder.sideCardAccent.getHeight() * completionPercentage));

                if(animateCardChanges) {
                    TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{lightColor, unwrapped});
                    holder.sideCardAccent.setBackground(transitionDrawable);

                    transitionDrawable.startTransition(300);
                } else {
                    holder.sideCardAccent.setBackground(unwrapped);
                }
            }
        });
    }

    private View.OnClickListener getDelayTaskClickListener(Task task, int position, TextView taskText, ConstraintLayout taskCardConstraintLayout, TextView actionTaskText, ConstraintLayout expandedOptionsLayout) {
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
                                task.incrementTaskMinutes(0, "", false);
                                task.completeTaskForTheDay();
                                mainDashboardDayAdapter.updateTaskCards(task);
                                mainDashboardDayAdapter.updateDayLayouts(task);

                                notifyDataSetChanged();

                                mainView.setTranslationX(0);
                            }
                        }).start();

                        if(dayPosition == 0) {
                            taskText.setText("Start\nTask");
                        } else {
                            taskText.setText("Start\nTask\nEarly");
                        }
                        taskText.setOnClickListener(getActionTaskListener(task, position, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, false));
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

    private void updateMinutesNotesView(Task task, ViewHolder holder) {
        updateMinutesNotesView(task, holder, false);
    }

    private void updateMinutesNotesView(Task task, ViewHolder holder, boolean updateCardTextAnimation) {
        List<LocalDateTime> timestamps = task.getMinutesNotesTimestamps();
        if(timestamps.isEmpty()) {
            holder.taskNotes.setLines(2);
            holder.taskNotes.setText(Html.fromHtml("<b>Minutes:</b><br>Nothing here yet!"));
        } else {
            StringBuilder minutesNotes = new StringBuilder();

            int lines = 1;
            for(int i = timestamps.size() - 1; i >= (timestamps.size() >= 3 ? timestamps.size() - 3 : 0); i--) {
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

                    lines += 1 + Math.ceil((double) bounds.width() / holder.dailyTimeRemaining.getWidth());
                }
            }

            holder.taskNotes.setText(Html.fromHtml("<b>Minutes (" + timestamps.size() + "):</b><br>" + minutesNotes.toString()));
            holder.taskNotes.setLines(lines);

            if(updateCardTextAnimation && holder.expandedOptionsLayout.getVisibility() == View.VISIBLE) {
                Utils.animateTaskCardOptionsLayout(holder.expandedOptionsLayout, true, task.getGroup(), holder.sideCardAccent);
            }
        }
    }

    private View.OnClickListener getActionTaskListener(Task task, int position, TextView taskText, ConstraintLayout taskCardConstraintLayout, TextView actionTaskText, ConstraintLayout expandedOptionsLayout, boolean hasTaskStarted) {
        if(hasTaskStarted) {
            return new View.OnClickListener() {
                @SuppressLint("ResourceType")
                @Override
                public void onClick(View v) {
                    Utils.vibrate(context, 20);

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle("For how many minutes did you work on this task?");
                    if(task.hasCarryoverSeconds()) {
                        Toast.makeText(context, "An extra minute has been added to make up for leftover seconds from previous logs.", Toast.LENGTH_LONG).show();
                    }

                    RelativeLayout inputLayout = new RelativeLayout(v.getContext());

                    EditText inputMinutes = new EditText(v.getContext());
                    inputMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
                    final LocalDateTime estimateTimestamp = task.getLastTaskWorkStartTime();
                    final String currentMinutes = String.valueOf(task.getCurrentWorkedMinutesWithCarryover());
                    inputMinutes.setText(currentMinutes);
                    inputMinutes.setSelectAllOnFocus(true);

                    EditText inputNotes = new EditText(v.getContext());
                    inputNotes.setHint("Optional: accomplishments, goals, etc.");
                    inputNotes.setMaxLines(1);
                    inputNotes.setInputType(InputType.TYPE_CLASS_TEXT);
                    inputNotes.setSelectAllOnFocus(true);

                    inputMinutes.setId(1);
                    RelativeLayout.LayoutParams relativeParamsMinutes = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    RelativeLayout.LayoutParams relativeParamsNotes = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    relativeParamsNotes.addRule(RelativeLayout.BELOW, 1);

                    inputLayout.addView(inputMinutes, relativeParamsMinutes);
                    inputLayout.addView(inputNotes, relativeParamsNotes);

                    builder.setView(inputLayout);

                    builder.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AlertDialog.Builder finishedTaskBuilder = new AlertDialog.Builder(v.getContext());
                            finishedTaskBuilder.setCancelable(false);
                            finishedTaskBuilder.setTitle("Did you finish the task?");

                            boolean usedEstimatedTime = currentMinutes.equals(inputMinutes.getText().toString());

                            int minutesWorked;
                            if(inputMinutes.getText().length() == 0) {
                                minutesWorked = 0;
                            } else {
                                minutesWorked = Integer.parseInt(inputMinutes.getText().toString());
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
                                                task.incrementTaskMinutes(minutesWorked, inputNotes.getText().toString(), false, usedEstimatedTime, estimateTimestamp);
                                                mainDashboardDayAdapter.updateTaskCards(task);
                                                mainDashboardDayAdapter.updateDayLayouts(task);

                                                notifyDataSetChanged();

                                                mainView.setTranslationX(0);
                                            }
                                        }).start();

                                        taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                                        if(dayPosition == 0) {
                                            taskText.setText("Start\nTask");
                                        } else {
                                            taskText.setText("Start\nTask\nEarly");
                                        }
                                        taskText.setOnClickListener(getActionTaskListener(task, position, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, false));

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
                                            task.incrementTaskMinutes(minutesWorked, inputNotes.getText().toString(), true, usedEstimatedTime, estimateTimestamp);
                                            mainDashboardDayAdapter.updateTaskCards(task);
                                            mainDashboardDayAdapter.updateDayLayouts(task);

                                            notifyDataSetChanged();

                                            mainView.setTranslationX(0);
                                        }
                                    }).start();

                                    taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                                    if(dayPosition == 0) {
                                        taskText.setText("Start\nTask");
                                    } else {
                                        taskText.setText("Start\nTask\nEarly");
                                    }
                                    taskText.setOnClickListener(getActionTaskListener(task, position, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, false));

                                    hideKeyboard(taskCardConstraintLayout);
                                }
                            });

                            finishedTaskBuilder.setNegativeButton("Not done yet!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    task.incrementTaskMinutes(minutesWorked, inputNotes.getText().toString(), false, usedEstimatedTime, estimateTimestamp);

                                    taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                                    if(dayPosition == 0) {
                                        taskText.setText("Start\nTask");
                                    } else {
                                        taskText.setText("Start\nTask\nEarly");
                                    }
                                    taskText.setOnClickListener(getActionTaskListener(task, position, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, false));

                                    hideKeyboard(actionTaskText);

                                    //delayed so that the keyboard can go away first
                                    v.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainDashboardDayAdapter.updateTaskCards(task);
                                            mainDashboardDayAdapter.updateDayLayouts(task);
                                        }
                                    }, 75);
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
                    inputMinutes.requestFocus();

                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            };
        } else {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.vibrate(context, 30);

                    task.startWorkingOnTask();

                    taskText.setText("Log\nWork");
                    taskText.setOnClickListener(getActionTaskListener(task, position, taskText, taskCardConstraintLayout, actionTaskText, expandedOptionsLayout, true));

                    task.getParent().saveTaskToFile();

                    taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColorActive));

                    //move to top
                    tasks.remove(task);
                    tasks.add(0, task);
                    notifyItemMoved(position, 0);
                    mainDashboardDayAdapter.smoothScrollToPosition(dayPosition);
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
