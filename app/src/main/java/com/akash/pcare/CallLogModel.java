package com.akash.pcare;

public class CallLogModel {

    private String number;
    private String contactName;   // New
    private String type;
    private String date;
    private String duration;
    private String callStatus;    // New

    // Constructor with all fields
    public CallLogModel(String number, String contactName, String type, String date, String duration, String callStatus) {
        this.number = number;
        this.contactName = contactName;
        this.type = type;
        this.date = date;
        this.duration = duration;
        this.callStatus = callStatus;
    }



    // Getters
    public String getNumber() { return number; }
    public String getContactName() { return contactName; }
    public String getType() { return type; }
    public String getDate() { return date; }
    public String getDuration() { return duration; }
    public String getCallStatus() { return callStatus; }

    // Setters
    public void setNumber(String number) { this.number = number; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public void setType(String type) { this.type = type; }
    public void setDate(String date) { this.date = date; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setCallStatus(String callStatus) { this.callStatus = callStatus; }
}
