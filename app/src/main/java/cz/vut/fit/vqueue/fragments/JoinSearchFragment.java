package cz.vut.fit.vqueue.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class JoinSearchFragment extends Fragment implements QueueAdapter.OnQueueClickListener {
    
    private MaterialButton backButton;
    private ProgressBar loadingIndicator;
    private TextView statusText;
    private RecyclerView queuesRecyclerView;
    private QueueAdapter queueAdapter;  // Changed from QueueAdapter to AdvertisedQueueAdapter
    private List<AdvertisedQueue> availableQueues;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_join_search, container, false);
        
        initializeViews(view);
        setupClickListeners();
        setupRecyclerView();
        startSearch(); // Start searching automatically
        
        return view;
    }
    
    private void initializeViews(View view) {
        backButton = view.findViewById(R.id.backButton);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        statusText = view.findViewById(R.id.statusText);
        queuesRecyclerView = view.findViewById(R.id.queuesRecyclerView);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });
    }
    
    private void setupRecyclerView() {
        availableQueues = new ArrayList<>();
        queueAdapter = new QueueAdapter(availableQueues, this);
        queuesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        queuesRecyclerView.setAdapter(queueAdapter);
    }
    
    private void startSearch() {
        // Show loading state
        loadingIndicator.setVisibility(View.VISIBLE);
        statusText.setText("Searching for nearby queues...");
        queuesRecyclerView.setVisibility(View.GONE);
        
        // Simulate BLE scanning (replace with actual BLE implementation)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Simulate finding some queues
            simulateFoundQueues();
        }, 3000); // 3 second delay to simulate scanning
    }
    
    private void simulateFoundQueues() {
        loadingIndicator.setVisibility(View.GONE);
        
       // Simulate finding 2 queues
       availableQueues.clear();
       availableQueues.add(new AdvertisedQueue("queue1", "Coffee Shop", 5, 10, "device1"));
       availableQueues.add(new AdvertisedQueue("queue2", "Pharmacy", 2, 5, "device2"));
        if (availableQueues.isEmpty()) {
            statusText.setText("No queues found nearby");
        } else {
            statusText.setText("Found " + availableQueues.size() + " queues nearby");
            queuesRecyclerView.setVisibility(View.VISIBLE);
            queueAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public void onQueueClick(AdvertisedQueue queue) {
        // Show queue details dialog using AlertDialog
        showQueueDetailsDialog(queue);
    }
    
    private void showQueueDetailsDialog(AdvertisedQueue queue) {
        String message = "Queue: " + queue.getName() + "\n" +
                        "Currently Serving: #" + queue.getCurrentNumber() + "\n" +
                        "Estimated Wait: " + queue.getWaitTimeMinutes() + " minutes\n" +
                        "You would be #" + (queue.getCurrentNumber() + 1) + " in line\n\n" +
                        "Do you want to join this queue?";
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Join Queue")
                .setMessage(message)
                .setPositiveButton("Join Queue", (dialog, which) -> {
                    // User confirmed joining the queue
                    Toast.makeText(getContext(), "Joined " + queue.getName() + "! You are #" + (queue.getCurrentNumber() + 1) + " in line", Toast.LENGTH_LONG).show();
                    
                    // Navigate to WaitFragment
                    WaitFragment waitFragment = new WaitFragment();
                    ((MainActivity) requireActivity()).replaceFragment(waitFragment);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
