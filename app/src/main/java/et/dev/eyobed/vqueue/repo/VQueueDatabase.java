package et.dev.eyobed.vqueue.repo;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import et.dev.eyobed.vqueue.model.QueueEntity;
import et.dev.eyobed.vqueue.model.WaiterEntity;

@Database(entities = { QueueEntity.class, WaiterEntity.class }, version = 1, exportSchema = false)
public abstract class VQueueDatabase extends RoomDatabase {
      public abstract QueueDao queueDao();

      private static volatile VQueueDatabase INSTANCE;
      private static final int NUMBER_OF_THREADS = 4;
      public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

      public static VQueueDatabase getDatabase(final Context context) {
            if (INSTANCE == null) {
                  synchronized (VQueueDatabase.class) {
                        if (INSTANCE == null) {
                              INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                          VQueueDatabase.class, "vqueue_database")
                                          .fallbackToDestructiveMigration()

                                          .build();
                        }
                  }
            }
            return INSTANCE;
      }
}
