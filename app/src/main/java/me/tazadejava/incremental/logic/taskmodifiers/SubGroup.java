package me.tazadejava.incremental.logic.taskmodifiers;

import com.google.gson.JsonObject;

import java.util.Objects;

public class SubGroup {

    private String name;

    private int completedTasks, originalEstimatedMinutesCompletion, totalMinutesWorked;
    private boolean hasMinutesBeenSet;

    public SubGroup(String name) {
        this.name = name;
        this.originalEstimatedMinutesCompletion = 60;
        hasMinutesBeenSet = false;
    }

    public SubGroup(JsonObject data) {
        name = data.get("name").getAsString();
        completedTasks = data.get("completedTasks").getAsInt();
        originalEstimatedMinutesCompletion = data.get("originalEstimatedMinutesCompletion").getAsInt();
        totalMinutesWorked = data.get("totalMinutesWorked").getAsInt();
        hasMinutesBeenSet = data.get("hasMinutesBeenSet").getAsBoolean();
    }

    public JsonObject save() {
        JsonObject data = new JsonObject();

        data.addProperty("name", name);
        data.addProperty("completedTasks", completedTasks);
        data.addProperty("originalEstimatedMinutesCompletion", originalEstimatedMinutesCompletion);
        data.addProperty("totalMinutesWorked", totalMinutesWorked);
        data.addProperty("hasMinutesBeenSet", hasMinutesBeenSet);

        return data;
    }

    public boolean haveMinutesBeenSet() {
        return hasMinutesBeenSet;
    }

    public void setOriginalEstimatedMinutesCompletion(int originalEstimatedMinutesCompletion) {
        this.originalEstimatedMinutesCompletion = originalEstimatedMinutesCompletion;
        hasMinutesBeenSet = true;
    }

    public String getName() {
        return name;
    }

    public int getAveragedEstimatedMinutes() {
        if(completedTasks > 0) {
            //gradually incorporate the originalEstimatedTime with the new calculated average

            double estimatedAverageMinutes = (double) totalMinutesWorked / completedTasks;

            if(completedTasks < 2) {
                //50 50
                return (int) ((0.5 * originalEstimatedMinutesCompletion) + (0.5 * estimatedAverageMinutes));
            } else if(completedTasks < 4) {
                //25 75
                return (int) ((0.25 * originalEstimatedMinutesCompletion) + (0.75 * estimatedAverageMinutes));
            } else {
                //0 100
                return (int) estimatedAverageMinutes;
            }
        }

        return originalEstimatedMinutesCompletion;
    }

    public void completeTask(int minutesWorked) {
        totalMinutesWorked += minutesWorked;
        completedTasks++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubGroup subGroup = (SubGroup) o;
        return Objects.equals(name, subGroup.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
