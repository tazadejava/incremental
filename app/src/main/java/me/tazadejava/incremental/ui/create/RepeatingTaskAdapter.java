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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RepeatingTaskAdapter extends RecyclerView.Adapter<RepeatingTaskAdapter.RepeatingTaskViewHolder> {

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

    private List<String> taskNames;

    private HashMap<EditText, TextWatcher> textWatchers = new HashMap<>();
    private int lastSelectedEdit;

    private Set<Integer> disabledPositions;

    private LocalDate startDate;

    public RepeatingTaskAdapter(RecyclerView recyclerView, LocalDate startDate, CreateTaskActivity activity) {
        this.recyclerView = recyclerView;
        this.startDate = startDate;
        this.activity = activity;

        taskNames = new ArrayList<>();
        disabledPositions = new HashSet<>();

        lastSelectedEdit = -1;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    @NonNull
    @Override
    public RepeatingTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fill_in_blank, parent, false);
        return new RepeatingTaskViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return taskNames.size() + 1;
    }

    private String getStartDateFormatted(int daysAdded) {
        LocalDate newDate = startDate.plusDays(daysAdded);
        return newDate.getMonthValue() + "/" + newDate.getDayOfMonth() + "/" + newDate.getYear();
    }

    @Override
    public void onBindViewHolder(@NonNull RepeatingTaskViewHolder holder, int position) {
        holder.fillInBlankIndex.setText((position + 1) + ")");
        holder.fillInBlankEdit.setText(position < taskNames.size() ? taskNames.get(position) : "");
        holder.fillInBlankEdit.setSelection(holder.fillInBlankEdit.getText().length());
        holder.fillInBlankEdit.setHint("Task, starting " + getStartDateFormatted(7 * position));

        holder.enabledWeekSwitch.setChecked(!disabledPositions.contains(position));

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
                System.out.println("CHANGED " + position);
                String text = holder.fillInBlankEdit.getText().toString();
                if (position == taskNames.size()) {
                    taskNames.add(text);
                    lastSelectedEdit = position;

                    refreshLayout();
                } else {
                    taskNames.set(position, text);
                }

                activity.updateSaveButton();
            }
        });

        textWatchers.put(holder.fillInBlankEdit, watcher);

        holder.fillInBlankEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                System.out.println(hasFocus + " NEW FOCUS " + position);
                if (!hasFocus && position == taskNames.size() - 1 && holder.enabledWeekSwitch.isChecked()) {
                    if (holder.fillInBlankEdit.getText().length() == 0) {
                        taskNames.remove(position);

                        refreshLayout();


                    }
                }

                activity.updateSaveButton();
            }
        });

        holder.enabledWeekSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(position == taskNames.size()) {
                    if(!isChecked) {
                        taskNames.add("");
                        lastSelectedEdit = position;

                        disabledPositions.add(position);

                        refreshLayout();
                    }
                }

                if(isChecked) {
                    disabledPositions.remove(position);
                }

                activity.updateSaveButton();
            }
        });
    }

    private void refreshLayout() {
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                for(Map.Entry<EditText, TextWatcher> entry : textWatchers.entrySet()) {
                    entry.getKey().removeTextChangedListener(entry.getValue());
                }
                textWatchers.clear();

                recyclerView.getRecycledViewPool().clear();
                notifyDataSetChanged();

                recyclerView.smoothScrollToPosition(getItemCount() - 1);
            }
        });
    }

    public String[] getTaskNames() {
        List<String> taskNamesRevised = new ArrayList<>();

        int position = 0;
        for(String str : taskNames) {
            if(disabledPositions.contains(position)) {
                taskNamesRevised.add(str);
            }

            position++;
        }

        return taskNamesRevised.toArray(new String[0]);
    }

    public boolean areAllTasksNamed() {
        for(int i = 0; i < taskNames.size(); i++) {
            if(taskNames.get(i).isEmpty() && !disabledPositions.contains(i)) {
                return false;
            }
        }

        return true;
    }
}
