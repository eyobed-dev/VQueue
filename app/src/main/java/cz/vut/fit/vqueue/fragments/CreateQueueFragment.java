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

public class CreateQueueFragment extends Fragment {

    private TextInputEditText queueNameEditText;
    private TextInputEditText waitTimeEditText;
    private MaterialButton createQueueButton;
    private QueueRepo queueRepo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_queue, container, false);

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        queueNameEditText = view.findViewById(R.id.queueNameEditText);
        waitTimeEditText = view.findViewById(R.id.waitTimeEditText);
        createQueueButton = view.findViewById(R.id.createQueueButton);
        queueRepo = QueueRepo.getInstance();
    }

    private void setupClickListeners() {
        createQueueButton.setOnClickListener(v -> {
            String queueName = queueNameEditText.getText().toString().trim();
            String waitTimeStr = waitTimeEditText.getText().toString().trim();

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
                    Toast.makeText(getContext(), "Wait time must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create queue using QueueRepository
                Queue queue = queueRepo.createQueue(queueName, waitTime);

                // Navigate to QueueManagerFragment
                // Navigate to QueueManagerFragment
                androidx.navigation.fragment.NavHostFragment.findNavController(CreateQueueFragment.this)
                        .navigate(R.id.action_createQueueFragment_to_queueManagerFragment);

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number for wait time", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
