package com.dji.ux.beta.sample.mission;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dji.mapkit.core.models.DJIBitmapDescriptor;
import com.dji.mapkit.core.models.DJIBitmapDescriptorFactory;
import com.dji.mapkit.core.models.DJILatLng;
import com.dji.mapkit.core.models.annotations.DJIMarkerOptions;
import com.dji.ux.beta.sample.R;
import com.dji.ux.beta.sample.utils.DroneState;

import dji.common.error.DJIError;
import dji.common.mission.followme.FollowMeHeading;
import dji.common.mission.followme.FollowMeMission;
import dji.common.mission.followme.FollowMeMissionEvent;
import dji.common.mission.followme.FollowMeMissionState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.mission.followme.FollowMeMissionOperator;
import dji.sdk.mission.followme.FollowMeMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.ux.beta.map.widget.map.MapWidget;

public class FollowMission {

    private static final String TAG = FollowMission.class.getSimpleName();
    private final Context mContext;

    private FollowMeMissionOperator instance;
    private double latitude;
    private double longitude;

    //TODO: disegnare icona nave che si muove quando cambia posizione

    public FollowMission(Context mContext){
        this.mContext = mContext;
    }

    private void setResultToToast(Context mContext, String s) {
        Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
    }

    public FollowMeMissionOperator getFollowM(){
        if(instance == null){
            if(DJISDKManager.getInstance().getMissionControl()!=null){
                instance=DJISDKManager.getInstance().getMissionControl().getFollowMeMissionOperator();
            }
        }
        return instance;
    }

    public void addListenerFollow() {
        if (getFollowM() != null){
            getFollowM().addListener(followEventNotificationListener);
        }
    }

    public void removeListenerFollow(){
        if (getFollowM() != null){
           getFollowM().removeListener(followEventNotificationListener);
        }
    }

    private final FollowMeMissionOperatorListener followEventNotificationListener = new FollowMeMissionOperatorListener() {
        @Override
        public void onExecutionUpdate(@NonNull FollowMeMissionEvent followMeMissionEvent) {
        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable DJIError djiError) {
            setResultToToast(mContext, "Follow Execution finished: " + (djiError == null ? "Success!" : djiError.getDescription()));
        }
    };

    public void startFollowShip(SharedPreferences preferences){

        latitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.latitude_fm), ""));
        longitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.longitude_fm), ""));
        float altitude = Float.parseFloat(preferences.getString(mContext.getString(R.string.altitude_fm), ""));

        if (getFollowM().getCurrentState().toString().equals(FollowMeMissionState.READY_TO_EXECUTE.toString())) {
            setResultToToast(mContext, "Ready to follow");

            FollowMeMission followMeMission = new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION, latitude, longitude, altitude);
            //followMeEvent = new FollowMeMissionEvent.Builder().distanceToTarget(34);
            Log.i(TAG,  "Altitude: " + altitude + "\nLatitude: " + latitude + "\nLongitude: " + longitude);

            //starts the new mission just created
            getFollowM().startMission(followMeMission, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError==null){
                        setResultToToast(mContext, "Following...");

                        Thread locationUpdateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (getFollowM().getCurrentState().toString().equals(FollowMeMissionState.EXECUTING.toString())){

                                    latitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.latitude_fm), ""));
                                    longitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.longitude_fm), ""));

                                    //TODO: cambiare millisecondi in sleep (consigliati 10Hz)

                                    getFollowM().updateFollowingTarget(new LocationCoordinate2D(latitude, longitude), new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            if (djiError!=null){
                                                //setResultToToast(mContext, "Failed to update target GPS");
                                                Log.i(TAG, "Location updating " + latitude + " " + longitude);
                                            }
                                        }
                                    });
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        Thread.currentThread().interrupt();
                                    }
                                }
                            }
                        });
                        locationUpdateThread.start();
                    } else {
                        setResultToToast(mContext, "Ready but stuck on a result");
                    }
                }
            });
        } else {
            setResultToToast(mContext, "Not ready to execute");
        }
    }

    public void stopFollowShip(){
        getFollowM().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Log.i(TAG, "FollowMission Stopped: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                setResultToToast(mContext,"FollowMission Stop: " + (djiError == null ? "Successfully" : djiError.getDescription()));
            }
        });
    }

    public void provaFollow(SharedPreferences preferences){

        Thread locationUpdate = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {

                    latitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.latitude_fm), ""));
                    longitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.longitude_fm), ""));
                    Log.i(TAG, "Location Value: " + "\n" + "Latitude: " + latitude
                            + "\n" + "Longitude: " + longitude + "\n");

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        locationUpdate.start();

    }

    public String getFollowState() {
        return getFollowM().getCurrentState().toString();
    }
}
