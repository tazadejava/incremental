package me.tazadejava.incremental.ui.statistics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.ui.main.BackPressedInterface;
import me.tazadejava.incremental.ui.main.IncrementalApplication;
import me.tazadejava.incremental.ui.main.MainActivity;

public class StatisticsFragment extends Fragment implements BackPressedInterface {

    private ImageButton leftNav, rightNav;

    private ViewGroup container;

    @Nullable
    @Override
    @CallSuper
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FloatingActionButton addTaskButton = getActivity().findViewById(R.id.fab);
        addTaskButton.setVisibility(View.GONE);
        ((MainActivity) getActivity()).setBackPressedInterface(this);

        this.container = container;

        //this shouldn't run; annotation prevents it
        return null;
    }

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

    @Override
    public void onBackPressed() {
        //removes the statistics from the background before returning to the dashboard
        container.removeAllViewsInLayout();
        ((MainActivity) getActivity()).getNavController().popBackStack();
    }
}
