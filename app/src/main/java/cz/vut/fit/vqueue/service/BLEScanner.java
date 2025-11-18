package cz.vut.fit.vqueue.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import cz.vut.fit.vqueue.model.AdvertisedQueue;
import cz.vut.fit.vqueue.utils.AppPreferences;

public class BLEScanner {
    private static final String TAG = "BLEScanner";
    private static final String SERVICE_UUID = "12345678-1234-1234-1234-123456789ABC";
    
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean isScanning = false;
    private ScanCallback scanCallback;
    private OnQueueFoundListener listener;
    private List<AdvertisedQueue> foundQueues;
    private AppPreferences appPreferences;
    
    public interface OnQueueFoundListener {
        void onQueueFound(AdvertisedQueue queue);
        void onScanComplete();
    }
    
    public BLEScanner(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        this.foundQueues = new ArrayList<>();
        this.appPreferences = new AppPreferences(context);
    }
    
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    
    public boolean isScanning() {
        return isScanning;
    }
    
    public void setOnQueueFoundListener(OnQueueFoundListener listener) {
        this.listener = listener;
    }
    
    public void startScan() {
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            return;
        }
        
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null");
            return;
        }
        
        if (isScanning) {
            Log.w(TAG, "Already scanning, stopping first");
            stopScan();
        }
        
        foundQueues.clear();
        
        // Check preference: show all devices or only queues
        boolean showAllDevices = appPreferences.isShowAllDevicesEnabled();
        
        List<ScanFilter> filters = null;
        if (!showAllDevices) {
            // Filter for queue service UUID only
            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                    .build();
            filters = new ArrayList<>();
            filters.add(scanFilter);
        }
        // If showAllDevices is true, filters remains null (scan all devices)
        
        // Configure scan settings
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        
        // Create scan callback
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                processScanResult(result, showAllDevices);
            }
            
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (ScanResult result : results) {
                    processScanResult(result, showAllDevices);
                }
            }
            
            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                isScanning = false;
                String errorMessage = "Scan failed: ";
                switch (errorCode) {
                    case SCAN_FAILED_ALREADY_STARTED:
                        errorMessage += "Already started";
                        break;
                    case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                        errorMessage += "Application registration failed";
                        break;
                    case SCAN_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += "Feature unsupported";
                        break;
                    case SCAN_FAILED_INTERNAL_ERROR:
                        errorMessage += "Internal error";
                        break;
                    default:
                        errorMessage += "Unknown error: " + errorCode;
                        break;
                }
                Log.e(TAG, errorMessage);
            }
        };
        
        // Start scanning with or without filters based on preference
        try {
            bluetoothLeScanner.startScan(filters, settings, scanCallback);
            isScanning = true;
            if (showAllDevices) {
                Log.i(TAG, "Started BLE scanning (all devices - testing mode)");
            } else {
                Log.i(TAG, "Started BLE scanning (queues only)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start scanning", e);
            isScanning = false;
        }
    }
    
    public void stopScan() {
        if (bluetoothLeScanner != null && isScanning && scanCallback != null) {
            try {
                bluetoothLeScanner.stopScan(scanCallback);
                isScanning = false;
                Log.i(TAG, "Stopped BLE scanning");
                
                if (listener != null) {
                    listener.onScanComplete();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop scanning", e);
            }
        }
    }
    
    private void processScanResult(ScanResult result, boolean showAllDevices) {
        try {
            String deviceAddress = result.getDevice().getAddress();
            String deviceName = result.getDevice().getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unknown Device";
            }
            
            AdvertisedQueue queue = null;
            
            // Try to parse queue data if available
            byte[] serviceData = result.getScanRecord().getServiceData(ParcelUuid.fromString(SERVICE_UUID));
            if (serviceData != null && serviceData.length >= 31) {
                queue = parseAdvertisementData(serviceData, deviceAddress);
            }
            
            // If not a queue device and showAllDevices is enabled, create a generic entry for display
            if (queue == null) {
                if (showAllDevices) {
                    // Use a special ID format to identify generic devices
                    queue = new AdvertisedQueue(
                        "GENERIC_DEVICE_" + deviceAddress.replace(":", ""),
                        deviceName,
                        -1, // currentNumber (-1 indicates not a queue)
                        -1, // waitTimeMinutes (-1 indicates not a queue)
                        deviceAddress
                    );
                } else {
                    // In normal mode, ignore non-queue devices
                    return;
                }
            }
            
            // Check if we already have this device (by device address)
            boolean exists = false;
            for (int i = 0; i < foundQueues.size(); i++) {
                AdvertisedQueue existing = foundQueues.get(i);
                if (existing.getDeviceAddress().equals(queue.getDeviceAddress())) {
                    // Update existing entry
                    foundQueues.set(i, queue);
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                foundQueues.add(queue);
            }
            
            if (listener != null) {
                listener.onQueueFound(queue);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing scan result", e);
        }
    }
    
    private AdvertisedQueue parseAdvertisementData(byte[] data, String deviceAddress) {
        try {
            // Format: [Header: 2 bytes] [Queue ID: 4 bytes] [Name: 16 bytes] 
            //         [Current Number: 2 bytes] [Wait Time: 2 bytes] [Status: 1 byte] [Reserved: 4 bytes]
            // Total: 31 bytes
            
            if (data.length < 31) {
                Log.w(TAG, "Advertisement data too short: " + data.length);
                return null;
            }
            
            int offset = 0;
            
            // Check header (2 bytes)
            if (data[offset++] != (byte) 0xAA || data[offset++] != (byte) 0xBB) {
                Log.w(TAG, "Invalid header in advertisement data");
                return null;
            }
            
            // Queue ID (4 bytes)
            byte[] idBytes = new byte[4];
            System.arraycopy(data, offset, idBytes, 0, 4);
            String queueId = new String(idBytes).trim();
            offset += 4;
            
            // Queue Name (16 bytes)
            byte[] nameBytes = new byte[16];
            System.arraycopy(data, offset, nameBytes, 0, 16);
            String queueName = new String(nameBytes).trim();
            // Remove null characters
            queueName = queueName.replace("\0", "");
            offset += 16;
            
            // Current Number (2 bytes)
            int currentNumber = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
            offset += 2;
            
            // Wait Time (2 bytes)
            int waitTime = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
            offset += 2;
            
            // Status (1 byte) - we can ignore for now or use it later
            offset += 1;
            
            // Create AdvertisedQueue
            return new AdvertisedQueue(queueId, queueName, currentNumber, waitTime, deviceAddress);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing advertisement data", e);
            return null;
        }
    }
    
    public List<AdvertisedQueue> getFoundQueues() {
        return new ArrayList<>(foundQueues);
    }
}
