package com.dji.ux.beta.sample.mission;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dji.ux.beta.sample.R;
import com.dji.ux.beta.sample.utils.ToastUtils;
import com.dji.ux.beta.sample.utils.ToastUtils.*;

import dji.common.error.DJIError;
import dji.common.mission.hotpoint.HotpointHeading;
import dji.common.mission.hotpoint.HotpointMission;
import dji.common.mission.hotpoint.HotpointMissionEvent;
import dji.common.mission.hotpoint.HotpointMissionState;
import dji.common.mission.hotpoint.HotpointStartPoint;
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
            ToastUtils.seToToast(mContext, "Execution finished: " + (djiError == null ? "Success!" : djiError.getDescription()));
        }
    };

    public void startHotPoint(SharedPreferences preferences){

        if (getHPState().equals(HotpointMissionState.READY_TO_EXECUTE.toString())){
            double latitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.latitude_pos), ""));
            double longitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.longitude_pos), ""));
            double altitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.altitude_pos), ""));
            double radius = Double.parseDouble(preferences.getString(mContext.getString(R.string.radius_pos), ""));
            float angularVel = 20.0f;

            HotpointMission mHotpointMission = new HotpointMission(new LocationCoordinate2D(latitude, longitude), altitude, radius,
                    angularVel, true, hotpointStartPoint, hotpointHeading);

            Log.i(TAG,  "Altitude: " + altitude + "\nLatitude: " + latitude + "\nLongitude: " + longitude);
            Log.i(TAG, "Hotpoint " + mHotpointMission.getHotpoint().toString());

            getHotpointM().startMission(mHotpointMission, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        ToastUtils.seToToast(mContext, "could not start hotpoint mission: " + djiError.getDescription());
                        Log.i(TAG, "State startHP " + getHPState());
                    } else {
                        ToastUtils.seToToast(mContext, "Mission Start: " + "Successfully");
                        Log.i(TAG, "Mission Start: " + "Successfully");
                        Log.i(TAG, "State startHP " + getHPState());
                        //TODO: inviare alla plancia
                    }

                }
            });
        }
    }

    public void pauseHotPoint(){
        getHotpointM().pause(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                ToastUtils.seToToast(mContext,"Mission Paused: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                Log.i(TAG, "Mission Paused: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                //TODO: inviare alla plancia
            }
        });
    }

    public void resumeHotPoint(){
        getHotpointM().resume(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                ToastUtils.seToToast(mContext,"Mission Resumed: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                Log.i(TAG, "Mission Resumed: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                //TODO: inviare alla plancia
            }
        });
    }

    public void stopHotPoint(){
        getHotpointM().stop(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                ToastUtils.seToToast(mContext,"Mission Stopped: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                Log.i(TAG, "Mission Stopped: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                //TODO: inviare alla plancia
            }
        });
    }
}
