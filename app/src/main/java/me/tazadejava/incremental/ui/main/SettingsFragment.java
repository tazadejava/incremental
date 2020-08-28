package me.tazadejava.incremental.ui.main;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import me.tazadejava.incremental.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.user_settings, rootKey);
    }

    @Override
    public void onStop() {
        super.onStop();

        ((IncrementalApplication) getActivity().getApplication()).checkSettings();
    }
}
