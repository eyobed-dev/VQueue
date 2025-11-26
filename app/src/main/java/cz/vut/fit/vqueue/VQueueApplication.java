package cz.vut.fit.vqueue;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class VQueueApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Set night mode once for entire app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}