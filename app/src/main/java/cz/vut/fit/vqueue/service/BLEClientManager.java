package cz.vut.fit.vqueue.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import java.util.UUID;

public class BLEClientManager {
    private static final String TAG = "BLEClientManager";

    // Service and Characteristic UUIDs (must match server)
    private static final String SERVICE_UUID = "12345678-1234-1234-1234-123456789ABC";
    private static final String JOIN_CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789ABD";
    private static final String STATUS_CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789ABE";

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt gatt;
    private OnJoinResultListener listener;
    private String deviceAddress;

    public interface OnJoinResultListener {
        void onJoinSuccess(int ticketNumber);
        void onJoinFailed(String error);
    }

    public BLEClientManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setOnJoinResultListener(OnJoinResultListener listener) {
        this.listener = listener;
    }

    public void connectAndJoin(String deviceAddress) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            if (listener != null) {
                listener.onJoinFailed("Bluetooth is not enabled");
            }
            return;
        }

        this.deviceAddress = deviceAddress;
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        if (device == null) {
            if (listener != null) {
                listener.onJoinFailed("Device not found");
            }
            return;
        }

        Log.i(TAG, "Connecting to device: " + deviceAddress);
        gatt = device.connectGatt(context, false, gattCallback);
    }

    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
    }

    private void joinQueue() {
        if (gatt == null) {
            if (listener != null) {
                listener.onJoinFailed("Not connected to device");
            }
            return;
        }

        BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
        if (service == null) {
            Log.e(TAG, "Service not found: " + SERVICE_UUID);
            if (listener != null) {
                listener.onJoinFailed("Service not found");
            }
            disconnect();
            return;
        }

        BluetoothGattCharacteristic joinCharacteristic = service
                .getCharacteristic(UUID.fromString(JOIN_CHARACTERISTIC_UUID));
        if (joinCharacteristic == null) {
            Log.e(TAG, "Join characteristic not found: " + JOIN_CHARACTERISTIC_UUID);
            if (listener != null) {
                listener.onJoinFailed("Join characteristic not found");
            }
            disconnect();
            return;
        }

        // Verify characteristic has WRITE property
        int properties = joinCharacteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0) {
            Log.e(TAG, "Characteristic does not support WRITE. Properties: " + properties);
            if (listener != null) {
                listener.onJoinFailed("Characteristic not writable");
            }
            disconnect();
            return;
        }

        // Enable notifications to receive ticket number (Android 13+ compatible!)
        Log.i(TAG, "Enabling notifications on join characteristic");
        boolean notificationEnabled = gatt.setCharacteristicNotification(joinCharacteristic, true);
        Log.i(TAG, "Notification enabled: " + notificationEnabled);

        // Write to CCCD to enable notifications on server side
        BluetoothGattDescriptor descriptor = joinCharacteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean descriptorWritten = gatt.writeDescriptor(descriptor);
            Log.i(TAG, "CCCD descriptor write initiated: " + descriptorWritten);
            // The actual join request will be sent in onDescriptorWrite callback
        } else {
            Log.e(TAG, "CCCD descriptor not found");
            // Try to proceed anyway
            sendJoinRequest(joinCharacteristic);
        }
    }

    private void sendJoinRequest(BluetoothGattCharacteristic joinCharacteristic) {
        // Send device ID to join the queue
        String deviceId = android.os.Build.MODEL;
        // Truncate to 20 bytes (BLE default MTU payload limit)
        if (deviceId.length() > 20) {
            deviceId = deviceId.substring(0, 20);
        }
        byte[] deviceIdBytes = deviceId.getBytes();
        
        Log.i(TAG, "Attempting to write " + deviceIdBytes.length + " bytes to join characteristic");

        // Handle API level differences for writing characteristics
        boolean writeSuccess;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Use new API
            writeSuccess = gatt.writeCharacteristic(joinCharacteristic, deviceIdBytes, 
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothGatt.GATT_SUCCESS;
        } else {
            // Android 12 and below - Use deprecated API
            joinCharacteristic.setValue(deviceIdBytes);
            joinCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            writeSuccess = gatt.writeCharacteristic(joinCharacteristic);
        }

        if (!writeSuccess) {
            Log.e(TAG, "Failed to initiate write operation");
            if (listener != null) {
                listener.onJoinFailed("Failed to write join request");
            }
            disconnect();
        } else {
            Log.i(TAG, "Write operation initiated successfully");
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server, status: " + status);
                // Request larger MTU for better data transfer (default is 23 bytes)
                boolean mtuRequested = gatt.requestMtu(512);
                Log.i(TAG, "MTU request initiated: " + mtuRequested);
                // Note: Service discovery will happen in onMtuChanged callback
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server, status: " + status);
                if (BLEClientManager.this.gatt != null) {
                    BLEClientManager.this.gatt.close();
                    BLEClientManager.this.gatt = null;
                }
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.i(TAG, "MTU changed to: " + mtu + ", status: " + status);
            // Now discover services after MTU is set
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered");
                // Now we can join the queue
                joinQueue();
            } else {
                if (listener != null) {
                    listener.onJoinFailed("Failed to discover services");
                }
                disconnect();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            
            Log.i(TAG, "onCharacteristicWrite called - UUID: " + characteristic.getUuid() + ", status: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Write successful - waiting for notification with ticket number");
                // Ticket number will be delivered via notification (onCharacteristicChanged)
                // No action needed here
            } else {
                Log.e(TAG, "Write failed with status: " + status);
                if (listener != null) {
                    listener.onJoinFailed("Failed to write characteristic (status: " + status + ")");
                }
                disconnect();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.i(TAG, "onDescriptorWrite called - UUID: " + descriptor.getUuid() + ", status: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Notifications enabled successfully, sending join request");
                // Now send the join request
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                if (characteristic != null && characteristic.getUuid().toString().equalsIgnoreCase(JOIN_CHARACTERISTIC_UUID)) {
                    sendJoinRequest(characteristic);
                }
            } else {
                Log.e(TAG, "Failed to enable notifications, status: " + status);
                if (listener != null) {
                    listener.onJoinFailed("Failed to enable notifications");
                }
                disconnect();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "onCharacteristicChanged called - UUID: " + characteristic.getUuid());

            String characteristicUuid = characteristic.getUuid().toString();
            if (characteristicUuid.equalsIgnoreCase(JOIN_CHARACTERISTIC_UUID)) {
                // Received notification with ticket number!
                byte[] value = characteristic.getValue();
                Log.i(TAG, "Notification received, value length: " + (value != null ? value.length : 0));

                if (value != null && value.length >= 2) {
                    int ticketNumber = (value[0] & 0xFF) | ((value[1] & 0xFF) << 8);
                    Log.i(TAG, "Received ticket number via notification: " + ticketNumber);

                    if (listener != null) {
                        listener.onJoinSuccess(ticketNumber);
                    }

                    // Disconnect after successful join
                    disconnect();
                } else {
                    Log.e(TAG, "Invalid notification value");
                    if (listener != null) {
                        listener.onJoinFailed("Invalid ticket number received");
                    }
                    disconnect();
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            
            Log.i(TAG, "onCharacteristicRead called - UUID: " + characteristic.getUuid() + ", status: " + status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                String characteristicUuid = characteristic.getUuid().toString();
                Log.i(TAG, "Read successful for characteristic: " + characteristicUuid);
                
                if (characteristicUuid.equalsIgnoreCase(JOIN_CHARACTERISTIC_UUID)) {
                    // Parse ticket number from response
                    byte[] value = characteristic.getValue();
                    Log.i(TAG, "Read value length: " + (value != null ? value.length : 0));
                    
                    if (value != null && value.length >= 2) {
                        int ticketNumber = (value[0] & 0xFF) | ((value[1] & 0xFF) << 8);
                        Log.i(TAG, "Received ticket number from read: " + ticketNumber);

                        if (listener != null) {
                            listener.onJoinSuccess(ticketNumber);
                        }

                        // Disconnect after successful join
                        disconnect();
                    } else {
                        Log.e(TAG, "Invalid or empty value from read");
                        if (listener != null) {
                            listener.onJoinFailed("Invalid ticket number received");
                        }
                        disconnect();
                    }
                }
            } else {
                Log.e(TAG, "Read failed with status: " + status);
                if (listener != null) {
                    listener.onJoinFailed("Failed to read characteristic (status: " + status + ")");
                }
                disconnect();
            }
        }
    };
}
