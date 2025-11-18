package cz.vut.fit.vqueue.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
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
            if (listener != null) {
                listener.onJoinFailed("Service not found");
            }
            disconnect();
            return;
        }
        
        BluetoothGattCharacteristic joinCharacteristic = service.getCharacteristic(UUID.fromString(JOIN_CHARACTERISTIC_UUID));
        if (joinCharacteristic == null) {
            if (listener != null) {
                listener.onJoinFailed("Join characteristic not found");
            }
            disconnect();
            return;
        }
        
        // Send device ID to join the queue
        String deviceId = android.os.Build.MODEL;
        byte[] deviceIdBytes = deviceId.getBytes();
        joinCharacteristic.setValue(deviceIdBytes);
        
        if (!gatt.writeCharacteristic(joinCharacteristic)) {
            if (listener != null) {
                listener.onJoinFailed("Failed to write join request");
            }
            disconnect();
        } else {
            Log.i(TAG, "Sent join request");
        }
    }
    
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server");
                // Discover services
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server");
                if (BLEClientManager.this.gatt != null) {
                    BLEClientManager.this.gatt.close();
                    BLEClientManager.this.gatt = null;
                }
            }
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
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String characteristicUuid = characteristic.getUuid().toString();
                if (characteristicUuid.equals(JOIN_CHARACTERISTIC_UUID)) {
                    // Wait a bit for the server to process and set the value, then read
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (gatt != null) {
                            gatt.readCharacteristic(characteristic);
                        }
                    }, 100);
                }
            } else {
                if (listener != null) {
                    listener.onJoinFailed("Failed to write characteristic");
                }
                disconnect();
            }
        }
        
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String characteristicUuid = characteristic.getUuid().toString();
                if (characteristicUuid.equals(JOIN_CHARACTERISTIC_UUID)) {
                    // Parse ticket number from response
                    byte[] value = characteristic.getValue();
                    if (value != null && value.length >= 2) {
                        int ticketNumber = (value[0] & 0xFF) | ((value[1] & 0xFF) << 8);
                        Log.i(TAG, "Received ticket number: " + ticketNumber);
                        
                        if (listener != null) {
                            listener.onJoinSuccess(ticketNumber);
                        }
                        
                        // Disconnect after successful join
                        disconnect();
                    } else {
                        if (listener != null) {
                            listener.onJoinFailed("Invalid ticket number received");
                        }
                        disconnect();
                    }
                }
            } else {
                if (listener != null) {
                    listener.onJoinFailed("Failed to read characteristic");
                }
                disconnect();
            }
        }
    };
}
