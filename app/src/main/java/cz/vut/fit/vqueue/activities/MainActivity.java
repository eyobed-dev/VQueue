package cz.vut.fit.vqueue.activities;


import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import com.google.android.material.appbar.MaterialToolbar;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.fragments.HomeFragment;
import cz.vut.fit.vqueue.stuff.PermissionGranter;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.transition.MaterialSharedAxis;

public class MainActivity extends AppCompatActivity {
    private PermissionGranter granter;
    
    private FrameLayout fragmentContainer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();

        // Register the permission result callback
        ActivityResultLauncher<String[]> permissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            // You could inspect 'result' here if needed
                            // e.g., check if critical permissions were denied
                        });

        granter = new PermissionGranter(this, permissionLauncher);
        granter.requestAllIfNeeded();

        // Only attach the initial fragment on first creation.
        // This prevents duplicate fragments after configuration changes.
        if (savedInstanceState == null) {
            showHomeFragment();
        }

        // Modern back navigation using OnBackPressedDispatcher:
        // - If there are fragments in back stack (more than home) → pop one.
        // - Otherwise → finish the activity / app.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int count = getSupportFragmentManager().getBackStackEntryCount();
                if (count > 1) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });
    }
    
    private void initializeViews() {
        fragmentContainer = findViewById(R.id.fragmentContainer);
        
        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
    
    private void showHomeFragment() {
        HomeFragment homeFragment = new HomeFragment();
        replaceFragment(homeFragment);
    }

    public void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        fragment.setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, true));
        fragment.setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, false));
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}