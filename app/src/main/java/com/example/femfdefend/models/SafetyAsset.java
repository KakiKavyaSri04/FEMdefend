package com.example.femfdefend.models;

import com.google.android.gms.maps.model.LatLng;

public class SafetyAsset {
    public enum AssetType { CCTV, POLICE, HOSPITAL, PHARMACY, STREET_LIGHT }
    
    private final LatLng location;
    private final AssetType type;
    private final String name;

    public SafetyAsset(LatLng location, AssetType type, String name) {
        this.location = location;
        this.type = type;
        this.name = name;
    }

    public LatLng getLocation() { return location; }
    public AssetType getType() { return type; }
    public String getName() { return name; }
}
