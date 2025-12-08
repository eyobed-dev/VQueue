package cz.vut.fit.vqueue.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import java.util.UUID;
import cz.vut.fit.vqueue.model.Queue;
import cz.vut.fit.vqueue.repo.QueueRepo;

public class GATTServerManager {
    private static final String TAG = "GATTServerManager";

    // Service and Characteristic UUIDs
    public static final String SERVICE_UUID = "12345678-1234-1234-1234-123456789ABC";
    private static final String JOIN_CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789ABD";

    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothGattServer gattServer;
    private QueueRepo queueRepo;
    private BLEAdvertisementManager bleManager;

    public GATTServerManager(Context context, BLEAdvertisementManager bleManager) {
        this.context = context;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.queueRepo = QueueRepo.getInstance();
        this.bleManager = bleManager;
    }

    public boolean setupGATTServer() {
        if (bluetoothManager == null) {
            Log.e(TAG, "BluetoothManager is null");
            return false;
        }

        try {
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback);
            if (gattServer == null) {
                Log.e(TAG, "Failed to open GATT server");
                return false;
            }

            // Create the service
            BluetoothGattService service = new BluetoothGattService(
                    UUID.fromString(SERVICE_UUID),
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);

            // Create join characteristic (for clients to join the queue)
            BluetoothGattCharacteristic joinCharacteristic = new BluetoothGattCharacteristic(
                    UUID.fromString(JOIN_CHARACTERISTIC_UUID),
                    BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);

            // Add characteristic to service
            service.addCharacteristic(joinCharacteristic);

            // Add service to GATT server
            boolean success = gattServer.addService(service);
            if (success) {
                Log.i(TAG, "GATT server setup successful");
                return true;
            } else {
                Log.e(TAG, "Failed to add service to GATT server");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up GATT server", e);
            return false;
        }
    }

    public void closeGATTServer() {
        if (gattServer != null) {
            try {
                gattServer.close();
                gattServer = null;
                Log.i(TAG, "GATT server closed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing GATT server", e);
            }
        }
    }

    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Client connected: " + device.getAddress());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Client disconnected: " + device.getAddress());
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset,
                    value);

            String characteristicUuid = characteristic.getUuid().toString();

            if (characteristicUuid.equals(JOIN_CHARACTERISTIC_UUID)) {
                handleJoinRequest(device, requestId, value, responseNeeded);
            } else {
                // Reject other write requests
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Service added successfully: " + service.getUuid());
            } else {
                Log.e(TAG, "Failed to add service: " + service.getUuid() + " Status: " + status);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            String characteristicUuid = characteristic.getUuid().toString();

            if (characteristicUuid.equals(JOIN_CHARACTERISTIC_UUID)) {
                // Return the assigned ticket number
                // The value was set in handleJoinRequest
                byte[] value = characteristic.getValue();
                if (value != null) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
                } else {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            } else {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }
    };

    private void handleJoinRequest(BluetoothDevice device, int requestId, byte[] value, boolean responseNeeded) {
        try {
            // Extract device ID from the request (assuming it's sent as a string)
            String deviceId = new String(value).trim();
            Log.i(TAG, "Join request from device: " + deviceId);

            // Add waiter to queue
            int ticketNumber = queueRepo.addWaiter(deviceId);

            if (ticketNumber > 0) {
                // Success - prepare ticket number response
                byte[] response = new byte[2];
                response[0] = (byte) (ticketNumber & 0xFF);
                response[1] = (byte) ((ticketNumber >> 8) & 0xFF);

                // Set the characteristic value so client can read it
                BluetoothGattCharacteristic joinCharacteristic = gattServer.getService(UUID.fromString(SERVICE_UUID))
                        .getCharacteristic(UUID.fromString(JOIN_CHARACTERISTIC_UUID));
                if (joinCharacteristic != null) {
                    joinCharacteristic.setValue(response);
                }

                if (responseNeeded) {
                    // Send empty response for write request (client expects no data on write
                    // response)
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }

                Log.i(TAG, "Assigned ticket number " + ticketNumber + " to device " + deviceId);

                // Update advertisement with new queue state
                Queue currentQueue = queueRepo.getCurrentQueue();
                if (currentQueue != null) {
                    bleManager.updateAdvertisement(currentQueue);
                }

                // Disconnect is handled by the client after reading the ticket
                // gattServer.cancelConnection(device);
                // Log.i(TAG, "Disconnected device " + deviceId + " after assigning ticket");

            } else {
                // Failed to add waiter
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
                Log.e(TAG, "Failed to add waiter for device: " + deviceId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling join request", e);
            if (responseNeeded) {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }
    }
}
