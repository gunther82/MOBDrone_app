package com.dji.ux.beta.sample.mission;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dji.ux.beta.sample.R;
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

    //private float waypointSpeed = 10.0f; //range [2,15] m/s.

    private final Context mContext;

    //TODO: decidere se rimuovere i controlli di stato prima di start(pause/resume/stop (sono presenti in start di follow e pos)

    public PointMission(Context mContext){
        this.mContext = mContext;
    }

    private void setResultToToast(Context mContext, String s) {
        Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
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
        public void onDownloadUpdate(@NonNull @NotNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {

        }

        @Override
        public void onUploadUpdate(@NonNull @NotNull WaypointMissionUploadEvent waypointMissionUploadEvent) {

        }

        @Override
        public void onExecutionUpdate(@NonNull @NotNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {
            if(waypointMissionExecutionEvent.getProgress().isWaypointReached){
                wayPointCount++;
                Log.i(TAG, "Waypoint reached " + wayPointCount);
                //TODO capire perche' conta i waypoint a 2 a 2 anziche' uno alla volta
            }

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable @org.jetbrains.annotations.Nullable DJIError djiError) {
            //setResultToToast(mContext, "Execution finished: " + (djiError == null ? "Success!" : djiError.getDescription()));
        }
    };

    public void createWaypointFromList(List<GPSPlancia> cleanedList){
        if(waypointMissionBuilder == null){
            waypointMissionBuilder = new WaypointMission.Builder();
        }
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
        if(waypointMissionBuilder!= null && !waypointMissionBuilder.getWaypointList().isEmpty()){
            setResultToToast(mContext, "All Positions added to the map!");
        }
    }

    //TODO old, check if can be removed
    public void createWaypoint(SharedPreferences preferences){

        double latitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.latitude), ""));
        double longitude = Double.parseDouble(preferences.getString(mContext.getString(R.string.longitude), ""));
        float altitude = Float.parseFloat(preferences.getString(mContext.getString(R.string.altitude), ""));

        Waypoint waypoint = new Waypoint(latitude, longitude, altitude);
        waypoint.gimbalPitch = -90.0f; //from = -135.0, to = 45.0)
        //waypoint.speed = waypointSpeed;
        Log.i(TAG, "WP speed " + waypoint.speed + " gimbal " + waypoint.gimbalPitch);

        if (waypointMissionBuilder != null) {
            waypointMissionBuilder.addWaypoint(waypoint);
            setResultToToast(mContext, "Position added to the map!");

        } else {
            waypointMissionBuilder = new WaypointMission.Builder();
            waypointMissionBuilder.addWaypoint(waypoint);
            setResultToToast(mContext, "Position added to the map!");
        }
        Log.i(TAG, "WP MissBuildList " + waypointMissionBuilder.getWaypointList().toString());
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
            setResultToToast(mContext,"loadWaypoint succeded");
        } else {
            setResultToToast(mContext,"loadWaypoint failed " + error.getDescription());
            Log.i(TAG, "loadWaypoint failed " + error.getDescription());
        }
    }

    public String getWaypointMissionState(){
        return getWaypointM().getCurrentState().toString();
    }

    public void uploadWayPointMission(){

        getWaypointM().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                Log.i(TAG, "sono in upload WP mission1 " + getWaypointMissionState());
                if(djiError == null){
                    setResultToToast(mContext,"Mission upload successfully!");
                    Log.i(TAG, "sono in upload WP mission2 " + getWaypointMissionState());
                } else {
                    setResultToToast(mContext,"Mission upload failed, error: " + djiError.getDescription() + " retrying...");
                    Log.i(TAG, "Mission upload failed, error: " + djiError.getDescription() + " retrying...");
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
                    setResultToToast(mContext,"Speed setted succesfully!");
                } else {
                    setResultToToast(mContext,"unable to set the desired speed: " + djiError.getDescription());
                }
            }
        });
    }

    public void startWaypointMission(){
        Log.i(TAG, "sono in start WP mission " + getWaypointMissionState());
        getWaypointM().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError == null){
                    setResultToToast(mContext,"Mission start succesfully!");
                }
                setResultToToast(mContext,"Mission Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                Log.i(TAG, "Mission Start: " + (djiError == null ? "Successfully" : djiError.getDescription()));
            }
        });
    }

    public void pauseWaypointMission(){

        if (getWaypointMissionState().equals(WaypointMissionState.EXECUTING.toString())){
            getWaypointM().pauseMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    setResultToToast(mContext,"Mission Pause: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    Log.i(TAG, "Mission Pause: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                }
            });
        } else {
            setResultToToast(mContext,"Mission Pause State Error: " + getWaypointMissionState());
        }

    }

    public void resumeWaypointMission(){
        if (getWaypointMissionState().equals(WaypointMissionState.EXECUTION_PAUSED.toString())){
            getWaypointM().resumeMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    setResultToToast(mContext,"Mission Resume: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                    Log.i(TAG, "Mission Resume: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                }
            });
        } else {
            setResultToToast(mContext,"Mission Resume State Error: " + getWaypointMissionState());
        }

    }

    public void stopWaypointMission(){
        if (getWaypointMissionState().equals(WaypointMissionState.EXECUTING.toString()) || getWaypointMissionState().equals(WaypointMissionState.EXECUTION_PAUSED.toString())){
            getWaypointM().stopMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    setResultToToast(mContext,"Mission Stop: " + (djiError == null ? "Successfully" : djiError.getDescription()));
                }
            });
        }
        setResultToToast(mContext,"Mission Stop State Error: " + getWaypointMissionState());

    }

    public void deleteWaypoint(){
        //TODO: testarre cosa succede se elimino i waypoint mentre è in esecuzione la missione --> la missione continua
        if(waypointMissionBuilder!=null && waypointMissionBuilder.getWaypointList().size()>0){
            waypointMissionBuilder.getWaypointList().clear();
            GPSPlancia.getGPSPlanciaList().clear();
            Log.i(TAG, "WP MissBuildList after delete" + waypointMissionBuilder.getWaypointList().toString());
            waypointMissionBuilder=null;
            setResultToToast(mContext,"All positions deleted");
            //return "All positions deleted.";
        } else {
            setResultToToast(mContext,"No position to delete");
            //return "No position to delete";
        }
    }

    public int getWayPointCount(){
        return this.wayPointCount;
    }

    public void deleteWPCount(){
        if(wayPointCount>0 && !waypointMissionBuilder.getWaypointList().isEmpty()){
            waypointMissionBuilder.getWaypointList().subList(0, wayPointCount/2).clear();
            //TODO capire perche' serve il /2
        }
    }
}