package cz.vut.fit.vqueue;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import androidx.activity.result.ActivityResultLauncher;

/**
 * MainActivity for the Android Audio Streaming Sender App
 * 
 * This app captures audio from the device's microphone and streams it
 * in real-time over Wi-Fi using UDP packets to a specified IP address.
 */
public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "AudioStreamingSender";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int UDP_PORT = 9786;
    private static final int BUFFER_SIZE_MULTIPLIER = 2;
    
    // UI Components
    private EditText ipAddressEditText;
    private FloatingActionButton startStopButton;
    private TextView statusText;
    private MaterialButton qrScanButton;
    
    // Audio streaming components
    private AudioRecord audioRecord;
    private DatagramSocket datagramSocket;
    private boolean isStreaming = false;
    private Thread streamingThread;
    private Handler mainHandler;
    
    // Audio buffer
    private int bufferSize;
    private byte[] audioBuffer;
    
    // QR Code scanner
    private ActivityResultLauncher<ScanOptions> qrCodeLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize UI components
        initializeUI();
        
        // Initialize handler for UI updates
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize QR code scanner
        initializeQRScanner();
        
        // Check and request permissions
        checkPermissions();
        
        // Initialize audio buffer size
        initializeAudioBuffer();
    }
    
    /**
     * Initialize UI components and set up event listeners
     */
    private void initializeUI() {
        ipAddressEditText = findViewById(R.id.ipAddressEditText);
        startStopButton = findViewById(R.id.startStopButton);
        statusText = findViewById(R.id.statusText);
        qrScanButton = findViewById(R.id.qrScanButton);

        
        // Set up button click listeners
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStreaming) {
                    stopStreaming();
                } else {
                    startStreaming();
                }
            }
        });
        
        qrScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanQRCode();
            }
        });
    }
    
    /**
     * Initialize QR code scanner
     */
    private void initializeQRScanner() {
        qrCodeLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String scannedData = result.getContents();
                Log.d(TAG, "QR Code scanned: " + scannedData);
                
                // Extract IP address from QR code
                String ipAddress = extractIPFromQR(scannedData);
                if (ipAddress != null) {
                    ipAddressEditText.setText(ipAddress);
                    Toast.makeText(this, "IP address set from QR code: " + ipAddress, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    /**
     * Extract IP address from QR code data
     */
    private String extractIPFromQR(String qrData) {
        // Expected format: "AUDIO_STREAMER:192.168.1.100:9786"
        if (qrData.startsWith("AUDIO_STREAMER:")) {
            String[] parts = qrData.split(":");
            if (parts.length >= 2) {
                return parts[1]; // Return IP address
            }
        }
        return null;
    }
    
    /**
     * Start QR code scanning
     */
    private void scanQRCode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan QR code from the screen");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(true);
        options.setOrientationLocked(true);
        
        qrCodeLauncher.launch(options);
    }
    
    /**
     * Initialize audio buffer size based on minimum buffer size
     */
    private void initializeAudioBuffer() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
            Log.e(TAG, "Failed to get minimum buffer size");
            bufferSize = SAMPLE_RATE / 10; // Fallback buffer size
        }
        
        // Use a smaller buffer for lower latency
        bufferSize = Math.max(bufferSize / BUFFER_SIZE_MULTIPLIER, 1024);
        audioBuffer = new byte[bufferSize];
        
        Log.d(TAG, "Audio buffer size: " + bufferSize + " bytes");
    }
    
    /**
     * Check and request necessary permissions
     */
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.RECORD_AUDIO}, 
                PERMISSION_REQUEST_CODE);
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST_CODE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Audio permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Audio permission denied. App cannot function without it.", 
                    Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied. QR scanning will not work.", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Start audio streaming to the specified IP address
     */
    private void startStreaming() {
        String ipAddress = ipAddressEditText.getText().toString().trim();
        
        if (ipAddress.isEmpty()) {
            Toast.makeText(this, "Please enter a valid IP address", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Initialize AudioRecord
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2 // Use larger buffer for AudioRecord initialization
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("AudioRecord initialization failed");
            }
            
            // Initialize DatagramSocket
            datagramSocket = new DatagramSocket();
            
            // Start streaming thread
            isStreaming = true;
            streamingThread = new Thread(new StreamingRunnable(ipAddress));
            streamingThread.start();
            
            // Update UI
            updateUIForStreaming(true);
            
            Toast.makeText(this, "Started streaming to " + ipAddress, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Audio streaming started to " + ipAddress);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start streaming", e);
            Toast.makeText(this, "Failed to start streaming: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
            cleanup();
        }
    }
    
    /**
     * Stop audio streaming
     */
    private void stopStreaming() {
        isStreaming = false;
        
        // Wait for streaming thread to finish
        if (streamingThread != null) {
            try {
                streamingThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for streaming thread to finish", e);
            }
        }
        
        cleanup();
        updateUIForStreaming(false);
        
        Toast.makeText(this, "Streaming stopped", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Audio streaming stopped");
    }
    
    /**
     * Clean up resources
     */
    private void cleanup() {
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord", e);
            }
            audioRecord = null;
        }
        
        if (datagramSocket != null) {
            try {
                datagramSocket.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing DatagramSocket", e);
            }
            datagramSocket = null;
        }
    }
    
    /**
     * Update UI based on streaming state
     */
    private void updateUIForStreaming(boolean streaming) {
        runOnUiThread(() -> {
            if (streaming) {
                // Recording state - red FAB with white microphone
                startStopButton.setBackgroundTintList(getResources().getColorStateList(R.color.primary));
                startStopButton.setImageResource(R.drawable.ic_microphone_recording);
                statusText.setText("Recording... Tap to stop");
                ipAddressEditText.setEnabled(false);
            } else {
                // Ready state - blue FAB with blue microphone
                startStopButton.setBackgroundTintList(getResources().getColorStateList(R.color.black));
                startStopButton.setImageResource(R.drawable.ic_microphone_button);
                statusText.setText("Tap to start recording");
                ipAddressEditText.setEnabled(true);
            }
        });
    }
    
    /**
     * Runnable class for handling audio streaming in a background thread
     */
    private class StreamingRunnable implements Runnable {
        private final String targetIP;
        
        public StreamingRunnable(String targetIP) {
            this.targetIP = targetIP;
        }
        
        @Override
        public void run() {
            try {
                InetAddress targetAddress = InetAddress.getByName(targetIP);
                
                // Start recording
                audioRecord.startRecording();
                
                Log.d(TAG, "Audio recording started, buffer size: " + bufferSize);
                
                while (isStreaming && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    // Read audio data from microphone
                    int bytesRead = audioRecord.read(audioBuffer, 0, bufferSize);
                    
                    if (bytesRead > 0) {
                        // Create UDP packet with audio data
                        DatagramPacket packet = new DatagramPacket(
                            audioBuffer, bytesRead, targetAddress, UDP_PORT);
                        
                        // Send packet
                        datagramSocket.send(packet);
                        
                        Log.v(TAG, "Sent " + bytesRead + " bytes of audio data");
                    } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(TAG, "AudioRecord read error: ERROR_INVALID_OPERATION");
                        break;
                    } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(TAG, "AudioRecord read error: ERROR_BAD_VALUE");
                        break;
                    }
                }
                
            } catch (UnknownHostException e) {
                Log.e(TAG, "Unknown host: " + targetIP, e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this, 
                    "Invalid IP address: " + targetIP, Toast.LENGTH_LONG).show());
            } catch (SocketException e) {
                Log.e(TAG, "Socket error", e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this, 
                    "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                Log.e(TAG, "IO error during streaming", e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this, 
                    "Streaming error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during streaming", e);
                mainHandler.post(() -> Toast.makeText(MainActivity.this, 
                    "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                // Ensure recording is stopped
                if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStreaming();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Optionally stop streaming when app goes to background
        // stopStreaming();
    }
}
