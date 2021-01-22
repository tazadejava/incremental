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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.SubGroup;
import me.tazadejava.incremental.logic.tasks.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.adapters.LargeHeightArrayAdapter;
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
            act.setTitle("Edit task");
        } else {
            act.setTitle(act.getIsBatchCreation() ? "Batch-create new tasks" : "Create new task");
        }
        act.setBackPressedInterface(this);


        View root = inflater.inflate(R.layout.fragment_create_task_group_time, container, false);

        //init helping buttons

        ImageView helpGroup = root.findViewById(R.id.helpGroup);
        ImageView helpSubgroup = root.findViewById(R.id.helpSubgroup);

        if(((IncrementalApplication) act.getApplication()).isDarkModeOn()) {
            helpGroup.setImageResource(R.drawable.ic_help_outline_white_18dp);
            helpSubgroup.setImageResource(R.drawable.ic_help_outline_white_18dp);
        }

        helpGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(act, "For example, a group can be thought of as a class subject (ex: Physics, Math, English...).", Toast.LENGTH_LONG).show();
            }
        });

        helpSubgroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(act, "Optional. Create a subgroup to group similar + repeating tasks. Over time, the estimated minutes for future tasks in the subgroup are automatically averaged.", Toast.LENGTH_LONG).show();
            }
        });

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
                if(act.getIsBatchCreation() != null && act.getIsBatchCreation()) {
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
        Button suggestedTime2Button = root.findViewById(R.id.suggestedTime2Button);
        Button suggestedTime3Button = root.findViewById(R.id.suggestedTime3Button);
        Button suggestedTime4Button = root.findViewById(R.id.suggestedTime4Button);

        if(taskManager.getActiveEditTask() != null) {
            updateSuggestedTimeAddButton(act, -15, suggestedTime1Button, minutesToCompleteTask);
            updateSuggestedTimeAddButton(act, 15, suggestedTime2Button, minutesToCompleteTask);
            updateSuggestedTimeAddButton(act, 30, suggestedTime3Button, minutesToCompleteTask);
            updateSuggestedTimeAddButton(act, 60, suggestedTime4Button, minutesToCompleteTask);
        } else {
            setSuggestedTimeButton(30, suggestedTime1Button, minutesToCompleteTask);
            setSuggestedTimeButton(60, suggestedTime2Button, minutesToCompleteTask);
            setSuggestedTimeButton(90, suggestedTime3Button, minutesToCompleteTask);
            setSuggestedTimeButton(120, suggestedTime4Button, minutesToCompleteTask);
        }

        Spinner groupSpinner = root.findViewById(R.id.groupSpinner);

        List<String> items = new ArrayList<>();

        items.add("Select group");

        items.addAll(taskManager.getAllCurrentGroupNames());

        items.add("Add new group...");

        ArrayAdapter<String> groupSpinnerAdapter = new LargeHeightArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, items);
        groupSpinner.setAdapter(groupSpinnerAdapter);

        Spinner subgroupSpinner = root.findViewById(R.id.subgroupSpinner);

        //initialize subgroup items before setting group click listener

        List<String> subgroupItems = new ArrayList<>();
        subgroupItems.add("No subgroup");
        if(act.getSelectedGroup() != null) {
            subgroupItems.addAll(act.getSelectedGroup().getAllCurrentSubgroupNames());
        }
        ArrayAdapter<String> subgroupSpinnerAdapter = new LargeHeightArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, subgroupItems);

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

                    LocalDate now = LocalDate.now();
                    for(TimePeriod period : taskManager.getTimePeriods()) {
                        if(!period.isInTimePeriod(now)) {
                            continue;
                        }

                        String beginDateFormatted = period.getBeginDate() != null ? period.getBeginDate().getMonthValue() + "/" + period.getBeginDate().getDayOfMonth() : "";
                        String endDateFormatted = period.getEndDate() != null ? period.getEndDate().getMonthValue() + "/" + period.getEndDate().getDayOfMonth() : "";
                        groupScope.add(period.getName() + " (" + beginDateFormatted + " - " + endDateFormatted + ")");
                    }

                    Spinner groupScopeSpinner = dialogView.findViewById(R.id.groupScopeSpinner);
                    groupScopeSpinner.setAdapter(new LargeHeightArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, groupScope));

                    groupScopeSpinner.setSelection(groupScope.size() - 1);

                    EditText input = dialogView.findViewById(R.id.groupNameText);
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

                    builder.setView(dialogView);

                    builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!input.getText().toString().isEmpty()) {
                                lastPosition = position;

                                String groupName = input.getText().toString();

                                if(taskManager.doesPersistentGroupExist(groupName) || taskManager.getCurrentTimePeriod().doesGroupExist(groupName)) {
                                    AlertDialog.Builder failedToCreateGroup = new AlertDialog.Builder(getContext());
                                    failedToCreateGroup.setTitle("Failed to create group");
                                    failedToCreateGroup.setMessage("A group with that name already exists!");

                                    failedToCreateGroup.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    });

                                    failedToCreateGroup.show();
                                } else {
                                    if(groupScopeSpinner.getSelectedItemPosition() == 0) {
                                        taskManager.addNewPersistentGroup(groupName);
                                    } else {
                                        taskManager.getCurrentTimePeriod().addNewGroup(groupName);
                                    }

                                    items.add(items.size() - 1, groupName);

                                    groupSpinner.setSelection(items.size() - 2);
                                    groupSpinnerAdapter.notifyDataSetChanged();
                                }

                                act.setSelectedGroup(taskManager.getCurrentGroupByName(groupName));
                                if(taskManager.getActiveEditTask() == null) {
                                    updateSuggestTimeButtons(taskManager, act, suggestedTime3Button, suggestedTime4Button, minutesToCompleteTask);
                                }
                                updateNextButton(groupSpinner, minutesToCompleteTask, nextButton);
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

                    input.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            InputMethodManager imm = (InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(input, 0);
                        }
                    }, 50);
                } else {
                    lastPosition = position;

                    if(groupSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION && groupSpinner.getSelectedItemPosition() != 0) {
                        act.setSelectedGroup(taskManager.getCurrentGroupByName(groupSpinner.getSelectedItem().toString()));
                        if(taskManager.getActiveEditTask() == null) {
                            updateSuggestTimeButtons(taskManager, act, suggestedTime3Button, suggestedTime4Button, minutesToCompleteTask);
                        }
                    }

                    updateNextButton(groupSpinner, minutesToCompleteTask, nextButton);

                    if(position == 0) {
                        //remove subgroup options
                        subgroupItems.clear();
                        subgroupItems.add("No subgroup");

                        subgroupSpinner.setEnabled(false);
                    } else {
                        subgroupItems.clear();
                        subgroupItems.add("No subgroup");
                        subgroupItems.addAll(act.getSelectedGroup().getAllCurrentSubgroupNames());
                        subgroupItems.add("Add new subgroup...");

                        subgroupSpinner.setEnabled(true);
                    }
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
                    try {
                        act.setMinutesToCompletion(Integer.parseInt(s.toString()));
                    } catch(NumberFormatException ex) {
                        String shortenedTime = s.toString().substring(0, s.length() - 1);
                        act.setMinutesToCompletion(Integer.parseInt(shortenedTime));
                        minutesToCompleteTask.setText(shortenedTime);
                        Toast.makeText(act, "Invalid amount of time specified!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        //create subgroup spinner

        subgroupSpinner.setAdapter(subgroupSpinnerAdapter);

        subgroupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            private int lastPosition = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!subgroupSpinner.isEnabled()) {
                    return;
                }
                //If the user has no classes or needs a new one, open a dialog to create a new one!
                if(position == subgroupItems.size() - 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Create new subgroup");
                    builder.setMessage("Group: " + groupSpinner.getSelectedItem());

                    EditText input = new EditText(getContext());
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

                    builder.setView(input);

                    builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(!input.getText().toString().isEmpty()) {
                                lastPosition = position;

                                String subgroupName = input.getText().toString();

                                if(!items.contains(subgroupName)) {
                                    items.add(items.size() - 1, subgroupName);

                                    act.getSelectedGroup().addNewSubgroup(subgroupName);

                                    taskManager.saveData(true, taskManager.getCurrentTimePeriod());

                                    subgroupItems.clear();
                                    subgroupItems.add("No subgroup");
                                    subgroupItems.addAll(act.getSelectedGroup().getAllCurrentSubgroupNames());
                                    subgroupItems.add("Add new subgroup...");

                                    subgroupSpinner.setSelection(items.size() - 2);
                                    subgroupSpinnerAdapter.notifyDataSetChanged();
                                } else {
                                    AlertDialog.Builder failedToCreateGroup = new AlertDialog.Builder(getContext());
                                    failedToCreateGroup.setTitle("Failed to create subgroup!");
                                    failedToCreateGroup.setMessage("A subgroup with that name already exists!");
                                    failedToCreateGroup.show();
                                }

                                act.setSelectedSubgroup(act.getSelectedGroup().getSubGroupByName(subgroupName));
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
                            subgroupSpinner.setSelection(lastPosition);
                        }
                    });

                    builder.show();
                    input.requestFocus();

                    input.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            InputMethodManager imm = (InputMethodManager) input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(input, 0);
                        }
                    }, 50);
                } else {
                    lastPosition = position;

                    if(subgroupSpinner.getSelectedItemPosition() != AdapterView.INVALID_POSITION && subgroupSpinner.getSelectedItemPosition() != 0) {
                        SubGroup subgroup = act.getSelectedGroup().getSubGroupByName(subgroupSpinner.getSelectedItem().toString());
                        act.setSelectedSubgroup(subgroup);

                        if(subgroup.haveMinutesBeenSet()) {
                            minutesToCompleteTask.setText(String.valueOf(subgroup.getAveragedEstimatedMinutes()));
                        }
                    }

                    updateNextButton(groupSpinner, minutesToCompleteTask, nextButton);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        subgroupSpinner.setEnabled(false);

        //check for previous data

        if(act.getSelectedGroup() != null) {
            groupSpinner.setSelection(groupSpinnerAdapter.getPosition(act.getSelectedGroup().getGroupName()));

            if(taskManager.getActiveEditTask() == null) {
                updateSuggestTimeButtons(taskManager, act, suggestedTime3Button, suggestedTime4Button, minutesToCompleteTask);
            }

            subgroupSpinner.setEnabled(true);

            subgroupItems.clear();
            subgroupItems.add("No subgroup");
            subgroupItems.addAll(act.getSelectedGroup().getAllCurrentSubgroupNames());
            subgroupItems.add("Add new subgroup...");

            if(act.getSelectedSubgroup() != null) {
                subgroupSpinner.setSelection(subgroupSpinnerAdapter.getPosition(act.getSelectedSubgroup().getName()));
            }
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
        int[] buttonColors = new int[] {ContextCompat.getColor(act, R.color.primaryColor), ContextCompat.getColor(act, R.color.primaryColor)};
        if(minMax[0] != Integer.MAX_VALUE && minMax[1] != Integer.MIN_VALUE) {
            if(minMax[0] == minMax[1]) {
                if(minMax[0] >= 120) {
                    //change the big one only
                    minMax[0] = 90;
                    buttonColors[1] = ContextCompat.getColor(act, R.color.secondaryColor);
                } else {
                    //change the small one only
                    minMax[1] = 120;
                    buttonColors[0] = ContextCompat.getColor(act, R.color.secondaryColor);
                }
            } else {
                buttonColors = new int[] {ContextCompat.getColor(act, R.color.secondaryColor), ContextCompat.getColor(act, R.color.secondaryColor)};
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

    private void updateSuggestedTimeAddButton(CreateTaskActivity act, int minutesAdd, Button button, EditText minutesToCompleteTask) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (minutesToCompleteTask.getText().length() == 0) {
                    minutesToCompleteTask.setText(String.valueOf(minutesAdd));
                } else {
                    minutesToCompleteTask.setText(String.valueOf(Integer.parseInt(minutesToCompleteTask.getText().toString()) + minutesAdd));
                }
            }
        });

        int updateColor = ContextCompat.getColor(act, R.color.secondaryColor);

        button.setBackgroundColor(updateColor);
        if(minutesAdd > 0) {
            button.setText("+ " + Utils.formatHourMinuteTimeFull(minutesAdd));
        } else {
            button.setText("- " + Utils.formatHourMinuteTimeFull(-minutesAdd));
        }
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
            //return to group task; active edit task will be erased when specific adapter is reached
            if(getActivity().getIntent().getBooleanExtra("isViewingGroupTasks", false)) {
                ((CreateTaskActivity) getActivity()).setBackPressedInterface(null);
                getActivity().onBackPressed();
            } else {
                taskManager.setActiveEditTask(null);

                Intent main = new Intent(getContext(), MainActivity.class);
                startActivity(main);
            }
        } else {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .replace(R.id.createTaskFrame, new CreateTaskTaskTypeFragment())
                    .commit();
        }
    }
}