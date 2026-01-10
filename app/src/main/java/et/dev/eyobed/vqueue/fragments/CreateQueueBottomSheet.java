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
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_create_queue_bottom_sheet,
                container,
                false);

        queueNameEditText = view.findViewById(R.id.queueNameEditText);
        waitTimeEditText = view.findViewById(R.id.waitTimeEditText);
        createQueueButton = view.findViewById(R.id.createQueueButton);
        queueRepo = QueueRepo.getInstance(requireContext());

        createQueueButton.setOnClickListener(v -> createQueue());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void createQueue() {
        String name = textOf(queueNameEditText);
        String waitStr = textOf(waitTimeEditText);

        if (name.isEmpty()) {
            toast(R.string.error_enter_queue_name);
            return;
        }

        int waitTime;
        try {
            waitTime = Integer.parseInt(waitStr);
            if (waitTime <= 0) {
                toast(R.string.error_wait_time_positive);
                return;
            }
        } catch (NumberFormatException e) {
            toast(R.string.error_invalid_wait_time);
            return;
        }

        Queue queue = queueRepo.createQueue(name, waitTime);

        if (listener != null) {
            listener.onQueueCreated(queue);
        }

        dismiss();
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null
                ? ""
                : editText.getText().toString().trim();
    }

    private void toast(int resId) {
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
    }
}
