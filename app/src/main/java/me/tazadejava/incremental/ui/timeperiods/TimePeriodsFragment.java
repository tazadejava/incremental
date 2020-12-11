package me.tazadejava.incremental.ui.timeperiods;

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
import me.tazadejava.incremental.ui.create.CreateTimePeriodActivity;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class TimePeriodsFragment extends Fragment implements BackPressedInterface {

    private RecyclerView groupView;
    private TimePeriodsListAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //change the FAB to create a new time period
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.VISIBLE);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newTimePeriod = new Intent(getContext(), CreateTimePeriodActivity.class);
                startActivity(newTimePeriod);
            }
        });

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_dashboard_nochart, container, false);

        groupView = root.findViewById(R.id.dashboard_day_list);
        groupView.setAdapter(adapter = new TimePeriodsListAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(), getContext()));
        groupView.setLayoutManager(new LinearLayoutManager(getContext()));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        //refresh contents
        groupView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        ((MainActivity) getActivity()).getNavController().popBackStack();
    }
}
