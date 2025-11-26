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

    private android.content.BroadcastReceiver queueUpdateReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (cz.vut.fit.vqueue.service.QueueMonitoringService.ACTION_QUEUE_UPDATE.equals(intent.getAction())) {
                int currentNumber = intent
                        .getIntExtra(cz.vut.fit.vqueue.service.QueueMonitoringService.EXTRA_CURRENT_NUMBER, -1);
                int waitTime = intent.getIntExtra(cz.vut.fit.vqueue.service.QueueMonitoringService.EXTRA_WAIT_TIME, -1);

                if (currentNumber != -1) {
                    currentServing = currentNumber;
                    estimatedWaitMinutes = waitTime;
                    updateQueueStatus();

                    if (currentServing >= userPosition) {
                        statusText.setText("It's your turn! Please proceed to the counter.");
                        statusText.setTextColor(getResources().getColor(R.color.status_success));
                        // Service stops itself on turn, so we just update UI here
                    }
                }
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        android.content.IntentFilter filter = new android.content.IntentFilter(
                cz.vut.fit.vqueue.service.QueueMonitoringService.ACTION_QUEUE_UPDATE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            requireContext().registerReceiver(queueUpdateReceiver, filter,
                    android.content.Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(queueUpdateReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(queueUpdateReceiver);
    }

    private void startMonitoring() {
        // Start QueueMonitoringService
        android.content.Intent intent = new android.content.Intent(getContext(),
                cz.vut.fit.vqueue.service.QueueMonitoringService.class);
        intent.setAction("START_MONITORING");

        if (getArguments() != null) {
            String deviceAddress = getArguments().getString("DEVICE_ADDRESS");
            int ticketNumber = getArguments().getInt("TICKET_NUMBER", -1);

            if (deviceAddress != null && ticketNumber != -1) {
                // Update local variables for UI
                userPosition = ticketNumber;
                // We don't know currentServing yet, wait for update
                currentServing = 0;
                updateQueueStatus();

                intent.putExtra("DEVICE_ADDRESS", deviceAddress);
                intent.putExtra("TICKET_NUMBER", ticketNumber);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    getContext().startForegroundService(intent);
                } else {
                    getContext().startService(intent);
                }
            }
        }
    }
}
