package cz.vut.fit.vqueue.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.activities.MainActivity;
import cz.vut.fit.vqueue.model.Queue;
import cz.vut.fit.vqueue.repo.QueueRepo;

/**
 * Screen where the "host" user creates a new queue.
 * - Collects queue name and average wait time.
 * - Creates a Queue instance via QueueRepo.
 * - Navigates to QueueManagerFragment to manage the active queue.
 */
public class CreateQueueFragment extends Fragment {

    private TextInputEditText queueNameEditText;
    private TextInputEditText waitTimeEditText;
    private MaterialButton createQueueButton;
    private MaterialButton backButton;

    // Central repository managing queue data on the host side.
    private QueueRepo queueRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_queue, container, false);

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    /**
     * Binds views from the layout and grabs the shared QueueRepo instance.
     */
    private void initializeViews(View view) {
        queueNameEditText = view.findViewById(R.id.queueNameEditText);
        waitTimeEditText = view.findViewById(R.id.waitTimeEditText);
        createQueueButton = view.findViewById(R.id.createQueueButton);
        backButton = view.findViewById(R.id.backButton);
        queueRepo = QueueRepo.getInstance();
    }

    /**
     * Sets up button behavior:
     * - "Create" validates input and creates a new queue.
     * - "Back" simply returns to the previous screen.
     */
    private void setupClickListeners() {
        createQueueButton.setOnClickListener(v -> {
            String queueName = queueNameEditText.getText() == null
                    ? ""
                    : queueNameEditText.getText().toString().trim();
            String waitTimeStr = waitTimeEditText.getText() == null
                    ? ""
                    : waitTimeEditText.getText().toString().trim();

            if (queueName.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a queue name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (waitTimeStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter wait time", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int waitTime = Integer.parseInt(waitTimeStr);
                if (waitTime <= 0) {
                    Toast.makeText(
                            getContext(),
                            "Wait time must be greater than 0",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                // Create and store a new queue in the repository.
                // QueueManagerFragment will read the current queue from QueueRepo.
                Queue queue = queueRepo.createQueue(queueName, waitTime);

                // Navigate to the manager screen for the active queue.
                QueueManagerFragment queueManagerFragment = new QueueManagerFragment();
                ((MainActivity) requireActivity()).replaceFragment(queueManagerFragment);

            } catch (NumberFormatException e) {
                Toast.makeText(
                        getContext(),
                        "Please enter a valid number for wait time",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        backButton.setOnClickListener(v -> {
            // Go back to the previous fragment (usually JoinSearchFragment).
            requireActivity().onBackPressed();
        });
    }
}
