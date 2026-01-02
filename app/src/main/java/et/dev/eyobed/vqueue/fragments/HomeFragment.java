package et.dev.eyobed.vqueue.fragments;

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

import et.dev.eyobed.vqueue.R;
import et.dev.eyobed.vqueue.adapters.QueueAdapter;
import et.dev.eyobed.vqueue.model.AdvertisedQueue;
import et.dev.eyobed.vqueue.service.BLEClientManager;
import et.dev.eyobed.vqueue.service.BLEScanner;

public class HomeFragment extends Fragment implements QueueAdapter.OnQueueClickListener {

    // UI Components
    private FloatingActionButton fabCreateQueue;
    private ProgressBar loadingIndicator;
    private RecyclerView queuesRecyclerView;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private View emptyStateContainer;
    private TextView headerTitle;

    // Data & Logic
    private QueueAdapter queueAdapter;
    private List<AdvertisedQueue> availableQueues;
    private BLEScanner bleScanner;
    private BLEClientManager bleClientManager;
    private String pendingDeviceAddress;
    private String pendingQueueName;


    // Scanning Management
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable scanTimeoutRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setHasOptionsMenu(true); // Keep the menu (Settings/About) logic
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the updated layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);

        headerTitle = view.findViewById(R.id.headerTitle);
        if (headerTitle != null) {
            headerTitle.setText("Available Queues");
        }

        View menuButton = view.findViewById(R.id.menuButton);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
                popup.show();
            });
        }

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
        queuesRecyclerView = view.findViewById(R.id.queuesRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);

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
                R.color.primary_variant);
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

                // Sort the list by RSSI (Received Signal Strength Indicator) in descending
                // order.
                // This ensures that the queues with the strongest signal (likely the closest
                // ones)
                // appear at the top of the list.
                Collections.sort(availableQueues, new Comparator<AdvertisedQueue>() {
                    @Override
                    public int compare(AdvertisedQueue q1, AdvertisedQueue q2) {
                        // A higher RSSI value means a stronger signal.
                        return Integer.compare(q2.getRssi(), q1.getRssi());
                    }
                });
            }

            @Override
            public void onScanComplete() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    // Stop refresh animation
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    if (availableQueues.isEmpty()) {

                        // statusText.setVisibility(View.GONE); // Optional: Hide text if we rely on
                        // illustration

                        queuesRecyclerView.setVisibility(View.GONE);
                        emptyStateContainer.setVisibility(View.VISIBLE);
                    } else {
                        emptyStateContainer.setVisibility(View.GONE);
                        queuesRecyclerView.setVisibility(View.VISIBLE);


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
                            Toast.LENGTH_SHORT).show();
                    // Navigate to WaitFragment with arguments
                    Bundle bundle = new Bundle();
                    bundle.putString("QUEUE_NAME", pendingQueueName); // Pass queue name for monitoring
                    bundle.putInt("TICKET_NUMBER", ticketNumber);
                    NavHostFragment.findNavController(HomeFragment.this)
                            .navigate(R.id.action_homeFragment_to_waitFragment, bundle);
                });
            }

            @Override
            public void onJoinFailed(String error) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getContext(), "Failed to join queue: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void startSearch() {
        // Cancel any pending scan timeout
        if (scanTimeoutRunnable != null) {
            mainHandler.removeCallbacks(scanTimeoutRunnable);
        }

        // Stop any existing scan first to reset state
        stopScanning();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        queuesRecyclerView.setVisibility(View.GONE);
        if (emptyStateContainer != null)
            emptyStateContainer.setVisibility(View.GONE);
        availableQueues.clear();
        if (queueAdapter != null) {
            queueAdapter.notifyDataSetChanged();
        }

        // Start BLE scanning
        if (bleScanner.isBluetoothEnabled()) {
            bleScanner.startScan();

            // Schedule stop scanning after 10 seconds
            scanTimeoutRunnable = this::stopScanning;
            mainHandler.postDelayed(scanTimeoutRunnable, 10000);
        } else {
            loadingIndicator.setVisibility(View.GONE);
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            Toast.makeText(getContext(), "Bluetooth is required for queue discovery", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopScanning() {
        if (scanTimeoutRunnable != null) {
            mainHandler.removeCallbacks(scanTimeoutRunnable);
        }
        if (bleScanner != null && bleScanner.isScanning()) {
            bleScanner.stopScan();
        }
    }

    @Override
    public void onQueueDetailsClick(AdvertisedQueue queue) {
        showQueueDetailsDialog(queue);
    }

    @Override
    public void onQueueJoinClick(AdvertisedQueue queue) {
        showJoinConfirmationDialog(queue);
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
                pendingQueueName = queue.getName(); // Store queue name for monitoring
                Toast.makeText(getContext(), "Connecting to queue...", Toast.LENGTH_SHORT).show();
                bleClientManager.connectAndJoin(queue.getDeviceAddress());
            });
        }

        builder.show();
    }

    private void showJoinConfirmationDialog(AdvertisedQueue queue) {
        // Check if this is a real queue (-1 indicates generic device)
        boolean isQueue = queue.getCurrentNumber() >= 0 && queue.getWaitTimeMinutes() >= 0;

        if (!isQueue) {
            Toast.makeText(requireContext(), "This device is not advertising a queue.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Join Queue")
                .setMessage("Do you want to join " + queue.getName() + "?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Join", (dialog, which) -> {
                    stopScanning();
                    pendingDeviceAddress = queue.getDeviceAddress();
                    pendingQueueName = queue.getName();
                    Toast.makeText(getContext(), "Connecting to queue...", Toast.LENGTH_SHORT).show();
                    bleClientManager.connectAndJoin(queue.getDeviceAddress());
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scanTimeoutRunnable != null) {
            mainHandler.removeCallbacks(scanTimeoutRunnable);
        }
        stopScanning();
        if (bleClientManager != null) {
            bleClientManager.disconnect();
        }
    }
}