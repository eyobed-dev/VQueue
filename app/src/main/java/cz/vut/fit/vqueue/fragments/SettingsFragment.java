package cz.vut.fit.vqueue.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.utils.AppPreferences;

public class SettingsFragment extends Fragment {

    private SwitchMaterial showAllDevicesSwitch;
    private MaterialButton backButton;
    private AppPreferences appPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        initializeViews(view);
        setupClickListeners();
        loadSettings();

        return view;
    }

    private void initializeViews(View view) {
        showAllDevicesSwitch = view.findViewById(R.id.showAllDevicesSwitch);
        backButton = view.findViewById(R.id.backButton);
        appPreferences = new AppPreferences(requireContext());
    }

    private void setupClickListeners() {
        showAllDevicesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appPreferences.setShowAllDevicesEnabled(isChecked);
        });

        backButton.setOnClickListener(v -> {
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigateUp();
        });
    }

    private void loadSettings() {
        showAllDevicesSwitch.setChecked(appPreferences.isShowAllDevicesEnabled());
    }
}
