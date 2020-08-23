package me.tazadejava.incremental.ui.groups;

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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.dashboard.Group;
import me.tazadejava.incremental.logic.dashboard.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

public class TaskGroupListAdapter extends RecyclerView.Adapter<TaskGroupListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout;
        public TextView taskGroupName, tasksCount, weeklyHoursCount, actionTaskText, timePeriodText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            taskGroupName = itemView.findViewById(R.id.taskGroupName);
            tasksCount = itemView.findViewById(R.id.tasksCount);
            weeklyHoursCount = itemView.findViewById(R.id.weeklyHoursCount);
            actionTaskText = itemView.findViewById(R.id.actionTaskText);

            timePeriodText = itemView.findViewById(R.id.timePeriodText);
        }
    }

    private Context context;

    private TaskManager taskManager;

    private List<Group> groups;

    public TaskGroupListAdapter(Context context) {
        this.context = context;

        taskManager = IncrementalApplication.taskManager;

        groups = new ArrayList<>(taskManager.getAllCurrentGroups());

        groups.sort(new Comparator<Group>() {
            @Override
            public int compare(Group group1, Group group2) {
                return group1.getGroupName().compareTo(group2.getGroupName());
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_taskgroup, parent, false);
        return new TaskGroupListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Group group = groups.get(position);

        updateCardColor(group, holder);

        holder.taskGroupName.setText(group.getGroupName());

        holder.taskGroupName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Set name");

                EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                input.setSelectAllOnFocus(false);
                builder.setView(input);

                input.setText(group.getGroupName());

                builder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(input.getText().length() > 0 && !input.getText().toString().equals(group.getGroupName())) {
                            if(taskManager.updateGroupName(group, input.getText().toString())) {
                                holder.taskGroupName.setText(input.getText().toString());

                                input.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                                        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                                    }
                                }, 50);
                            } else {
                                AlertDialog.Builder error = new AlertDialog.Builder(context);
                                error.setTitle("Something went wrong! Does the group name already exist?");
                                error.show();
                            }
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                builder.show();
                input.requestFocus();

                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });

        int tasksCount = taskManager.getCurrentTimePeriod().getTasksCountThisWeekByGroup(group);
        holder.tasksCount.setText(tasksCount + " task" + (tasksCount == 1 ? "" : "s"));

        //TODO: need better infrastructure to be able to calculate hours worked in a week
        holder.weeklyHoursCount.setText("- hours worked this week");

        holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                group.randomizeColor();
                updateCardColor(group, holder);
                taskManager.saveData(true);
            }
        });

        TimePeriod timePeriodScope = taskManager.getGroupScope(group);

        if(timePeriodScope == null) {
            holder.timePeriodText.setText("Global Scope");
        } else {
            holder.timePeriodText.setText(timePeriodScope.getName());
        }
    }

    private void updateCardColor(Group group, ViewHolder viewHolder) {
        viewHolder.taskCardConstraintLayout.post(new Runnable() {
            @Override
            public void run() {
                LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(context, R.drawable.task_card_gradient).mutate();

                GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
                GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);

                darkColor.setColor(group.getBeginColor());
                lightColor.setColor(group.getEndColor());

                unwrapped.setLayerSize(1, (int) (viewHolder.taskCardConstraintLayout.getWidth() * 0.5), unwrapped.getLayerHeight(1));

                viewHolder.taskCardConstraintLayout.setBackground(unwrapped);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }
}
