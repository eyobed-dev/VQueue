package et.dev.eyobed.vqueue.model;

public class AdvertisedQueue {
    private String id;
    private String name;
    private int currentNumber;
    private int waitTimeMinutes;
    private String deviceAddress;
    private int rssi;

    public AdvertisedQueue(String id, String name, int currentNumber, int waitTimeMinutes, String deviceAddress, int rssi) {
        this.id = id;
        this.name = name;
        this.currentNumber = currentNumber;
        this.waitTimeMinutes = waitTimeMinutes;
        this.deviceAddress = deviceAddress;
        this.rssi = rssi;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCurrentNumber() {
        return currentNumber;
    }

    public int getWaitTimeMinutes() {
        return waitTimeMinutes;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public int getRssi() {
        return rssi;
    }

    // Setters 
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCurrentNumber(int currentNumber) {
        this.currentNumber = currentNumber;
    }

    public void setWaitTimeMinutes(int waitTimeMinutes) {
        this.waitTimeMinutes = waitTimeMinutes;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public boolean isQueue() {
        return currentNumber >= 0 && waitTimeMinutes >= 0;
    }
}