package et.dev.eyobed.vqueue.fragments;

import android.os.Bundle;
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
import et.dev.eyobed.vqueue.R;

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

    private android.content.BroadcastReceiver queueStatusReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wait, container, false);

        initializeViews(view);
        setupClickListeners();
        updateQueueStatus();
        setupBroadcastReceiver();
        startMonitoring();

        return view;
    }

    private void setupBroadcastReceiver() {
        queueStatusReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                if ("QUEUE_STATUS_UPDATE".equals(intent.getAction())) {
                    int currentNumber = intent.getIntExtra("CURRENT_NUMBER", 0);
                    int myTicket = intent.getIntExtra("MY_TICKET", 0);
                    int waitTime = intent.getIntExtra("WAIT_TIME", 0);

                    // Update UI on main thread
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            currentServing = currentNumber;
                            userPosition = myTicket;
                            estimatedWaitMinutes = waitTime;
                            updateQueueStatus();
                        });
                    }
                }
            }
        };

        // Register receiver
        if (getContext() != null) {
            android.content.IntentFilter filter = new android.content.IntentFilter("QUEUE_STATUS_UPDATE");
            getContext().registerReceiver(queueStatusReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister receiver
        if (queueStatusReceiver != null && getContext() != null) {
            getContext().unregisterReceiver(queueStatusReceiver);
        }
    }

    private void initializeViews(View view) {
        positionText = view.findViewById(R.id.positionText);
        estimatedWaitText = view.findViewById(R.id.estimatedWaitText);
        currentNumberText = view.findViewById(R.id.currentNumberText);
        statusText = view.findViewById(R.id.statusText);
        waitProgressBar = view.findViewById(R.id.waitProgressBar);
        leaveQueueButton = view.findViewById(R.id.leaveQueueButton);
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





    private void startMonitoring() {
        // Start QueueMonitoringService
        android.content.Intent intent = new android.content.Intent(getContext(),
                et.dev.eyobed.vqueue.service.QueueMonitoringService.class);
        intent.setAction("START_MONITORING");

        if (getArguments() != null) {
            String queueName = getArguments().getString("QUEUE_NAME");
            int ticketNumber = getArguments().getInt("TICKET_NUMBER", -1);

            if (queueName != null && ticketNumber != -1) {
                // Update local variables for UI
                userPosition = ticketNumber;
                // We don't know currentServing yet, wait for update
                currentServing = 0;
                updateQueueStatus();

                intent.putExtra("QUEUE_NAME", queueName);
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
