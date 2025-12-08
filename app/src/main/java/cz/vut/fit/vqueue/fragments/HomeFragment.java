package cz.vut.fit.vqueue.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.activities.MainActivity;
import cz.vut.fit.vqueue.adapters.QueueAdapter;
import cz.vut.fit.vqueue.model.AdvertisedQueue;
import cz.vut.fit.vqueue.service.BLEClientManager;
import cz.vut.fit.vqueue.service.BLEScanner;

public class HomeFragment extends Fragment implements QueueAdapter.OnQueueClickListener {

    // UI Components
    private FloatingActionButton fabCreateQueue;
    private ProgressBar loadingIndicator;
    private TextView statusText;
    private RecyclerView queuesRecyclerView;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;

    // Data & Logic
    private QueueAdapter queueAdapter;
    private List<AdvertisedQueue> availableQueues;
    private BLEScanner bleScanner;
    private BLEClientManager bleClientManager;
    private String pendingDeviceAddress;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Keep the menu (Settings/About) logic
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the updated layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        setupClickListeners();
        setupRecyclerView();

        // Automatically start searching for queues when the home screen loads
        startSearch();

        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.home_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        NavController navController = NavHostFragment.findNavController(this);

        // Checks if the Menu Item ID matches a Destination ID in the graph.
        // If they match, it navigates automatically.
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    private void initializeViews(View view) {
        fabCreateQueue = view.findViewById(R.id.fabCreateQueue);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        statusText = view.findViewById(R.id.statusText);
        queuesRecyclerView = view.findViewById(R.id.queuesRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Initialize BLE components
        bleScanner = new BLEScanner(getContext());
        bleClientManager = new BLEClientManager(getContext());
        
        // Setup pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Trigger a new scan when user pulls to refresh
            startSearch();
        });
        
