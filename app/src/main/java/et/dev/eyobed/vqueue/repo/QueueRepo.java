package et.dev.eyobed.vqueue.repo;

import et.dev.eyobed.vqueue.model.Queue;
import et.dev.eyobed.vqueue.model.Waiter;
import et.dev.eyobed.vqueue.model.WaiterEntity;

public class QueueRepo {
    private static volatile QueueRepo instance;
    private Queue currentQueue;
    private final QueueDao queueDao;
    private final java.util.concurrent.ExecutorService executor;

    private QueueRepo(android.content.Context context) {
        VQueueDatabase db = VQueueDatabase.getDatabase(context);
        queueDao = db.queueDao();
        executor = VQueueDatabase.databaseWriteExecutor;
        loadActiveQueueFromDb();
    }

    public static synchronized QueueRepo getInstance(android.content.Context context) {
        if (instance == null) {
            instance = new QueueRepo(context.getApplicationContext());
        }
        return instance;
    }

    private void loadActiveQueueFromDb() {
        executor.execute(() -> {
            et.dev.eyobed.vqueue.model.QueueEntity entity = queueDao.getActiveQueue();
            if (entity != null) {
                // Restore queue state
                et.dev.eyobed.vqueue.model.Queue queue = new et.dev.eyobed.vqueue.model.Queue(entity.getName(),
                        entity.getWaitTimeMinutes());
                // We need to forcefully set the ID to match the persisted one
                // Using reflection or adding a setter in Queue (better to add setter or
                // constructor)
                // For now, let's assume we can map it.
                // Since Queue generates a random ID in constructor, we must override it.
                // NOTE: Queue.java needs a setId method or protected constructor.
                // Assuming we can fix Queue.java or add a setter.
                restoreQueueState(queue, entity);

                // Load waiters
                // int count = queueDao.getWaiterCount(entity.getId());
                // For this prototype, we just restore the queue metadata and current number
                // Real implementation would allow restoring individual waiters too

                synchronized (this) {
                    this.currentQueue = queue;
                }
            }
        });
    }

    // Helper to map Entity -> Model (Needs corresponding setters in Model)
    private void restoreQueueState(Queue queue, et.dev.eyobed.vqueue.model.QueueEntity entity) {
        try {
            // Restore basic properties
            queue.setId(entity.getId());
            queue.setCurrentNumber(entity.getCurrentNumber());
            queue.setStatus(Queue.QueueStatus.valueOf(entity.getStatus()));

            // Restore Waiters
            java.util.List<WaiterEntity> waiterEntities = queueDao.getWaiters(entity.getId());
            java.util.List<Waiter> waiters = new java.util.ArrayList<>();
            if (waiterEntities != null) {
                for (WaiterEntity waiterEntity : waiterEntities) {
                    Waiter waiter = new Waiter(waiterEntity.getTicketNumber(), waiterEntity.getDeviceId());
                    waiter.setJoinedAt(waiterEntity.getJoinedAtTimestamp());
                    // Since Waiter.joinedAt is final and set in constructor, we might want to allow
                    // setting it
                    // or just accept the new time. Ideally we should restore timestamp too.
                    // But Waiter constructor sets timestamp to now.
                    // Let's check Waiter model if needed, but for now getting the correct order
                    // (ticketNum) is key.
                    // Actually, let's verify if Waiter allows setting timestamp or if we can pass
                    // it in constructor.
                    // Looking at existing code, Waiter seems simple.
                    // We will just recreate them for now. The order is preserved by DB query.
                    waiters.add(waiter);
                }
            }
            queue.setWaiters(waiters);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Queue createQueue(String name, int waitTimeMinutes) {
        currentQueue = new Queue(name, waitTimeMinutes);
        persistQueue(currentQueue);
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
            persistQueue(currentQueue);
        }
    }

    public int addWaiter(String deviceId) {
        if (currentQueue != null) {
            int ticketNumber = currentQueue.getNextTicketNumber();
            Waiter waiter = new Waiter(ticketNumber, deviceId);
            currentQueue.addWaiter(waiter);

            // Persist waiter
            executor.execute(() -> {
                et.dev.eyobed.vqueue.model.WaiterEntity entity = new et.dev.eyobed.vqueue.model.WaiterEntity();
                entity.setQueueId(currentQueue.getId());
                entity.setTicketNumber(ticketNumber);
                entity.setDeviceId(deviceId);
                entity.setJoinedAtTimestamp(waiter.getJoinedAt());
                queueDao.insertWaiter(entity);
            });

            return ticketNumber;
        }
        return -1; // Error: no active queue
    }

    public void updateQueueStatus(Queue.QueueStatus status) {
        if (currentQueue != null) {
            currentQueue.setStatus(status);
            persistQueue(currentQueue);
        }
    }

    public void callNext() {
        if (currentQueue != null && currentQueue.isActive()) {
            currentQueue.setCurrentNumber(currentQueue.getCurrentNumber() + 1);
            persistQueue(currentQueue);
        }
    }

    public void pauseQueue() {
        if (currentQueue != null) {
            currentQueue.setStatus(Queue.QueueStatus.PAUSED);
            persistQueue(currentQueue);
        }
    }

    public void resumeQueue() {
        if (currentQueue != null) {
            currentQueue.setStatus(Queue.QueueStatus.ACTIVE);
            persistQueue(currentQueue);
        }
    }

    public void stopQueue() {
        if (currentQueue != null) {
            currentQueue.setStatus(Queue.QueueStatus.STOPPED);
            persistQueue(currentQueue);
            // Ideally mark as stopped in DB, not delete, so we have history
        }
    }

    public void clearQueue() {
        // This is usually called after stopping
        currentQueue = null;
    }

    public void clearWaiters() {
        if (currentQueue != null) {
            currentQueue.setWaiters(new java.util.ArrayList<>());
            currentQueue.setCurrentNumber(0); // Reset current number too? Typically yes.
            // Queue state update
            persistQueue(currentQueue);

            // Db delete
            executor.execute(() -> {
                queueDao.deleteWaiters(currentQueue.getId());
            });
        }
    }

    private void persistQueue(Queue queue) {
        executor.execute(() -> {
            et.dev.eyobed.vqueue.model.QueueEntity entity = new et.dev.eyobed.vqueue.model.QueueEntity(
                    queue.getId(),
                    queue.getName(),
                    queue.getWaitTimeMinutes(),
                    queue.getCurrentNumber(),
                    queue.getStatus().name());
            queueDao.insertOrUpdateQueue(entity);
        });
    }
}
