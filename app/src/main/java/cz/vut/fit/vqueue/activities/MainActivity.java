package cz.vut.fit.vqueue.activities;

import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.appbar.MaterialToolbar;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.utils.PermissionGranter;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupPermissions();
        setupToolbar();
        setupNavigation();
    }

    private void setupPermissions() {
        ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result.isEmpty()) {
                        return; // Already granted, callback shouldn't have fired
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

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            throw new IllegalStateException("Toolbar not found in layout");
        }
        setSupportActionBar(toolbar);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);

        if (navHostFragment == null) {
            throw new IllegalStateException(
                    "NavHostFragment not found. Ensure fragmentContainer exists in activity_main.xml"
            );
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
                .setMessage("This app requires certain permissions to function properly.")
                .setPositiveButton("OK", null)
                .show();
    }
}