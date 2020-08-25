package me.tazadejava.incremental.ui.archive;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.TimePeriod;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

public class PastTasksListAdapter extends RecyclerView.Adapter<PastTasksListAdapter.ViewHolder> {

    public class ViewHolder extends RecyclerView.ViewHolder {

        public ConstraintLayout taskCardConstraintLayout;
        public TextView timePeriodName, timePeriodDatesText, timePeriodActiveText, actionTaskText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            taskCardConstraintLayout = itemView.findViewById(R.id.task_card_constraint_layout);

            timePeriodName = itemView.findViewById(R.id.timePeriodName);
            timePeriodDatesText = itemView.findViewById(R.id.timePeriodDatesText);
            timePeriodActiveText = itemView.findViewById(R.id.timePeriodActiveText);
            actionTaskText = itemView.findViewById(R.id.actionTaskText);
        }
    }

    private Context context;

    private TaskManager taskManager;

    private List<TimePeriod> timePeriods;

    public PastTasksListAdapter(Context context) {
        this.context = context;

        taskManager = IncrementalApplication.taskManager;

        timePeriods = new ArrayList<>();

        for(TimePeriod period : taskManager.getTimePeriods()) {
            if(period.getBeginDate() != null && period.getEndDate() != null) {
                timePeriods.add(period);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_task, parent, false);
        return new PastTasksListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimePeriod timePeriod = timePeriods.get(position);

//        updateCardColor(group, holder);

        holder.timePeriodName.setText(timePeriod.getName());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        LocalDate beginDate = timePeriod.getBeginDate();
        LocalDate endDate = timePeriod.getEndDate();

        holder.timePeriodDatesText.setText(beginDate.format(formatter) + " to " + endDate.format(formatter));

        holder.timePeriodActiveText.setText(IncrementalApplication.taskManager.getCurrentTimePeriod() == timePeriod ? "Active" : "");

        holder.actionTaskText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: implement. needs a lot of rules to make sure other time periods are not being intruded on
                Toast.makeText(context, "Coming soon...", Toast.LENGTH_SHORT).show();
            }
        });
    }

//    private void updateCardColor(Group group, ViewHolder viewHolder) {
//        viewHolder.taskCardConstraintLayout.post(new Runnable() {
//            @Override
//            public void run() {
//                LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(context, R.drawable.task_card_gradient).mutate();
//
//                GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
//                GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);
//
//                darkColor.setColor(group.getBeginColor());
//                lightColor.setColor(group.getEndColor());
//
//                unwrapped.setLayerSize(1, (int) (viewHolder.taskCardConstraintLayout.getWidth() * 0.5), unwrapped.getLayerHeight(1));
//
//                viewHolder.taskCardConstraintLayout.setBackground(unwrapped);
//            }
//        });
//    }

    @Override
    public int getItemCount() {
        return timePeriods.size();
    }
}
