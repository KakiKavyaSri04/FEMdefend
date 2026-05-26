package com.example.femfdefend.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String uid;
    private String fullName;
    private String email;
    private String phone;
    private String emergencyContact;
    private List<EmergencyContact> emergencyContacts;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String uid, String fullName, String email, String phone, String emergencyContact) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.emergencyContact = emergencyContact;
        this.emergencyContacts = new ArrayList<>();
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public List<EmergencyContact> getEmergencyContacts() {
        return emergencyContacts;
    }

    public void setEmergencyContacts(List<EmergencyContact> emergencyContacts) {
        this.emergencyContacts = emergencyContacts;
    }

    public void addEmergencyContact(EmergencyContact contact) {
        if (emergencyContacts == null) {
            emergencyContacts = new ArrayList<>();
        }
        emergencyContacts.add(contact);
    }

    public void removeEmergencyContact(EmergencyContact contact) {
        if (emergencyContacts != null) {
            emergencyContacts.remove(contact);
        }
    }
} 