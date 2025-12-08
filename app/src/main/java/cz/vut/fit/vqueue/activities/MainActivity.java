package cz.vut.fit.vqueue.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.utils.PermissionGranter;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("theme_preference", "system");

        // 3. Apply the theme immediately
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
        setContentView(R.layout.activity_main);

        setupPermissions();
        setupNavigation();
    }

    private void setupPermissions() {
        ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result.isEmpty()) {
                        return; // Already granted, callback shouldn't have fired so ignore
                    }
                    boolean anyDenied = false;
                    for (Boolean granted : result.values()) {
                        if (granted == null || !granted) {
                            anyDenied = true;
                            break;
                        }
                    }
                    if (anyDenied) {
                        showPermissionDeniedDialog();
                    }

                });

        PermissionGranter permissionHandler = new PermissionGranter(this, permissionLauncher);
        permissionHandler.requestAllIfNeeded();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);

        if (navHostFragment == null) {
            throw new IllegalStateException(
                    "NavHostFragment not found.");
        }

        navController = navHostFragment.getNavController();

        appBarConfiguration = new AppBarConfiguration.Builder(R.id.homeFragment).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController == null) {
            return super.onSupportNavigateUp();
        }
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void showPermissionDeniedDialog() {
        // Implement proper user feedback
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app requires certain permissions to function properly. You need to grant them in settings")
                .setPositiveButton("OK", null)
                .show();
    }
}