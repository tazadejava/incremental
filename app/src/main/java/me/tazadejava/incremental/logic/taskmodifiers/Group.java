package me.tazadejava.incremental.logic.taskmodifiers;

import android.graphics.Color;

import com.chroma.Chroma;
import com.chroma.ColorSpace;
import com.google.gson.JsonObject;

import java.util.Objects;

public class Group {

    private String name;
    private double color;

    private int beginColor, endColor;

    public Group(String name) {
        this.name = name;

        setColor(Math.random() * 360d);
    }

    //load from file
    public Group(JsonObject data) {
        name = data.get("name").getAsString();

        setColor(data.get("color").getAsDouble());
    }

    public JsonObject save() {
        JsonObject data = new JsonObject();

        data.addProperty("name", name);
        data.addProperty("color", color);

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
        Chroma originalColor = new Chroma("#1d98cb");
        double[] lch = originalColor.getLCH();

        lch[2] = color;

        beginColor = Color.parseColor(new Chroma(ColorSpace.LCH, lch[0], lch[1], lch[2], 255).hexString());
        endColor = Color.parseColor(new Chroma(ColorSpace.LCH, 100, lch[1], lch[2], 255).hexString());
    }

    public String getGroupName() {
        return name;
    }

    public int getBeginColor() {
        return beginColor;
    }

    public int getEndColor() {
        return endColor;
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
