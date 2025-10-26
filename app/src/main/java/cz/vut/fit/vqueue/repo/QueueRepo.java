package cz.vut.fit.vqueue.repo;

import cz.vut.fit.vqueue.model.Queue;
import cz.vut.fit.vqueue.model.Waiter;

public class QueueRepo {
    private static QueueRepo instance;
    private Queue currentQueue;
    
    private QueueRepo() {
        // Private constructor for singleton
    }
    
    public static synchronized QueueRepo getInstance() {
        if (instance == null) {
            instance = new QueueRepo();
        }
        return instance;
    }
    
    public Queue createQueue(String name, int waitTimeMinutes) {
        currentQueue = new Queue(name, waitTimeMinutes);
        return currentQueue;
    }
    
    public Queue getCurrentQueue() {
        return currentQueue;
    }
    
    public boolean hasActiveQueue() {
        return currentQueue != null && currentQueue.isActive();
    }
    
    public void updateCurrentNumber(int newNumber) {
        if (currentQueue != null) {
            currentQueue.setCurrentNumber(newNumber);
        }
    }
    
    public int addWaiter(String deviceId) {
        if (currentQueue != null) {
            int ticketNumber = currentQueue.getNextTicketNumber();
            Waiter waiter = new Waiter(ticketNumber, deviceId);
            currentQueue.addWaiter(waiter);
            return ticketNumber;
        }
        return -1; // Error: no active queue
    }
    
    public void updateQueueStatus(Queue.QueueStatus status) {
        if (currentQueue != null) {
            currentQueue.setStatus(status);
        }
    }
    
    public void callNext() {
        if (currentQueue != null && currentQueue.isActive()) {
            currentQueue.setCurrentNumber(currentQueue.getCurrentNumber() + 1);
        }
    }
    
    public void pauseQueue() {
        if (currentQueue != null) {
            currentQueue.setStatus(Queue.QueueStatus.PAUSED);
        }
    }
    
    public void resumeQueue() {
        if (currentQueue != null) {
            currentQueue.setStatus(Queue.QueueStatus.ACTIVE);
        }
    }
    
    public void stopQueue() {
        if (currentQueue != null) {
            currentQueue.setStatus(Queue.QueueStatus.STOPPED);
        }
    }
    
    public void clearQueue() {
        currentQueue = null;
    }
}
