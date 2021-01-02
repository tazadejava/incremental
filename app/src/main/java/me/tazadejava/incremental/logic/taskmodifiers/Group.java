package me.tazadejava.incremental.logic.taskmodifiers;

import android.graphics.Color;

import com.chroma.Chroma;
import com.chroma.ColorSpace;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Group {

    private String name;
    private double color;

    private int darkColor, lightColor;

    private HashMap<String, SubGroup> subGroups = new HashMap<>();

    public Group(String name) {
        this.name = name;

        setColor(Math.random() * 360d);
    }

    //load from file
    public Group(JsonObject data) {
        name = data.get("name").getAsString();

        setColor(data.get("color").getAsDouble());

        if(data.has("subgroups")) {
            for (Map.Entry<String, JsonElement> subgroup : data.get("subgroups").getAsJsonObject().entrySet()) {
                subGroups.put(subgroup.getKey(), new SubGroup(subgroup.getValue().getAsJsonObject()));
            }
        }
    }

    public JsonObject save() {
        JsonObject data = new JsonObject();

        data.addProperty("name", name);
        data.addProperty("color", color);

        JsonObject subgroups = new JsonObject();

        for(String subgroupName : subGroups.keySet()) {
            subgroups.add(subgroupName, subGroups.get(subgroupName).save());
        }

        data.add("subgroups", subgroups);

        return data;
    }

    public void setColor(double color) {
        this.color = color;
        recomputeColorValues();
    }

    public void setGroupName(String name) {
        this.name = name;
    }

    public void randomizeColor() {
        setColor(Math.random() * 360d);
    }

    private void recomputeColorValues() {
        List<Integer> components = new ArrayList<>();

        int uniqueColorPick = (int) (((color % 60) / 60d) * 151) + 50;

        int color1 = 180;
        int color2 = 60;

        if(color < 60) {
            components.add(color1);
            components.add(color2);
            components.add(255 - uniqueColorPick);
        } else if(color < 120) {
            components.add(color1);
            components.add(uniqueColorPick);
            components.add(color2);
        } else if(color < 180) {
            components.add(255 - uniqueColorPick);
            components.add(color1);
            components.add(color2);
        } else if(color < 240) {
            components.add(color2);
            components.add(color1);
            components.add(uniqueColorPick);
        } else if(color < 300) {
            components.add(color2);
            components.add(255 - uniqueColorPick);
            components.add(color1);
        } else {
            components.add(uniqueColorPick);
            components.add(color2);
            components.add(color1);
        }

        int darkSubtract = 30;
        darkColor = Color.rgb(components.get(0) - darkSubtract, components.get(1) - darkSubtract, components.get(2) - darkSubtract);
        lightColor = Color.rgb(components.get(0), components.get(1), components.get(2));
    }

    public String getGroupName() {
        return name;
    }

    public int getDarkColor() {
        return darkColor;
    }

    public int getLightColor() {
        return lightColor;
    }

    public double getColorValue() {
        return color;
    }

    public void addNewSubgroup(String subGroupName) {
        subGroups.put(subGroupName, new SubGroup(subGroupName));
    }

    public SubGroup getSubGroupByName(String name) {
        return subGroups.getOrDefault(name, null);
    }

    public List<String> getAllCurrentSubgroupNames() {
        List<String> names = new ArrayList<>(subGroups.keySet());
        return names;
    }

    public Collection<SubGroup> getAllSubgroups() {
        return subGroups.values();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Double.compare(group.color, color) == 0 &&
                Objects.equals(name, group.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, color);
    }
}
