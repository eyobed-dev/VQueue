package cz.vut.fit.vqueue.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.activities.MainActivity;

public class HomeFragment extends Fragment {
    
    private View createQueueArea;
    private View joinQueueArea;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initializeViews(view);
        setupClickListeners();
        
        return view;
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.home_menu, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.menu_settings) {
            SettingsFragment settingsFragment = new SettingsFragment();
            ((MainActivity) requireActivity()).replaceFragment(settingsFragment);
            return true;
        } else if (id == R.id.menu_about) {
            AboutFragment aboutFragment = new AboutFragment();
            ((MainActivity) requireActivity()).replaceFragment(aboutFragment);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void initializeViews(View view) {
        createQueueArea = view.findViewById(R.id.createQueueArea);
        joinQueueArea = view.findViewById(R.id.joinQueueArea);
    }
    
    private void setupClickListeners() {
        createQueueArea.setOnClickListener(v -> {
            CreateQueueFragment createQueueFragment = new CreateQueueFragment();
            ((MainActivity) requireActivity()).replaceFragment(createQueueFragment);
        });
        
        joinQueueArea.setOnClickListener(v -> {
            JoinSearchFragment joinSearchFragment = new JoinSearchFragment();
            ((MainActivity) requireActivity()).replaceFragment(joinSearchFragment);
        });
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        ((AppCompatActivity)getActivity()).getSupportActionBar().hide();
//    }
//    @Override
//    public void onStop() {
//        super.onStop();
//        ((AppCompatActivity)getActivity()).getSupportActionBar().show();
//    }

}
