package cz.vut.fit.vqueue.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.activities.MainActivity;
import cz.vut.fit.vqueue.model.Queue;
import cz.vut.fit.vqueue.repo.QueueRepo;
import cz.vut.fit.vqueue.service.QueueManagementService;

/**
 * Screen for the "host" who owns the queue.
 * - Shows current queue status (name, current number, estimated wait).
 * - Controls the QueueManagementService and underlying BLE advertising/GATT server.
 * - Allows the host to call the next person, pause/resume, or completely stop the queue.
 */
public class QueueManagerFragment extends Fragment {

    private TextView queueNameText;
    private TextView currentNumberText;
    private TextView waitTimeText;
    private MaterialButton nextButton;
    private MaterialButton pauseButton;
    private MaterialButton stopButton;

    // Repository holding the currently managed queue.
    private QueueRepo queueRepo;
    private Queue currentQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_queue_manager, container, false);

        initializeViews(view);
        setupClickListeners();
        loadQueueData();
        updateQueueInfo();

        return view;
    }

    /**
     * Binds UI elements from layout and initializes repository references.
     */
    private void initializeViews(View view) {
        queueNameText = view.findViewById(R.id.queueNameText);
        currentNumberText = view.findViewById(R.id.currentNumberText);
        waitTimeText = view.findViewById(R.id.waitTimeText);
        nextButton = view.findViewById(R.id.nextButton);
        pauseButton = view.findViewById(R.id.pauseButton);
        stopButton = view.findViewById(R.id.stopButton);
        queueRepo = QueueRepo.getInstance();
    }

    /**
     * Loads the current active queue from the repository.
     * If there is no active queue, the user is sent back to "home".
     * Also starts the QueueManagementService which handles BLE + queue logic in the background.
     */
    private void loadQueueData() {
        currentQueue = queueRepo.getCurrentQueue();
        if (currentQueue == null) {
            Toast.makeText(getContext(), "No active queue found", Toast.LENGTH_SHORT).show();
            ((MainActivity) requireActivity()).resetToHome();
            return;
        }

        // Start the service that owns BLE advertising and GATT server for this queue.
        Intent intent = new Intent(getContext(), QueueManagementService.class);
        intent.putExtra("action", "start_queue");
        requireContext().startService(intent);
    }

    /**
     * Wires up the three main actions:
     * - Next: call the next number in the queue.
     * - Pause: toggle between paused/resumed state.
     * - Stop: delete the queue, stop service and go back to home.
     */
    private void setupClickListeners() {
        // Call next number in the queue.
        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QueueManagementService.class);
            intent.putExtra("action", "call_next");
            requireContext().startService(intent);

            // Refresh queue data from repository (service should update it).
            currentQueue = queueRepo.getCurrentQueue();
            updateQueueInfo();

            if (currentQueue != null) {
                Toast.makeText(
                        getContext(),
                        "Called number: " + currentQueue.getCurrentNumber(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Toggle pause / resume on the queue.
        pauseButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QueueManagementService.class);

            if (currentQueue != null && currentQueue.isPaused()) {
                intent.putExtra("action", "resume_queue");
                requireContext().startService(intent);

                pauseButton.setText("Pause Queue");
                pauseButton.setTextColor(getResources().getColor(R.color.warning));
                pauseButton.setStrokeColor(
                        getResources().getColorStateList(R.color.warning)
                );
                Toast.makeText(getContext(), "Queue resumed", Toast.LENGTH_SHORT).show();
            } else {
                intent.putExtra("action", "pause_queue");
                requireContext().startService(intent);

                pauseButton.setText("Resume Queue");
                pauseButton.setTextColor(getResources().getColor(R.color.success));
                pauseButton.setStrokeColor(
                        getResources().getColorStateList(R.color.success)
                );
                Toast.makeText(getContext(), "Queue paused", Toast.LENGTH_SHORT).show();
            }

            // Keep local reference in sync with repository.
            currentQueue = queueRepo.getCurrentQueue();
            updateQueueInfo();
        });

        // Completely stop and delete the queue.
        stopButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QueueManagementService.class);
            intent.putExtra("action", "stop_queue");
            requireContext().startService(intent);

            Toast.makeText(getContext(), "Queue deleted", Toast.LENGTH_SHORT).show();

            // Completely reset navigation and go to home.
            ((MainActivity) requireActivity()).resetToHome();
        });
    }

    /**
     * Updates all queue-related labels (name, current number, wait time, pause state).
     * Safe to call repeatedly after any action that might modify the queue.
     */
    private void updateQueueInfo() {
        if (currentQueue == null) {
            return;
        }

        queueNameText.setText(currentQueue.getName());
        currentNumberText.setText("Currently Serving: #" + currentQueue.getCurrentNumber());
        waitTimeText.setText("Estimated Wait: " + currentQueue.getWaitTimeMinutes() + " min");

        if (currentQueue.isPaused()) {
            pauseButton.setText("Resume Queue");
            pauseButton.setTextColor(getResources().getColor(R.color.success));
            pauseButton.setStrokeColor(
                    getResources().getColorStateList(R.color.success)
            );
        } else {
            pauseButton.setText("Pause Queue");
            pauseButton.setTextColor(getResources().getColor(R.color.warning));
            pauseButton.setStrokeColor(
                    getResources().getColorStateList(R.color.warning)
            );
        }
    }
}
