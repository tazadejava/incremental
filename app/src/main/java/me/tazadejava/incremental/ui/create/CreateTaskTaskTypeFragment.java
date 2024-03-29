package me.tazadejava.incremental.ui.create;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;

import androidx.fragment.app.Fragment;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class CreateTaskTaskTypeFragment extends Fragment implements BackPressedInterface {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CreateTaskActivity act = (CreateTaskActivity) getActivity();

        act.setTitle("Create new task");
        act.setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_create_task_task_type, container, false);

        Button cancelButton = root.findViewById(R.id.backButton);
        Button nextButton = root.findViewById(R.id.nextButton);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                act.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(R.id.createTaskFrame, new CreateTaskGroupTimeFragment())
                        .commit();
            }
        });

        RadioButton oneTimeTaskRadioButton = root.findViewById(R.id.oneTimeTaskRadioButton);
        RadioButton repeatingTaskRadioButton = root.findViewById(R.id.repeatingTaskRadioButton);

        oneTimeTaskRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!nextButton.isEnabled()) {
                    nextButton.setEnabled(true);
                }

                if (b) {
                    act.setIsBatchCreation(false);
                }
            }
        });

        repeatingTaskRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(!nextButton.isEnabled()) {
                    nextButton.setEnabled(true);
                }

                if(b) {
                    act.setIsBatchCreation(true);
                }
            }
        });

        //check for previous data

        if(act.getIsBatchCreation() != null) {
            nextButton.setEnabled(true);

            if(act.getIsBatchCreation()) {
                repeatingTaskRadioButton.setChecked(true);
            } else {
                oneTimeTaskRadioButton.setChecked(true);
            }
        } else {
            oneTimeTaskRadioButton.setChecked(true);
        }

        return root;
    }

    @Override
    public void onBackPressed() {
        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();
        if(taskManager.getActiveEditTask() != null) {
            taskManager.setActiveEditTask(null);

            //return to group task; active edit task will be erased when specific adapter is reached
            if(getActivity().getIntent().getBooleanExtra("isViewingGroupTasks", false)) {
                taskManager.setActiveEditTask(null);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(R.id.createTaskFrame, new CreateTaskTaskTypeFragment())
                        .commit();
//                Intent groupTask = new Intent(getContext(), MainActivity.class);
//                groupTask.putExtra("isViewingGroupTasks", true);
//                startActivity(groupTask);
                return;
            }
        }

        Intent main = new Intent(getContext(), MainActivity.class);
        startActivity(main);
    }
}