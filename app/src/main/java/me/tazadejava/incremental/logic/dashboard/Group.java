package me.tazadejava.incremental.logic.dashboard;

public class Group {

    private String name;
    private double color;

    public Group(String name) {
        this.name = name;

        color = Math.random() * 360d;
    }

    public Group(String name, double color) {
        this.name = name;
        this.color = color;
    }

    public String getGroupName() {
        return name;
    }

    public double getColor() {
        return color;
    }
}
