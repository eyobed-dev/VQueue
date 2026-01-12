package et.dev.eyobed.vqueue.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

/**
 * Utility class for managing app preferences.
 */
public class AppPreferences {

    // Preference keys - must match keys in res/xml/preferences.xml
    private static final String PREF_SHOW_ALL_DEVICES = "show_all_devices";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        // Use default shared preferences (same as PreferenceFragmentCompat)
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isShowAllDevicesEnabled() {
        return prefs.getBoolean(PREF_SHOW_ALL_DEVICES, false);
    }

    public void setShowAllDevicesEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_SHOW_ALL_DEVICES, enabled).apply();
    }
}
