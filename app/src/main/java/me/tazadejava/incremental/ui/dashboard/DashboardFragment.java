package me.tazadejava.incremental.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
import me.tazadejava.incremental.ui.main.MainActivity;

public class DashboardFragment extends Fragment {

    private RecyclerView dashboardView;
    private MainDashboardAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //change the FAB to create a new task
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent createTask = new Intent(getContext(), CreateTaskActivity.class);
                startActivity(createTask);
            }
        });

        View root = inflater.inflate(R.layout.fragment_home, container, false);

        dashboardView = root.findViewById(R.id.dashboard_day_list);
        dashboardView.setAdapter(adapter = new MainDashboardAdapter(getContext()));
        dashboardView.setLayoutManager(new LinearLayoutManager(getContext()));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        //refresh contents
        dashboardView.setAdapter(adapter);
    }
}