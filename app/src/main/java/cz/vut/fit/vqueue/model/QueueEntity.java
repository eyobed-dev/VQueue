package cz.vut.fit.vqueue.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "queues")
public class QueueEntity {
      @PrimaryKey
      @NonNull
      private String id;

      private String name;
      private int waitTimeMinutes;
      private int currentNumber;
      private String status; // ACTIVE, PAUSED, STOPPED

      // Default constructor
      public QueueEntity() {
      }

      public QueueEntity(@NonNull String id, String name, int waitTimeMinutes, int currentNumber, String status) {
            this.id = id;
            this.name = name;
            this.waitTimeMinutes = waitTimeMinutes;
            this.currentNumber = currentNumber;
            this.status = status;
      }

      @NonNull
      public String getId() {
            return id;
      }

      public void setId(@NonNull String id) {
            this.id = id;
      }

      public String getName() {
            return name;
      }

      public void setName(String name) {
            this.name = name;
      }

      public int getWaitTimeMinutes() {
            return waitTimeMinutes;
      }

      public void setWaitTimeMinutes(int waitTimeMinutes) {
            this.waitTimeMinutes = waitTimeMinutes;
      }

      public int getCurrentNumber() {
            return currentNumber;
      }

      public void setCurrentNumber(int currentNumber) {
            this.currentNumber = currentNumber;
      }

      public String getStatus() {
            return status;
      }

      public void setStatus(String status) {
            this.status = status;
      }
}
