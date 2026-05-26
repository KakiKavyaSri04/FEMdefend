package com.example.femfdefend.models;

public class EmergencyContact {
    private String id;
    private String name;
    private String phone;
    private String relationship;

    // Required empty constructor for Firebase
    public EmergencyContact() {
    }

    public EmergencyContact(String id, String name, String phone, String relationship) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmergencyContact that = (EmergencyContact) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
} 