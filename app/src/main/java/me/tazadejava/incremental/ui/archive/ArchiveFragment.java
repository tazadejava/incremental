package me.tazadejava.incremental.ui.archive;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.MainActivity;

public class ArchiveFragment extends Fragment implements BackPressedInterface {

    private RecyclerView groupView;
    private PastTasksListAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_dashboard_nochart, container, false);

        groupView = root.findViewById(R.id.dashboard_day_list);
        groupView.setAdapter(adapter = new PastTasksListAdapter(getContext()));
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