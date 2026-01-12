package et.dev.eyobed.vqueue.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import et.dev.eyobed.vqueue.R;
import et.dev.eyobed.vqueue.adapters.WaiterAdapter;
import et.dev.eyobed.vqueue.model.Queue;
import et.dev.eyobed.vqueue.repo.QueueRepo;
import et.dev.eyobed.vqueue.service.QueueManagementService;
import et.dev.eyobed.vqueue.utils.Constants;

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
    private TextView queueSizeText;
    private View emptyStateContainer;

    private QueueRepo queueRepo;
    private Queue currentQueue;
    private BroadcastReceiver queueUpdateReceiver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_queue_manager, container, false);

        initializeViews(view);
        setupClickListeners();
        setupBroadcastReceiver();
        loadQueueData();
        updateQueueInfo();

        return view;
    }

    private void initializeViews(View view) {
        queueNameText = view.findViewById(R.id.queueNameText);
        currentNumberText = view.findViewById(R.id.currentNumberText);
        waitTimeText = view.findViewById(R.id.waitTimeText);
        queueSizeText = view.findViewById(R.id.queueSizeText);
        nextButton = view.findViewById(R.id.nextButton);
        manualAddButton = view.findViewById(R.id.manualAddButton);
        pauseButton = view.findViewById(R.id.pauseButton);
        stopButton = view.findViewById(R.id.stopButton);
        waitersRecyclerView = view.findViewById(R.id.waitersRecyclerView);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);

        // Setup RecyclerView
        waitersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        waiterAdapter = new WaiterAdapter(new ArrayList<>());
        waitersRecyclerView.setAdapter(waiterAdapter);

        queueRepo = QueueRepo.getInstance(requireContext());

        // Add menu clear queue 
        requireActivity().addMenuProvider(new androidx.core.view.MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.queue_manager_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull android.view.MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_clear_queue) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.title_clear_queue)
                            .setMessage(R.string.message_clear_queue)
                            .setPositiveButton(R.string.btn_clear, (dialog, which) -> {
                                queueRepo.clearWaiters();
                                updateQueueInfo();
                                Toast.makeText(getContext(), R.string.msg_queue_cleared, Toast.LENGTH_SHORT).show();

                                notifyServiceOfChange();
                            })
                            .setNegativeButton(R.string.btn_cancel, null)
                            .show();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), androidx.lifecycle.Lifecycle.State.RESUMED);
    }

    private void setupBroadcastReceiver() {
        queueUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (et.dev.eyobed.vqueue.utils.Constants.ACTION_QUEUE_UPDATED.equals(intent.getAction())) {
                    // Refresh queue data and update UI
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            currentQueue = queueRepo.getCurrentQueue();
                            updateQueueInfo();
                            Log.i("QueueManager", "Queue updated via broadcast - UI refreshed");
                        });
                    }
                }
            }
        };

        // Register receiver
        if (getContext() != null) {
            IntentFilter filter = new IntentFilter(et.dev.eyobed.vqueue.utils.Constants.ACTION_QUEUE_UPDATED);
            getContext().registerReceiver(queueUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister receiver
        if (queueUpdateReceiver != null && getContext() != null) {
            getContext().unregisterReceiver(queueUpdateReceiver);
        }
    }

    private void loadQueueData() {
        currentQueue = queueRepo.getCurrentQueue();
        if (currentQueue == null) {
            Toast.makeText(getContext(), R.string.error_no_active_queue, Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
        } else {
            sendCommandToService(Constants.ACTION_START_QUEUE);
        }
    }

    private void setupClickListeners() {
        nextButton.setOnClickListener(v -> {
            if (currentQueue != null) {
                int totalWaiters = currentQueue.getWaiters() != null ? currentQueue.getWaiters().size() : 0;
                if (totalWaiters == 0 || currentQueue.getCurrentNumber() >= totalWaiters) {
                    Toast.makeText(getContext(), R.string.msg_no_more_waiters, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Update queue locally first to keep UI in sync
            queueRepo.callNext();
            currentQueue = queueRepo.getCurrentQueue();

            // Notify service to update advertisement only (don't increment again!)
            sendCommandToService(Constants.ACTION_UPDATE_ADVERTISEMENT);

            updateQueueInfo();
            Toast.makeText(getContext(), getString(R.string.msg_called_number, currentQueue.getCurrentNumber()),
                    Toast.LENGTH_SHORT)
                    .show();
        });

        manualAddButton.setOnClickListener(v -> showManualAddDialog());

        pauseButton.setOnClickListener(v -> {
            if (currentQueue.isPaused()) {
                sendCommandToService(Constants.ACTION_RESUME_QUEUE);
                pauseButton.setText(R.string.btn_pause);
                Toast.makeText(getContext(), R.string.msg_queue_resumed, Toast.LENGTH_SHORT).show();
            } else {
                sendCommandToService(Constants.ACTION_PAUSE_QUEUE);
                pauseButton.setText(R.string.btn_resume);
                Toast.makeText(getContext(), R.string.msg_queue_paused, Toast.LENGTH_SHORT).show();
            }
            currentQueue = queueRepo.getCurrentQueue();
        });

        stopButton.setOnClickListener(v -> {
            sendCommandToService(Constants.ACTION_STOP_QUEUE);

            Toast.makeText(getContext(), R.string.msg_queue_stopped, Toast.LENGTH_SHORT).show();
            // Go back to home
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_queueManagerFragment_to_homeFragment);
        });

    }

    private void showManualAddDialog() {
        EditText input = new EditText(getContext());
        input.setHint(R.string.hint_enter_name);
        input.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_add_person)
                .setMessage(R.string.message_add_person_manual)
                .setView(input)
                .setPositiveButton(R.string.btn_add, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        name = getString(R.string.default_waiter_name);
                    }
                    addManualWaiter(name);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void addManualWaiter(String name) {
        if (currentQueue != null) {
            int ticketNumber = queueRepo.addWaiter(name);
            if (ticketNumber > 0) {
                Toast.makeText(getContext(), getString(R.string.msg_added_waiter_format, name, ticketNumber),
                        Toast.LENGTH_SHORT).show();
                updateQueueInfo();

                notifyServiceOfChange();
            } else {
                Toast.makeText(getContext(), R.string.error_failed_add_person, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateQueueInfo() {
        Log.i("QueueManager", "updateQueueInfo called");

        if (currentQueue != null) {
            queueNameText.setText(currentQueue.getName());
            currentNumberText.setText(String.valueOf(currentQueue.getCurrentNumber()));
            waitTimeText.setText(getString(R.string.wait_time_format, currentQueue.getWaitTimeMinutes()));

            // Update Queue Count
            int totalWaiters = currentQueue.getWaiters() != null ? currentQueue.getWaiters().size() : 0;

            queueSizeText.setText(String.valueOf(totalWaiters));

            Log.i("QueueManager", "Current number: " + currentQueue.getCurrentNumber());

            // Update pause button state
            if (pauseButton != null) {
                if (currentQueue.isPaused()) {
                    pauseButton.setText(R.string.btn_resume);
                } else {
                    pauseButton.setText(R.string.btn_pause);
                }
            }

            // Update waiters list
            if (waiterAdapter != null && currentQueue.getWaiters() != null) {
                Log.i("QueueManager",
                        "Updating adapter with " + currentQueue.getWaiters().size() + " waiters");
                waiterAdapter.updateData(currentQueue.getWaiters(), currentQueue.getCurrentNumber());

                if (currentQueue.getWaiters().isEmpty()) {
                    waitersRecyclerView.setVisibility(View.GONE);
                    emptyStateContainer.setVisibility(View.VISIBLE);
                    Log.i("QueueManager", "Queue is empty");
                } else {
                    waitersRecyclerView.setVisibility(View.VISIBLE);
                    emptyStateContainer.setVisibility(View.GONE);
                    Log.i("QueueManager", "Queue has waiters, calling scrollToCurrentPerson");

                    // Auto-scroll to currently served person
                    scrollToCurrentPerson();
                }
            } else {
                Log.w("QueueManager", "Adapter or waiters is null");
            }
        } else {
            Log.w("QueueManager", "currentQueue is null");
        }
    }

//scroll to last guy in line
    private void scrollToCurrentPerson() {
        if (currentQueue != null && currentQueue.getWaiters() != null && !currentQueue.getWaiters().isEmpty()) {
            int currentNumber = currentQueue.getCurrentNumber();

            // Find the position of the currently served person
            for (int i = 0; i < currentQueue.getWaiters().size(); i++) {
                if (currentQueue.getWaiters().get(i).getTicketNumber() == currentNumber) {
                    final int position = i;
                    Log.i("QueueManager",
                            "Scrolling to position " + position + " (ticket #" + currentNumber + ")");

                    // Post to ensure RecyclerView layout is complete
                    waitersRecyclerView.post(() -> {
                        waitersRecyclerView.smoothScrollToPosition(position);
                        Log.i("QueueManager", "Smooth scroll triggered");
                    });
                    break;
                }
            }
        }
    }

    private void notifyServiceOfChange() {
        sendCommandToService(Constants.ACTION_UPDATE_ADVERTISEMENT);
    }

    private void sendCommandToService(String action) {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), QueueManagementService.class);
            intent.setAction(action);
            getContext().startService(intent);
        }
    }
}
