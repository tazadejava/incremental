package me.tazadejava.incremental.ui.main;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.create.CreateTimePeriodActivity;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    private TextView currentTimePeriod;

    private BackPressedInterface backPressedInterface;

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard, R.id.nav_days_off, R.id.nav_time_periods, R.id.nav_task_groups, R.id.nav_archive)
                .setDrawerLayout(drawer)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        TaskManager taskManager = IncrementalApplication.taskManager;

        currentTimePeriod = navigationView.getHeaderView(0).findViewById(R.id.currentTimePeriod);
        currentTimePeriod.setText(taskManager.getCurrentTimePeriod().getName());

        if(taskManager.getCurrentTimePeriod().getBeginDate() != null && taskManager.getCurrentTimePeriod().getEndDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            LocalDate beginDate = IncrementalApplication.taskManager.getCurrentTimePeriod().getBeginDate();
            LocalDate endDate = IncrementalApplication.taskManager.getCurrentTimePeriod().getEndDate();

            TextView currentTimePeriodDates = navigationView.getHeaderView(0).findViewById(R.id.currentTimePeriodDates);
            currentTimePeriodDates.setText(beginDate.format(formatter) + " to " + endDate.format(formatter));
        }

        //check if need new time period
        if(IncrementalApplication.taskManager.hasTimePeriodExpired()) {
            showRenewalTimePeriodDialog();
        }

        currentTimePeriod.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(currentTimePeriod.getWindowToken(), 0);
            }
        }, 50);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_settings:
                Intent settings = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settings);
                return true;
            case R.id.action_save_backup:
                saveBackup();
                return true;
            case R.id.action_load_backup:
                loadBackup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveBackup() {
        AlertDialog.Builder confirmSave = new AlertDialog.Builder(this);

        confirmSave.setTitle("Save task data to a backup location?");
        confirmSave.setMessage("Your data will be stored in the directory of your choosing in a secured zip file.");

        confirmSave.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                startActivityForResult(intent, 1001);
            }
        });

        confirmSave.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        confirmSave.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if(requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if(resultData != null) {
                uri = resultData.getData();

                try {
                    InputStream in = getContentResolver().openInputStream(uri);

                    OutputStream newFile = new FileOutputStream(new File("testFile.txt"));

                    byte[] buffer = new byte[1024];

                    int len;
                    while((len = in.read(buffer)) > 0) {
                        newFile.write(buffer, 0, len);
                    }

                    newFile.close();
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("aj");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, resultData);
        }
    }

    private void loadBackup() {

    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void showRenewalTimePeriodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        boolean defaultTimePeriod = IncrementalApplication.taskManager.getCurrentTimePeriod().getName().isEmpty();

        if(defaultTimePeriod) {
            builder.setTitle("Create a new time period?");
            builder.setMessage("A time period is a good way to automatically partition your tasks into specific start and end dates.");
        } else {
            builder.setTitle("Your time period has expired!");
            builder.setMessage("Do you want to define a new time period?");
        }

        builder.setPositiveButton("Define New Time Period", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent newTimePeriodAct = new Intent(MainActivity.this, CreateTimePeriodActivity.class);
                startActivity(newTimePeriodAct);
            }
        });

        builder.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.show();
    }

    public NavController getNavController() {
        return navController;
    }

    public void setBackPressedInterface(BackPressedInterface backPressedInterface) {
        this.backPressedInterface = backPressedInterface;
    }

    @Override
    public void onBackPressed() {
        if(backPressedInterface != null) {
            backPressedInterface.onBackPressed();
        }
    }
}
