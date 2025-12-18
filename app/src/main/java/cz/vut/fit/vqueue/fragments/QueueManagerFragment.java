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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.adapters.WaiterAdapter;
import cz.vut.fit.vqueue.model.Queue;
import cz.vut.fit.vqueue.repo.QueueRepo;
import cz.vut.fit.vqueue.service.QueueManagementService;

public class QueueManagerFragment extends Fragment {

    private TextView queueNameText;
    private TextView currentNumberText;
    private TextView waitTimeText;
    private MaterialButton nextButton;
    private MaterialButton manualAddButton;
    private MaterialButton pauseButton;
    private MaterialButton stopButton;
    private RecyclerView waitersRecyclerView;
    private WaiterAdapter waiterAdapter;
    private TextView emptyStateText;

    private QueueRepo queueRepo;
    private Queue currentQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_queue_manager, container, false);

        initializeViews(view);
        setupClickListeners();
        loadQueueData();
        updateQueueInfo();

        return view;
    }

    private void initializeViews(View view) {
        queueNameText = view.findViewById(R.id.queueNameText);
        currentNumberText = view.findViewById(R.id.currentNumberText);
        waitTimeText = view.findViewById(R.id.waitTimeText);
        nextButton = view.findViewById(R.id.nextButton);
        manualAddButton = view.findViewById(R.id.manualAddButton);
        pauseButton = view.findViewById(R.id.pauseButton);
        stopButton = view.findViewById(R.id.stopButton);
        waitersRecyclerView = view.findViewById(R.id.waitersRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);

        // Setup RecyclerView
        waitersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        waiterAdapter = new WaiterAdapter(new ArrayList<>());
        waitersRecyclerView.setAdapter(waiterAdapter);

        queueRepo = QueueRepo.getInstance(requireContext());
    }

    private void loadQueueData() {
        currentQueue = queueRepo.getCurrentQueue();
        if (currentQueue == null) {
            Toast.makeText(getContext(), "No active queue found", Toast.LENGTH_SHORT).show();
            androidx.navigation.fragment.NavHostFragment.findNavController(this).popBackStack();
        } else {
            // Start the queue service and begin advertising
            Intent intent = new Intent(getContext(), QueueManagementService.class);
            intent.putExtra("action", "start_queue");
            getContext().startService(intent);
        }
    }

    private void setupClickListeners() {
        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QueueManagementService.class);
            intent.putExtra("action", "call_next");
            getContext().startService(intent);

            currentQueue = queueRepo.getCurrentQueue();
            updateQueueInfo();
            Toast.makeText(getContext(), "Called number: " + currentQueue.getCurrentNumber(), Toast.LENGTH_SHORT)
                    .show();
        });

        manualAddButton.setOnClickListener(v -> showManualAddDialog());

        pauseButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QueueManagementService.class);
            if (currentQueue.isPaused()) {
                intent.putExtra("action", "resume_queue");
                pauseButton.setText("Pause");
                Toast.makeText(getContext(), "Queue resumed", Toast.LENGTH_SHORT).show();
            } else {
                intent.putExtra("action", "pause_queue");
                pauseButton.setText("Resume");
                Toast.makeText(getContext(), "Queue paused", Toast.LENGTH_SHORT).show();
            }
            getContext().startService(intent);
            currentQueue = queueRepo.getCurrentQueue();
        });

        stopButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), QueueManagementService.class);
            intent.putExtra("action", "stop_queue");
            getContext().startService(intent);

            Toast.makeText(getContext(), "Queue stopped", Toast.LENGTH_SHORT).show();
            // Go back to home
            androidx.navigation.fragment.NavHostFragment.findNavController(this)
                    .navigate(R.id.action_queueManagerFragment_to_homeFragment);
        });

    }

    private void showManualAddDialog() {
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Enter name (optional)");
        input.setPadding(50, 30, 50, 30);

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Add Person to Queue")
                .setMessage("Add someone without a smartphone to the queue")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        name = "Walk-in";
                    }
                    addManualWaiter(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addManualWaiter(String name) {
        if (currentQueue != null) {
            int ticketNumber = queueRepo.addWaiter(name);
            if (ticketNumber > 0) {
                Toast.makeText(getContext(), "Added: " + name + " - Ticket #" + ticketNumber,
                        Toast.LENGTH_LONG).show();
                updateQueueInfo();

                // Update advertisement
                Intent intent = new Intent(getContext(), QueueManagementService.class);
                intent.putExtra("action", "update_advertisement");
                getContext().startService(intent);
            } else {
                Toast.makeText(getContext(), "Failed to add person", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateQueueInfo() {
        android.util.Log.i("QueueManager", "updateQueueInfo called");

        if (currentQueue != null) {
            queueNameText.setText(currentQueue.getName());
            currentNumberText.setText(String.valueOf(currentQueue.getCurrentNumber()));
            waitTimeText.setText(currentQueue.getWaitTimeMinutes() + " minutes");

            android.util.Log.i("QueueManager", "Current number: " + currentQueue.getCurrentNumber());

            // Update pause button state
            if (pauseButton != null) {
                if (currentQueue.isPaused()) {
                    pauseButton.setText("Resume");
                } else {
                    pauseButton.setText("Pause");
                }
            }

            // Update waiters list
            if (waiterAdapter != null && currentQueue.getWaiters() != null) {
                android.util.Log.i("QueueManager",
                        "Updating adapter with " + currentQueue.getWaiters().size() + " waiters");
                waiterAdapter.updateData(currentQueue.getWaiters(), currentQueue.getCurrentNumber());

                if (currentQueue.getWaiters().isEmpty()) {
                    waitersRecyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.VISIBLE);
                    android.util.Log.i("QueueManager", "Queue is empty");
                } else {
                    waitersRecyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                    android.util.Log.i("QueueManager", "Queue has waiters, calling scrollToCurrentPerson");

                    // Auto-scroll to currently served person
                    scrollToCurrentPerson();
                }
            } else {
                android.util.Log.w("QueueManager", "Adapter or waiters is null");
            }
        } else {
            android.util.Log.w("QueueManager", "currentQueue is null");
        }
    }

    private void scrollToCurrentPerson() {
        if (currentQueue != null && currentQueue.getWaiters() != null && !currentQueue.getWaiters().isEmpty()) {
            int currentNumber = currentQueue.getCurrentNumber();

            // Find the position of the currently served person
            for (int i = 0; i < currentQueue.getWaiters().size(); i++) {
                if (currentQueue.getWaiters().get(i).getTicketNumber() == currentNumber) {
                    final int position = i;
                    android.util.Log.i("QueueManager",
                            "Scrolling to position " + position + " (ticket #" + currentNumber + ")");

                    // Post to ensure RecyclerView layout is complete
                    waitersRecyclerView.post(() -> {
                        waitersRecyclerView.smoothScrollToPosition(position);
                        android.util.Log.i("QueueManager", "Smooth scroll triggered");
                    });
                    break;
                }
            }
        }
    }
}
