package me.tazadejava.incremental.ui.dashboard;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

public class IntegerValueFormatter extends ValueFormatter {

    private DecimalFormat mFormat;

    public IntegerValueFormatter() {
        mFormat = new DecimalFormat("###,###,##0");
    }

    public String getFormattedValue(float value) {
        return mFormat.format(value);
    }
}
