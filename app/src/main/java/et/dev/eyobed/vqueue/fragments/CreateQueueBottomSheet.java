package et.dev.eyobed.vqueue.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import et.dev.eyobed.vqueue.R;
import et.dev.eyobed.vqueue.model.Queue;
import et.dev.eyobed.vqueue.repo.QueueRepo;

public class CreateQueueBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText queueNameEditText;
    private TextInputEditText waitTimeEditText;
    private MaterialButton createQueueButton;
    private QueueRepo queueRepo;
    private OnQueueCreatedListener listener;

    public interface OnQueueCreatedListener {
        void onQueueCreated(Queue queue);
    }

    public void setOnQueueCreatedListener(OnQueueCreatedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_queue_bottom_sheet, container, false);

        initializeViews(view);
        setupClickListeners();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make the bottom sheet resize when keyboard appears
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void initializeViews(View view) {
        queueNameEditText = view.findViewById(R.id.queueNameEditText);
        waitTimeEditText = view.findViewById(R.id.waitTimeEditText);
        createQueueButton = view.findViewById(R.id.createQueueButton);
        queueRepo = QueueRepo.getInstance(requireContext());
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

                // Notify listener
                if (listener != null) {
                    listener.onQueueCreated(queue);
                }

                // Dismiss bottom sheet
                dismiss();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number for wait time", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
