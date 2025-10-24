package cz.vut.fit.vqueue.model;

public class Waiter {
    private int ticketNumber;
    private String deviceId;
    private long joinedAt;
    
    public Waiter(int ticketNumber, String deviceId) {
        this.ticketNumber = ticketNumber;
        this.deviceId = deviceId;
        this.joinedAt = System.currentTimeMillis();
    }
    
    // Getters
    public int getTicketNumber() {
        return ticketNumber;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public long getJoinedAt() {
        return joinedAt;
    }
    
    // Setters
    public void setTicketNumber(int ticketNumber) {
        this.ticketNumber = ticketNumber;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }
}

