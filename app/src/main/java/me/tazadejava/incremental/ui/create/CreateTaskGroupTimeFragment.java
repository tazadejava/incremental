package me.tazadejava.incremental.ui.create;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.core.content.ContextCompat;
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
        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

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

        //suggested times

        Button suggestedTime1Button = root.findViewById(R.id.suggestedTime1Button);
        setSuggestedTimeButton(30, suggestedTime1Button, minutesToCompleteTask);

        Button suggestedTime2Button = root.findViewById(R.id.suggestedTime2Button);
        setSuggestedTimeButton(60, suggestedTime2Button, minutesToCompleteTask);

        Button suggestedTime3Button = root.findViewById(R.id.suggestedTime3Button);
        setSuggestedTimeButton(90, suggestedTime3Button, minutesToCompleteTask);

        Button suggestedTime4Button = root.findViewById(R.id.suggestedTime4Button);
        setSuggestedTimeButton(120, suggestedTime4Button, minutesToCompleteTask);

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
                                updateSuggestTimeButtons(taskManager, act, suggestedTime3Button, suggestedTime4Button, minutesToCompleteTask);
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

                    InputMethodManager imm = (InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                } else {
                    lastPosition = position;

                    if(groupSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION && groupSpinner.getSelectedItemPosition() != 0) {
                        act.setSelectedGroup(taskManager.getCurrentGroupByName(groupSpinner.getSelectedItem().toString()));
                        updateSuggestTimeButtons(taskManager, act, suggestedTime3Button, suggestedTime4Button, minutesToCompleteTask);
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

            updateSuggestTimeButtons(taskManager, act, suggestedTime3Button, suggestedTime4Button, minutesToCompleteTask);
        }

        if(act.getMinutesToCompletion() != -1) {
            //this method should call the textwatcher, and enable the next button if applicable
            minutesToCompleteTask.setText(String.valueOf(act.getMinutesToCompletion()));
        }

        return root;
    }

    private void updateSuggestTimeButtons(TaskManager taskManager, CreateTaskActivity act, Button suggestedTime3Button, Button suggestedTime4Button, EditText minutesToCompleteTask) {
        //get min and max minutes for this group, if exists

        int[] minMax = taskManager.getCurrentTimePeriod().getMinMaxEstimatedMinutesByGroup(act.getSelectedGroup());
        int[] buttonColors = new int[] {ContextCompat.getColor(act, R.color.colorPrimary), ContextCompat.getColor(act, R.color.colorPrimary)};
        if(minMax[0] != Integer.MAX_VALUE && minMax[1] != Integer.MIN_VALUE) {
            if(minMax[0] == minMax[1]) {
                if(minMax[0] >= 120) {
                    //change the big one only
                    minMax[0] = 90;
                    buttonColors[1] = ContextCompat.getColor(act, R.color.colorAccent);
                } else {
                    //change the small one only
                    minMax[1] = 120;
                    buttonColors[0] = ContextCompat.getColor(act, R.color.colorAccent);
                }
            } else {
                buttonColors = new int[] {ContextCompat.getColor(act, R.color.colorAccent), ContextCompat.getColor(act, R.color.colorAccent)};
            }
        } else {
            minMax[0] = 90;
            minMax[1] = 120;
        }

        suggestedTime3Button.setText(Utils.formatHourMinuteTimeFull(minMax[0]));
        suggestedTime3Button.setBackgroundColor(buttonColors[0]);
        setSuggestedTimeButton(minMax[0], suggestedTime3Button, minutesToCompleteTask);

        suggestedTime4Button.setText(Utils.formatHourMinuteTimeFull(minMax[1]));
        suggestedTime4Button.setBackgroundColor(buttonColors[1]);
        setSuggestedTimeButton(minMax[1], suggestedTime4Button, minutesToCompleteTask);
    }

    private void setSuggestedTimeButton(int minutes, Button button, EditText minutesToCompleteTask) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                minutesToCompleteTask.setText(String.valueOf(minutes));
            }
        });
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
        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();
        if(taskManager.getActiveEditTask() != null) {
            taskManager.setActiveEditTask(null);

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