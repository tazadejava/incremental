package me.tazadejava.incremental.ui.groups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.DayOfWeek;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.WeeklyTimeInvariant;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class TimeInvariantFragment extends Fragment implements BackPressedInterface {

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.GONE);

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_time_invariants, container, false);

        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();
        TimePeriod timePeriod = taskManager.getCurrentTimePeriod();

        String groupName = getArguments().getString("group");
        Group group = timePeriod.getGroupByName(groupName);

        EditText[] timeInvariants = new EditText[] {root.findViewById(R.id.timeInvariant1),
                root.findViewById(R.id.timeInvariant2), root.findViewById(R.id.timeInvariant3),
                root.findViewById(R.id.timeInvariant4), root.findViewById(R.id.timeInvariant5),
                root.findViewById(R.id.timeInvariant6),root.findViewById(R.id.timeInvariant7)};

        WeeklyTimeInvariant existingInvariant = timePeriod.getTimeInvariant(group);
        if(existingInvariant != null) {
            for(int i = 0; i < 7; i++) {
                int time = existingInvariant.getMinutes(DayOfWeek.of(i + 1));
                if(time != 0) {
                    timeInvariants[i].setText("" + time);
                }
            }
        }

        Button saveButton = root.findViewById(R.id.saveTimeInvariantsButton);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WeeklyTimeInvariant invariant = new WeeklyTimeInvariant();

                for(int i = 0; i < 7; i++) {
                    invariant.setAddedMinutes(DayOfWeek.of(i + 1),
                            timeInvariants[i].getText().length() == 0 ? 0 : Integer.parseInt(timeInvariants[i].getText().toString()));
                }

                timePeriod.setTimeInvariant(group, invariant);
                timePeriod.saveTimePeriodInfo(taskManager.getGson());
                onBackPressed();
            }
        });

        return root;
    }

    @Override
    public void onBackPressed() {
        ((MainActivity) getActivity()).getNavController().popBackStack();
    }
}
