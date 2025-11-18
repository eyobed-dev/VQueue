package cz.vut.fit.vqueue.fragments;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.activities.MainActivity;
import cz.vut.fit.vqueue.adapters.QueueAdapter;
import cz.vut.fit.vqueue.model.AdvertisedQueue;

/**
 * "Client" home screen.
 * - Simulates scanning for nearby queues (BLE advertising will replace this later).
 * - Displays the list of discovered queues using a RecyclerView.
 * - Allows user to tap a queue, see details, and join (navigates to WaitFragment).
 */
public class JoinSearchFragment extends Fragment implements QueueAdapter.OnQueueClickListener {

    private MaterialButton createQueueButton;
    private ProgressBar loadingIndicator;
    private TextView statusText;
    private RecyclerView queuesRecyclerView;

    private QueueAdapter queueAdapter;
    private List<AdvertisedQueue> availableQueues;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_search, container, false);

        initializeViews(view);
        setupClickListeners();
        setupRecyclerView();

        // For now, start a fake search animation and show mock queues.
        startSearch();

        return view;
    }

    /**
     * Binds view references from the layout.
     */
    private void initializeViews(View view) {
        createQueueButton = view.findViewById(R.id.createQueueButton);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        statusText = view.findViewById(R.id.statusText);
        queuesRecyclerView = view.findViewById(R.id.queuesRecyclerView);
    }

    /**
     * Sets up button to go to the "host" flow (CreateQueueFragment).
     */
    private void setupClickListeners() {
        createQueueButton.setOnClickListener(v -> {
            CreateQueueFragment createQueueFragment = new CreateQueueFragment();
            ((MainActivity) requireActivity()).replaceFragment(createQueueFragment);
        });
    }

    /**
     * Prepares the RecyclerView with an initially empty list of queues.
     */
    private void setupRecyclerView() {
        availableQueues = new ArrayList<>();
        queueAdapter = new QueueAdapter(availableQueues, this);
        queuesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        queuesRecyclerView.setAdapter(queueAdapter);
    }

    /**
     * Simulates a BLE scan:
     * - Shows a progress indicator.
     * - After a delay, fills the list with mocked AdvertisedQueue items.
     *
     * Later this will be replaced by a real BLE scan via BluetoothLeScanner.
     */
    private void startSearch() {
        loadingIndicator.setVisibility(View.VISIBLE);
        statusText.setText("Searching for nearby queues...");
        queuesRecyclerView.setVisibility(View.GONE);

        // Fake asynchronous search for demonstration.
        new Handler(Looper.getMainLooper()).postDelayed(this::simulateFoundQueues, 3000);
    }

    /**
     * Inserts fake queues and updates the UI.
     * Only used for the offline / mock version.
     */
    private void simulateFoundQueues() {
        loadingIndicator.setVisibility(View.GONE);

        availableQueues.clear();
        availableQueues.add(new AdvertisedQueue("caf-001", "Cafeteria", 23, 5, "AA:BB:CC:DD:01"));
        availableQueues.add(new AdvertisedQueue("lib-102", "Library Desk", 41, 3, "AA:BB:CC:DD:02"));
        availableQueues.add(new AdvertisedQueue("ito-207", "IT Office", 7, 4, "AA:BB:CC:DD:03"));

        queuesRecyclerView.setVisibility(View.VISIBLE);
        queueAdapter.notifyDataSetChanged();
        statusText.setText("Tap a queue to see details or join");
    }

    /**
     * Callback from QueueAdapter when user taps a queue item.
     */
    @Override
    public void onQueueClick(AdvertisedQueue queue) {
        showQueueDetailsDialog(queue);
    }

    /**
     * Shows a confirmation dialog with queue details and an option to join.
     * Joining currently just navigates to WaitFragment (mock client).
     */
    private void showQueueDetailsDialog(AdvertisedQueue queue) {
        String message =
                "Queue: " + queue.getName() + "\n" +
                        "Currently Serving: #" + queue.getCurrentNumber() + "\n" +
                        "Estimated Wait: " + queue.getWaitTimeMinutes() + " minutes\n" +
                        "You would be #" + (queue.getCurrentNumber() + 1) + " in line\n\n" +
                        "Do you want to join this queue?";

        new AlertDialog.Builder(requireContext())
                .setTitle("Join Queue")
                .setMessage(message)
                .setPositiveButton("Join Queue", (dialog, which) -> {
                    Toast.makeText(
                            getContext(),
                            "Joined " + queue.getName() + ". You are #" + (queue.getCurrentNumber() + 1) + " in line",
                            Toast.LENGTH_LONG
                    ).show();

                    // In the real BLE version we will also establish a GATT connection here.
                    WaitFragment waitFragment = new WaitFragment();
                    ((MainActivity) requireActivity()).replaceFragment(waitFragment);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
