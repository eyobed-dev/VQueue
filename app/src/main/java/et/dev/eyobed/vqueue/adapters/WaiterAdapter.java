package et.dev.eyobed.vqueue.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import et.dev.eyobed.vqueue.R;
import et.dev.eyobed.vqueue.model.Waiter;

public class WaiterAdapter extends RecyclerView.Adapter<WaiterAdapter.WaiterViewHolder> {

    private List<Waiter> waiters;
    private int currentServingNumber;

    public WaiterAdapter(List<Waiter> waiters) {
        this.waiters = waiters;
    }

    public void updateData(List<Waiter> newWaiters, int currentServingNumber) {
        this.waiters = newWaiters;
        this.currentServingNumber = currentServingNumber;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WaiterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_waiter, parent, false);
        return new WaiterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaiterViewHolder holder, int position) {
        Waiter waiter = waiters.get(position);
        holder.bind(waiter);
    }

    @Override
    public int getItemCount() {
        return waiters.size();
    }

    class WaiterViewHolder extends RecyclerView.ViewHolder {
        private TextView ticketNumberText;
        private TextView deviceIdText;
        private TextView statusText;
        private ImageView statusIcon;

        public WaiterViewHolder(@NonNull View itemView) {
            super(itemView);
            ticketNumberText = itemView.findViewById(R.id.ticketNumberText);
            deviceIdText = itemView.findViewById(R.id.deviceIdText);
            statusText = itemView.findViewById(R.id.statusText);
            statusIcon = itemView.findViewById(R.id.statusIcon);
        }

        public void bind(Waiter waiter) {
            ticketNumberText.setText("#" + waiter.getTicketNumber());
            deviceIdText.setText(waiter.getDeviceId());

            if (waiter.getTicketNumber() < currentServingNumber) {
                statusText.setText("Served");
                statusText.setTextColor(itemView.getContext().getResources().getColor(R.color.text_secondary));
                statusIcon.setColorFilter(itemView.getContext().getResources().getColor(R.color.text_secondary));
            } else if (waiter.getTicketNumber() == currentServingNumber) {
                statusText.setText("Currently Serving");
                statusText.setTextColor(itemView.getContext().getResources().getColor(R.color.functional_green));
                statusIcon.setColorFilter(itemView.getContext().getResources().getColor(R.color.functional_green));
            } else {
                statusText.setText("Waiting");
                statusText.setTextColor(itemView.getContext().getResources().getColor(R.color.functional_orange));
                statusIcon.setColorFilter(itemView.getContext().getResources().getColor(R.color.functional_orange));
            }
        }
    }
}
