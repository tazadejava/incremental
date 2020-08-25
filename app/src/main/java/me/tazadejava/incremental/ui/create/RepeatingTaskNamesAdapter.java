package me.tazadejava.incremental.ui.create;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.tazadejava.incremental.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepeatingTaskNamesAdapter extends RecyclerView.Adapter<RepeatingTaskNamesAdapter.RepeatingTaskViewHolder> {

    public class RepeatingTaskViewHolder extends RecyclerView.ViewHolder {

        public TextView fillInBlankIndex;
        public EditText fillInBlankEdit;
        public Switch enabledWeekSwitch;

        public RepeatingTaskViewHolder(@NonNull View itemView) {
            super(itemView);

            fillInBlankIndex = itemView.findViewById(R.id.fillInBlankIndex);
            fillInBlankEdit = itemView.findViewById(R.id.fillInBlankEdit);
            enabledWeekSwitch = itemView.findViewById(R.id.enabledWeekSwitch);
        }
    }

    private RecyclerView recyclerView;
    private CreateTaskActivity activity;

    private String[] taskNames;

    private HashMap<EditText, TextWatcher> textWatchers = new HashMap<>();
    private int lastSelectedEdit;

    private Set<Integer> disabledPositions;

    private LocalDate startDate, dueDate;

    private int repeatSize = 1;

    public RepeatingTaskNamesAdapter(RecyclerView recyclerView, LocalDate startDate, LocalDate dueDate, CreateTaskActivity activity) {
        this.recyclerView = recyclerView;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.activity = activity;

        taskNames = new String[1];

        taskNames[0] = "";

        disabledPositions = new HashSet<>();

        lastSelectedEdit = -1;
    }

    public void setRepeatSize(int amount) {
        repeatSize = amount;

        String[] newList = new String[amount];

        for(int i = 0; i < amount; i++) {
            if(i < taskNames.length) {
                newList[i] = taskNames[i];
            } else {
                newList[i] = "";
            }
        }

        taskNames = newList;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setTaskNamesAndDisabled(String[] taskNames, Set<Integer> disabled) {
        this.taskNames = taskNames;
        this.disabledPositions = disabled;
    }

    @NonNull
    @Override
    public RepeatingTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fill_in_blank, parent, false);
        return new RepeatingTaskViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return repeatSize;
    }

    private String getFormattedDate(LocalDate date, int daysAdded) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        LocalDate newDate = date.plusDays(daysAdded);
        return newDate.format(formatter);
    }

    @Override
    public void onBindViewHolder(@NonNull RepeatingTaskViewHolder holder, int position) {
        holder.fillInBlankIndex.setText(getFormattedDate(startDate, 7 * position) + "-" + getFormattedDate(dueDate, 7 * position) + ")");
        holder.fillInBlankEdit.setText(taskNames[position]);
        holder.fillInBlankEdit.setSelection(holder.fillInBlankEdit.getText().length());

        LocalDate adjustedDueDate = dueDate.plusDays(7 * position);
        if(LocalDate.now().isAfter(adjustedDueDate)) {
            holder.fillInBlankEdit.setEnabled(false);
            holder.enabledWeekSwitch.setEnabled(false);
        } else {
            holder.fillInBlankEdit.setEnabled(true);
            holder.enabledWeekSwitch.setEnabled(true);
        }

        if(position == getItemCount() - 1) {
            holder.enabledWeekSwitch.setChecked(true);
            holder.enabledWeekSwitch.setEnabled(false);

            if(disabledPositions.contains(position)) {
                disabledPositions.remove(position);
            }
        } else {
            holder.enabledWeekSwitch.setChecked(!disabledPositions.contains(position));
            holder.enabledWeekSwitch.setEnabled(true);
        }

        if(position == lastSelectedEdit) {
            holder.fillInBlankEdit.requestFocus();
        }

        if (textWatchers.containsKey(holder.fillInBlankEdit)) {
            holder.fillInBlankEdit.removeTextChangedListener(textWatchers.get(holder.fillInBlankEdit));
            textWatchers.remove(holder.fillInBlankEdit);
        }

        TextWatcher watcher;
        holder.fillInBlankEdit.addTextChangedListener(watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() > 0 && position < taskNames.length) {
                    taskNames[position] = s.toString();
                }

                activity.updateSaveButton();
            }
        });

        textWatchers.put(holder.fillInBlankEdit, watcher);

        holder.enabledWeekSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    disabledPositions.add(position);
                } else {
                    disabledPositions.remove(position);
                }

                activity.updateSaveButton();
            }
        });
    }

    public String[] getTaskNames() {
        String[] taskNamesWithDisables = new String[taskNames.length];

        for(int i = 0; i < taskNames.length; i++) {
            if(disabledPositions.contains(i)) {
                taskNamesWithDisables[i] = "";
            } else {
                taskNamesWithDisables[i] = taskNames[i];
            }
        }

        return taskNamesWithDisables;
    }

    public boolean areAllTasksNamed() {
        for(int i = 0; i < taskNames.length; i++) {
            if(taskNames[i].isEmpty() && !disabledPositions.contains(i)) {
                return false;
            }
        }

        return true;
    }
}
