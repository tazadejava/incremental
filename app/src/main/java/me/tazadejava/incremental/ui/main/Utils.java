package me.tazadejava.incremental.ui.main;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLOutput;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.taskmodifiers.Group;

public class Utils {

    public static boolean isInteger(String value) {
        if(value.isEmpty()) {
            return false;
        }

        try {
            Integer.parseInt(value);

            return true;
        } catch(NumberFormatException ex) {
            return false;
        }
    }

    public static String formatLocalDateWithDayOfWeek(LocalDate date) {
        String dayOfWeek = date.getDayOfWeek().toString();
        return (dayOfWeek.charAt(0) + dayOfWeek.substring(1).toLowerCase()) + ", " + date.getMonthValue()
                + "/" + date.getDayOfMonth() + "/" + date.getYear();
    }

    public static String formatLocalDate(LocalDate date) {
        return date.getMonthValue() + "/" + date.getDayOfMonth() + "/" + date.getYear();
    }

    public static String formatLocalTime(LocalDateTime time) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("h:mm ");
        return time.format(format) + (time.getHour() >= 12 ? "pm" : "am");
    }

    public static String formatLocalTime(LocalTime time) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("h:mm ");
        return time.format(format) + (time.getHour() >= 12 ? "pm" : "am");
    }

    public static String formatHourMinuteTime(int minutes) {
        if(minutes < 60) {
            return minutes + " min";
        } else {
            int hours = minutes / 60;
            minutes %= 60;

            if(minutes == 0) {
                return hours + " hr";
            } else {
                return hours + " hr " + minutes + " min";
            }
        }
    }

    public static String formatHourMinuteTimeColonShorthand(int minutes) {
        if(minutes < 60) {
            return "0:" + String.format("%02d", minutes);
        } else {
            int hours = minutes / 60;
            minutes %= 60;

            return hours + ":" + String.format("%02d", minutes);
        }
    }

    public static String formatHourMinuteTimeFull(int minutes) {
        if(minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        } else {
            int hours = minutes / 60;
            minutes %= 60;

            if(minutes == 0) {
                return hours + " hour" + (hours == 1 ? "" : "s");
            } else {
                return hours + " hour" + (hours == 1 ? "" : "s") + " " + minutes + " minute" + (minutes == 1 ? "" : "s");
            }
        }
    }

    /**
     *
     * @param numberOfWeeks: 1 for the end of the first week
     * @return
     */
    public static int getDaysUntilEndOfWeek(int numberOfWeeks) {
        LocalDate date = LocalDate.now();

        date = date.plusDays(Utils.getDaysBetweenDaysOfWeek(date.getDayOfWeek(), DayOfWeek.SUNDAY));
        date = date.plusDays(7 * (numberOfWeeks - 1));

        return (int) ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    public static int getDaysBetweenDaysOfWeek(DayOfWeek start, DayOfWeek end) {
        int dayOfWeekAdjustmentDays = end.getValue() - start.getValue();
        if(dayOfWeekAdjustmentDays < 0) {
            dayOfWeekAdjustmentDays += 7;
        }
        return dayOfWeekAdjustmentDays;
    }

    public static void hideKeyboard(View v) {
        v.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }, 50);
    }

    public static boolean unzipFile(File zippedFile) {
        if(unzipFile(zippedFile, true)) {
            return true;
        }
        return unzipFile(zippedFile, false);
    }

    private static boolean unzipFile(File zippedFile, boolean newPassword) {
        try {
            ZipFile zip = new ZipFile(zippedFile, ZipCredentials.getZipPassword(newPassword ? LocalDate.now() : LocalDate.of(2020, 1, 1)).toCharArray());

            if(!zip.isValidZipFile()) {
                return false;
            }

            zip.extractAll(zippedFile.getParent());

            return true;
        } catch (ZipException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean zipFiles(File sourceFile, File destinationFile) {
        try {
            ZipParameters zipParameters = new ZipParameters();
            zipParameters.setEncryptFiles(true);
            zipParameters.setEncryptionMethod(EncryptionMethod.AES);

            ZipFile zipFile = new ZipFile(destinationFile, ZipCredentials.getZipPassword(LocalDate.now()).toCharArray());
            zipFile.addFolder(sourceFile, zipParameters);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void moveDirectory(File source, File target) {
        if(source.isDirectory()) {
            File newDirectory = new File(target.getAbsolutePath() + "/" + source.getName() + "/");
            newDirectory.mkdirs();

            File[] files = source.listFiles();

            for (File file : files) {
                moveDirectory(file, new File(newDirectory, file.getName()));
            }
            source.delete();
        } else {
            source.renameTo(target);
        }
    }

    public static void deleteDirectory(File file) {
        File[] contents = file.listFiles();

        if(contents != null) {
            for(File loopFile : contents) {
                if(!Files.isSymbolicLink(loopFile.toPath())) {
                    deleteDirectory(loopFile);
                }
            }
        }

        file.delete();
    }

    public static int getAndroidAttrColor(Context context, int attrID) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(attrID, tv, true);
        return ContextCompat.getColor(context, tv.resourceId);
    }

    public static int getThemeAttrColor(Context context, int attrID) {
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(attrID, tv, true);
        return tv.data;
    }

    public static void vibrate(Context context, int milliseconds) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(milliseconds);
        }
    }

    public static void setViewGradient(int darkColorVal, int lightColorVal, View view, double percentage) {
        view.post(new Runnable() {
            @Override
            public void run() {
                LayerDrawable unwrapped = (LayerDrawable) AppCompatResources.getDrawable(view.getContext(), R.drawable.task_card_gradient).mutate();

                GradientDrawable lightColor = (GradientDrawable) unwrapped.getDrawable(0);
                GradientDrawable darkColor = (GradientDrawable) unwrapped.getDrawable(1);
                darkColor.setColor(darkColorVal);
                lightColor.setColor(lightColorVal);
                unwrapped.setLayerGravity(1, Gravity.BOTTOM);

                unwrapped.setLayerSize(1, unwrapped.getLayerWidth(1), (int) (view.getHeight() * percentage));

                view.setBackground(unwrapped);
            }
        });
    }

    public static double getViewGradientPercentage(View view) {
        return (double) ((LayerDrawable) view.getBackground()).getLayerHeight(1) / view.getHeight();
    }

    public static void setViewGradient(Group group, View view, double percentage) {
        setViewGradient(group.getDarkColor(), group.getLightColor(), view, percentage);
    }

    public static void animateTaskCardOptionsLayout(ConstraintLayout expandedOptionsLayout, boolean forceVisible, Group group, View sideCardAccent, double accentPercentage) {
        if(forceVisible) {
            expandedOptionsLayout.setVisibility(View.GONE);
        }

        animateTaskCardOptionsLayout(expandedOptionsLayout, group, sideCardAccent, accentPercentage);
    }

    public static void animateTaskCardOptionsLayout(ConstraintLayout expandedOptionsLayout, Group group, View sideCardAccent, double accentPercentage) {
        if(expandedOptionsLayout.getVisibility() == View.VISIBLE) {
            int height = expandedOptionsLayout.getHeight();

            Animation anim = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    expandedOptionsLayout.getLayoutParams().height = (int) (height * (1 - interpolatedTime));
                    expandedOptionsLayout.setAlpha((1f - interpolatedTime));
                    expandedOptionsLayout.requestLayout();

                    if(group != null) {
                        setViewGradient(group, sideCardAccent, accentPercentage);
                    }
                }
            };
            anim.setDuration(300);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    expandedOptionsLayout.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });

            expandedOptionsLayout.startAnimation(anim);
        } else {
            expandedOptionsLayout.setVisibility(View.VISIBLE);
            expandedOptionsLayout.setAlpha(1);

            expandedOptionsLayout.measure(View.MeasureSpec.makeMeasureSpec(expandedOptionsLayout.getMaxWidth(), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(expandedOptionsLayout.getMaxHeight(), View.MeasureSpec.AT_MOST));

            int measuredHeight = expandedOptionsLayout.getMeasuredHeight();

            expandedOptionsLayout.getLayoutParams().height = 0;

            Animation anim = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {
                    expandedOptionsLayout.getLayoutParams().height = (int) (measuredHeight * interpolatedTime);
                    expandedOptionsLayout.requestLayout();

                    if(group != null) {
                        setViewGradient(group, sideCardAccent, accentPercentage);
                    }
                }
            };
            anim.setDuration(300);
            anim.setInterpolator(new DecelerateInterpolator());

            expandedOptionsLayout.startAnimation(anim);
        }
    }

    public static long localDateToMilliseconds(LocalDate date) {
        return (date.toEpochDay() + 1) * 24 * 60 * 60 * 1000;
    }
}
