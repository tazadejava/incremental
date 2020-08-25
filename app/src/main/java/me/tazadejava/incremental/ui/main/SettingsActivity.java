package me.tazadejava.incremental.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import me.tazadejava.incremental.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
