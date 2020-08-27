package me.tazadejava.incremental.ui.create;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.ui.main.Utils;

public class MultipleDaysOfWeekAdapter extends RecyclerView.Adapter<MultipleDaysOfWeekAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout dayOfWeekLayout;

        public Switch enableStartDueDate;

        public Spinner startDayOfWeek, dueDayOfWeek;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            dayOfWeekLayout = itemView.findViewById(R.id.dayOfWeekLayout);

            enableStartDueDate = itemView.findViewById(R.id.enableStartDueDate);
            startDayOfWeek = itemView.findViewById(R.id.startDayOfWeek);
            dueDayOfWeek = itemView.findViewById(R.id.dueDayOfWeek);
        }
    }

    private RecyclerView recyclerView;
    private Button multipleDatesButton;
    private LocalDate startDate;

    private int additionalWeeks = 0;

    private List<LocalDate> startDayOfWeeks = new ArrayList<>();
    private List<LocalDate> dueDayOfWeeks = new ArrayList<>();

    public MultipleDaysOfWeekAdapter(RecyclerView recyclerView, Button multipleDatesButton, LocalDate startDate) {
        this.recyclerView = recyclerView;
        this.multipleDatesButton = multipleDatesButton;
        this.startDate = startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        notifyDataSetChanged();
    }

    public void addAdditionalWeek() {
        additionalWeeks++;

        startDayOfWeeks.add(null);
        dueDayOfWeeks.add(null);

        notifyDataSetChanged();
    }

    public void removeAdditionalWeek() {
        if(additionalWeeks == 0) {
            recyclerView.setVisibility(View.GONE);
            return;
        }

        startDayOfWeeks.remove(startDayOfWeeks.size() - 1);
        dueDayOfWeeks.remove(dueDayOfWeeks.size() - 1);

        additionalWeeks--;

        if(!multipleDatesButton.isEnabled()) {
            multipleDatesButton.setEnabled(true);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_multiple_days_repeating_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if(position % 2 == 0) {
            holder.dayOfWeekLayout.setBackgroundColor(Color.WHITE);
        } else {
            holder.dayOfWeekLayout.setBackgroundColor(Color.LTGRAY);
        }

        holder.enableStartDueDate.setOnCheckedChangeListener(null);
        holder.enableStartDueDate.setChecked(true);
        holder.enableStartDueDate.setEnabled(position == getItemCount() - 1);

        holder.enableStartDueDate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(position == getItemCount() - 1) {
                    removeAdditionalWeek();
                }
            }
        });

        List<String> startDays = new ArrayList<>();
        List<String> dueDays = new ArrayList<>();

        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd");

        List<LocalDate> potentialStartDays = getAvailableBeginDays(startDayOfWeeks.get(position));
        List<LocalDate> potentialEndDays = getAvailableDueDays(startDayOfWeeks.get(position));

        for(LocalDate date : potentialStartDays) {
            DayOfWeek dow = date.getDayOfWeek();
            String startDateText = dow.toString().charAt(0) + dow.toString().substring(1).toLowerCase() + " (" + date.format(format) + ")";

            startDays.add(startDateText);
        }

        LocalDate referenceStartDate = startDayOfWeeks.get(position) == null ? startDate.plusDays(1) : startDayOfWeeks.get(position);
        for(LocalDate date : potentialEndDays) {
            DayOfWeek dow = date.getDayOfWeek();
            String dueDateText = dow.toString().charAt(0) + dow.toString().substring(1).toLowerCase()
                    + " (+" + Utils.getDaysBetweenDaysOfWeek(referenceStartDate.getDayOfWeek(), dow) + " day" + (Utils.getDaysBetweenDaysOfWeek(referenceStartDate.getDayOfWeek(), dow) == 1 ? "" : "s") + ")";

            dueDays.add(dueDateText);
        }

        holder.startDayOfWeek.setAdapter(new ArrayAdapter<>(holder.startDayOfWeek.getContext(), android.R.layout.simple_spinner_item, startDays));

        if(startDayOfWeeks.get(position) != null) {
            holder.startDayOfWeek.setSelection(potentialStartDays.indexOf(startDayOfWeeks.get(position)));
        }

        holder.startDayOfWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!potentialStartDays.get(i).equals(startDayOfWeeks.get(position))) {
                    startDayOfWeeks.set(position, potentialStartDays.get(i));
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ArrayAdapter<String> dueDateAdapter;
        holder.dueDayOfWeek.setAdapter(dueDateAdapter = new ArrayAdapter<>(holder.dueDayOfWeek.getContext(), android.R.layout.simple_spinner_item, dueDays));

        if(dueDayOfWeeks.get(position) != null) {
            holder.dueDayOfWeek.setSelection(potentialEndDays.indexOf(dueDayOfWeeks.get(position)));
        }

        holder.dueDayOfWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!potentialEndDays.get(i).equals(dueDayOfWeeks.get(position))) {
                    dueDayOfWeeks.set(position, potentialEndDays.get(i));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private List<LocalDate> getAvailableBeginDays(LocalDate currentPositionDate) {
        List<LocalDate> availableDays = new ArrayList<>();

        LocalDate currentDate = startDate.plusDays(1);
        for(int i = 0; i < 6; i++) {
            if(!startDayOfWeeks.contains(currentDate) || currentDate.equals(currentPositionDate)) {
                availableDays.add(currentDate);
            }

            currentDate = currentDate.plusDays(1);
        }

        return availableDays;
    }

    private List<LocalDate> getAvailableDueDays(LocalDate firstDate) {
        List<LocalDate> availableDays = new ArrayList<>();

        LocalDate currentDate = firstDate == null ? startDate.plusDays(1) : firstDate;
        for(int i = 0; i < 7; i++) {
            availableDays.add(currentDate);

            currentDate = currentDate.plusDays(1);
        }

        return availableDays;
    }

    public HashMap<LocalDate, DayOfWeek> getAllAdditionalDaysOfWeek() {
        HashMap<LocalDate, DayOfWeek> days = new HashMap<>();

        for(int i = 0; i < additionalWeeks; i++) {
            if(startDayOfWeeks.get(i) != null && dueDayOfWeeks.get(i) != null) {
                 days.put(startDayOfWeeks.get(i), dueDayOfWeeks.get(i).getDayOfWeek());
            }
        }

        return days;
    }

    @Override
    public int getItemCount() {
        return additionalWeeks;
    }
}
