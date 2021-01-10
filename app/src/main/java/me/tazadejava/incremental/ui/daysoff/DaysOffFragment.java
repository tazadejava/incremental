package me.tazadejava.incremental.ui.daysoff;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.GlobalTaskWorkPreference;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class DaysOffFragment extends Fragment implements BackPressedInterface {

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.GONE);

        ((MainActivity) getActivity()).setBackPressedInterface(this);

        View root = inflater.inflate(R.layout.fragment_days_off, container, false);

        Switch[] daysOff = new Switch[] {root.findViewById(R.id.takeMondayOff), root.findViewById(R.id.takeTuesdayOff), root.findViewById(R.id.takeWednesdayOff),
                root.findViewById(R.id.takeThursdayOff),root.findViewById(R.id.takeFridayOff),root.findViewById(R.id.takeSaturdayOff),root.findViewById(R.id.takeSundayOff)};

        TaskManager taskManager = ((IncrementalApplication) getActivity().getApplication()).getTaskManager();

        GlobalTaskWorkPreference workPreferences = taskManager.getCurrentTimePeriod().getWorkPreferences();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE (MM/dd)").withLocale(Locale.ENGLISH);

        TextView daysOffText = root.findViewById(R.id.daysOffText);

        daysOffText.setText("Scheduled days off");

        LocalDate currentDate = LocalDate.now();
        for(int i = 0; i < 7; i++) {
            LocalDate indexDate = currentDate.plusDays(i);

            daysOff[i].setText(indexDate.format(formatter));

            if(indexDate.equals(LocalDate.now())) {
                daysOff[i].setTypeface(daysOff[i].getTypeface(), Typeface.BOLD);
            } else if (indexDate.isBefore(LocalDate.now())) {
                daysOff[i].setTypeface(daysOff[i].getTypeface());
                daysOff[i].setEnabled(false);
            } else {
                daysOff[i].setTypeface(daysOff[i].getTypeface());
            }

            daysOff[i].setChecked(workPreferences.isBlackedOutDay(indexDate));

            daysOff[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b) {
                        workPreferences.addBlackedOutDay(indexDate);
                    } else {
                        workPreferences.removeBlackedOutDay(indexDate);
                    }

                    taskManager.saveData(true);
                }
            });
        }

        return root;
    }

    @Override
    public void onBackPressed() {
        ((MainActivity) getActivity()).getNavController().popBackStack();
    }
}