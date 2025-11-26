package cz.vut.fit.vqueue.service;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import android.os.ParcelUuid;
import java.util.UUID;

import androidx.core.app.ActivityCompat;

import cz.vut.fit.vqueue.model.Queue;

public class BLEAdvertisementManager {
    private static final String TAG = "BLEAdvertisementManager";

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private boolean isAdvertising = false;

    public BLEAdvertisementManager(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            this.bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        }
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean isAdvertising() {
        return isAdvertising;
    }

    public void startAdvertising(Queue queue) {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            return;
        }

        if (bluetoothLeAdvertiser == null) {
            Log.e(TAG, "BluetoothLeAdvertiser is null");
            return;
        }

        if (isAdvertising) {
            Log.w(TAG, "Already advertising, stopping first");
            stopAdvertising();
        }

        try {
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setConnectable(true)
                    .build();

            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(false).setIncludeTxPowerLevel(false)
                    .addManufacturerData(0x1234, formatAdvertisementData(queue))
                    .build();

            // Add Service UUID to Scan Response so external scanners can find it
            AdvertiseData scanResponse = new AdvertiseData.Builder()
                    .addServiceUuid(new ParcelUuid(UUID.fromString(GATTServerManager.SERVICE_UUID)))
                    .build();

            bluetoothLeAdvertiser.startAdvertising(settings, data, scanResponse, advertiseCallback);
            Log.i(TAG, "Started advertising queue: " + queue.getName());

        } catch (Exception e) {
            Log.e(TAG, "Failed to start advertising", e);
        }
    }

    public void stopAdvertising() {
        if (bluetoothLeAdvertiser != null && isAdvertising) {
            try {
                bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
                isAdvertising = false;
                Log.i(TAG, "Stopped advertising");
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop advertising", e);
            }
        }
    }

    public void updateAdvertisement(Queue queue) {
        if (isAdvertising) {
            Log.i(TAG, "Updating advertisement for queue: " + queue.getName());
            stopAdvertising();
            // Small delay to ensure stop is processed
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                startAdvertising(queue);
            }, 100);
        }
    }

    private byte[] formatAdvertisementData(Queue queue) {
        // [Header: 2B] [QueueID short hash: 2B] [ShortName: 10B] [Current#: 2B]
        // [WaitTime: 2B] [Status: 1B]
        // Total: 19 bytes (safe for BLE)

        byte[] data = new byte[19];
        int offset = 0;

        // Header (magic number to identify our packets)
        data[offset++] = (byte) 0xAA;
        data[offset++] = (byte) 0xBB;

        // Short queue ID (hash of full UUID)
        int idHash = queue.getId().hashCode();
        data[offset++] = (byte) (idHash & 0xFF);
        data[offset++] = (byte) ((idHash >> 8) & 0xFF);

        // Short name (fixed 10 bytes)
        String name = queue.getName();
        if (name.length() > 10)
            name = name.substring(0, 10);
        byte[] nameBytes = name.getBytes();
        System.arraycopy(nameBytes, 0, data, offset, nameBytes.length);
        // Pad remaining bytes with 0 if name is shorter than 10
        for (int i = nameBytes.length; i < 10; i++) {
            data[offset + i] = 0;
        }
        offset += 10;

        // Current number (2 bytes)
        int current = queue.getCurrentNumber();
        data[offset++] = (byte) (current & 0xFF);
        data[offset++] = (byte) ((current >> 8) & 0xFF);

        // Wait time (2 bytes)
        int wait = queue.getWaitTimeMinutes();
        data[offset++] = (byte) (wait & 0xFF);
        data[offset++] = (byte) ((wait >> 8) & 0xFF);

        // Status (1 byte)
        data[offset++] = (byte) queue.getStatus().ordinal();

        return data;
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            isAdvertising = true;
            Log.i(TAG, "Advertising started successfully");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            isAdvertising = false;
            String errorMessage = "Failed to start advertising: ";
            switch (errorCode) {
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    errorMessage += "Data too large";
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    errorMessage += "Too many advertisers";
                    break;
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    errorMessage += "Already started";
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    errorMessage += "Internal error";
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    errorMessage += "Feature unsupported";
                    break;
                default:
                    errorMessage += "Unknown error: " + errorCode;
                    break;
            }
            Log.e(TAG, errorMessage);
        }
    };
}
