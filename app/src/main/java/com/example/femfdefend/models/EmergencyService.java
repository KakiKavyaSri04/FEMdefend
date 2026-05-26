package com.example.femfdefend.models;

public class EmergencyService {
    private String name;
    private String phoneNumber;
    private String type;

    public EmergencyService() {
        // Required empty constructor for Firestore
    }

    public EmergencyService(String name, String phoneNumber, String type) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
