package cz.vut.fit.vqueue.utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import java.util.ArrayList;
import java.util.List;

public class PermissionGranter {

    private final FragmentActivity activity;
    private final ActivityResultLauncher<String[]> permissionLauncher;

    public PermissionGranter(@NonNull FragmentActivity activity,
                             @NonNull ActivityResultLauncher<String[]> launcher) {
        this.activity = activity;
        this.permissionLauncher = launcher;
    }

    /**
     * Get required permissions based on Android version
     */
    public static String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        // Bluetooth permissions - API level dependent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        } else {
            permissions.add(Manifest.permission.BLUETOOTH);
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        // Location permissions - needed for BLE on Android < 12
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ doesn't need location for BLE if you use neverForLocation flag
            // But include if you actually need location data
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Foreground service (manifest-only, but added for Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            // Note: This still needs manifest declaration
            // Including here for completeness, though it's not runtime-requested
        }

        return permissions.toArray(new String[0]);
    }

    /**
     * Request all missing permissions
     */
    public void requestAllIfNeeded() {
        List<String> missing = new ArrayList<>();
        String[] required = getRequiredPermissions();

        for (String permission : required) {
            if (ActivityCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }

        if (!missing.isEmpty()) {
            permissionLauncher.launch(missing.toArray(new String[0]));
        }

    }


}