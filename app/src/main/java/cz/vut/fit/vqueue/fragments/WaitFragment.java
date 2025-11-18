package cz.vut.fit.vqueue.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import cz.vut.fit.vqueue.R;

public class WaitFragment extends Fragment {
    
    private TextView positionText;
    private TextView estimatedWaitText;
    private TextView currentNumberText;
    private TextView statusText;
    private ProgressBar waitProgressBar;
    private MaterialButton leaveQueueButton;
    private MaterialButton backButton;
    
    private int userPosition = 3;
    private int currentServing = 2;
    private int estimatedWaitMinutes = 15;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wait, container, false);
        
        initializeViews(view);
        setupClickListeners();
        updateQueueStatus();
        startMonitoring();
        
        return view;
    }
    
    private void initializeViews(View view) {
        positionText = view.findViewById(R.id.positionText);
        estimatedWaitText = view.findViewById(R.id.estimatedWaitText);
        currentNumberText = view.findViewById(R.id.currentNumberText);
        statusText = view.findViewById(R.id.statusText);
        waitProgressBar = view.findViewById(R.id.waitProgressBar);
        leaveQueueButton = view.findViewById(R.id.leaveQueueButton);
        backButton = view.findViewById(R.id.backButton);
    }
    
    private void setupClickListeners() {
        leaveQueueButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Left the queue", Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
        });
        
        backButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });
    }
    
    private void updateQueueStatus() {
        positionText.setText("Position: #" + userPosition);
        estimatedWaitText.setText("Estimated Wait: " + estimatedWaitMinutes + " minutes");
        currentNumberText.setText("Currently Serving: #" + currentServing);
        
        // Update progress bar (simplified calculation)
        int progress = Math.max(0, 100 - (userPosition - currentServing) * 20);
        waitProgressBar.setProgress(progress);
    }
    
    private void startMonitoring() {
        // Simulate queue monitoring (replace with actual BLE monitoring)
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Simulate queue progression
                if (currentServing < userPosition) {
                    currentServing++;
                    estimatedWaitMinutes = Math.max(0, estimatedWaitMinutes - 5);
                    updateQueueStatus();
                    
                    if (currentServing == userPosition) {
                        // User's turn!
                        Toast.makeText(getContext(), "It's your turn! Please proceed to the counter.", Toast.LENGTH_LONG).show();
                        statusText.setText("It's your turn! Please proceed to the counter.");
                        statusText.setTextColor(getResources().getColor(R.color.success));
                    }
                }
                
                // Continue monitoring if not user's turn
                if (currentServing < userPosition) {
                    handler.postDelayed(this, 5000); // Check every 5 seconds
                }
            }
        }, 5000);
    }
}
