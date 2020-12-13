package me.tazadejava.incremental.ui.statistics;

import android.view.View;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.ui.main.IncrementalApplication;

public class StatisticsFragment extends Fragment {

    private ImageButton leftNav, rightNav;

    protected void setupNav(View root, View container, Class<? extends Fragment> leftFragment, Class<? extends Fragment> rightFragment) {
        leftNav = root.findViewById(R.id.leftNavigation);
        rightNav = root.findViewById(R.id.rightNavigation);

        if(((IncrementalApplication) getActivity().getApplication()).isDarkModeOn()) {
            leftNav.setImageResource(R.drawable.ic_navigate_before_white_24dp);
            rightNav.setImageResource(R.drawable.ic_navigate_next_white_24dp);
        } else {
            leftNav.setImageResource(R.drawable.ic_navigate_before_black_24dp);
            rightNav.setImageResource(R.drawable.ic_navigate_next_black_24dp);
        }

        leftNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getParentFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(container.getId(), leftFragment, null).commit();
            }
        });

        rightNav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getParentFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                        .replace(container.getId(), rightFragment, null).commit();
            }
        });
    }
}
