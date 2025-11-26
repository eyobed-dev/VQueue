package cz.vut.fit.vqueue.activities;

import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.appbar.MaterialToolbar;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.utils.PermissionGranter;
<<<<<<< HEAD
=======
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.transition.MaterialSharedAxis;
>>>>>>> 09cd85d03acc3849e8f6258bf6e53199f7cde3fe

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private PermissionGranter granter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        setContentView(R.layout.activity_main);

        // --- 1. Permissions Logic (Kept exactly as you had it) ---
        ActivityResultLauncher<String[]> permissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            // Inspect result if needed
                        });
        granter = new PermissionGranter(this, permissionLauncher);
        granter.requestAllIfNeeded();


        // --- 2. Setup Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // --- 3. Setup Navigation Component ---
        // We find the NavHostFragment using the ID from your XML layout.
        // Make sure your XML uses <androidx.fragment.app.FragmentContainerView>
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // --- 4. Define Top-Level Screens ---
        // Pass the ID of your Home Fragment (from nav_graph.xml) here.
        // This prevents the "Back Arrow" from showing on the Home screen.
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.homeFragment // ENSURE THIS ID MATCHES YOUR NAV_GRAPH.XML
        ).build();


        // --- 5. Link Toolbar to Navigation ---
        // This makes the Title update and the Back Button work automatically.
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    // --- 6. Handle the Back Button / Up Arrow ---
    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}