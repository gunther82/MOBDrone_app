package com.dji.ux.beta.sample.mission;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dji.ux.beta.sample.R;
import com.dji.ux.beta.sample.cameraview.CameraActivity;
import com.dji.ux.beta.sample.utils.ToastUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecuteState;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;


public class PointMission {

    private static final String TAG = PointMission.class.getSimpleName();

    private WaypointMissionOperator instance;
    public static WaypointMission.Builder waypointMissionBuilder;
    private final WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    //private final WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.GO_HOME; this action if you want the drone to come back to first WP when execution finished
    private final WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;
    private int wayPointCount = 0;
    private int nextWaypointIndex = -1;

    //private float waypointSpeed = 10.0f; //range [2,15] m/s.

    private final Context mContext;

    //TODO: decidere se rimuovere i controlli di stato prima di start(pause/resume/stop (sono presenti in start di follow e pos)

    public PointMission(Context mContext){
        this.mContext = mContext;
    }

    private void setResultToToast(Context mContext, String s) {
        Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
    }

    private void setResultToToastCallback(Context mContext, String s) {
        Looper.prepare();
        Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
        Looper.loop();
    }

    public WaypointMissionOperator getWaypointM (){
        if (instance == null) {
            if(DJISDKManager.getInstance().getMissionControl()!= null) {
                instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
            }
        }
        return instance;
    }

    public void addListenerWaypoint() {
        if (getWaypointM() != null){
            getWaypointM().addListener(waypointEventNotificationListener);
        }
    }

    public void removeListenerWaypoint(){
        if (getWaypointM() != null){
            getWaypointM().removeListener(waypointEventNotificationListener);
        }
    }

    private final WaypointMissionOperatorListener waypointEventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {

        }

