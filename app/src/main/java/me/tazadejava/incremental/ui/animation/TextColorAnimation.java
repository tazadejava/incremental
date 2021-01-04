package me.tazadejava.incremental.ui.animation;

import android.graphics.Color;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;

public class TextColorAnimation extends ColorAnimation {

    private TextView textView;

    public TextColorAnimation(TextView view, int fromColor, int toColor) {
        super(view, fromColor, toColor);

        textView = view;
    }

    public TextColorAnimation(TextView view, int[] fromColor, int[] toColor) {
        super(view, fromColor, toColor);

        textView = view;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        textView.setTextColor(Color.rgb(fromColor[0] + (int) ((toColor[0] - fromColor[0]) * interpolatedTime), fromColor[1] + (int) ((toColor[1] - fromColor[1]) * interpolatedTime),
                fromColor[2] + (int) ((toColor[2] - fromColor[2]) * interpolatedTime)));
    }
}