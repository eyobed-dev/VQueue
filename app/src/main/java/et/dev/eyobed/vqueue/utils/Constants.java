package et.dev.eyobed.vqueue.utils;

import java.util.UUID;

public class Constants {
  // BLE UUIDs
  public static final String SERVICE_UUID_STRING = "12345678-1234-1234-1234-123456789ABC";
  public static final UUID SERVICE_UUID = UUID.fromString(SERVICE_UUID_STRING);

  public static final String JOIN_CHARACTERISTIC_UUID_STRING = "12345678-1234-1234-1234-123456789ABD";
  public static final UUID JOIN_CHARACTERISTIC_UUID = UUID.fromString(JOIN_CHARACTERISTIC_UUID_STRING);

  // CCCD (Client Characteristic Configuration Descriptor)
  public static final String CCCD_UUID_STRING = "00002902-0000-1000-8000-00805f9b34fb";
  public static final UUID CCCD_UUID = UUID.fromString(CCCD_UUID_STRING);

  // Notification IDs
  public static final int NOTIFICATION_ID_QUEUE_MANAGEMENT = 1;
  public static final int NOTIFICATION_ID_QUEUE_MONITORING = 2;
  public static final int NOTIFICATION_ID_ALARM = 3;

  // Notification Channels
  public static final String CHANNEL_ID_MANAGEMENT = "QueueManagementChannel";
  public static final String CHANNEL_ID_MONITORING = "QueueMonitoringChannel";

  // Service Actions
  public static final String ACTION_START_QUEUE = "et.dev.eyobed.vqueue.START_QUEUE";
  public static final String ACTION_STOP_QUEUE = "et.dev.eyobed.vqueue.STOP_QUEUE";
  public static final String ACTION_CALL_NEXT = "et.dev.eyobed.vqueue.CALL_NEXT";
  public static final String ACTION_PAUSE_QUEUE = "et.dev.eyobed.vqueue.PAUSE_QUEUE";
  public static final String ACTION_RESUME_QUEUE = "et.dev.eyobed.vqueue.RESUME_QUEUE";
  public static final String ACTION_UPDATE_ADVERTISEMENT = "et.dev.eyobed.vqueue.UPDATE_ADVERTISEMENT";

  // Broadcast Actions
  public static final String ACTION_QUEUE_UPDATED = "et.dev.eyobed.vqueue.QUEUE_UPDATED";

  // Protocol Magic Numbers
  public static final byte MAGIC_HEADER_1 = (byte) 0xAA;
  public static final byte MAGIC_HEADER_2 = (byte) 0xBB;

  // Protocol Constants
  public static final int ADVERTISEMENT_PACKET_SIZE = 19;
  public static final int DEVICE_NAME_LENGTH = 10;
  public static final int MANUFACTURER_ID = 0x1234;
}
