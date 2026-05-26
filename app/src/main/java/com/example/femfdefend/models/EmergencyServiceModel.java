package com.example.femfdefend.models;

import com.google.firebase.firestore.GeoPoint;

public class EmergencyServiceModel {
    private String id;
    private String name;
    private String serviceType; // POLICE, HOSPITAL, NGO
    private String contactPhone;
    private String address;
    private GeoPoint location;
    private boolean isOpen24Hours;
    private String email;
    private String website;
    private String emergencyNumber;

    // Empty constructor for Firestore
    public EmergencyServiceModel() {}

    public EmergencyServiceModel(String name, String serviceType, String contactPhone, 
            String address, GeoPoint location, boolean isOpen24Hours) {
        this.name = name;
        this.serviceType = serviceType;
        this.contactPhone = contactPhone;
        this.address = address;
        this.location = location;
        this.isOpen24Hours = isOpen24Hours;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }

    public boolean isOpen24Hours() { return isOpen24Hours; }
    public void setOpen24Hours(boolean open24Hours) { isOpen24Hours = open24Hours; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getEmergencyNumber() { return emergencyNumber; }
    public void setEmergencyNumber(String emergencyNumber) { this.emergencyNumber = emergencyNumber; }
}
