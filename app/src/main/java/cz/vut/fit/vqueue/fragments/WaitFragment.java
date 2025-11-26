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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
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
            Toast.makeText(getContext(), "Left the queue", Toast.LENGTH_SHORT).show();
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.action_waitFragment_to_homeFragment);
        });

        backButton.setOnClickListener(v -> {
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigateUp();
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
        // Start QueueMonitoringService
        android.content.Intent intent = new android.content.Intent(getContext(),
                cz.vut.fit.vqueue.service.QueueMonitoringService.class);
        intent.setAction("START_MONITORING");
        // We need to pass the device address and ticket number.
        // Ideally these should be passed as arguments to the fragment or stored in a
        // shared ViewModel/Repo.
        // For now, we'll assume they are available or mock them for the refactor.
        // In a real app, you'd get these from the join result.

        // Mock data for now as per previous logic, but service needs real data.
        // Since we don't have the real data passed here yet, we'll just log it.
        // TODO: Pass real device address and ticket number

        // For demonstration of the service call:
        // getContext().startForegroundService(intent);

        // Reverting to dummy for now to avoid crash without real data,
        // but the service is ready to be used.
        // To properly integrate, we need to pass arguments to WaitFragment.

        // Let's implement a proper start if arguments are present
        if (getArguments() != null) {
            String deviceAddress = getArguments().getString("DEVICE_ADDRESS");
            int ticketNumber = getArguments().getInt("TICKET_NUMBER", -1);

            if (deviceAddress != null && ticketNumber != -1) {
                intent.putExtra("DEVICE_ADDRESS", deviceAddress);
                intent.putExtra("TICKET_NUMBER", ticketNumber);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    getContext().startForegroundService(intent);
                } else {
                    getContext().startService(intent);
                }
            }
        }

        // Keep UI updates via handler for visual feedback in this demo
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // ... existing dummy logic for UI demo ...
                if (currentServing < userPosition) {
                    currentServing++;
                    estimatedWaitMinutes = Math.max(0, estimatedWaitMinutes - 5);
                    updateQueueStatus();

                    if (currentServing == userPosition) {
                        statusText.setText("It's your turn! Please proceed to the counter.");
                        statusText.setTextColor(getResources().getColor(R.color.status_success));

                        // Stop service when turn arrives
                        android.content.Intent stopIntent = new android.content.Intent(getContext(),
                                cz.vut.fit.vqueue.service.QueueMonitoringService.class);
                        stopIntent.setAction("STOP_MONITORING");
                        getContext().startService(stopIntent);
                    } else {
                        handler.postDelayed(this, 5000);
                    }
                }
            }
        }, 5000);
    }
}
