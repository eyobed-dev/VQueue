package cz.vut.fit.vqueue.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPreferences {
    private static final String PREFS_NAME = "VQueuePreferences";
    private static final String KEY_SHOW_ALL_DEVICES = "show_all_devices";
    
    private SharedPreferences sharedPreferences;
    
    public AppPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public boolean isShowAllDevicesEnabled() {
        return sharedPreferences.getBoolean(KEY_SHOW_ALL_DEVICES, false);
    }
    
    public void setShowAllDevicesEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_ALL_DEVICES, enabled).apply();
    }
}
