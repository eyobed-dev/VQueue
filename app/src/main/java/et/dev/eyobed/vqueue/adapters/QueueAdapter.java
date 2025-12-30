package et.dev.eyobed.vqueue.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Random;

import et.dev.eyobed.vqueue.R;
import et.dev.eyobed.vqueue.model.AdvertisedQueue;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private final List<AdvertisedQueue> queues;
    private final OnQueueClickListener listener;

    public interface OnQueueClickListener {
        void onQueueDetailsClick(AdvertisedQueue queue);

        void onQueueJoinClick(AdvertisedQueue queue);
    }

    public QueueAdapter(List<AdvertisedQueue> queues, OnQueueClickListener listener) {
        this.queues = queues;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_queue, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        AdvertisedQueue queue = queues.get(position);
        holder.bind(queue, listener);
    }

    @Override
    public int getItemCount() {
        return queues.size();
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        private final TextView queueName;
        private final TextView queueWaitInfo;
        private final TextView rssi;
        private final View headerBackground;
        private final MaterialButton btnJoin;
        private final MaterialCardView cardView;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            queueName = itemView.findViewById(R.id.queueName);
            queueWaitInfo = itemView.findViewById(R.id.queueWaitInfo);
            cardView = itemView.findViewById(R.id.cardView);
            rssi = itemView.findViewById(R.id.rssi);
            headerBackground = itemView.findViewById(R.id.headerBackground);
            btnJoin = itemView.findViewById(R.id.btnJoin);
        }

        public void bind(final AdvertisedQueue queue, final OnQueueClickListener listener) {
            // Generate a random but deterministic color based on the device address
            Random random = new Random(queue.getDeviceAddress().hashCode());

            // Use HSV to generate attractive, vibrant colors
            float[] hsv = new float[3];
            hsv[0] = random.nextFloat() * 360f;
            hsv[1] = 0.6f + (random.nextFloat() * 0.4f);
            hsv[2] = 0.8f + (random.nextFloat() * 0.2f);

            // Create darker shade for left (start)
            float[] hsvDark = new float[] { hsv[0], hsv[1], hsv[2] * 0.7f };
            int colorStart = Color.HSVToColor(hsvDark);

            // Create lighter shade for right (end)
            float[] hsvLight = new float[] { hsv[0], hsv[1], Math.min(1.0f, hsv[2] * 1.1f) };
            int colorEnd = Color.HSVToColor(hsvLight);

            // Create gradient (Left to Right)
            android.graphics.drawable.GradientDrawable gradient = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[] { colorStart, colorEnd });

            // Apply the gradient to the header background
            if (headerBackground != null) {
                headerBackground.setBackground(gradient);
            }

            if (queue.getName() != null && !queue.getName().isEmpty()) {
                queueName.setText(queue.getName());
            } else {
                queueName.setText("Unknown Device");
            }

            if (rssi != null) {
                rssi.setText("Signal: " + queue.getRssi() + " dBm");
            }

            // Set Details (Wait time or generic info)
            if (queueWaitInfo != null) {
                if (queue.getWaitTimeMinutes() >= 0 && queue.getCurrentNumber() >= 0) {
                    queueWaitInfo.setText(queue.getWaitTimeMinutes() + " min wait • 3 people ahead");
                    queueWaitInfo.setVisibility(View.VISIBLE);
                } else {
                    queueWaitInfo.setVisibility(View.GONE);
                }
            }

            // Set listener on the cardView specifically for Details
            if (cardView != null) {
                cardView.setOnClickListener(v -> listener.onQueueDetailsClick(queue));
            }

            // Set listener on the Join button specifically for Join
            if (btnJoin != null) {
                btnJoin.setOnClickListener(v -> listener.onQueueJoinClick(queue));
            }
        }
    }
}