package cz.vut.fit.vqueue;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

/**
 * SplashActivity - Shows a splash screen when the app starts
 * Displays the app logo and transitions to MainActivity after a delay
 */
public class SplashActivity extends AppCompatActivity {
    
    private static final int SPLASH_DELAY = 1000; // 2 seconds
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Hide action bar for full-screen splash
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        // Transition to MainActivity after delay
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Close splash activity
                
                // Add smooth transition animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }, SPLASH_DELAY);
    }
}
