package et.dev.eyobed.vqueue.fragments;

import android.app.Activity;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private TextView statusText;
    private RecyclerView queuesRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyStateContainer;

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
        setHasOptionsMenu(true); // this is to show the menu. deprecated but i don't care
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

        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    private void initializeViews(View view) {
        fabCreateQueue = view.findViewById(R.id.fabCreateQueue);
        statusText = view.findViewById(R.id.statusText);
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
                // Use Navigation component instead of manual fragment transactions
                NavHostFragment.findNavController(this).navigate(R.id.action_homeFragment_to_queueManagerFragment);
            });
            bottomSheet.show(getChildFragmentManager(), "CreateQueueBottomSheet");
        });
    }

    private void setupRecyclerView() {
        availableQueues = new ArrayList<>();
        queueAdapter = new QueueAdapter(this);

        queuesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        queuesRecyclerView.setAdapter(queueAdapter);

        // Setup BLE scanner listener
        bleScanner.setOnQueueFoundListener(new BLEScanner.OnQueueFoundListener() {
            @Override
            public void onQueueFound(AdvertisedQueue queue) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (getView() == null) return;

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

                        // Sort the list by RSSI (descending) so closest devices are first
                        Collections.sort(availableQueues, new Comparator<AdvertisedQueue>() {
                            @Override
                            public int compare(AdvertisedQueue q1, AdvertisedQueue q2) {
                                return Integer.compare(q2.getRssi(), q1.getRssi());
                            }
                        });

                        updateStatusText();
                        queuesRecyclerView.setVisibility(View.VISIBLE);
                        emptyStateContainer.setVisibility(View.GONE);
                        queueAdapter.submitList(new ArrayList<>(availableQueues));
                    });
                }
            }

    @Override
    public void onScanComplete() {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (getView() != null) {
                            swipeRefreshLayout.setRefreshing(false);
                            updateStatusText();

                            if (availableQueues.isEmpty()) {
                                queuesRecyclerView.setVisibility(View.GONE);
                                emptyStateContainer.setVisibility(View.VISIBLE);
                            } else {
                                emptyStateContainer.setVisibility(View.GONE);
                                queuesRecyclerView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                }
            }
        });

        // Setup BLE client listener
        bleClientManager.setOnJoinResultListener(new BLEClientManager.OnJoinResultListener() {
            @Override
            public void onJoinSuccess(int ticketNumber) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (getView() == null)
                            return;

                        Toast.makeText(getContext(), getString(R.string.join_success, ticketNumber),
                                Toast.LENGTH_SHORT).show();

                        // Navigate using the Navigation Component
                        Bundle bundle = new Bundle();
                        bundle.putString("QUEUE_NAME", pendingQueueName);
                        bundle.putInt("TICKET_NUMBER", ticketNumber);

                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.action_homeFragment_to_waitFragment, bundle);
                    });
                }
            }

            @Override
            public void onJoinFailed(String error) {
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (getView() == null)
                            return;
                        Toast.makeText(getContext(), getString(R.string.join_failed, error), Toast.LENGTH_SHORT).show();
                    });
                }
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

        statusText.setText(R.string.scanning_for_queues);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        queuesRecyclerView.setVisibility(View.GONE);
        if (emptyStateContainer != null)
            emptyStateContainer.setVisibility(View.GONE);
        availableQueues.clear();
        if (queueAdapter != null) {
            queueAdapter.submitList(new ArrayList<>());
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
            statusText.setText(R.string.bluetooth_disabled);
            Toast.makeText(getContext(), R.string.bluetooth_required_discovery, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatusText() {
        if (availableQueues.isEmpty()) {
            statusText.setText(R.string.no_queues_nearby);
            return;
        }

        int queueCount = 0;
        for (AdvertisedQueue q : availableQueues) {
            if (q.getCurrentNumber() >= 0 && q.getWaitTimeMinutes() >= 0) {
                queueCount++;
            }
        }

        if (queueCount > 0) {
            String summary = getResources().getQuantityString(
                    R.plurals.found_queues_summary,
                    availableQueues.size(),
                    availableQueues.size(),
                    queueCount);
            statusText.setText(summary);
        } else {
            statusText.setText(getString(R.string.found_nearby_no_valid, availableQueues.size()));
        }
        statusText.setVisibility(View.VISIBLE);
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
        handleQueueSelection(queue);
    }

    @Override
    public void onQueueJoinClick(AdvertisedQueue queue) {
        handleQueueSelection(queue);
    }

    private void handleQueueSelection(AdvertisedQueue queue) {
        if (!queue.isQueue()) {
            Toast.makeText(requireContext(), R.string.message_device_info, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_join_queue)
                .setMessage(getString(R.string.message_join_confirm, queue.getName()))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.btn_join, (dialog, which) -> connectToQueue(queue))
                .show();
    }

    private void connectToQueue(AdvertisedQueue queue) {
        Activity activity = getActivity();
        if (activity == null)
            return;

        stopScanning();
        pendingDeviceAddress = queue.getDeviceAddress();
        pendingQueueName = queue.getName();

        Toast.makeText(activity, R.string.connecting_to_queue, Toast.LENGTH_SHORT).show();
        bleClientManager.connectAndJoin(queue.getDeviceAddress());
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