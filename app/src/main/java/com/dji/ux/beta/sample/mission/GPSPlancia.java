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
        if(!getGPSPlanciaList().contains(gpsPlancia)){
            gpsPlanciaList.add(new GPSPlancia(latitude, longitude, altitude));
        }
    }

    private static boolean checkPosDistance(LatLng oldPos, LatLng currentPos, double distanceMeters){
        double distance = SphericalUtil.computeDistanceBetween(oldPos, currentPos);
        return distance >= distanceMeters;
    }

    public static List<GPSPlancia> getCleanedGPSList(){
        Log.i(TAG, "Initial GPS list length: " + getGPSPlanciaList().size());
        List<GPSPlancia> cleanedList = new ArrayList<>();
        if(!getGPSPlanciaList().isEmpty()){
            cleanedList.add(getGPSPlanciaList().get(0));
            LatLng oldPos = new LatLng((getGPSPlanciaList().get(0).getLatitude()), cleanedList.get(0).getLongitude());
            for (GPSPlancia gps: getGPSPlanciaList()) {
                LatLng currentPos = new LatLng(gps.getLatitude(), gps.getLongitude());
                if(checkPosDistance(oldPos, currentPos, 20.0)){
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

}
