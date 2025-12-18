package cz.vut.fit.vqueue.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Random;

import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.model.AdvertisedQueue;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private final List<AdvertisedQueue> queues;
    private final OnQueueClickListener listener;

    public interface OnQueueClickListener {
        void onQueueClick(AdvertisedQueue queue);
    }

    public QueueAdapter(List<AdvertisedQueue> queues, OnQueueClickListener listener) {
        this.queues = queues;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the new circular layout
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
        private final MaterialCardView cardView;
        private final View colorCircle;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            queueName = itemView.findViewById(R.id.queueName);
            queueWaitInfo = itemView.findViewById(R.id.queueWaitInfo);
            cardView = itemView.findViewById(R.id.cardView);
            rssi = itemView.findViewById(R.id.rssi);
            colorCircle = itemView.findViewById(R.id.colorCircle);
        }

        public void bind(final AdvertisedQueue queue, final OnQueueClickListener listener) {
            // Generate a random but deterministic color based on the device address
            // This ensures the color stays the same for the specific queue but is
            // unique-ish
            Random random = new Random(queue.getDeviceAddress().hashCode());

            // Use HSV to generate attractive, vibrant colors (avoiding murky browns/grays)
            // Hue: Random 0-360
            // Saturation: 0.6 - 1.0 (Vibrant)
            // Value: 0.8 - 1.0 (Bright)
            float[] hsv = new float[3];
            hsv[0] = random.nextFloat() * 360f;
            hsv[1] = 0.6f + (random.nextFloat() * 0.4f);
            hsv[2] = 0.8f + (random.nextFloat() * 0.2f);

            int selectedColor = Color.HSVToColor(hsv);

            // Apply the color to the circle
            colorCircle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selectedColor));

            // Set text colors to black for better contrast on white card
            queueName.setTextColor(Color.BLACK);
            queueWaitInfo.setTextColor(Color.BLACK);
            rssi.setTextColor(Color.BLACK);
            // Set Device/Queue Name
            if (queue.getName() != null && !queue.getName().isEmpty()) {
                queueName.setText(queue.getName());
                rssi.setText(queue.getRssi() + " dBm");
            } else {
                queueName.setText("Unknown Device");
            }

            // Set Details (Wait time or generic info)
            if (queue.getWaitTimeMinutes() >= 0 && queue.getCurrentNumber() >= 0) {
                queueWaitInfo.setText(queue.getWaitTimeMinutes() + " min wait");
                queueWaitInfo.setVisibility(View.VISIBLE);
            } else {
                // If it's just a device not fully advertising queue stats yet
                queueWaitInfo.setVisibility(View.GONE);
            }

            // FIX: Set listener on the cardView specifically.
            // Because the CardView is clickable (for the ripple), it intercepts
            // clicks before they reach itemView.
            cardView.setOnClickListener(v -> listener.onQueueClick(queue));
        }
    }
}