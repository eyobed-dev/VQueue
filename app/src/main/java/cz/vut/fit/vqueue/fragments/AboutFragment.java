package cz.vut.fit.vqueue.fragments;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.appbar.MaterialToolbar;
import cz.vut.fit.vqueue.R;

public class AboutFragment extends Fragment {

    private TextView versionText;
    private TextView gitlabLink;
    private TextView emailLink;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        initializeViews(view);
        setupClickListeners();
        setAppVersion();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void initializeViews(View view) {
        versionText = view.findViewById(R.id.versionText);
        gitlabLink = view.findViewById(R.id.gitlabLink);
        emailLink = view.findViewById(R.id.emailLink);
    }

    private void setupClickListeners() {
        gitlabLink.setOnClickListener(v -> {
            // Replace with your actual gitlab repository URL
            String url = "https://gitlab.com/your-username/your-repo";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        emailLink.setOnClickListener(v -> {
            // Replace with your actual contact email
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:your.email@example.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback on VQueue App");
            startActivity(intent);
        });
    }

    /**
     * Dynamically gets the app version from package info and sets it to the
     * TextView.
     */
    private void setAppVersion() {
        try {
            PackageInfo pInfo = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(),
                    0);
            String version = pInfo.versionName;
            versionText.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            versionText.setText("Version 1.0.0"); // Fallback
        }
    }
}
