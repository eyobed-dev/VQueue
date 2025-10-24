package cz.vut.fit.vqueue.model;

public class AdvertisedQueue {
    private String id; // This could be a unique identifier extracted from advertisement
    private String name;
    private int currentNumber;
    private int waitTimeMinutes;
    private String deviceAddress; // The BLE address of the advertising device

    public AdvertisedQueue(String id, String name, int currentNumber, int waitTimeMinutes, String deviceAddress) {
        this.id = id;
        this.name = name;
        this.currentNumber = currentNumber;
        this.waitTimeMinutes = waitTimeMinutes;
        this.deviceAddress = deviceAddress;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public int getCurrentNumber() { return currentNumber; }
    public int getWaitTimeMinutes() { return waitTimeMinutes; }
    public String getDeviceAddress() { return deviceAddress; }

    // Setters (if needed, but for immutable data, they might not be)
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCurrentNumber(int currentNumber) { this.currentNumber = currentNumber; }
    public void setWaitTimeMinutes(int waitTimeMinutes) { this.waitTimeMinutes = waitTimeMinutes; }
    public void setDeviceAddress(String deviceAddress) { this.deviceAddress = deviceAddress; }
}