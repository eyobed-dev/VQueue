package cz.vut.fit.vqueue.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import cz.vut.fit.vqueue.R;
import cz.vut.fit.vqueue.model.AdvertisedQueue;
import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> { // Changed to QueueAdapter

    private List<AdvertisedQueue> queues;
    private OnQueueClickListener listener;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        AdvertisedQueue queue = queues.get(position);
        holder.bind(queue);
    }

    @Override
    public int getItemCount() {
        return queues.size();
    }

    class QueueViewHolder extends RecyclerView.ViewHolder {
        private TextView queueNameText;
        private TextView currentNumberText;
        private TextView waitTimeText;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            queueNameText = itemView.findViewById(R.id.queueNameText);
            currentNumberText = itemView.findViewById(R.id.currentNumberText);
            waitTimeText = itemView.findViewById(R.id.waitTimeText);
        }

        public void bind(AdvertisedQueue queue) {
            queueNameText.setText(queue.getName());
            
            // Check if this is a real queue (-1 indicates generic device)
            boolean isQueue = queue.getCurrentNumber() >= 0 && queue.getWaitTimeMinutes() >= 0;
            
            if (isQueue) {
                // Real queue - show queue info
                currentNumberText.setText("Currently: #" + queue.getCurrentNumber());
                waitTimeText.setText("Wait: " + queue.getWaitTimeMinutes() + " min");
            } else {
                // Generic BLE device - show device info
                currentNumberText.setText("BLE Device");
                waitTimeText.setText(queue.getDeviceAddress());
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQueueClick(queue);
                }
            });
        }
    }
}