        // Set refresh colors to match app theme
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.primary_variant
        );
    }

    private void setupClickListeners() {
        // The FAB shows a bottom sheet to create a queue
        fabCreateQueue.setOnClickListener(v -> {
            CreateQueueBottomSheet bottomSheet = new CreateQueueBottomSheet();
            bottomSheet.setOnQueueCreatedListener(queue -> {
                // Navigate to QueueManagerFragment after queue is created
                NavHostFragment.findNavController(this).navigate(R.id.action_homeFragment_to_queueManagerFragment);
            });
            bottomSheet.show(getChildFragmentManager(), "CreateQueueBottomSheet");
        });
    }

    private void setupRecyclerView() {
        availableQueues = new ArrayList<>();
        queueAdapter = new QueueAdapter(availableQueues, this);

        queuesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        queuesRecyclerView.setAdapter(queueAdapter);

        // Setup BLE scanner listener
        bleScanner.setOnQueueFoundListener(new BLEScanner.OnQueueFoundListener() {
            @Override
            public void onQueueFound(AdvertisedQueue queue) {
                // Check if queue already exists
                boolean exists = false;
                for (AdvertisedQueue existing : availableQueues) {
                    if (existing.getDeviceAddress().equals(queue.getDeviceAddress())) {
                        // Update existing queue
                        int index = availableQueues.indexOf(existing);
                        availableQueues.set(index, queue);
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    availableQueues.add(queue);
                }

                // Sort the list by RSSI (Received Signal Strength Indicator) in descending order.
                // This ensures that the queues with the strongest signal (likely the closest ones)
                // appear at the top of the list.
                Collections.sort(availableQueues, new Comparator<AdvertisedQueue>() {
                    @Override
                    public int compare(AdvertisedQueue q1, AdvertisedQueue q2) {
                        // A higher RSSI value means a stronger signal.
                        return Integer.compare(q2.getRssi(), q1.getRssi());
                    }
                });

                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    int queueCount = 0;
                    for (AdvertisedQueue q : availableQueues) {
                        if (q.getCurrentNumber() >= 0 && q.getWaitTimeMinutes() >= 0) {
                            queueCount++;
                        }
                    }
                    if (queueCount > 0) {
                        statusText.setText(
                                "Found " + availableQueues.size() + " Queues(s) (" + queueCount + " queue(s))");
                    } else {
                        statusText.setText("Found " + availableQueues.size() + " Queues(s) nearby");
                    }
                    queuesRecyclerView.setVisibility(View.VISIBLE);
                    queueAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onScanComplete() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    // Stop refresh animation
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    if (availableQueues.isEmpty()) {
                        statusText.setText("No Queues found nearby");
                    } else {
                        int queueCount = 0;
                        for (AdvertisedQueue q : availableQueues) {
                            if (q.getCurrentNumber() >= 0 && q.getWaitTimeMinutes() >= 0) {
                                queueCount++;
                            }
                        }
                        if (queueCount > 0) {
                            statusText.setText("Found " + availableQueues.size() + " Queues(s) (" + queueCount
                                    + " queue(s)). Tap a queue to join.");
                        } else {
                            statusText
                                    .setText("Found " + availableQueues.size() + " Queues(s) nearby. No queues found.");
                        }
                    }
                });
            }
        });

        // Setup BLE client listener
        bleClientManager.setOnJoinResultListener(new BLEClientManager.OnJoinResultListener() {
            @Override
            public void onJoinSuccess(int ticketNumber) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Successfully joined queue! Your ticket number: #" + ticketNumber,
                            Toast.LENGTH_LONG).show();
                    // Navigate to WaitFragment with arguments
                    Bundle bundle = new Bundle();
                    bundle.putString("DEVICE_ADDRESS", pendingDeviceAddress);
                    bundle.putInt("TICKET_NUMBER", ticketNumber);
                    NavHostFragment.findNavController(HomeFragment.this)
                            .navigate(R.id.action_homeFragment_to_waitFragment, bundle);
                });
            }

            @Override
            public void onJoinFailed(String error) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Failed to join queue: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void startSearch() {
        // Show loading state
        loadingIndicator.setVisibility(View.VISIBLE);
        statusText.setText("Scanning for nearby devices...");
        queuesRecyclerView.setVisibility(View.GONE);
        availableQueues.clear();

        // Start BLE scanning
        if (bleScanner.isBluetoothEnabled()) {
            bleScanner.startScan();

            // Stop scanning after 10 seconds
            new Handler(Looper.getMainLooper()).postDelayed(this::stopScanning, 10000);
        } else {
            loadingIndicator.setVisibility(View.GONE);
            statusText.setText("Bluetooth is not enabled. Please enable Bluetooth and try again.");
            Toast.makeText(getContext(), "Bluetooth is required for queue discovery", Toast.LENGTH_LONG).show();
        }
    }

    private void stopScanning() {
        if (bleScanner != null && bleScanner.isScanning()) {
            bleScanner.stopScan();
        }
    }

    @Override
    public void onQueueClick(AdvertisedQueue queue) {
        showQueueDetailsDialog(queue);
    }

    private void showQueueDetailsDialog(AdvertisedQueue queue) {
        // Check if this is a real queue (-1 indicates generic device)
        boolean isQueue = queue.getCurrentNumber() >= 0 && queue.getWaitTimeMinutes() >= 0;

        String message;
        String title;

        if (isQueue) {
            title = "Join Queue";
            message = "Queue: " + queue.getName() + "\n" +
                    "Currently Serving: #" + queue.getCurrentNumber() + "\n" +
                    "Estimated Wait: " + queue.getWaitTimeMinutes() + " minutes\n" +
                    "You would be #" + (queue.getCurrentNumber() + 1) + " in line\n\n" +
                    "Do you want to join this queue?";
        } else {
            title = "Device Info";
            message = "Device Name: " + queue.getName() + "\n" +
                    "Address: " + queue.getDeviceAddress() + "\n\n" +
                    "This device is not advertising a queue.\n" +
                    "It may not be a VQueue server.";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Cancel", null);

        // Only show join button for real queues
        if (isQueue) {
            builder.setPositiveButton("Join Queue", (dialog, which) -> {
                stopScanning();
                pendingDeviceAddress = queue.getDeviceAddress();
                Toast.makeText(getContext(), "Connecting to queue...", Toast.LENGTH_SHORT).show();
                bleClientManager.connectAndJoin(queue.getDeviceAddress());
            });
        }

        builder.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopScanning();
        if (bleClientManager != null) {
            bleClientManager.disconnect();
        }
    }
}