package et.dev.eyobed.vqueue.fragments;

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

import et.dev.eyobed.vqueue.R;

public class AboutFragment extends Fragment {

    private TextView versionText;
    private TextView privacyPolicyLink;
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
        setupClickListeners();
        setAppVersion();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void initializeViews(View view) {
        versionText = view.findViewById(R.id.versionText);
        privacyPolicyLink = view.findViewById(R.id.privacyPolicyLink);
        emailLink = view.findViewById(R.id.emailLink);
    }

    private void setupClickListeners() {
        privacyPolicyLink.setOnClickListener(v -> {
            String url = "https://www.eyobed.dev.et/VQueue/privacy";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        emailLink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:xnuriey00@fit.vut.cz"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback on VQueue App");
            startActivity(intent);
        });
    }

    private void setAppVersion() {
        try {
            PackageInfo pInfo = requireActivity().getPackageManager().getPackageInfo(requireActivity().getPackageName(),
                    0);
            String version = pInfo.versionName;
            versionText.setText("Version " + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            versionText.setText("Version 1.0.0");
        }
    }
}
