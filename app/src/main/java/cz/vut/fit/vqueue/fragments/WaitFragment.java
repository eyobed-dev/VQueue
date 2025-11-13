package cz.vut.fit.vqueue.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.activities.MainActivity;

/**
 * Waiting screen shown to a client after joining a queue.
 *
 * Behavior:
 * - Blocks the system back button: user must explicitly leave the queue.
 * - "Leave queue" brings the user back to the home screen (JoinSearchFragment)
 *   and will later be responsible for cleaning up the BLE client connection.
 *
 * Currently, this fragment only shows static UI and mocks the leave behavior.
 * Real-time updates will be wired to QueueConnectionService + ViewModel later.
 */
public class WaitFragment extends Fragment {

    private MaterialButton leaveButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wait, container, false);

        // Intercept the system back button while this fragment is visible.
        // The user should not be able to silently "back out" of the queue.
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                Toast.makeText(
                                        getContext(),
                                        "You must leave the queue first",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });

        // In older versions there was a "backButton" in the layout.
        // If it still exists, hide it to force using "Leave queue".
        View backHome = view.findViewById(R.id.backButton);
        if (backHome != null) {
            backHome.setVisibility(View.GONE);
        }

        // Main action: explicit leave button.
        // Layout id: @+id/leaveQueueButton
        leaveButton = view.findViewById(R.id.leaveQueueButton);
        leaveButton.setOnClickListener(v -> {
            // TODO: add real BLE client teardown / unsubscribe / disconnect here.
            Toast.makeText(getContext(), "You left the queue", Toast.LENGTH_SHORT).show();
            ((MainActivity) requireActivity()).resetToHome();
        });

        return view;
    }
}
