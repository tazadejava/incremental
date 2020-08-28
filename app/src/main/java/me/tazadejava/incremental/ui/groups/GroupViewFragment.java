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
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class GroupViewFragment extends Fragment implements BackPressedInterface {

    private RecyclerView groupView;
    private TaskGroupListAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //change the FAB to create a new group
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.VISIBLE);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCreateGroupDialog();
            }
        });

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_dashboard_nochart, container, false);

        groupView = root.findViewById(R.id.dashboard_day_list);
        groupView.setAdapter(adapter = new TaskGroupListAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(), getContext()));
        groupView.setLayoutManager(new LinearLayoutManager(getContext()));

        return root;
    }

    private void openCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Create new group");

        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

        View dialogView = View.inflate(getContext(), R.layout.dialog_new_group, null);

        List<String> groupScope = new ArrayList<>();

        groupScope.add("Global Group (no expiration date)");
        for(TimePeriod period : taskManager.getTimePeriods()) {
            String beginDateFormatted = period.getBeginDate() != null ? period.getBeginDate().getMonthValue() + "/" + period.getBeginDate().getDayOfMonth() : "";
            String endDateFormatted = period.getEndDate() != null ? period.getEndDate().getMonthValue() + "/" + period.getEndDate().getDayOfMonth() : "";
            groupScope.add(period.getName() + " (" + beginDateFormatted + " - " + endDateFormatted + ")");
        }

        Spinner groupScopeSpinner = dialogView.findViewById(R.id.groupScopeSpinner);
        groupScopeSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, groupScope));

        groupScopeSpinner.setSelection(groupScope.size() - 1);

        EditText input = dialogView.findViewById(R.id.groupNameText);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        builder.setView(dialogView);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!input.getText().toString().isEmpty()) {
                    String groupName = input.getText().toString();
                    if(taskManager.doesPersistentGroupExist(groupName) || taskManager.getCurrentTimePeriod().doesGroupExist(groupName)) {
                        AlertDialog.Builder error = new AlertDialog.Builder(getContext());
                        error.setTitle("A group under that name already exists!");
                        error.show();
                    } else {
                        if(groupScopeSpinner.getSelectedItemPosition() == 0) {
                            taskManager.addNewPersistentGroup(groupName);
                        } else {
                            taskManager.getTimePeriods().get(groupScopeSpinner.getSelectedItemPosition() - 1).addNewGroup(groupName);
                        }

                        groupView.setAdapter(adapter = new TaskGroupListAdapter(((IncrementalApplication) getActivity().getApplication()).getTaskManager(), getContext()));

                        Utils.hideKeyboard(input);
                    }
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Utils.hideKeyboard(input);
            }
        });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Utils.hideKeyboard(input);
            }
        });

        builder.show();

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        input.requestFocus();
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
