package me.tazadejava.incremental.ui.timeperiods;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.tazadejava.incremental.R;

public class TimePeriodsFragment extends Fragment {

    private RecyclerView groupView;
    private TimePeriodsListAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        groupView = root.findViewById(R.id.dashboard_day_list);
        groupView.setAdapter(adapter = new TimePeriodsListAdapter(getContext()));
        groupView.setLayoutManager(new LinearLayoutManager(getContext()));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        //refresh contents
        groupView.setAdapter(adapter);
    }
}
