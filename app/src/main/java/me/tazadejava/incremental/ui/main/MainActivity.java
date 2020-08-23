package me.tazadejava.incremental.ui.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.ui.create.CreateTaskActivity;
import me.tazadejava.incremental.ui.create.CreateTimePeriodActivity;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    private TextView currentTimePeriod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

<<<<<<< Updated upstream
        FloatingActionButton addTaskButton = findViewById(R.id.fab);
        addTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent createTask = new Intent(MainActivity.this, CreateTaskActivity.class);
                startActivity(createTask);
            }
        });

=======
>>>>>>> Stashed changes
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard, R.id.nav_task_groups, R.id.nav_archive)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        currentTimePeriod = navigationView.getHeaderView(0).findViewById(R.id.currentTimePeriod);
        currentTimePeriod.setText(IncrementalApplication.taskManager.getCurrentTimePeriod().getName());

        //check if need new time period
        if(IncrementalApplication.taskManager.hasTimePeriodExpired()) {
            showRenewalTimePeriodDialog();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
}
