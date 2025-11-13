package cz.vut.fit.vqueue.activities;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.fragments.JoinSearchFragment;
import cz.vut.fit.vqueue.stuff.PermissionGranter;

/**
 * Single-activity host for the whole app.
 * - Requests all runtime permissions needed for BLE / foreground service.
 * - Hosts a fragment container and controls navigation between fragments.
 * - Centralizes back-stack behavior and provides a "reset to home" API.
 */
public class MainActivity extends AppCompatActivity {

    private PermissionGranter granter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Launcher used by PermissionGranter to request multiple permissions at once.
        ActivityResultLauncher<String[]> permissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            // If you want to react to denied permissions, do it here.
                        });

        // Ask for all required permissions on startup if needed.
        granter = new PermissionGranter(this, permissionLauncher);
        granter.requestAllIfNeeded();

        // Only attach the initial fragment on first creation.
        if (savedInstanceState == null) {
            // For now, the "home" screen is the join/search screen.
            replaceFragment(new JoinSearchFragment(), false);
        }

        // Custom back navigation:
        // - If there are fragments in back stack → pop one.
        // - Otherwise → finish the activity / app.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int count = getSupportFragmentManager().getBackStackEntryCount();
                if (count > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });
    }

    /**
     * Convenience overload: replace current fragment and add transaction to back stack.
     */
    public void replaceFragment(Fragment fragment) {
        replaceFragment(fragment, true);
    }

    /**
     * Replaces the fragment in the main container.
     *
     * @param fragment       new fragment instance to show
     * @param addToBackStack whether this navigation should be reversible via back button
     */
    public void replaceFragment(Fragment fragment, boolean addToBackStack) {
        var tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentContainer, fragment);
        if (addToBackStack) {
            tx.addToBackStack(null);
        }
        tx.commit();
    }

    /**
     * Clears the entire fragment back stack and shows the "home" screen.
     * Currently, the home screen is JoinSearchFragment.
     * Used when a queue is stopped/deleted or when the user leaves a queue.
     */
    public void resetToHome() {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        replaceFragment(new JoinSearchFragment(), false);
    }
}
