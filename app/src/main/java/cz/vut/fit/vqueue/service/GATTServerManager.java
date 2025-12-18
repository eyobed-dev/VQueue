package cz.vut.fit.vqueue.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import java.util.UUID;
import cz.vut.fit.vqueue.utils.Constants;
import cz.vut.fit.vqueue.model.Queue;
import cz.vut.fit.vqueue.repo.QueueRepo;

public class GATTServerManager {
    private static final String TAG = "GATTServerManager";

    // Service and Characteristic UUIDs used from Constants class

    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothGattServer gattServer;
    private QueueRepo queueRepo;
    private BLEAdvertisementManager bleManager;

    public GATTServerManager(Context context, BLEAdvertisementManager bleManager) {
        this.context = context;
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.queueRepo = QueueRepo.getInstance(context);
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
                    Constants.SERVICE_UUID,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY);

            // Create join characteristic (for clients to join the queue)
            // Added NOTIFY property for Android 13+ compatibility
            BluetoothGattCharacteristic joinCharacteristic = new BluetoothGattCharacteristic(
                    Constants.JOIN_CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE |
                            BluetoothGattCharacteristic.PROPERTY_READ |
                            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_WRITE |
                            BluetoothGattCharacteristic.PERMISSION_READ);

            // Add Client Characteristic Configuration Descriptor (required for
            // notifications)
            BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                    Constants.CCCD_UUID, // Standard CCCD UUID
                    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            joinCharacteristic.addDescriptor(descriptor);

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

                // List all services to verify they're available
                if (gattServer != null) {
                    java.util.List<BluetoothGattService> services = gattServer.getServices();
                    Log.i(TAG, "GATT Server has " + services.size() + " service(s):");
                    for (BluetoothGattService svc : services) {
                        Log.i(TAG, "  Service: " + svc.getUuid() + " with " +
                                svc.getCharacteristics().size() + " characteristic(s)");
                        for (BluetoothGattCharacteristic chr : svc.getCharacteristics()) {
                            Log.i(TAG, "    Characteristic: " + chr.getUuid() +
                                    " Properties: " + chr.getProperties() +
                                    " Permissions: " + chr.getPermissions());
                        }
                    }
                } else {
                    Log.e(TAG, "GATT Server is NULL when client connected!");
                }
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

            Log.i(TAG, "onCharacteristicWriteRequest called!");
            Log.i(TAG, "  Device: " + device.getAddress());
            Log.i(TAG, "  RequestId: " + requestId);
            Log.i(TAG, "  Characteristic UUID: " + characteristic.getUuid());
            Log.i(TAG, "  PreparedWrite: " + preparedWrite);
            Log.i(TAG, "  ResponseNeeded: " + responseNeeded);
            Log.i(TAG, "  Offset: " + offset);
            Log.i(TAG, "  Value length: " + (value != null ? value.length : 0));

            String characteristicUuid = characteristic.getUuid().toString();
            Log.i(TAG, "  Comparing UUID: " + characteristicUuid + " with " + Constants.JOIN_CHARACTERISTIC_UUID);

            if (characteristic.getUuid().equals(Constants.JOIN_CHARACTERISTIC_UUID)) {
                Log.i(TAG, "  UUID match! Handling join request");
                handleJoinRequest(device, requestId, value, responseNeeded);
            } else {
                Log.w(TAG, "  UUID mismatch! Rejecting write request");
                // Reject other write requests
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
                }
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            Log.i(TAG, "onDescriptorWriteRequest called!");
            Log.i(TAG, "  Device: " + device.getAddress());
            Log.i(TAG, "  Descriptor UUID: " + descriptor.getUuid());
            Log.i(TAG, "  ResponseNeeded: " + responseNeeded);

            // Check if this is the CCCD descriptor (for enabling notifications)
            if (descriptor.getUuid().equals(Constants.CCCD_UUID)) {
                Log.i(TAG, "  CCCD write request - client enabling notifications");

                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
                    Log.i(TAG, "  Sent success response for CCCD write");
                }
            } else {
                Log.w(TAG, "  Unknown descriptor write request");
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

            if (characteristic.getUuid().equals(Constants.JOIN_CHARACTERISTIC_UUID)) {
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

                // Set the characteristic value for potential read requests
                BluetoothGattCharacteristic joinCharacteristic = gattServer.getService(Constants.SERVICE_UUID)
                        .getCharacteristic(Constants.JOIN_CHARACTERISTIC_UUID);
                if (joinCharacteristic != null) {
                    joinCharacteristic.setValue(response);
                    Log.i(TAG, "Set characteristic value for potential read request");
                }

                // Send write response first
                if (responseNeeded) {
                    Log.i(TAG, "Sending write response (status: SUCCESS)");
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }

                // Send notification with ticket number (works on all Android versions!)
                if (joinCharacteristic != null) {
                    boolean notificationSent = gattServer.notifyCharacteristicChanged(device, joinCharacteristic,
                            false);
                    Log.i(TAG, "Notification sent: " + notificationSent + ", ticket number: " + ticketNumber);
                }

                Log.i(TAG, "Assigned ticket number " + ticketNumber + " to device " + deviceId);

                // Update advertisement with new queue state
                Queue currentQueue = queueRepo.getCurrentQueue();
                if (currentQueue != null) {
                    bleManager.updateAdvertisement(currentQueue);
                }

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
