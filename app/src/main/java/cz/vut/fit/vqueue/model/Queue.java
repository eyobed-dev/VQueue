package cz.vut.fit.vqueue.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Queue {
    private String id;
    private String name;
    private int waitTimeMinutes;
    private int currentNumber;
    private QueueStatus status;
    private String deviceId;
    private List<Waiter> waiters;
    private long createdAt;
    
    public enum QueueStatus {
        ACTIVE, PAUSED, STOPPED
    }
    
    public Queue(String name, int waitTimeMinutes) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.waitTimeMinutes = waitTimeMinutes;
        this.currentNumber = 0;
        this.status = QueueStatus.ACTIVE;
        this.deviceId = android.os.Build.MODEL; // Use device model as ID
        this.waiters = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getWaitTimeMinutes() { return waitTimeMinutes; }
    public int getCurrentNumber() { return currentNumber; }
    public QueueStatus getStatus() { return status; }
    public String getDeviceId() { return deviceId; }
    public List<Waiter> getWaiters() { return waiters; }
    public long getCreatedAt() { return createdAt; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setWaitTimeMinutes(int waitTimeMinutes) { this.waitTimeMinutes = waitTimeMinutes; }
    public void setCurrentNumber(int currentNumber) { this.currentNumber = currentNumber; }
    public void setStatus(QueueStatus status) { this.status = status; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    // Helper methods
    public int getNextTicketNumber() {
        return waiters.size() + 1;
    }
    
    public void addWaiter(Waiter waiter) {
        waiters.add(waiter);
    }
    
    public int getWaitingCount() {
        return waiters.size();
    }
    
    public boolean isActive() {
        return status == QueueStatus.ACTIVE;
    }
    
    public boolean isPaused() {
        return status == QueueStatus.PAUSED;
    }
    
    public boolean isStopped() {
        return status == QueueStatus.STOPPED;
    }
}
