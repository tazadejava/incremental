package me.tazadejava.incremental.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.NotificationManagerCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import me.tazadejava.incremental.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    public static final String PREF_CONTACT_INFO = "contactInformation";
    public static final String PREF_ACTIVE_TASK_NOTIFICATION = "persistentNotification";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.user_settings, rootKey);

        Preference contactButton = findPreference(PREF_CONTACT_INFO);
        contactButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent sendEmail = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "tazadejava@gmail.com", null));
                startActivity(Intent.createChooser(sendEmail, "Send email..."));
                return true;
            }
        });

//        SwitchPreferenceCompat activeTaskNotification = findPreference(PREF_ACTIVE_TASK_NOTIFICATION);
//        activeTaskNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object newValue) {
//                boolean isActive = (Boolean) newValue;
//
//                if(!isActive) {
//                    NotificationManagerCompat nMan = NotificationManagerCompat.from(getContext());
//                    nMan.cancel(IncrementalApplication.PERSISTENT_NOTIFICATION_ID);
//                }
//
//                return true;
//            }
//        });
    }

    @Override
    public void onStop() {
        super.onStop();

        ((IncrementalApplication) getActivity().getApplication()).checkSettings();
    }
}
