package me.tazadejava.incremental.ui.create;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;
import me.tazadejava.incremental.ui.main.Utils;

public class CreateTaskGroupTimeFragment extends Fragment implements BackPressedInterface {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CreateTaskActivity act = (CreateTaskActivity) getActivity();
        TaskManager taskManager = IncrementalApplication.taskManager;

        if(taskManager.getActiveEditTask() != null) {
            act.setTitle(act.isRepeatingTask() ? "Edit routine task" : "Edit one-time task");
        } else {
            act.setTitle(act.isRepeatingTask() ? "Create new routine task" : "Create new one-time task");
        }
        act.setBackPressedInterface(this);


        View root = inflater.inflate(R.layout.fragment_create_task_group_time, container, false);

        Button backButton = root.findViewById(R.id.backButton);
        Button nextButton = root.findViewById(R.id.nextButton);

        if(taskManager.getActiveEditTask() != null) {
            backButton.setText("CANCEL");
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(act.isRepeatingTask()) {
                    act.getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                            .replace(R.id.createTaskFrame, new CreateTaskRepeatingNameDateFragment())
                            .commit();
                } else {
                    act.getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                            .replace(R.id.createTaskFrame, new CreateTaskOneTimeNameDateFragment())
                            .commit();
                }

                Utils.hideKeyboard(nextButton);
            }
        });

        EditText minutesToCompleteTask = root.findViewById(R.id.minutesToCompleteTask);

        Spinner groupSpinner = root.findViewById(R.id.groupSpinner);

        List<String> items = new ArrayList<>();

        items.add("Select class");

        items.addAll(taskManager.getAllCurrentGroupNames());

        items.add("Add new class...");

        ArrayAdapter<String> groupSpinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, items);
        groupSpinner.setAdapter(groupSpinnerAdapter);

        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            private int lastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //If the user has no classes or needs a new one, open a dialog to create a new one!
                if(position == items.size() - 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Create new group");

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
                    input.setInputType(InputType.TYPE_CLASS_TEXT);

                    builder.setView(dialogView);

                    builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!input.getText().toString().isEmpty()) {
                                lastPosition = position;

                                String groupName = input.getText().toString();

                                if(!items.contains(groupName)) {
                                    items.add(items.size() - 1, groupName);

                                    if(groupScopeSpinner.getSelectedItemPosition() == 0) {
                                        taskManager.addNewPersistentGroup(groupName);
                                    } else {
                                        taskManager.getTimePeriods().get(groupScopeSpinner.getSelectedItemPosition() - 1).addNewGroup(groupName);
                                    }

                                    groupSpinner.setSelection(items.size() - 2);
                                    groupSpinnerAdapter.notifyDataSetChanged();
                                } else {
                                    AlertDialog.Builder failedToCreateGroup = new AlertDialog.Builder(getContext());
                                    failedToCreateGroup.setTitle("Failed to create group!");
                                    failedToCreateGroup.setMessage("A group with that name already exists!");
                                    failedToCreateGroup.show();
                                }

                                act.setSelectedGroup(taskManager.getCurrentGroupByName(groupName));
                                updateNextButton(groupSpinner, minutesToCompleteTask, nextButton);

                                Utils.hideKeyboard(input);
                            }
                        }
                    });

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });

                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            groupSpinner.setSelection(lastPosition);
                        }
                    });

                    builder.show();
                    input.requestFocus();
                } else {
                    lastPosition = position;

                    if(groupSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION && groupSpinner.getSelectedItemPosition() != 0) {
                        act.setSelectedGroup(taskManager.getCurrentGroupByName(groupSpinner.getSelectedItem().toString()));
                    }

                    updateNextButton(groupSpinner, minutesToCompleteTask, nextButton);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        minutesToCompleteTask.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateNextButton(groupSpinner, minutesToCompleteTask, nextButton);

                if(s.length() > 0) {
                    act.setMinutesToCompletion(Integer.parseInt(s.toString()));
                }
            }
        });

        //check for previous data

        if(act.getSelectedGroup() != null) {
            groupSpinner.setSelection(groupSpinnerAdapter.getPosition(act.getSelectedGroup().getGroupName()));
        }

        if(act.getMinutesToCompletion() != -1) {
            //this method should call the textwatcher, and enable the next button if applicable
            minutesToCompleteTask.setText(String.valueOf(act.getMinutesToCompletion()));
        }

        return root;
    }

    private void updateNextButton(Spinner groupSpinner, EditText minutesToCompleteTask, Button nextButton) {
        if(minutesToCompleteTask.getText().length() == 0) {
            nextButton.setEnabled(false);
            return;
        }

        if(groupSpinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION || groupSpinner.getSelectedItemPosition() == 0) {
            nextButton.setEnabled(false);
            return;
        }

        nextButton.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        if(IncrementalApplication.taskManager.getActiveEditTask() != null) {
            IncrementalApplication.taskManager.setActiveEditTask(null);

            Intent main = new Intent(getContext(), MainActivity.class);
            startActivity(main);
        } else {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.createTaskFrame, new CreateTaskTaskTypeFragment())
                    .commit();
        }
    }
}