package me.tazadejava.incremental.ui.animation;

import android.graphics.Color;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ColorAnimation extends Animation {

    protected View view;
    protected int[] fromColor, toColor;

    public ColorAnimation(View view, int fromColor, int toColor) {
        this(view, new int[] {Color.red(fromColor), Color.green(fromColor), Color.blue(fromColor)},
                new int[] {Color.red(toColor), Color.green(toColor), Color.blue(toColor)});
    }

    /**
     * RGB
     * @param view
     * @param fromColor
     * @param toColor
     */
    public ColorAnimation(View view, int[] fromColor, int[] toColor) {
        this.view = view;
        this.fromColor = fromColor;
        this.toColor = toColor;

        this.view.setHasTransientState(true);
        setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setHasTransientState(false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        view.setBackgroundColor(Color.rgb(fromColor[0] + (int) ((toColor[0] - fromColor[0]) * interpolatedTime), fromColor[1] + (int) ((toColor[1] - fromColor[1]) * interpolatedTime),
                fromColor[2] + (int) ((toColor[2] - fromColor[2]) * interpolatedTime)));
    }
}