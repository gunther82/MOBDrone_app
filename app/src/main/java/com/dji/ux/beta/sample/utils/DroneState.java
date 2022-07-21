package com.dji.ux.beta.sample.utils;

/*
Util class that provides info onm drone status such as battery charge, altitude, speed, etc..
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.dji.mapkit.core.models.DJIBitmapDescriptor;
import com.dji.mapkit.core.models.DJIBitmapDescriptorFactory;
import com.dji.ux.beta.sample.R;
import com.dji.ux.beta.sample.SampleApplication;
import com.dji.ux.beta.sample.mission.GPSPlancia;
import com.google.android.gms.maps.model.LatLng;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flighthub.FlightHubManager;
import dji.sdk.flighthub.model.RealTimeFlightData;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;


public class DroneState {

    private static final String TAG = DroneState.class.getSimpleName();
    protected static StringBuilder stringState = new StringBuilder();
    protected static StringBuilder stringStatePlancia = new StringBuilder();
    protected static StringBuilder stringStream = new StringBuilder();
    protected static StringBuilder stringJetson;
    protected static String altitude;
    protected static String latitude;
    protected static String longitude;
    protected static String whichLatitude;
    protected static int batteryCharge;
    private static FlightController flightController;
    private static String tracking;
    private static String searching;
    private static String following;
    private static String serialNumber;
    private static String speed;
    private static LatLng homeLocation;

    public static LatLng getHomeLocation(){
        BaseProduct product = SampleApplication.getProductInstance();
        if (product instanceof Aircraft) { flightController = ((Aircraft) product).getFlightController(); }
        if(flightController != null){
            flightController.getHomeLocation(new CommonCallbacks.CompletionCallbackWith<LocationCoordinate2D>() {
                @Override
                public void onSuccess(LocationCoordinate2D locationCoordinate2D) {
                    homeLocation = new LatLng(locationCoordinate2D.getLatitude(), locationCoordinate2D.getLongitude());
                }

                @Override
                public void onFailure(DJIError djiError) {

                }
            });
        } else{
            homeLocation = new LatLng(43.719259015524976, 10.421048893480233);
        }
        return homeLocation;
    }


    private static void getDroneLocation(BaseProduct product){

        if (product instanceof Aircraft) { flightController = ((Aircraft) product).getFlightController(); }

        if (flightController !=null){
            flightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(@NonNull @NotNull FlightControllerState flightControllerState) {

                    if (flightControllerState.getAircraftLocation() != null) {
                        if(!Double.isNaN(flightControllerState.getAircraftLocation().getLatitude()) && !Double.isNaN(flightControllerState.getAircraftLocation().getLongitude())) {
                            latitude = String.valueOf(flightControllerState.getAircraftLocation().getLatitude());
                            longitude = String.valueOf(flightControllerState.getAircraftLocation().getLongitude());
                            altitude = String.valueOf(flightControllerState.getAircraftLocation().getAltitude());
                        } else {
                            latitude = "0.0";
                            longitude = "0.0";
                            latitude = "0.0";
                        }
                    }
                }
            });
        }
    }

    public static void getDroneBattery(){

        try {
            SampleApplication.getProductInstance().getBattery().setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState batteryState) {
                    batteryCharge = batteryState.getChargeRemainingInPercent();
                }
            });
        } catch (Exception ignored){
        }
    }

    public static void setTracking(){
        whichLatitude = "Drone Latitude: ";
        //tracking = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator().getCurrentState().toString();
        tracking = DJISDKManager.getInstance().getMissionControl().getHotpointMissionOperator().getCurrentState().toString();
        searching = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator().getCurrentState().toString();
        if(searching.equals("EXECUTING")){
            whichLatitude = "Searching Drone Latitude: ";
        }
        following = DJISDKManager.getInstance().getMissionControl().getFollowMeMissionOperator().getCurrentState().toString();
    }

    public static StringBuilder getDroneState(){

        BaseProduct product = SampleApplication.getProductInstance();
        if (product != null && product.isConnected()) {

            getDroneLocation(product);
            getDroneBattery();
            setTracking();
            stringState.delete(0, stringState.length());
            stringState.append("\n");
            stringState.append("Product Connected!").append("\n");
            stringState.append("Product Name: ").append(product.getModel().getDisplayName()).append("\n");
            stringState.append("Battery charge: ").append(batteryCharge).append("%\n");
            stringState.append(whichLatitude).append(altitude).append("m\n");
            stringState.append("Drone latitude: ").append(latitude).append("\n");
            stringState.append("Drone longitude: ").append(longitude).append("\n");
            stringState.append("Flight State: ").append(tracking).append("\n");
            stringState.append("Following State: ").append(following).append("\n");

        } else {
            stringState.delete(0, stringState.length());
            stringState.append("No product connected.").append("\n");
        }
        return stringState;
    }

    public static StringBuilder getStreamInfo(){
        BaseProduct product = SampleApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if(DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
                stringStream.append("Streaming live on: ").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveUrl()).append("\n");
                long startTime = DJISDKManager.getInstance().getLiveStreamManager().getStartTime();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String sd = sdf.format(new Date(Long.parseLong(String.valueOf(startTime))));
                stringStream.append("Start Time: ").append(sd).append("\n");;
                stringStream.append("Video Resolution: ").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoResolution()).append("\n");;
                stringStream.append("Video BitRate:").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoBitRate()).append(" kpbs\n");
                stringStream.append("Audio BitRate:").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveAudioBitRate()).append(" kpbs\n");
                stringStream.append("Video FPS:").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoFps()).append("\n");
                stringStream.append("Video Cache size:").append(DJISDKManager.getInstance().getLiveStreamManager().getLiveVideoCacheSize()).append(" frame");
            } else {
                stringStream.append("No streaming available.").append("\n");
            }
        } else {
            stringStream.append("No product connected.").append("\n");
        }
        return stringStream;
    }

    public static StringBuilder getDroneStatePlancia(boolean disableConsole){
        BaseProduct product = SampleApplication.getProductInstance();
        if (product != null && product.isConnected()) {

            getDroneLocation(product);
            getDroneBattery();
            setTracking();
//            getRealTimeData();
            getSpeed();
            stringStatePlancia.delete(0, stringStatePlancia.length());
//            stringStatePlancia.append("\n");
            stringStatePlancia.append("true").append("#");
            stringStatePlancia.append(disableConsole).append("#");
            stringStatePlancia.append(batteryCharge).append("#");
            stringStatePlancia.append(speed).append("#");
//            stringStatePlancia.append("0").append("#");
            stringStatePlancia.append(altitude).append("#");
            stringStatePlancia.append(latitude).append("#");
            stringStatePlancia.append(longitude).append("#");
            if(tracking != null && tracking.equals("EXECUTING"))
                stringStatePlancia.append("true").append("#");
            else
                stringStatePlancia.append("false").append("#");

            if(following != null && following.equals("EXECUTING"))
                stringStatePlancia.append("true").append("#");
            else
                stringStatePlancia.append("false").append("#");

            if(searching != null && searching.equals("EXECUTING"))
                stringStatePlancia.append("true").append("#");
            else
                stringStatePlancia.append("false").append("#");
            if(DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
                stringStatePlancia.append(DJISDKManager.getInstance().getLiveStreamManager().getLiveUrl()).append("#");
            } else {
                stringStatePlancia.append("NoStream").append("#");
            }
        } else {
            stringStatePlancia.delete(0, stringStatePlancia.length());
            stringStatePlancia.append("False").append("#");
        }
        return stringStatePlancia;
    }

    public static StringBuilder getGPSJetson(){
        stringJetson = new StringBuilder();
        BaseProduct product = SampleApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            getDroneLocation(product);
            stringJetson.delete(0, stringJetson.length());
            //stringJetson.append("hotpoint_coordinates").append("-"); if we want to automatic search
            stringJetson.append("hotpoint_jetson").append("-");
            stringJetson.append(latitude).append("-");
            stringJetson.append(longitude).append("-");
            stringJetson.append(altitude).append("-");
            stringJetson.append("5").append("\n\r");

        } else {
            stringJetson.append("no_connected").append("\n\r");
        }
        return stringJetson;
    }

    private static void getSpeed() {
        String velX = Float.toString(flightController.getState().getVelocityX());
        String velY = Float.toString(flightController.getState().getVelocityY());
        String velZ = Float.toString(flightController.getState().getVelocityZ());
        speed = velX + ":" + velY + ":" + velZ;
//        Log.i(TAG, "speed " + speed);
    }

    // gets real time flight data of the aircraft with the given serial number
    private static void getRealTimeData() {
        //TODO:se non va togliere commento
        BaseProduct product = SampleApplication.getProductInstance();
        if (product instanceof Aircraft) { flightController = ((Aircraft) product).getFlightController(); }
        flightController.getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
            @Override
            public void onSuccess(String s) {
                serialNumber = s;
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        });
        final ArrayList<String> serialNumbers = new ArrayList<>(1);
        serialNumbers.add(serialNumber);

        try {
            DJISDKManager.getInstance().getFlightHubManager().getAircraftRealTimeFlightData(serialNumbers, new CommonCallbacks.CompletionCallbackWith<List<RealTimeFlightData>>() {
                @Override
                public void onSuccess(List<RealTimeFlightData> realTimeFlightData) {
                    for (RealTimeFlightData s : realTimeFlightData) {
                        speed = String.valueOf(s.getSpeed());
                        Log.i(TAG, "speed " + s.getSpeed());
                    }
                }
                @Override
                public void onFailure(DJIError error) {
                    Log.i(TAG, "getAircraftRealTimeFlightData failed: " + error.getDescription());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}