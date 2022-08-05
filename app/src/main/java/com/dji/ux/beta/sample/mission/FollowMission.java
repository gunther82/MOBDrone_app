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
import com.dji.ux.beta.sample.utils.ToastUtils;

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
    Thread locationUpdateThread;

    //TODO: disegnare icona nave che si muove quando cambia posizione

    public FollowMission(Context mContext){
        this.mContext = mContext;
    }

    private void setResultToToast(String s) {
        ToastUtils.setResultToToast(s);
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
            Log.i(TAG,  "FollowingMission: onExecutionUpdate state is: " + followMeMissionEvent.getCurrentState() + " distance to target is: " + followMeMissionEvent.getDistanceToTarget());
        }

        @Override
        public void onExecutionStart() {
            Log.i(TAG,  "FollowMission started.");
            setResultToToast("FollowMission started");
        }

        @Override
        public void onExecutionFinish(@Nullable DJIError djiError) {
            Log.i(TAG, "FollowMission Execution finished: " + (djiError == null ? "Success!" : djiError.getDescription()));
            setResultToToast("FollowMission Execution finished: " + (djiError == null ? "Success!" : djiError.getDescription()));
            locationUpdateThread.interrupt();
        }
    };

    public void startFollowShip(SharedPreferences preferences){
        Log.i(TAG, "Starting FollowMission, current state: " + getFollowState());
        latitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.latitude_fm), ""));
        longitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.longitude_fm), ""));
        float altitude = Float.parseFloat(preferences.getString(mContext.getString(R.string.altitude_fm), ""));

        if (getFollowM().getCurrentState().toString().equals(FollowMeMissionState.READY_TO_EXECUTE.toString())) {
            setResultToToast("Ready to follow");

            FollowMeMission followMeMission = new FollowMeMission(FollowMeHeading.TOWARD_FOLLOW_POSITION, latitude, longitude, altitude);
            //followMeEvent = new FollowMeMissionEvent.Builder().distanceToTarget(34);
            Log.i(TAG,  "Altitude: " + altitude + "\nLatitude: " + latitude + "\nLongitude: " + longitude);
            Log.i(TAG,  "FollowMeInfo: " + followMeMission.getLatitude() + " " + followMeMission.getLongitude());

            //starts the new mission just created
            getFollowM().startMission(followMeMission, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError==null){
//                        setResultToToast("Follow mission created successfully, starting follow thread...");
                        Log.i(TAG,  "Follow mission created successfully with location destination: " + getFollowM().getFollowingTarget() + ", starting follow thread...");
                        locationUpdateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (!Thread.currentThread().isInterrupted()) {
//                                    if(getFollowState().equals(FollowMeMissionState.EXECUTING.toString()))
//                                    {
                                        latitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.latitude_fm), ""));
                                        longitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.longitude_fm), ""));
                                        Log.i(TAG, "FollowMission: " + getFollowState() + ", following target: " + getFollowM().getFollowingTarget());
//                                        Log.i(TAG, "FollowMeInfo: " + followMeMission.getLatitude() + " " + followMeMission.getLongitude());

                                        getFollowM().updateFollowingTarget(new LocationCoordinate2D(latitude, longitude), new CommonCallbacks.CompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                if (djiError != null) {
//                                                    Log.i(TAG, "FollowMission: Failed to update target GPS: " + djiError.getDescription());
                                                    //                                                setResultToToast("Failed to update target GPS: " + djiError.getDescription());
                                                } else {
                                                    Log.i(TAG, "Location updating " + latitude + " " + longitude);
                                                    setResultToToast("Location updating " + latitude + " " + longitude);
                                                }
                                            }
                                        });
                                        try {
                                            //TODO: cambiare millisecondi in sleep (consigliati 10Hz)
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                            Thread.currentThread().interrupt();
                                        }
                                    }
//                                }
                            }
                        });
                        locationUpdateThread.start();
                    } else {
                        setResultToToast("FollowMission: error: " + djiError.getDescription());
                        Log.i(TAG, "FollowMission: error: " + djiError.getDescription());
                    }
                }
            });
        } else {
            setResultToToast("Not ready to execute FollowMission");
        }
    }

    public void stopFollowShip(){
        Log.i(TAG, "Stopping FollowMission, current state: " + getFollowState());
        getFollowM().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Log.i(TAG, "FollowMission Stopped: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                setResultToToast("FollowMission Stop: " + (djiError == null ? "Successfully" : djiError.getDescription()));
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
