package cz.vut.fit.vqueue.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "waiters", foreignKeys = @ForeignKey(entity = QueueEntity.class, parentColumns = "id", childColumns = "queueId", onDelete = CASCADE), indices = {
            @Index("queueId") })
public class WaiterEntity {
      @PrimaryKey(autoGenerate = true)
      private int id;

      private String queueId;
      private int ticketNumber;
      private String deviceId;
      private long joinedAtTimestamp;

      public WaiterEntity() {
      }

      public int getId() {
            return id;
      }

      public void setId(int id) {
            this.id = id;
      }

      public String getQueueId() {
            return queueId;
      }

      public void setQueueId(String queueId) {
            this.queueId = queueId;
      }

      public int getTicketNumber() {
            return ticketNumber;
      }

      public void setTicketNumber(int ticketNumber) {
            this.ticketNumber = ticketNumber;
      }

      public String getDeviceId() {
            return deviceId;
      }

      public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
      }

      public long getJoinedAtTimestamp() {
            return joinedAtTimestamp;
      }

      public void setJoinedAtTimestamp(long joinedAtTimestamp) {
            this.joinedAtTimestamp = joinedAtTimestamp;
      }
}
