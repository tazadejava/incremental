package me.tazadejava.incremental.ui.dashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.NonrepeatingTask;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.Task;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.animation.TextColorAnimation;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
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
    private HashMap<Task, Integer> inactiveTaskPositions = new HashMap<>();

    private boolean updatePersistentNotification;
    private Task logTask;

    private String loggingTaskID; //task id to log when loaded, parsed from notification

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
        this.tasks.sort((task, task2) -> {
            if(task.isTaskCurrentlyWorkedOn()) {
                return -1;
            } else if(task2.isTaskCurrentlyWorkedOn()) {
                return 1;
            } else {
                return 0;
            }
        });

        if(!this.tasks.isEmpty() && this.tasks.get(0).isTaskCurrentlyWorkedOn()) {
            if (context.getIntent().hasExtra("logTask") && context.getIntent().getIntExtra("dayPosition", -1) == dayPosition) {
                if (context.getIntent().getStringExtra("logTask").equals(this.tasks.get(0).getName())) {
                    logTask = this.tasks.get(0);
                    context.getIntent().removeExtra("logTask");
                }
            }
        }

        //check for notification log
        if(context.getIntent().hasExtra("logTask")) {
            //todo: if not in frame, need to scroll to task first?
            loggingTaskID = context.getIntent().getStringExtra("logTask");
            context.getIntent().removeExtra("logTask");
            refreshLayout();
        }
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

        //set card to defaults before loading info
        holder.actionTaskText.setTextColor(Utils.getThemeAttrColor(context, R.attr.cardTextColor));
        holder.expandedOptionsLayout.setVisibility(View.GONE);

        //update info

        updateTaskCards(task, holder, dayPosition == 0);

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

        holder.thirdActionTaskText.setOnClickListener(getDelayTaskClickListener(task, position, holder));
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
                Utils.animateTaskCardOptionsLayout(holder.expandedOptionsLayout, task.getGroup(), holder.sideCardAccent, task.getTaskCompletionPercentage());
            }
        });

        holder.taskNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateMinutesNotesView(task, holder, false);
                Utils.animateTaskCardOptionsLayout(holder.expandedOptionsLayout, task.getGroup(), holder.sideCardAccent, task.getTaskCompletionPercentage(), true);
            }
        });

        holder.dailyTimeRemaining.post(new Runnable() {
            @Override
            public void run() {
                //run later to update
                updateMinutesNotesView(task, holder, true);
            }
        });

        taskLayout.put(task, holder);

        holder.taskName.setText(task.getName());
        holder.taskClass.setText(task.getGroupName());

        holder.taskClass.setTextColor(task.getGroup().getLightColor());

        updateTaskMinutesText(task, holder);

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

        boolean containsTaskAndIsNotDoneToday = tasksToday.contains(task) && !task.isDoneWithTaskToday();
        if(dayPosition == 0 || !containsTaskAndIsNotDoneToday) {
            holder.actionTaskText.setVisibility(View.VISIBLE);
            holder.horizontalLine.setVisibility(View.VISIBLE);
            if (task.isTaskCurrentlyWorkedOn()) {
                holder.actionTaskText.setText("Log\nWork");

                holder.actionTaskText.setOnClickListener(getActionTaskListener(task, true, holder));

                //send notification if in progress
                sendOngoingTaskNotification(task, true);
            } else {
                if(dayPosition == 0) {
                    holder.actionTaskText.setText("Start\nTask");
                } else {
                    holder.actionTaskText.setText("Start\nTask\nEarly");
                }

                holder.actionTaskText.setOnClickListener(getActionTaskListener(task, false, holder));
            }

            //pull up dialog when notification is clicked
            if(loggingTaskID != null && loggingTaskID.equals(task.getTaskID())) {
                loggingTaskID = null;
                holder.actionTaskText.post(new Runnable() {
                    @Override
                    public void run() {
                        holder.actionTaskText.callOnClick();
                    }
                });
            }
        } else {
            holder.actionTaskText.setVisibility(View.GONE);
            holder.horizontalLine.setVisibility(View.GONE);
        }


        if(logTask == task) {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    holder.actionTaskText.performClick();
                }
            }, 250);
        }
    }

    private void updateTaskMinutesText(Task task, ViewHolder holder) {
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
    }

    protected void updateTaskCardsAndAnimation(Task task) {
        if(taskLayout.containsKey(task)) {
            updateTaskCards(task, taskLayout.get(task), true);
        }
    }

    private void updateTaskCards(Task task, ViewHolder holder, boolean updateAnimation) {
        updateMinutesNotesView(task, holder, updateAnimation, true);

        //first, init a baseline drawable
        LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(context, R.drawable.task_card_gradient).mutate();
        GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
        GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);
        darkColor.setColor(task.getGroup().getDarkColor());
        lightColor.setColor(task.getGroup().getLightColor());
        unwrapped.setLayerGravity(1, Gravity.BOTTOM);

        unwrapped.setLayerSize(1, unwrapped.getLayerWidth(1), 0);

        //if percentage existed before, store before changing main background
        int previousHeight = -1;
        if(holder.sideCardAccent.getBackground() instanceof LayerDrawable) {
            LayerDrawable sideCardBefore = (LayerDrawable) holder.sideCardAccent.getBackground();
            previousHeight = sideCardBefore.getLayerHeight(1);
        }

        //init baseline colors before setting later
        holder.sideCardAccent.setBackground(unwrapped);

        double completionPercentage = task.getTaskCompletionPercentage();
        if(updateAnimation && completionPercentage != 0 && !mainDashboardDayAdapter.hasTaskBeenAnimated(task)) {
            mainDashboardDayAdapter.markTaskAsAnimated(task);

            float previousPercentage;
            if(previousHeight != -1 && previousHeight != holder.sideCardAccent.getHeight()) {
                previousPercentage = (float) previousHeight / holder.sideCardAccent.getHeight();
            } else {
                previousPercentage = 1f;
            }

            //need to update completion percentage over time
            holder.sideCardAccent.post(new Runnable() {
                @Override
                public void run() {
                    Animation anim = new Animation() {
                        @Override
                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                            LayerDrawable unwrapped2 = (LayerDrawable) unwrapped.getConstantState().newDrawable();
                            if(previousPercentage != 1) {
                                unwrapped2.setLayerSize(1, unwrapped2.getLayerWidth(0), (int) ((double) holder.sideCardAccent.getHeight() * (((completionPercentage - previousPercentage) * interpolatedTime) + previousPercentage)));
                            } else {
                                unwrapped2.setLayerSize(1, unwrapped2.getLayerWidth(0), (int) ((double) holder.sideCardAccent.getHeight() * (completionPercentage * interpolatedTime)));
                            }
                            holder.sideCardAccent.setBackground(unwrapped2);
                        }
                    };
                    anim.setDuration(1000);
                    anim.setInterpolator(new DecelerateInterpolator());

                    holder.sideCardAccent.startAnimation(anim);
                }
            });
        } else {
            Utils.setViewGradient(task.getGroup(), holder.sideCardAccent, completionPercentage);
        }
    }

    private View.OnClickListener getDelayTaskClickListener(Task task, int position, ViewHolder holder) {
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
                        ConstraintLayout mainView = ((ConstraintLayout) holder.taskCardConstraintLayout.getParent().getParent());
                        mainView.animate()
                                .translationXBy(width).setDuration(800).setInterpolator(new OvershootInterpolator()).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                task.startWorkingOnTask();
                                task.logMinutes(0, "", false);
                                task.completeTaskForTheDay();
                                mainDashboardDayAdapter.updateTaskCards(task, null);
                                mainDashboardDayAdapter.updateDayLayouts(task, null);

                                notifyDataSetChanged();

                                mainView.setTranslationX(0);
                            }
                        }).start();

                        if(dayPosition == 0) {
                            holder.actionTaskText.setText("Start\nTask");
                        } else {
                            holder.actionTaskText.setText("Start\nTask\nEarly");
                        }
                        holder.actionTaskText.setOnClickListener(getActionTaskListener(task, false, holder));
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

    private void updateMinutesNotesView(Task task, ViewHolder holder, boolean limitLines) {
        updateMinutesNotesView(task, holder, false, limitLines);
    }

    private void updateMinutesNotesView(Task task, ViewHolder holder, boolean updateCardTextAnimation, boolean limitLines) {
        List<LocalDateTime> timestamps = task.getMinutesNotesTimestamps();
        if(timestamps.isEmpty()) {
            holder.taskNotes.setLines(2);
            holder.taskNotes.setText(Html.fromHtml("<b>Minutes:</b><br>Nothing here yet!"));
        } else {
            StringBuilder minutesNotes = new StringBuilder();

            boolean darkModeOn = ((IncrementalApplication) context.getApplication()).isDarkModeOn();

            int lines = 1;
            int maxLineIndex = limitLines ? (timestamps.size() >= 3 ? timestamps.size() - 3 : 0) : 0;
            for(int i = timestamps.size() - 1; i >= maxLineIndex; i--) {
                LocalDateTime dateTime = timestamps.get(i);
                int minutes = task.getMinutesFromTimestamp(dateTime);
                String notes = task.getNotesFromTimestamp(dateTime);

                if(notes.isEmpty()) {
                    minutesNotes.append("<b>" + dateTime.getMonthValue() + "/" + dateTime.getDayOfMonth() + " @ " + Utils.formatLocalTime(dateTime) + ", worked " + minutes + " min.</b> <br>");
                    lines += 1;
                } else {
                    minutesNotes.append("<b>" + dateTime.getMonthValue() + "/" + dateTime.getDayOfMonth() + " @ " + Utils.formatLocalTime(dateTime) + ", worked " + minutes + " min:</b> <br>");

                    if(darkModeOn) {
                        minutesNotes.append("<font color='lightgray'>" + notes + "</font><br>");
                    } else {
                        minutesNotes.append("<font color='gray'>" + notes + "</font><br>");
                    }

                    Paint paint = new Paint();
                    paint.setTextSize(holder.taskNotes.getTextSize());
                    Rect bounds = new Rect();
                    paint.getTextBounds(notes, 0, notes.length(), bounds);

                    lines += 1 + Math.ceil((double) bounds.width() / holder.dailyTimeRemaining.getWidth());
                }
            }

            if(limitLines && timestamps.size() >= 3) {
                minutesNotes.append("..." + (timestamps.size() - 3) + " more");
                lines++;
            }

            holder.taskNotes.setText(Html.fromHtml("<b>Minutes (" + Utils.formatHourMinuteTime(task.getTotalLoggedMinutesOfWork()) + " so far):</b><br>" + minutesNotes.toString()));
            holder.taskNotes.setLines(lines);

            if(updateCardTextAnimation && holder.expandedOptionsLayout.getVisibility() == View.VISIBLE) {
                Utils.animateTaskCardOptionsLayout(holder.expandedOptionsLayout, true, task.getGroup(), holder.sideCardAccent, Utils.getViewGradientPercentage(holder.sideCardAccent));
            }
        }
    }

    private View.OnClickListener getActionTaskListener(Task task, boolean hasTaskStarted, ViewHolder holder) {
        ConstraintLayout taskCardConstraintLayout = holder.taskCardConstraintLayout;
        TextView actionTaskText = holder.actionTaskText;
        ConstraintLayout expandedOptionsLayout = holder.expandedOptionsLayout;

        if(hasTaskStarted) {
            return new View.OnClickListener() {
                @SuppressLint("ResourceType")
                @Override
                public void onClick(View v) {
                    Utils.vibrate(context, 20);

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                    builder.setTitle("For how many minutes did you work on this task?");
                    if(task.hasCarryoverSeconds()) {
                        Toast.makeText(context, "An extra minute has been added to accommodate for previous logs.", Toast.LENGTH_SHORT).show();
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

                            int minutesWorked;
                            if(inputMinutes.getText().length() == 0) {
                                minutesWorked = 0;
                            } else {
                                try {
                                    minutesWorked = Integer.parseInt(inputMinutes.getText().toString());
                                } catch(NumberFormatException ex) {
                                    AlertDialog.Builder errorDialog = new AlertDialog.Builder(v.getContext());
                                    errorDialog.setTitle("Something went wrong!");
                                    errorDialog.setMessage("The inputted minutes value was invalid. Please try again.");

                                    errorDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    });

                                    errorDialog.show();
                                    return;
                                }
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

                                        task.completeTaskForTheDay();
                                        task.logMinutes(minutesWorked, inputNotes.getText().toString(), false, estimateTimestamp);

                                        mainDashboardDayAdapter.unmarkTaskAsAnimated(task);
                                        updateTaskCards(task, holder, true);

                                        if(task.getTotalMinutesLeftOfWork() == 0) {
                                            openAddEstimationForTaskDialog(task);
                                        }

                                        mainView.animate()
                                                .translationXBy(width).setDuration(800).setInterpolator(new OvershootInterpolator()).withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                mainDashboardDayAdapter.updateTaskCards(task, null);
                                                mainDashboardDayAdapter.updateDayLayouts(task, null);

                                                notifyDataSetChanged();

                                                mainView.setTranslationX(0);
                                            }
                                        }).start();

                                        taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                                        if(dayPosition == 0) {
                                            actionTaskText.setText("Start\nTask");
                                        } else {
                                            actionTaskText.setText("Start\nTask\nEarly");
                                        }
                                        actionTaskText.setOnClickListener(getActionTaskListener(task, false, holder));
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

                                    task.logMinutes(minutesWorked, inputNotes.getText().toString(), true, estimateTimestamp);

                                    mainDashboardDayAdapter.unmarkTaskAsAnimated(task);
                                    updateTaskCards(task, holder, true);

                                    mainView.animate()
                                            .translationXBy(width).setStartDelay(200).setDuration(800).setInterpolator(new AnticipateOvershootInterpolator(4)).withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainDashboardDayAdapter.updateTaskCards(task, null);
                                            mainDashboardDayAdapter.updateDayLayouts(task, null);

                                            notifyDataSetChanged();
                                            mainView.setTranslationX(0);
                                        }
                                    }).start();

                                    //animate text colors to indicate success
                                    Animation textColorAnim = new Animation() {
                                        @Override
                                        protected void applyTransformation(float interpolatedTime, Transformation t) {
                                            holder.actionTaskText.setAlpha(1 - interpolatedTime);
                                            holder.dailyTimeRemaining.setAlpha(1 - interpolatedTime);
                                            holder.taskDueDate.setAlpha(1 - interpolatedTime);
                                            holder.totalTimeRemaining.setAlpha(1 - interpolatedTime);
                                        }
                                    };
                                    textColorAnim.setDuration(200);
                                    textColorAnim.setAnimationListener(new Animation.AnimationListener() {
                                        @Override
                                        public void onAnimationStart(Animation animation) {

                                        }

                                        @Override
                                        public void onAnimationEnd(Animation animation) {
                                            holder.actionTaskText.setAlpha(1);
                                            holder.dailyTimeRemaining.setAlpha(1);
                                            holder.taskDueDate.setAlpha(1);
                                            holder.totalTimeRemaining.setAlpha(1);
                                        }

                                        @Override
                                        public void onAnimationRepeat(Animation animation) {

                                        }
                                    });
                                    textColorAnim.setInterpolator(new DecelerateInterpolator());
                                    expandedOptionsLayout.startAnimation(textColorAnim);

                                    //animate main text color as well
                                    TextColorAnimation colorAnimation = new TextColorAnimation(holder.taskName, holder.taskName.getTextColors().getDefaultColor(),
                                            task.getGroup().getLightColor());
                                    colorAnimation.setDuration(200);
                                    colorAnimation.setInterpolator(new DecelerateInterpolator());
                                    colorAnimation.setAnimationListener(new Animation.AnimationListener() {
                                        @Override
                                        public void onAnimationStart(Animation animation) {

                                        }

                                        @Override
                                        public void onAnimationEnd(Animation animation) {
                                            holder.taskName.setTextColor(holder.taskName.getTextColors());
                                        }

                                        @Override
                                        public void onAnimationRepeat(Animation animation) {

                                        }
                                    });
                                    holder.taskName.startAnimation(colorAnimation);

                                    taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                                    if(dayPosition == 0) {
                                        actionTaskText.setText("Start\nTask");
                                    } else {
                                        actionTaskText.setText("Start\nTask\nEarly");
                                    }
                                    actionTaskText.setOnClickListener(getActionTaskListener(task, false, holder));
                                }
                            });

                            finishedTaskBuilder.setNegativeButton("Not done yet!", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    task.logMinutes(minutesWorked, inputNotes.getText().toString(), false, estimateTimestamp);

                                    taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColor));
                                    if(dayPosition == 0) {
                                        actionTaskText.setText("Start\nTask");
                                    } else {
                                        actionTaskText.setText("Start\nTask\nEarly");
                                    }
                                    actionTaskText.setOnClickListener(getActionTaskListener(task, false, holder));

                                    if(task.getTotalMinutesLeftOfWork() == 0) {
                                        openAddEstimationForTaskDialog(task);
                                    }

                                    //delayed so that the keyboard can go away first
                                    v.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainDashboardDayAdapter.unmarkTaskAsAnimated(task);

                                            //two cases:
                                            //1: if it is the only active task, then move back to where it was before, easy
                                            //2: if there are multiple active tasks, then need to calculate where to place the task now (this probably could be optimized w/ an algo in future)

                                            if(inactiveTaskPositions.size() == 1 && inactiveTaskPositions.containsKey(task)) {
                                                //switch back to original position
                                                int newPosition = inactiveTaskPositions.get(task);
                                                tasks.remove(task);
                                                tasks.add(newPosition, task);
                                                notifyItemMoved(0, newPosition);
                                            } else {
                                                //calculate task location
                                                List<Task> taskLocations = getUpdatedTasks();
                                                taskLocations.sort((task, task2) -> {
                                                    if(task.isTaskCurrentlyWorkedOn()) {
                                                        return -1;
                                                    } else if(task2.isTaskCurrentlyWorkedOn()) {
                                                        return 1;
                                                    } else {
                                                        return 0;
                                                    }
                                                });

                                                //count currently worked tasks so that if inactiveTaskPositions was lost, can still only move one item
                                                boolean anotherTaskCurrentlyWorkedOn = false;
                                                int taskIndex = 0;
                                                boolean countingTaskIndex = true;
                                                for(Task loopTask : tasks) {
                                                    if(loopTask.isTaskCurrentlyWorkedOn()) {
                                                        anotherTaskCurrentlyWorkedOn = true;
                                                    }
                                                    if(loopTask == task) {
                                                        countingTaskIndex = false;
                                                    }

                                                    if(countingTaskIndex) {
                                                        taskIndex++;
                                                    }
                                                }

                                                if(!anotherTaskCurrentlyWorkedOn) {
                                                    int newPosition = taskLocations.indexOf(task);
                                                    tasks = taskLocations;
                                                    notifyItemMoved(taskIndex, newPosition);
                                                } else {
                                                    tasks = taskLocations;
                                                    notifyItemRangeChanged(0, getItemCount());
                                                }
                                            }

                                            inactiveTaskPositions.remove(task);
                                            updateTaskCards(task, holder, true);

                                            //update text and graphs
                                            mainDashboardDayAdapter.updateTaskCards(task, TaskAdapter.this);
                                            mainDashboardDayAdapter.updateDayLayouts(task, TaskAdapter.this);

                                            //manually update estimation texts
                                            updateTaskMinutesText(task, holder);
                                            mainDashboardDayAdapter.refreshEstimatedTime(TaskAdapter.this);
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
                        }
                    });

                    builder.show();
                    inputMinutes.requestFocus();

                    inputMinutes.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            InputMethodManager imm = (InputMethodManager) inputMinutes.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(inputMinutes, 0);
                        }
                    }, 150);

                    //cancel notification
                    NotificationManagerCompat nMan = NotificationManagerCompat.from(context);
                    nMan.cancel(IncrementalApplication.PERSISTENT_NOTIFICATION_ID);
                }
            };
        } else {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.vibrate(context, 30);

                    task.startWorkingOnTask();

                    actionTaskText.setText("Log\nWork");
                    actionTaskText.setOnClickListener(getActionTaskListener(task, true, holder));

                    task.getParent().saveTaskToFile();

                    taskCardConstraintLayout.setBackgroundColor(Utils.getThemeAttrColor(context, R.attr.cardColorActive));

                    //move to top
                    int oldPosition = tasks.indexOf(task);
                    inactiveTaskPositions.put(task, oldPosition);
                    tasks.remove(task);
                    tasks.add(0, task);
                    notifyItemMoved(oldPosition, 0);
                    mainDashboardDayAdapter.smoothScrollToPosition(dayPosition);

                    sendOngoingTaskNotification(task, false);
                }
            };
        }
    }

    private void sendOngoingTaskNotification(Task task, boolean useTaskStartTime) {
        //create persistent notification

        //pause work action
        PendingIntent pauseWorkAction = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        //log work action
        Intent logWorkIntent = new Intent(context, MainActivity.class);
        logWorkIntent.putExtra("logTask", task.getTaskID());
        PendingIntent logWorkAction = PendingIntent.getActivity(context, 1, logWorkIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, IncrementalApplication.NOTIFICATION_MAIN_CHANNEL)
                .setSmallIcon(R.drawable.icon_b_w)
                .setColor(ContextCompat.getColor(context, R.color.secondaryColor))
                .setContentTitle("Task in progress:")
                .setContentText(task.getName())
                .setWhen(useTaskStartTime ? task.getLastTaskWorkStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : System.currentTimeMillis())
                .setUsesChronometer(true)
                .setOngoing(true)
//                            .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
//                            .addAction(R.drawable.icon, "Pause", pauseWorkAction)
                .addAction(R.drawable.icon, "Log Work", logWorkAction)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat nMan = NotificationManagerCompat.from(context);
        nMan.notify(IncrementalApplication.PERSISTENT_NOTIFICATION_ID, builder.build());
    }

    private void openAddEstimationForTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View dialogView = context.getLayoutInflater().inflate(R.layout.dialog_reestimate_task_time, null);

        EditText minutesText = dialogView.findViewById(R.id.minutesToCompleteTask);
        Button[] suggestedTimes = new Button[] {dialogView.findViewById(R.id.suggestedTime1Button), dialogView.findViewById(R.id.suggestedTime2Button),
                dialogView.findViewById(R.id.suggestedTime3Button), dialogView.findViewById(R.id.suggestedTime4Button)};

        updateSuggestedTimeAddButton(context, 5, suggestedTimes[0], minutesText);
        updateSuggestedTimeAddButton(context, 15, suggestedTimes[1], minutesText);
        updateSuggestedTimeAddButton(context, 30, suggestedTimes[2], minutesText);
        updateSuggestedTimeAddButton(context, 60, suggestedTimes[3], minutesText);

        builder.setView(dialogView);
        builder.setTitle("You've exceeded your estimated time!");
        builder.setMessage("\nHow many minutes would you like to add to the task's estimated time?");

        builder.setNegativeButton("DISMISS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setPositiveButton("ADD TIME", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //assumption: task must be nonrepeating
                ((NonrepeatingTask) task.getParent()).updateAndSaveTask(task.getStartDate(), task.getName(),
                        task.getDueDateTime(), task.getGroup(), task.getSubgroup(),
                        task.getTotalLoggedMinutesOfWork() + Integer.parseInt(minutesText.getText().toString()));

                //refresh layout
                refreshLayout();
            }
        });

        AlertDialog dialog = builder.show();

        TextView messageText = dialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);
    }

    private void updateSuggestedTimeAddButton(Context context, int minutesAdd, Button button, EditText minutesToCompleteTask) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (minutesToCompleteTask.getText().length() == 0) {
                    minutesToCompleteTask.setText(String.valueOf(minutesAdd));
                } else {
                    minutesToCompleteTask.setText(String.valueOf(Integer.parseInt(minutesToCompleteTask.getText().toString()) + minutesAdd));
                }
            }
        });

        int updateColor = ContextCompat.getColor(context, R.color.secondaryColor);

        button.setBackgroundColor(updateColor);
        if(minutesAdd > 0) {
            button.setText("+ " + Utils.formatHourMinuteTimeFull(minutesAdd));
        } else {
            button.setText("- " + Utils.formatHourMinuteTimeFull(-minutesAdd));
        }
    }

    public void refreshLayout() {
        tasks = getUpdatedTasks();
        taskLayout.clear();
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
