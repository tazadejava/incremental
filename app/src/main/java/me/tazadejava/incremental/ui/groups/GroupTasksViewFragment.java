package me.tazadejava.incremental.ui.groups;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class GroupTasksViewFragment extends Fragment implements BackPressedInterface {

    public static final int DELETE_ID = 135;
    public static final int SELECT_INCOMPLETE_TASKS_POS = 6;

    private RecyclerView groupView;
    private SpecificGroupTaskAdapter adapter;

    private Menu menu;

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
                        new SpecificGroupTaskAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(), this,
                                getContext(), group, timePeriod));
            }
        } else {
            String groupName = getArguments().getString("group");
            Group group = taskManager.getPersistentGroupByName(groupName);

            groupView.setAdapter(adapter =
                    new SpecificGroupTaskAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(), this,
                            getContext(), group, taskManager.getCurrentTimePeriod()));
        }

        groupView.setLayoutManager(new LinearLayoutManager(getContext()));

        return root;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        this.menu = menu;

        menu.add(DELETE_ID, DELETE_ID, 0, "Delete").setIcon(R.drawable.ic_delete_white_18dp).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.setGroupVisible(DELETE_ID, false);

        menu.getItem(SELECT_INCOMPLETE_TASKS_POS).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case DELETE_ID:
                adapter.batchDeleteTasks();
                return true;
            case R.id.action_select_incomplete_tasks:
                adapter.selectAllIncompleteTasks();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setBatchDeleteIconAndOptionsActive(boolean active) {
        menu.setGroupVisible(DELETE_ID, active);
    }

    @Override
    public void onResume() {
        super.onResume();

        //refresh contents
        groupView.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        menu.getItem(SELECT_INCOMPLETE_TASKS_POS).setVisible(false);
    }

    @Override
    public void onBackPressed() {
        ((MainActivity) getActivity()).getNavController().popBackStack();
    }
}
