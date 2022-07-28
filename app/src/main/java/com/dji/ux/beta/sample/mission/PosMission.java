package com.dji.ux.beta.sample.mission;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dji.ux.beta.sample.R;
import com.dji.ux.beta.sample.utils.ToastUtils;

import dji.common.error.DJIError;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointMissionEvent;
import dji.common.mission.hotpoint.HotpointMissionState;
import dji.common.mission.hotpoint.HotpointStartPoint;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.mission.hotpoint.HotpointMissionOperator;
import dji.sdk.mission.hotpoint.HotpointMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;
//TODO: qui ho utilizzato il toast di ToastUtils
//TODO: mandare gli stati delle missioni alla plancia quando viene avviata/stoppata ecc..

public class PosMission {

    private static final String TAG = PosMission.class.getSimpleName();

    private HotpointMissionOperator instance;
    private final HotpointStartPoint hotpointStartPoint = HotpointStartPoint.NEAREST;
    private final HotpointHeading hotpointHeading = HotpointHeading.TOWARDS_HOT_POINT;

    private final Context mContext;

    public PosMission(Context mContext) {
        this.mContext = mContext;
    }

    private void setResultToToastCallback(Context mContext, String s) {
        Looper.prepare();
        Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
        Looper.loop();
    }

    public HotpointMissionOperator getHotpointM(){
        if (instance==null){
            if(DJISDKManager.getInstance().getMissionControl()!=null){
                instance = DJISDKManager.getInstance().getMissionControl().getHotpointMissionOperator();
            }
        }
        return instance;
    }

    public String getHPState(){
        return getHotpointM().getCurrentState().toString();
    }

    public void addListenerHotpoint() {
        if (getHotpointM() != null){
            getHotpointM().addListener(hotpointEventNotificationListener);
        }
    }

    public void removeListenerHotpoint(){
        if (getHotpointM() != null){
            getHotpointM().removeListener(hotpointEventNotificationListener);
        }
    }

    private final HotpointMissionOperatorListener hotpointEventNotificationListener = new HotpointMissionOperatorListener() {
        @Override
        public void onExecutionUpdate(@NonNull HotpointMissionEvent hotpointMissionEvent) {

        }

        @Override
        public void onExecutionStart() {
        }

        @Override
        public void onExecutionFinish(@Nullable DJIError djiError) {
            ToastUtils.seToToast(mContext, "Execution of HotpointMission finished: " + (djiError == null ? "Success!" : djiError.getDescription()));
        }
    };

    public void startHotPoint(SharedPreferences preferences){
        Log.i(TAG, "Starting HotpointMission, current state: " + getHPState());
        if (getHPState().equals(HotpointMissionState.READY_TO_EXECUTE.toString())){
            double latitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.latitude_pos), ""));
            double longitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.longitude_pos), ""));
            double altitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.altitude_pos), ""));
            double radius = Double.parseDouble(preferences.getString(mContext.getString(R.string.radius_pos), "5.0"));
            float angularVel = 20.0f;

            HotpointMission mHotpointMission = new HotpointMission(new LocationCoordinate2D(latitude, longitude), altitude, radius,
                    angularVel, true, hotpointStartPoint, hotpointHeading);

            Log.i(TAG,  "Altitude: " + altitude + "\nLatitude: " + latitude + "\nLongitude: " + longitude + "\nradius: " + radius);
            Log.i(TAG, "Hotpoint " + mHotpointMission.getHotpoint().toString());

            getHotpointM().startMission(mHotpointMission, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        Log.i(TAG, "Error while starting HotpointMission, current state: " + getHPState() + ", ERROR: " + djiError.getDescription());
                        setResultToToastCallback(mContext, "Could not start hotpoint mission: " + djiError.getDescription());
                    } else {
                        Log.i(TAG, "HotpointMission started Successfully! Current state: " + getHPState());
                        setResultToToastCallback(mContext, "HotpointMission started Successfully!");
                        //TODO: inviare alla plancia
                    }
                }
            });
        }
    }

    public void pauseHotPoint(){
        if (getHPState().equals(WaypointMissionState.EXECUTING.toString())){
            getHotpointM().pause(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.i(TAG, "HotpointMission Paused: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    setResultToToastCallback(mContext,"HotpointMission Paused: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    //TODO: inviare alla plancia
                }
            });
        } else {
            ToastUtils.seToToast(mContext,"Couldn't pause HotpointMission because state is not executing, current state: " + getHPState());
        }
    }

    public void resumeHotPoint(){
        if (getHPState().equals(WaypointMissionState.EXECUTION_PAUSED.toString())){
            getHotpointM().resume(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.i(TAG, "HotpointMission Resumed: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    setResultToToastCallback(mContext,"HotpointMission Resumed: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    //TODO: inviare alla plancia
                }
            });
        } else {
            ToastUtils.seToToast(mContext,"Couldn't resume HotpointMission because state is not paused, current state: " + getHPState());
        }
    }

    public void stopHotPoint(){
        if (getHPState().equals(WaypointMissionState.EXECUTING.toString()) || getHPState().equals(WaypointMissionState.EXECUTION_PAUSED.toString())){
            getHotpointM().stop(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.i(TAG, "HotpointMission Stopped: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    setResultToToastCallback(mContext,"HotpointMission Stopped: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    //TODO: inviare alla plancia
                }
            });
        }
        else {
            ToastUtils.seToToast(mContext, "Couldn't stop HotpointMission because state is not executing or paused, current state: " + getHPState());
        }
    }
}
