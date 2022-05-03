package com.dji.ux.beta.sample.cameraview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJIBitmapDescriptor;
import com.dji.mapkit.core.models.DJIBitmapDescriptorFactory;
import com.dji.mapkit.core.models.DJILatLng;
import com.dji.mapkit.core.models.annotations.DJICircle;
import com.dji.mapkit.core.models.annotations.DJICircleOptions;
import com.dji.mapkit.core.models.annotations.DJIMarker;
import com.dji.mapkit.core.models.annotations.DJIMarkerOptions;
import com.dji.ux.beta.sample.R;
import com.dji.ux.beta.sample.mission.FollowMission;
import com.dji.ux.beta.sample.mission.GPSPlancia;
import com.dji.ux.beta.sample.mission.PointMission;
import com.dji.ux.beta.sample.mission.PosMission;
import com.dji.ux.beta.sample.utils.DroneState;
import com.dji.ux.beta.sample.utils.ToastUtils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.common.airlink.PhysicalSource;
import dji.common.flightcontroller.flyzone.FlyZoneCategory;
import dji.common.mission.hotpoint.HotpointMissionState;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;
import dji.sdk.sdkmanager.LiveVideoBitRateMode;
import dji.sdk.sdkmanager.LiveVideoResolution;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.ux.beta.core.extension.ViewExtensions;
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.widget.fpv.FPVWidget;
import dji.ux.beta.core.widget.systemstatus.SystemStatusWidget;
import dji.ux.beta.core.widget.useraccount.UserAccountLoginWidget;
import dji.ux.beta.map.widget.map.MapWidget;

