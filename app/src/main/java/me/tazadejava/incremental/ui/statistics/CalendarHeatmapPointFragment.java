package me.tazadejava.incremental.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.LocalDate;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class CalendarHeatmapPointFragment extends Fragment implements BackPressedInterface {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.GONE);

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_dashboard_nochart, container, false);

        RecyclerView groupView = root.findViewById(R.id.dashboard_day_list);

        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

        if(getArguments().getString("date") != null) {
            LocalDate date = LocalDate.parse(getArguments().getString("date"));
            groupView.setAdapter(new CalendarHeatmapPointAdapter(taskManager, date));
        } else {
            String[] datesString = getArguments().getStringArray("dates");
            LocalDate[] dates = new LocalDate[7];
            for(int i = 0; i < datesString.length; i++) {
                dates[i] = LocalDate.parse(datesString[i]);
            }
            groupView.setAdapter(new CalendarHeatmapPointAdapter(taskManager, dates));
        }

        groupView.setLayoutManager(new LinearLayoutManager(getContext()));

        return root;
    }

    @Override
    public void onBackPressed() {
        ((MainActivity) getActivity()).getNavController().popBackStack();
    }
}
