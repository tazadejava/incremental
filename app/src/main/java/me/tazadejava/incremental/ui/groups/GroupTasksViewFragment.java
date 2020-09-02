package me.tazadejava.incremental.ui.groups;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class GroupTasksViewFragment extends Fragment implements BackPressedInterface {

    private RecyclerView groupView;
    private SpecificGroupTaskAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.GONE);

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_dashboard_nochart, container, false);

        groupView = root.findViewById(R.id.dashboard_day_list);

        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

        String scope = getArguments().getString("scope");

        TimePeriod timePeriod = null;
        if(scope != null) {
            for(TimePeriod tp : taskManager.getTimePeriods()) {
                if(tp.getName().equals(scope)) {
                    timePeriod = tp;
                    break;
                }
            }

            if(timePeriod != null) {
                String groupName = getArguments().getString("group");
                Group group = timePeriod.getGroupByName(groupName);

                groupView.setAdapter(adapter =
                        new SpecificGroupTaskAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(),
                                getContext(), group, timePeriod));
            }
        } else {
            String groupName = getArguments().getString("group");
            Group group = taskManager.getPersistentGroupByName(groupName);

            groupView.setAdapter(adapter =
                    new SpecificGroupTaskAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(),
                            getContext(), group, taskManager.getCurrentTimePeriod()));
        }

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
