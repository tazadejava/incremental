package me.dracorrein.incremental.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.dracorrein.incremental.R;

public class DashboardFragment extends Fragment {

    private RecyclerView dashboardView;
    private DashboardAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        dashboardView = root.findViewById(R.id.dashboard_day_list);
        dashboardView.setAdapter(adapter = new DashboardAdapter(getContext()));
        dashboardView.setLayoutManager(new LinearLayoutManager(getContext()));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        adapter.update();
    }
}