public class CameraActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    private static final String TAG = CameraActivity.class.getSimpleName();

    //private final String liveShowUrl = "rtmp://192.168.43.225/live/drone"; //hotspot
    private final String liveShowUrl = "rtmp://192.168.1.100/live/drone"; //localhost
    //private final String liveShowUrl = "rtmp://146.48.85.126/live/drone"; //eduroam ethernet
    //private final String liveShowUrl = "rtmp://146.48.54.180/live/drone"; //eduroam WiFi

    private final String ACTION = "FROM CAMERA";

    @BindView(R.id.widget_fpv)
    protected FPVWidget fpvWidget;
    @BindView(R.id.widget_fpv_interaction)
    protected FPVInteractionWidget fpvInteractionWidget;
    @BindView(R.id.widget_map)
    protected MapWidget mapWidget;
    @BindView(R.id.widget_secondary_fpv)
    protected FPVWidget secondaryFPVWidget;
    @BindView(R.id.root_view)
    protected ConstraintLayout parentView;
    @BindView(R.id.widget_panel_system_status_list)
    protected SystemStatusListPanelWidget systemStatusListPanelWidget;
    //@BindView(R.id.live_button)
    private FloatingActionButton streamButton;
    private FloatingActionButton stopSearch;
    private FloatingActionButton wayPointSearch;

    private boolean isMapMini = true;
    private int widgetHeight;
    private int widgetWidth;
    private int widgetMargin;
    private int deviceWidth;
    private int deviceHeight;
    private CompositeDisposable compositeDisposable;
    private UserAccountLoginWidget userAccountLoginWidget;
    private int dialogCount = 0;
    private final double radiusInMeters = 20.0;
    private DJICircle mCircle;
    private LatLng centerRadius;
    private LatLng currentPersonPos;

    private LiveStreamManager.OnLiveChangeListener listener;
    private final LiveStreamManager.LiveStreamVideoSource currentVideoSource = LiveStreamManager.LiveStreamVideoSource.Primary;

    private float waypointSpeed = 5.0f; //range [2,15] m/s.

    private final PointMission mPointMission = new PointMission(this);
    private final FollowMission mFollowMission = new FollowMission(this);
    private final PosMission mPosMission = new PosMission(this);

    private List<DJIMarker> listMarker = new ArrayList<>();

    //TODO: settare button a non clickable quando drone non connesso
    //TODO: sistemare le icone nella top bar (troppo piccole)
    //TODO: mostrare tutti i toast sfruttando metodo setResultToast
    //TODO: instance.setAutoFlightSpeed();
    //TODO: setExitMissionOnRCSignalLostEnabled(boolean enabled)
    //TODO: metodo stile fromActivitytoService per notificare la plancia degli stati della missione
    //TODO: far scegliere angolo gimbalpitch


    private void setResultToToast(final String string){
        CameraActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        sharedPreferences = getSharedPreferences(getString(R.string.my_pref), Context.MODE_PRIVATE);

        widgetHeight = (int) getResources().getDimension(R.dimen.mini_map_height);
        widgetWidth = (int) getResources().getDimension(R.dimen.mini_map_width);
        widgetMargin = (int) getResources().getDimension(R.dimen.mini_map_margin);

        if (isLiveStreamManagerOn()){
            Log.d(TAG, "live streaming manager on");
            DJISDKManager.getInstance().getLiveStreamManager().registerListener(listener);
        }

        //add listener for waypoint
        mPointMission.addListenerWaypoint();
        mFollowMission.addListenerFollow();
        mPosMission.addListenerHotpoint();
        //Log.i(TAG, "on create wp state " + mPointMission.getWaypointMissionState());

        streamButton = findViewById(R.id.streamBtn);
        streamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!DJISDKManager.getInstance().getLiveStreamManager().isStreaming()){
                    startLiveStream();
                    streamButton.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.baseline_cast_connected_black_48));
                } else {
                    stopLiveShow();
                    streamButton.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.baseline_cast_black_48));
                }
            }
        });

        stopSearch = findViewById(R.id.stopSearch);
        //stopSearch.setVisibility(View.GONE);
        stopSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPosMission.getHPState().equals(HotpointMissionState.EXECUTING.toString()) || mPosMission.getHPState().equals(HotpointMissionState.EXECUTION_PAUSED.toString())){
                    mPosMission.stopHotPoint();
                    mPointMission.configWaypointMission(waypointSpeed);
                    mPointMission.uploadWayPointMission();
                    mPointMission.startWaypointMission();
                    //stopSearch.setVisibility(View.GONE);
                }
            }
        });

        wayPointSearch = findViewById(R.id.searchBtn);
        wayPointSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPointMission.getWaypointMissionState().equals(WaypointMissionState.EXECUTING.toString()) || mPointMission.getWaypointMissionState().equals(WaypointMissionState.EXECUTION_PAUSED.toString())){
                    mPointMission.stopWaypointMission();
                    Toast.makeText(getApplicationContext(), "Waypoint search stopped", Toast.LENGTH_SHORT).show();
                    wayPointSearch.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_start_search));
                } else {
                    deletePositions();
                    fromActivityToService("mob_mission");
                    wayPointSearch.setImageDrawable(ContextCompat.getDrawable(getBaseContext(), R.drawable.ic_stop_search));
                }
            }
        });

        initListener();
        initSettings();

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;

        ButterKnife.bind(this);
        mapWidget.initGoogleMap(map -> {
            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            //define home location as center radius
            centerRadius = DroneState.getHomeLocation();
            Log.i(TAG, "homeLocation " + centerRadius.toString());
            DJILatLng latLng = new DJILatLng(centerRadius.latitude, centerRadius.longitude);
            DJICircleOptions circleOptions = new DJICircleOptions().center(latLng)
                    .radius(radiusInMeters).fillColor(getResources().getColor(R.color.background_blue))
                    .strokeColor(getResources().getColor(R.color.background_blue)).strokeWidth(8);
            mCircle = mapWidget.getMap().addSingleCircle(circleOptions);
            map.setOnMapClickListener(new DJIMap.OnMapClickListener() {
                @Override
                public void onMapClick(DJILatLng djiLatLng) {
                    onViewClick(mapWidget);
                    //stopLiveShow();
                    //startLiveStream();
                }
            });
        });
        mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.AUTHORIZATION, true);
        mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.WARNING, true);
        mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.ENHANCED_WARNING, true);
        mapWidget.getFlyZoneHelper().setFlyZoneVisible(FlyZoneCategory.RESTRICTED, true);
        mapWidget.getUserAccountLoginWidget().setVisibility(View.GONE);
        mapWidget.onCreate(savedInstanceState);

        // Setup top bar state callbacks
        TopBarPanelWidget topBarPanel = findViewById(R.id.panel_top_bar);
        SystemStatusWidget systemStatusWidget = topBarPanel.getSystemStatusWidget();
        if (systemStatusWidget != null){
            systemStatusWidget.setStateChangeCallback(findViewById(R.id.widget_panel_system_status_list));
        }

        if(!DJISDKManager.getInstance().getLiveStreamManager().isStreaming()){ startLiveStream();}
    }


    // -------------- STREAMING REGION ----------------------

    private void initSettings(){
        if (!isLiveStreamManagerOn()) {
            return;
        }
        //DJISDKManager.getInstance().allowStreamWhenAppInBackground(true);
        DJISDKManager.getInstance().getLiveStreamManager().setAudioMuted(true);
        DJISDKManager.getInstance().getLiveStreamManager().setLiveVideoBitRateMode(LiveVideoBitRateMode.AUTO);
        DJISDKManager.getInstance().getLiveStreamManager().setLiveVideoResolution(LiveVideoResolution.VIDEO_RESOLUTION_1280_720); //per il momento quella con latenza/qualità migliore
        DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(liveShowUrl);
        DJISDKManager.getInstance().getLiveStreamManager().setVideoEncodingEnabled(false);
        DJISDKManager.getInstance().getLiveStreamManager().setAudioStreamingEnabled(false);
    }

    private void initListener() {
        listener = new LiveStreamManager.OnLiveChangeListener() {
            @Override
            public void onStatusChanged(int i) {
                ToastUtils.setResultToToast("status changed : " + i);
            }
        };
    }

    private boolean isLiveStreamManagerOn() {
        if(DJISDKManager.getInstance().getLiveStreamManager()==null){
            ToastUtils.setResultToToast("No live stream manager!");
            return false;
        }
        return true;
    }

    void startLiveStream(){
        Toast.makeText(getApplicationContext(), "start live show: " + isLiveStreamManagerOn(), Toast.LENGTH_SHORT).show();

        if (!isLiveStreamManagerOn()) {
            return;
        }
        if(DJISDKManager.getInstance().getLiveStreamManager().isStreaming()){
            Toast.makeText(getApplicationContext(), "already started", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread() {
            @Override
            public void run() {
                final int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                DJISDKManager.getInstance().getLiveStreamManager().setStartTime();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplication(), "RESULT: " + result, Toast.LENGTH_LONG).show();
                        if(result == 0){
                            fromActivityToService("isStreaming");
                        }
                    }
                });
            }
        }.start();
    }

    private void stopLiveShow() {
        if (!isLiveStreamManagerOn()) {
            return;
        }
        DJISDKManager.getInstance().getLiveStreamManager().stopStream();
        ToastUtils.setResultToToast("Stop Live Show");
    }
    // ------- END STREAMING REGION ---------------


    // ------- MISSIONS REGION ---------------

    private void drawWayPoint(List<GPSPlancia> listWP){
        int vector = R.drawable.uxsdk_ic_telemetry_location;
        if (mapWidget.getMap() != null){
            for (GPSPlancia gps : listWP) {
                DJILatLng latLng = new DJILatLng(gps.getLatitude(), gps.getLongitude());
                DJIMarker markerName = mapWidget.getMap().addMarker(new DJIMarkerOptions()
                        .position(latLng)
                        .icon(bitmapDescriptorFromVector(this, vector)));

                listMarker.add(markerName);
            }
        }
    }

    private void drawHotpoint(){
        int strokeColor = 0xffff0000; //red outline
        int shadeColor = 0x44ff0000; //opaque red fill
        double latitude = Double.parseDouble(sharedPreferences.getString(getString(R.string.latitude_pos), ""));
        double longitude = Double.parseDouble(sharedPreferences.getString(getString(R.string.longitude_pos), ""));
        int vector = R.drawable.ic_noun_drrowning;
        if(mapWidget.getMap()!=null){
            DJILatLng latLng = new DJILatLng(latitude, longitude);
            DJICircleOptions circleOptions = new DJICircleOptions().center(latLng)
                    .radius(radiusInMeters).fillColor(shadeColor).strokeColor(strokeColor).strokeWidth(8);
            mapWidget.getMap().addMarker(new DJIMarkerOptions()
                    .position(latLng)
                    .icon(bitmapDescriptorFromVector(this, vector)));
            mCircle = mapWidget.getMap().addSingleCircle(circleOptions);
        }
    }

    private void handleWPMissionButton(){
        if(mPointMission.getWaypointMissionState().equals(WaypointMissionState.EXECUTING.toString())){
            Toast.makeText(this, "Search already running!", Toast.LENGTH_SHORT).show();
        } else {
            List<GPSPlancia> cleanedList = GPSPlancia.getCleanedGPSList();
            drawWayPoint(cleanedList);
            mPointMission.createWaypointFromList(cleanedList);
            mPointMission.configWaypointMission(waypointSpeed);
            mPointMission.uploadWayPointMission();
            mPointMission.startWaypointMission();
        }
    }

    private void handleSearchPerson() {

        double latNewPos = Double.parseDouble(sharedPreferences.getString(getString(R.string.latitude_pos), ""));
        double longNewPos = Double.parseDouble(sharedPreferences.getString(getString(R.string.longitude_pos), ""));
        currentPersonPos = new LatLng(latNewPos, longNewPos);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("A person has been detected in open sea.");
        builder.setCancelable(false);
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                dialogCount = 0;
            }
        });
        builder.setIcon(R.drawable.ic_noun_drrowning);
        builder.setTitle("Person Detected!");
        AlertDialog alert = builder.create();

        if(ckeckPersonInRadius(centerRadius, currentPersonPos)){
            if (dialogCount == 0) {
                alert.show();
                dialogCount = 1;
            }
            if(mPointMission.getWaypointMissionState().equals(WaypointMissionState.EXECUTING.toString())){
                mPointMission.stopWaypointMission();
                deletePositionsCount();
            }
            drawHotpoint();
            Log.i(TAG, mPosMission.getHPState());
            mPosMission.startHotPoint(sharedPreferences);
            if(mPosMission.getHPState().equals(HotpointMissionState.EXECUTING.toString())){
                stopSearch.setVisibility(View.VISIBLE);
            }
            centerRadius = currentPersonPos;
        }
    }

    private boolean ckeckPersonInRadius(LatLng oldPos, LatLng newPos){
        double distanceMeters = SphericalUtil.computeDistanceBetween(oldPos, newPos);

        if(distanceMeters < radiusInMeters){
            Toast.makeText(this, "Found person in no-go zone", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            mCircle.remove();
        }
        return true;
    }


    private void deletePositions(){
        mPointMission.deleteWaypoint();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mapWidget.getMap()!=null) {
                    if(listMarker!=null){
                        for(DJIMarker marker : listMarker){
                            marker.remove();
                        }
                    }
                }
            }
        });
    }

    private void deletePositionsCount(){
        mPointMission.deleteWPCount();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mapWidget.getMap()!=null && listMarker!= null){
                    listMarker.subList(0, mPointMission.getWayPointCount()).clear();
                }
            }
        });
    }

    private void handleMission(String action){
        //TODO:eliminare cooridnare/upload
        switch (action) {
            case "speed_wp":
                waypointSpeed = sharedPreferences.getFloat(getString(R.string.speed_waypoint), 0.0f);
                Log.i(TAG, "speed value in camera act: " + waypointSpeed);
                Toast.makeText(this, "WP speed: " + waypointSpeed, Toast.LENGTH_SHORT).show();
                mPointMission.setWaypointMissionSpeed(waypointSpeed);
                break;
            case "coordinates_wp":
                //handleWPMissionButton();
                //drawWayPoint(GPSPlancia.getGPSPlanciaList());
                mPointMission.createWaypoint(sharedPreferences);
                fromActivityToService("Mission State: " + mPointMission.getWaypointMissionState());
                Log.i(TAG, "coordinates_wp " + mPointMission.getWaypointMissionState());
                break;
            case "upload_waypoint":
                //mPointMission.configWaypointMission(waypointSpeed);
                mPointMission.uploadWayPointMission();
                fromActivityToService("Mission State: " + mPointMission.getWaypointMissionState());
                Log.i(TAG, "upload_mission " + mPointMission.getWaypointMissionState());
                break;
            case "start_waypoint_list":
                handleWPMissionButton();
                Log.i(TAG, "start_mission " + mPointMission.getWaypointMissionState());
                fromActivityToService("Mission State: " + mPointMission.getWaypointMissionState());
                break;
            case "start_waypoint":
                mPointMission.startWaypointMission();
                Log.i(TAG, "start_mission " + mPointMission.getWaypointMissionState());
                fromActivityToService("Mission State: " + mPointMission.getWaypointMissionState());
                break;
            case "pause_waypoint":
                mPointMission.pauseWaypointMission();
                Log.i(TAG, "pause_mission " + mPointMission.getWaypointMissionState());
                fromActivityToService("Mission State: " + mPointMission.getWaypointMissionState());
                break;
            case "resume_waypoint":
                mPointMission.resumeWaypointMission();
                Log.i(TAG, "resume_mission " + mPointMission.getWaypointMissionState());
                fromActivityToService("Mission State: " + mPointMission.getWaypointMissionState());
                break;
            case "stop_waypoint":
                mPointMission.stopWaypointMission();
                Log.i(TAG, "stop_mission " + mPointMission.getWaypointMissionState());
                fromActivityToService("Mission State: " + mPointMission.getWaypointMissionState());
                break;
            case "del_pos":
                //String msg = deletePositions();
                deletePositions();
                //mPointMission.deleteWaypoint();
                //fromActivityToService(msg);
                break;
            case "start_follow":
                mFollowMission.startFollowShip(sharedPreferences);
                fromActivityToService("Following State: " + mFollowMission.getFollowState());
                break;
            case "stop_follow":
                mFollowMission.stopFollowShip();
                fromActivityToService("Following State: " + mFollowMission.getFollowState());
                break;
            case "start_hotpoint":
                drawHotpoint();
                mPosMission.startHotPoint(sharedPreferences);
                fromActivityToService("Hotpoint State: " + mPosMission.getHPState());
                break;
            case "pause_hotpoint":
                mPosMission.pauseHotPoint();
                fromActivityToService("Hotpoint State: " + mPosMission.getHPState());
                break;
            case "resume_hotpoint":
                mPosMission.resumeHotPoint();
                fromActivityToService("Hotpoint State: " + mPosMission.getHPState());
                break;
            case "stop_hotpoint":
                mPosMission.stopHotPoint();
                fromActivityToService("Hotpoint State: " + mPosMission.getHPState());
                break;
            case "person":
                handleSearchPerson();
                break;
        }
    }
    //------- END MISSIONS REGION ---------------

    private void fromActivityToService(String msg){
        Intent intent = new Intent("FROM CAMERA");
        intent.putExtra("state", msg);
        LocalBroadcastManager.getInstance(CameraActivity.this).sendBroadcast(intent);
    }

    private final BroadcastReceiver missionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if("FROM_SERVER".equals(intent.getAction())){
                String msg = intent.getStringExtra("mission");
                handleMission(msg);
            }
        }
    };

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
        if (isLiveStreamManagerOn()){
            DJISDKManager.getInstance().getLiveStreamManager().unregisterListener(listener);
        }
        mPointMission.removeListenerWaypoint();
        mPosMission.removeListenerHotpoint();
        mFollowMission.removeListenerFollow();
        LocalBroadcastManager.getInstance(CameraActivity.this).unregisterReceiver(missionReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("FROM_SERVER");
        LocalBroadcastManager.getInstance(CameraActivity.this).registerReceiver(missionReceiver, filter);
        mapWidget.onResume();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(secondaryFPVWidget.getCameraName()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateSecondaryVideoVisibility));

        compositeDisposable.add(systemStatusListPanelWidget.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pressed -> {
                    if (pressed) {
                        ViewExtensions.hide(systemStatusListPanelWidget);
                    }
                }));
    }

    @Override
    protected void onPause() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        mapWidget.onPause();
        IntentFilter filter = new IntentFilter("FROM_SERVER");
        LocalBroadcastManager.getInstance(CameraActivity.this).registerReceiver(missionReceiver, filter);
        super.onPause();
    }
    //endregion

    //region Utils

    /**
     * Handles a click event on the FPV widget
     */
    @OnClick(R.id.widget_fpv)
    public void onFPVClick() {
        onViewClick(fpvWidget);
    }

    /**
     * Handles a click event on the secondary FPV widget
     */
    @OnClick(R.id.widget_secondary_fpv)
    public void onSecondaryFPVClick() {
        swapVideoSource();
    }


    /**
     * Swaps the FPV and Map Widgets.
     *
     * @param view The thumbnail view that was clicked.
     */

    private void onViewClick(View view) {
        if (view == fpvWidget && !isMapMini){
            //reorder widgets
            parentView.removeView(fpvWidget);
            parentView.addView(fpvWidget, 0);
            //resize widgets
            resizeViews(fpvWidget, mapWidget);
            //enable interaction on FPV
            fpvInteractionWidget.setInteractionEnabled(true);
            //disable user login widget on map
            //userAccountLoginWidget.setVisibility(View.GONE);
            isMapMini = true;
            //stopLiveShow();
            //startLiveStream();
        } else if (view == mapWidget && isMapMini) {
            //reorder widgets
            parentView.removeView(fpvWidget);
            parentView.addView(fpvWidget, parentView.indexOfChild(mapWidget) + 1);
            //resize widgets
            resizeViews(mapWidget, fpvWidget);
            //disable interaction on FPV
            fpvInteractionWidget.setInteractionEnabled(false);
            //enable user login widget on map
            //userAccountLoginWidget.setVisibility(View.VISIBLE);
            isMapMini = false;
        }
    }

    /**
     * Helper method to resize the FPV and Map Widgets.
     *
     * @param viewToEnlarge The view that needs to be enlarged to full screen.
     * @param viewToShrink  The view that needs to be shrunk to a thumbnail.
     */
    private void resizeViews(View viewToEnlarge, View viewToShrink) {
        //enlarge first widget
        ResizeAnimation enlargeAnimation = new ResizeAnimation(viewToEnlarge, widgetWidth, widgetHeight, deviceWidth, deviceHeight, 0);
        viewToEnlarge.startAnimation(enlargeAnimation);

        //shrink second widget
        ResizeAnimation shrinkAnimation = new ResizeAnimation(viewToShrink, deviceWidth, deviceHeight, widgetWidth, widgetHeight, widgetMargin);
        viewToShrink.startAnimation(shrinkAnimation);
    }

    /**
     * Swap the video sources of the FPV and secondary FPV widgets.
     */
    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == SettingDefinitions.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(SettingDefinitions.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(SettingDefinitions.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(SettingDefinitions.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(SettingDefinitions.VideoSource.SECONDARY);
        }
    }

    /**
     * Hide the secondary FPV widget when there is no secondary camera.
     *
     * @param cameraName The name of the secondary camera.
     */
    private void updateSecondaryVideoVisibility(String cameraName) {
        if (cameraName.equals(PhysicalSource.UNKNOWN.name())) {
            secondaryFPVWidget.setVisibility(View.GONE);
        } else {
            secondaryFPVWidget.setVisibility(View.VISIBLE);
        }
    }


    //function to use custom icon for maps
    /*
    private DJIBitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorDrawableResourceId) {
        Drawable background = ContextCompat.getDrawable(context, R.drawable.outline_pin_drop_black_24);
        background.setBounds(0, 0, background.getIntrinsicWidth(), background.getIntrinsicHeight());
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId);
        vectorDrawable.setBounds(40, 20, vectorDrawable.getIntrinsicWidth() + 40, vectorDrawable.getIntrinsicHeight() + 20);
        Bitmap bitmap = Bitmap.createBitmap(background.getIntrinsicWidth(), background.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        background.draw(canvas);
        vectorDrawable.draw(canvas);
        return DJIBitmapDescriptorFactory.fromBitmap(bitmap);
    }

     */

    private DJIBitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        assert vectorDrawable != null;
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return DJIBitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * Animation to change the size of a view.
     */

    private static class ResizeAnimation extends Animation{

        private static final int DURATION = 300;

        private final View view;
        private final int toHeight;
        private final int fromHeight;
        private final int toWidth;
        private final int fromWidth;
        private final int margin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            this.toHeight = toHeight;
            this.toWidth = toWidth;
            this.fromHeight = fromHeight;
            this.fromWidth = fromWidth;
            view = v;
            this.margin = margin;
            setDuration(DURATION);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (toHeight - fromHeight) * interpolatedTime + fromHeight;
            float width = (toWidth - fromWidth) * interpolatedTime + fromWidth;
            ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) view.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = margin;
            p.bottomMargin = margin;
            view.requestLayout();
        }
    }
}