        @Override
        public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
            if (waypointMissionUploadEvent.getProgress() != null
                    && waypointMissionUploadEvent.getProgress().isSummaryUploaded
                    && waypointMissionUploadEvent.getProgress().uploadedWaypointIndex == (waypointMissionUploadEvent.getProgress().totalWaypointCount) - 1) {
                        Log.i(TAG, "onUploadUpdate: Mission uploaded successfully, current state: " + getWaypointMissionState());
                        setResultToToastCallback(mContext, "Mission upload successfully!");
            }

//            if(getWaypointMissionState().equals(WaypointMissionState.READY_TO_EXECUTE.toString())) {
//                Log.i(TAG, "Mission uploaded successfully, current state: " + getWaypointMissionState());
//            }
        }

        @Override
        public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
            Log.i(TAG, "onExecutionUpdate ");
            if(waypointMissionExecutionEvent.getProgress().isWaypointReached){
                Log.i(TAG, "Waypoint reached ");
//                wayPointCount++;
//                Log.i(TAG, "Waypoint reached " + wayPointCount);
                //TODO capire perche' viene invocato piu' volte (tipicamente 2) quando raggiunge un waypoint
                nextWaypointIndex = waypointMissionExecutionEvent.getProgress().targetWaypointIndex;
//                Log.i(TAG, "onExecutionUpdate: nextWaypointIndex: " + nextWaypointIndex);
                Log.i(TAG, "Waypoint reached " + nextWaypointIndex);
            }
        }

        @Override
        public void onExecutionStart() {
            Log.i(TAG, "Execution started, number of waypoints: " + waypointMissionBuilder.getWaypointCount());
        }

        @Override
        public void onExecutionFinish(@Nullable @org.jetbrains.annotations.Nullable DJIError djiError) {
            Log.i(TAG, "Execution ended with nextWaypointIndex: " + nextWaypointIndex + " and waypointMissionBuilder.getWaypointCount(): " + waypointMissionBuilder.getWaypointCount());
            if(nextWaypointIndex == waypointMissionBuilder.getWaypointCount()-1) {
                Log.i(TAG, "All waypoints reached");
                deleteWaypoint();
            }
            setResultToToastCallback(mContext, "Execution finished: " + (djiError == null ? "Success!" : djiError.getDescription()));
//            waypointMissionBuilder = null;
        }
    };

    public void createWaypointFromList(List<GPSPlancia> cleanedList){
        if(waypointMissionBuilder == null){
            waypointMissionBuilder = new WaypointMission.Builder();
        }
//        Log.i(TAG, "waypointMissionBuilder.getWaypointCount(): " + waypointMissionBuilder.getWaypointCount());
        if (cleanedList.isEmpty()){
            setResultToToast(mContext, "List of positions is empty!");
        } else {
            for (GPSPlancia gps: cleanedList) {

                Waypoint waypoint = new Waypoint(gps.getLatitude(), gps.getLongitude(), gps.getAltitude());
                //waypoint.addAction(new WaypointAction(WaypointActionType.STAY, 0));
                waypoint.gimbalPitch = -90.0f; //from = -135.0, to = 45.0)
                waypointMissionBuilder.addWaypoint(waypoint);
            }
        }
        Log.i(TAG, "WP MissBuildList " + waypointMissionBuilder.getWaypointList().toString());
        Log.i(TAG, "WP MissBuildList size: " + waypointMissionBuilder.getWaypointList().size());
//        Log.i(TAG, "WP WaypointCount(): " + waypointMissionBuilder.getWaypointCount());
        if(waypointMissionBuilder!= null && !waypointMissionBuilder.getWaypointList().isEmpty()){
            setResultToToast(mContext, "All Positions added to the map!");
        }
    }

    public void configWaypointMission(float waypointSpeed){
        if (waypointMissionBuilder == null){
            waypointMissionBuilder = new WaypointMission.Builder()
                    .finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(waypointSpeed)
                    .maxFlightSpeed(waypointSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                    .setGimbalPitchRotationEnabled(true); //per controllare gimbal pitch
        } else{
            waypointMissionBuilder
                    .finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(waypointSpeed)
                    .maxFlightSpeed(waypointSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                    .setGimbalPitchRotationEnabled(true); //per controllare gimbal pitch
        }

        Log.i(TAG, "is gimbal rotation enabled? " + waypointMissionBuilder.isGimbalPitchRotationEnabled());
        Log.i(TAG, "Auto Flight Speed " + waypointMissionBuilder.getAutoFlightSpeed());

        DJIError error = getWaypointM().loadMission(waypointMissionBuilder.build());
        if(error == null){
            setResultToToast(mContext,"loadMission success");
        } else {
            setResultToToast(mContext,"loadMission failed " + error.getDescription());
            Log.i(TAG, "loadMission failed " + error.getDescription());
        }
    }

    public String getWaypointMissionState(){
        return getWaypointM().getCurrentState().toString();
    }

    public void uploadAndStartWayPointMission(){
        Log.i(TAG, "Uploading WP mission, current state is: " + getWaypointMissionState());
        getWaypointM().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError == null){
                    Log.i(TAG, "Mission uploaded successfully, current state: " + getWaypointMissionState());
//                    setResultToToastCallback(mContext,"Mission upload successfully!");
                    startWaypointMission();
                } else {
                    Log.i(TAG, "Mission upload failed, error: " + djiError.getDescription() + " retrying...");
                    setResultToToastCallback(mContext,"Mission upload failed, error: " + djiError.getDescription() + " retrying...");
                    getWaypointM().retryUploadMission(null);
                }
            }
        });
    }

    public void setWaypointMissionSpeed(float waypointSpeed){ //(@FloatRange(from = -15.0f, to = 15.0f)
        getWaypointM().setAutoFlightSpeed(waypointSpeed, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError == null){
                    setResultToToastCallback(mContext,"Speed set successfully!");
                } else {
                    setResultToToastCallback(mContext,"Unable to set the desired speed: " + djiError.getDescription());
                }
            }
        });
    }

    public void startWaypointMission(){
        Log.i(TAG, "Starting WP mission, current state: " + getWaypointMissionState());
        getWaypointM().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError == null){
                    Log.i(TAG, "WaypointMission started successfully!");
                    setResultToToastCallback(mContext,"Mission start successfully!");
                }
                else {
                    Log.i(TAG, "Error while starting WaypointMission: " + djiError.getDescription());
                    setResultToToastCallback(mContext, "Error while starting WaypointMission: " + djiError.getDescription());
                }
            }
        });
    }

    public int pauseWaypointMission(){
        if (getWaypointMissionState().equals(WaypointMissionState.EXECUTING.toString())){
            getWaypointM().pauseMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.i(TAG, "WaypointMission Pause: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    setResultToToastCallback(mContext,"WaypointMission Pause: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                }
            });
        } else {
            setResultToToast(mContext,"Couldn't pause WaypointMission because state is not executing, current state: " + getWaypointMissionState());
        }
        return nextWaypointIndex;
    }

    public void resumeWaypointMission(){
        if (getWaypointMissionState().equals(WaypointMissionState.EXECUTION_PAUSED.toString())){
            getWaypointM().resumeMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.i(TAG, "WaypointMission Resume: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    setResultToToastCallback(mContext,"WaypointMission Resume: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                }
            });
        } else {
            setResultToToast(mContext,"Couldn't resume WaypointMission because state is not paused, current state: " + getWaypointMissionState());
        }
    }

    public int stopWaypointMission(){
        if (getWaypointMissionState().equals(WaypointMissionState.EXECUTING.toString()) || getWaypointMissionState().equals(WaypointMissionState.EXECUTION_PAUSED.toString())){
            getWaypointM().stopMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.i(TAG, "WaypointMission Stop: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    setResultToToastCallback(mContext,"WaypointMission Stop: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                }
            });
        }
        else {
            setResultToToast(mContext, "Couldn't stop WaypointMission because state is not executing or paused, current state: " + getWaypointMissionState());
        }
        return nextWaypointIndex;
    }

    private boolean deleteWaypointsFromMissionBuilder() {
        if(waypointMissionBuilder!=null) {
            Log.i(TAG, "Deleting: " + waypointMissionBuilder.getWaypointCount() + " elements in waypointMissionBuilder.getWaypointList()");
//            Log.i(TAG, "The waypointMissionBuilder.getWaypointList() contains: " + waypointMissionBuilder.getWaypointList().size() + " elements: " + waypointMissionBuilder.getWaypointList().toString());

            int totalWp = waypointMissionBuilder.getWaypointCount();

            for (int i = 0; i < totalWp; i++) {
                waypointMissionBuilder.removeWaypoint(0);
            }
            Log.i(TAG, "WP MissBuildList after delete" + waypointMissionBuilder.getWaypointList().toString());
            Log.i(TAG, "WP MissBuildCount after delete: " + waypointMissionBuilder.getWaypointCount());

            return true;
        }
        else {
            Log.i(TAG, "waypointMissionBuilder is null, nothing to delete");
            return false;
        }
    }

    //remove all waypoints from waypointMissionBuilder (and set it tu null) and from GPSPlancia
    public void deleteWaypoint(){
        boolean deleted = deleteWaypointsFromMissionBuilder();

        if (deleted) {
            GPSPlancia.getGPSPlanciaList().clear();
            nextWaypointIndex = -1;
            setResultToToast(mContext,"All positions deleted");
        } else {
            setResultToToast(mContext,"No position to delete");
            //return "No position to delete";
        }
//        wayPointCount = 0;
    }

    public int getWayPointCount(){
        return this.wayPointCount;
    }

    public int getNextWaypointIndex() {
        return this.nextWaypointIndex;
    }

    // remove visited waypoints from waypointMissionBuilder and from GPSPlancia
    public void deleteWPCount(){
//        if(wayPointCount>0 && !waypointMissionBuilder.getWaypointList().isEmpty()){
//            waypointMissionBuilder.getWaypointList().subList(0, wayPointCount/2).clear();
//            //TODO capire perche' serve il /2
//        }

        boolean deleted = deleteWaypointsFromMissionBuilder();

        if (deleted) {
            setResultToToast(mContext,"Visited positions deleted");
        }
        if(nextWaypointIndex>=0 && !GPSPlancia.getGPSPlanciaList().isEmpty()){
            GPSPlancia.updateGPSList(nextWaypointIndex);
        }
        nextWaypointIndex = -1;
//        wayPointCount = 0;
    }
}