package me.tazadejava.incremental.ui.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class LargeHeightArrayAdapter<T> extends ArrayAdapter<T> {

    public LargeHeightArrayAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public LargeHeightArrayAdapter(@NonNull Context context, int resource, List<T> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        convertView = super.getDropDownView(position, convertView, parent);

        ViewGroup.LayoutParams params = convertView.getLayoutParams();
        params.height = 120;
        convertView.setLayoutParams(params);
        convertView.setForegroundGravity(Gravity.CENTER_VERTICAL);

        if(convertView instanceof TextView) {
            ((TextView) convertView).setGravity(Gravity.CENTER_VERTICAL);
        }

        return convertView;
    }
}
