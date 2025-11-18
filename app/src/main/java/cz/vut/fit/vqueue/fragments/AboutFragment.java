package cz.vut.fit.vqueue.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import cz.vut.fit.vqueue.R;

public class AboutFragment extends Fragment {
    
    private MaterialButton backButton;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        
        initializeViews(view);
        setupClickListeners();
        
        return view;
    }
    
    private void initializeViews(View view) {
        backButton = view.findViewById(R.id.backButton);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });
    }
}
