package com.dji.ux.beta.sample.mission;

import android.location.Location;
import android.util.Log;

import com.dji.ux.beta.sample.R;
import com.dji.ux.beta.sample.utils.ToastUtils;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GPSPlancia {

    private static final String TAG = GPSPlancia.class.getSimpleName();
    private double latitude;
    private double longitude;
    private float altitude;
    private static CopyOnWriteArrayList<GPSPlancia> gpsPlanciaList = new CopyOnWriteArrayList<>();
    private static HashSet<GPSPlancia> gpsPlanciaHashSet = new HashSet<>();

    public GPSPlancia(double latitude, double longitude, float altitude){
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public float getAltitude() {
        return altitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public static CopyOnWriteArrayList<GPSPlancia> getGPSPlanciaList() {
        return gpsPlanciaList;
    }

    public static void populateGPSPlancia(String latitudeString, String longitudeString, String altitudeString) {
        double latitude = Double.parseDouble(latitudeString);
        double longitude = Double.parseDouble(longitudeString);
        float altitude = Float.parseFloat(altitudeString);
        GPSPlancia gpsPlancia = new GPSPlancia(latitude, longitude, altitude);
        if(!gpsPlanciaList.contains(gpsPlancia)){
            gpsPlanciaList.add(new GPSPlancia(latitude, longitude, altitude));
        }
    }

    private static boolean checkPosDistance(LatLng oldPos, LatLng currentPos, double distanceMeters){
        double distance = SphericalUtil.computeDistanceBetween(oldPos, currentPos);
        return distance >= distanceMeters;
    }

    //remove waypoints that are closer than interdictionRadius between them
    public static List<GPSPlancia> getCleanedGPSList(double interdictionRadius){
        Log.i(TAG, "Initial GPS list length: " + gpsPlanciaList.size());
        List<GPSPlancia> cleanedList = new ArrayList<>();
        if(!gpsPlanciaList.isEmpty()){
            cleanedList.add(gpsPlanciaList.get(0));
            LatLng oldPos = new LatLng((gpsPlanciaList.get(0).getLatitude()), cleanedList.get(0).getLongitude());
            for (GPSPlancia gps: gpsPlanciaList) {
                LatLng currentPos = new LatLng(gps.getLatitude(), gps.getLongitude());
                if(checkPosDistance(oldPos, currentPos, interdictionRadius)){
                    cleanedList.add(gps);
                    oldPos = currentPos;
                }
                if(cleanedList.size()==99){
                    break;
                }
            }
        } else {
            ToastUtils.setResultToToast("GPS list empty!");
        }

        Log.i(TAG, "Cleaned GPS list length: " + cleanedList.size());
        return cleanedList;
    }

    //remove first waypointCount element from gpsPlanciaList
    public static void updateGPSList(int waypointCount) {
        int wpToRemove = waypointCount/2;
        Log.i(TAG, "Initial GPS list length: " + gpsPlanciaList.size() + ", waypointCount: " + waypointCount + ", therefore removing " + wpToRemove + " waypoints");
        //TODO capire perche' serve il /2
        for (int i = 0; i < wpToRemove; i++) {
            gpsPlanciaList.remove(0);
        }
        Log.i(TAG, "Updated GPS list length: " + gpsPlanciaList.size());
    }
}
