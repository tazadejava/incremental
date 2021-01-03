package me.tazadejava.incremental.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import me.tazadejava.incremental.R;
import me.tazadejava.incremental.logic.tasks.TaskManager;
import me.tazadejava.incremental.ui.create.CreateTimePeriodActivity;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;

    private TextView currentTimePeriod;

    private BackPressedInterface backPressedInterface;

    private NavigationView navigationView;
    private NavController navController;

    private Menu menu;

    private TaskManager taskManager;
    private boolean creatingNewTimePeriod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean darkMode = prefs.getAll().containsKey("darkModeOn") && ((Boolean) (prefs.getAll().get("darkModeOn")));
        setTheme(darkMode ? R.style.AppThemeDarkNoActionBar : R.style.AppThemeNoActionBar);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        taskManager = ((IncrementalApplication) getApplication()).getTaskManager();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard, R.id.nav_days_off, R.id.nav_time_periods, R.id.nav_task_groups, R.id.nav_statistics, R.id.nav_archive)
                .setDrawerLayout(drawer)
                .build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        currentTimePeriod = navigationView.getHeaderView(0).findViewById(R.id.currentTimePeriod);

        updateNavBarTimePeriod();

        //check if need new time period
        if(taskManager.hasTimePeriodExpired()) {
            creatingNewTimePeriod = true;
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

    public void updateNavBarTimePeriod() {
        TaskManager taskManager = ((IncrementalApplication) getApplication()).getTaskManager();

        currentTimePeriod.setText(taskManager.getCurrentTimePeriod().getName());

        if(taskManager.getCurrentTimePeriod().getBeginDate() != null && taskManager.getCurrentTimePeriod().getEndDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            LocalDate beginDate = taskManager.getCurrentTimePeriod().getBeginDate();
            LocalDate endDate = taskManager.getCurrentTimePeriod().getEndDate();

            TextView currentTimePeriodDates = navigationView.getHeaderView(0).findViewById(R.id.currentTimePeriodDates);
            currentTimePeriodDates.setText(beginDate.format(formatter) + " to " + endDate.format(formatter));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;

        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);

            SpannableString span = new SpannableString(item.getTitle());

            span.setSpan(new ForegroundColorSpan(Utils.getAndroidAttrColor(this, android.R.attr.textColorPrimary)), 0, span.length(), 0);

            item.setTitle(span);
        }

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
                if(backPressedInterface instanceof CustomOptionsMenu) {
                    return ((CustomOptionsMenu) backPressedInterface).onOptionsItemSelected(item);
                } else {
                    return super.onOptionsItemSelected(item);
                }
        }
    }

    private void saveBackup() {
        AlertDialog.Builder confirmSave = new AlertDialog.Builder(this);

        confirmSave.setTitle("Save task data to a backup location?");
        confirmSave.setMessage("Your data will be stored in the directory of your choosing in an \".incremental\" file.");

        confirmSave.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new Thread() {
                    @Override
                    public void run() {
                        String fileName = "databackup-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME).replace(":", "-").replace(".", "-");

                        try {
                            File dataFolder = new File(getFilesDir().getAbsolutePath() + "/data/");

                            if(dataFolder.exists()) {
                                File destinationZip = new File(dataFolder.getParentFile().getAbsolutePath() + "/" + fileName + ".incremental");
                                Utils.zipFiles(dataFolder, destinationZip);

                                currentTimePeriod.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent shareFile = new Intent(Intent.ACTION_SEND);

                                        shareFile.setType("application/zip");
                                        shareFile.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(MainActivity.this, "me.tazadejava.incremental.provider", destinationZip));
                                        shareFile.putExtra(Intent.EXTRA_SUBJECT, fileName + ".incremental");
                                        shareFile.putExtra(Intent.EXTRA_TEXT, fileName + ".incremental");

                                        startActivityForResult(Intent.createChooser(shareFile, "Upload backup file"), 2001);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });

        confirmSave.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        confirmSave.show();
    }

    private void loadBackup() {
        AlertDialog.Builder confirmSave = new AlertDialog.Builder(this);

        confirmSave.setTitle("Restore your data from an external backup?");
        confirmSave.setMessage("You must select a valid .incremental file.\nWARNING: All current data will be overridden.");

        confirmSave.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                intent.setType("application/zip");

                intent.addCategory(Intent.CATEGORY_OPENABLE);

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
        switch(requestCode) {
            case 1001:
                if(resultCode == Activity.RESULT_OK) {
                    Uri uri = null;
                    if (resultData != null) {
                        uri = resultData.getData();

                        File destinationFile = new File(getFilesDir().getAbsolutePath() + "/output.zip");

                        try {
                            BufferedInputStream bis = new BufferedInputStream(getContentResolver().openInputStream(uri));
                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destinationFile, false));

                            byte[] buffer = new byte[1024];

                            bis.read(buffer);

                            do {
                                bos.write(buffer);
                            } while(bis.read(buffer) != -1);

                            bis.close();
                            bos.close();

                            //now, temporarily move the old data folder to a recovery location, just in case restoration fails

                            File backupFolder = new File(getFilesDir().getAbsolutePath() + "/data_temp/");
                            Utils.moveDirectory(new File(getFilesDir().getAbsolutePath() + "/data/"), backupFolder);

                            //replace the data folder with the new one!

                            if(!Utils.unzipFile(destinationFile)) {
                                //if failed, restore the original data structure
                                Utils.moveDirectory(backupFolder, new File(getFilesDir().getAbsolutePath() + "/data/"));
                                backupFolder.delete();

                                //send failed notification

                                AlertDialog.Builder failed = new AlertDialog.Builder(MainActivity.this);
                                failed.setTitle("The restoration file is invalid!");
                                failed.setMessage("Something went wrong. The restoration could not be completed.");

                                failed.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if(creatingNewTimePeriod) {
                                            showRenewalTimePeriodDialog();
                                        }
                                    }
                                });

                                failed.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        if(creatingNewTimePeriod) {
                                            showRenewalTimePeriodDialog();
                                        }
                                    }
                                });

                                failed.show();

                                return;
                            }

                            //finally, delete all movement traces

                            destinationFile.delete();
                            Utils.deleteDirectory(backupFolder);

                            AlertDialog.Builder finished = new AlertDialog.Builder(MainActivity.this);
                            finished.setTitle("The restoration completed successfully!");
                            finished.setMessage("Please restart the app to view changes.");
                            finished.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ((IncrementalApplication) getApplication()).reset(false);
                                    finishAffinity();
                                }
                            });

                            finished.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    ((IncrementalApplication) getApplication()).reset(false);
                                    finishAffinity();
                                }
                            });

                            finished.show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        if(creatingNewTimePeriod) {
                            showRenewalTimePeriodDialog();
                        }
                    }
                } else {
                    if(creatingNewTimePeriod) {
                        showRenewalTimePeriodDialog();
                    }
                }
                break;
            case 2001:
                File mainFolder = new File(getFilesDir().getAbsolutePath() + "/");

                for(File file : mainFolder.listFiles()) {
                    if(!file.isDirectory()) {
                        if(file.getName().endsWith(".incremental")) {
                            file.delete();
                        }
                    }
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, resultData);
                break;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void showRenewalTimePeriodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        boolean defaultTimePeriod = taskManager.getCurrentTimePeriod().getName().isEmpty();

        if(defaultTimePeriod) {
            builder.setTitle("Hi! Time to create a new time period.");
            builder.setMessage("A time period is a good way to separate your assignments/tasks into specific start/end dates.");
        } else {
            builder.setTitle("Your time period has expired.");
            builder.setMessage("It's time to define a new time period!");
        }

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent newTimePeriodAct = new Intent(MainActivity.this, CreateTimePeriodActivity.class);
                startActivity(newTimePeriodAct);
            }
        });

        if(defaultTimePeriod) {
            builder.setNeutralButton("LOAD FROM BACKUP", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AlertDialog.Builder confirmSave = new AlertDialog.Builder(MainActivity.this);

                    confirmSave.setTitle("Restore your data from an external backup?");
                    confirmSave.setMessage("You must select a valid .incremental file.");

                    confirmSave.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                            intent.setType("application/zip");

                            intent.addCategory(Intent.CATEGORY_OPENABLE);

                            startActivityForResult(intent, 1001);
                        }
                    });

                    confirmSave.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            showRenewalTimePeriodDialog();
                        }
                    });

                    confirmSave.setCancelable(false);
                    confirmSave.show();
                }
            });
        }

        builder.setCancelable(false);
        builder.show();
    }

    public NavController getNavController() {
        return navController;
    }

    public void setBackPressedInterface(BackPressedInterface backPressedInterface) {
        this.backPressedInterface = backPressedInterface;
    }

    public Menu getOptionsMenu() {
        return menu;
    }

    @Override
    public void onBackPressed() {
        if(backPressedInterface != null) {
            backPressedInterface.onBackPressed();
        }
    }
}
