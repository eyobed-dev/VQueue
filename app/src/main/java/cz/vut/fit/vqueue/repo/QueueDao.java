package cz.vut.fit.vqueue.repo;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import cz.vut.fit.vqueue.model.QueueEntity;
import cz.vut.fit.vqueue.model.WaiterEntity;

@Dao
public interface QueueDao {
      // Queue Operations
      @Insert(onConflict = OnConflictStrategy.REPLACE)
      void insertOrUpdateQueue(QueueEntity queue);

      @Query("SELECT * FROM queues WHERE status = 'ACTIVE' LIMIT 1")
      QueueEntity getActiveQueue();

      @Query("SELECT * FROM queues WHERE id = :queueId LIMIT 1")
      QueueEntity getQueueById(String queueId);

      @Update
      void updateQueue(QueueEntity queue);

      @Query("DELETE FROM queues WHERE id = :queueId")
      void deleteQueue(String queueId);

      @Query("DELETE FROM queues")
      void deleteAllQueues();

      // Waiter Operations
      @Insert
      long insertWaiter(WaiterEntity waiter);

      @Query("SELECT MAX(ticketNumber) FROM waiters WHERE queueId = :queueId")
      Integer getLastTicketNumber(String queueId);

      @Query("SELECT count(*) FROM waiters WHERE queueId = :queueId")
      int getWaiterCount(String queueId);
}
