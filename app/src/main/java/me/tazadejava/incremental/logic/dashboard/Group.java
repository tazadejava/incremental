package me.tazadejava.incremental.logic.dashboard;

import android.graphics.Color;

import com.chroma.Chroma;
import com.chroma.ColorSpace;
import com.google.gson.JsonObject;

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
}
