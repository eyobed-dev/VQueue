package cz.vut.fit.vqueue.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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
                .inflate(R.layout.item_queue_circle, parent, false);
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
        private final View cardView;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            queueName = itemView.findViewById(R.id.queueName);
            queueWaitInfo = itemView.findViewById(R.id.queueWaitInfo);
            cardView = itemView.findViewById(R.id.cardView);
        }

        public void bind(final AdvertisedQueue queue, final OnQueueClickListener listener) {
            // Set Device/Queue Name
            if (queue.getName() != null && !queue.getName().isEmpty()) {
                queueName.setText(queue.getName());
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