package com.example.femfdefend.utils;

import android.content.Context;
import android.util.Log;

import com.example.femfdefend.models.SafetyAsset;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GeoJsonParser {
    private static final String TAG = "GeoJsonParser";
    private static final String FILE_NAME = "safety_assets.geojson";

    public static List<SafetyAsset> parseSafetyAssets(Context context) {
        List<SafetyAsset> assets = new ArrayList<>();
        try {
            // 1. Read file from assets
            InputStream is = context.getAssets().open(FILE_NAME);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            
            String jsonStr = new String(buffer, StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(jsonStr);
            JSONArray features = jsonObject.getJSONArray("features");
            
            Log.d(TAG, "Parsing " + features.length() + " features from GeoJSON...");

            // 2. Parse features based on OpenStreetMap properties schema
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                
                // Get Geometry (Coordinates)
                JSONObject geometry = feature.getJSONObject("geometry");
                String geomType = geometry.getString("type");
                
                if (!"Point".equals(geomType)) continue; // We only support Point features
                
                JSONArray coordinates = geometry.getJSONArray("coordinates");
                double longitude = coordinates.getDouble(0);
                double latitude = coordinates.getDouble(1);
                LatLng latLng = new LatLng(latitude, longitude);

                // Get Properties (Type and Name)
                JSONObject properties = feature.getJSONObject("properties");
                String name = properties.optString("name", "Unnamed Asset");
                
                SafetyAsset.AssetType type = null;
                if (properties.has("amenity")) {
                    String amenity = properties.getString("amenity").toLowerCase();
                    if ("police".equals(amenity)) {
                        type = SafetyAsset.AssetType.POLICE;
                    } else if ("hospital".equals(amenity)) {
                        type = SafetyAsset.AssetType.HOSPITAL;
                    } else if ("pharmacy".equals(amenity)) {
                        type = SafetyAsset.AssetType.PHARMACY;
                    }
                }
                
                if (type == null && properties.has("highway")) {
                    String highway = properties.getString("highway").toLowerCase();
                    if ("street_lamp".equals(highway) || "lighting".equals(highway)) {
                        type = SafetyAsset.AssetType.STREET_LIGHT;
                    }
                }
                
                if (type == null && (properties.has("man_made") || properties.has("surveillance"))) {
                    String manMade = properties.optString("man_made", "").toLowerCase();
                    String surveillance = properties.optString("surveillance", "").toLowerCase();
                    if ("surveillance".equals(manMade) || !surveillance.isEmpty()) {
                        type = SafetyAsset.AssetType.CCTV;
                    }
                }
                
                if (type == null) {
                    continue; // Skip features that do not match our safety assets
                }

                assets.add(new SafetyAsset(latLng, type, name));
            }
            
            Log.i(TAG, "Successfully parsed " + assets.size() + " safety assets.");
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing GeoJSON safety assets: " + e.getMessage(), e);
        }
        return assets;
    }
}
