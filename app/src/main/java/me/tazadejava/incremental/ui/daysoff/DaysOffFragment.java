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
        DayOfWeek[] correspondingDaysOfWeek = new DayOfWeek[] {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};

        TaskManager taskManager = IncrementalApplication.taskManager;

        GlobalTaskWorkPreference workPreferences = taskManager.getCurrentTimePeriod().getWorkPreferences();

        LocalDate monday = LocalDate.now();

        while(monday.getDayOfWeek() != DayOfWeek.MONDAY) {
            monday = monday.minusDays(1);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");

        TextView daysOffText = root.findViewById(R.id.daysOffText);

        daysOffText.setText("Days off during week of " + monday.format(formatter) + " to " + monday.plusDays(6).format(formatter));

        for(int i = 0; i < 7; i++) {
            final int dayIndex = i;

            daysOff[i].setText(daysOff[i].getText().toString() + " (" + monday.plusDays(i).format(formatter) + ")");

            if(monday.plusDays(i).equals(LocalDate.now())) {
                daysOff[i].setTypeface(daysOff[i].getTypeface(), Typeface.BOLD);
            } else {
                daysOff[i].setTypeface(daysOff[i].getTypeface());
            }

            daysOff[i].setChecked(workPreferences.isBlackedOutDayOfWeek(correspondingDaysOfWeek[i]));

            daysOff[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if(b) {
                        workPreferences.addBlackedOutDayOfWeek(correspondingDaysOfWeek[dayIndex]);
                    } else {
                        workPreferences.removeBlackedOutDayOfWeek(correspondingDaysOfWeek[dayIndex]);
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