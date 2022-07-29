/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.dji.ux.beta.sample;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dji.ux.beta.sample.cameraview.CameraActivity;
import com.dji.ux.beta.sample.connection.TcpClientService;
import com.dji.ux.beta.sample.utils.ToastUtils;
import com.ncorti.slidetoact.SlideToActView;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Handles the connection to the product and provides links to the different test activities. Also
 * shows the current connection state and displays logs for the different steps of the SDK
 * registration process.
 */
//@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {

//    SharedPreferences sharedPreferences;

    //region Constants
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE, // Gimbal rotation
            Manifest.permission.INTERNET, // API requests
            Manifest.permission.ACCESS_WIFI_STATE, // WIFI connected products
            Manifest.permission.ACCESS_COARSE_LOCATION, // Maps
            Manifest.permission.ACCESS_NETWORK_STATE, // WIFI connected products
            Manifest.permission.ACCESS_FINE_LOCATION, // Maps
            Manifest.permission.CHANGE_WIFI_STATE, // Changing between WIFI and USB connection
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // Log files
            Manifest.permission.BLUETOOTH, // Bluetooth connected products
            Manifest.permission.BLUETOOTH_ADMIN, // Bluetooth connected products
            Manifest.permission.READ_EXTERNAL_STORAGE, // Log files
            Manifest.permission.READ_PHONE_STATE, // Device UUID accessed upon registration
            Manifest.permission.RECORD_AUDIO, // Speaker accessory
            //Manifest.permission.BLUETOOTH_SCAN,
            //Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final String TIME_FORMAT = "MMM dd, yyyy 'at' h:mm:ss a";
    private static final String TAG = "MainActivity";
    private static final String ACTION = "FROM_MAIN";

    //TODO: eliminare broadcast receiver e prendere indirizzo ip server dallo shared preference
    //TODO: inviare al server stato iniziale connessione del drone
    //TODO: applicazione crasha se Bluetooth acceso (Android 12 bug)
    //TODO: eliminare il toast del log

    //endregion
    private static boolean isAppStarted = false;
    @BindView(R.id.text_view_version)
    protected TextView versionTextView;
    @BindView(R.id.text_view_registered)
    protected TextView registeredTextView;
    @BindView(R.id.text_view_product_name)
    protected TextView productNameTextView;
    @BindView(R.id.text_view_server_ip)
    protected TextView ipServerTextView;
    @BindView(R.id.camera_button)
    protected SlideToActView cameraButton;
    //region Fields
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private int lastProgress = -1;
    private DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(DJIError error) {
            isRegistrationInProgress.set(false);
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                runOnUiThread(() -> {
                    registeredTextView.setText(R.string.registered);
                });
                Log.i(TAG, "Registration success");
            } else {
                showToast( "Register sdk fails, check network is available");
                Log.i(TAG, "Registration failed");
            }
        }

        @Override
        public void onProductDisconnect() {
            runOnUiThread(() -> {
                //addLog("Disconnected from product");
                productNameTextView.setText(R.string.no_product);
                fromActivityToService(ACTION, "drone_connection", getString(R.string.no_product));
            });
            Log.i(TAG, "Drone disconnected");
        }

        @Override
        public void onProductConnect(BaseProduct product) {
            if (product != null) {
                runOnUiThread(() -> {
                    //addLog("Connected to product");
                    if (product.getModel() != null) {
                        productNameTextView.setText(getString(R.string.product_name, product.getModel().getDisplayName()));
                        fromActivityToService(ACTION, "drone_connection", getString(R.string.product_name, product.getModel().getDisplayName()));
                    } else if (product instanceof Aircraft) {
                        Aircraft aircraft = (Aircraft) product;
                        if (aircraft.getRemoteController() != null) {
                            productNameTextView.setText(getString(R.string.remote_controller));
                            fromActivityToService(ACTION, "drone_connection", getString((R.string.remote_controller)));
                        }
                    }
                });
                Log.i(TAG, String.format("onProductConnect newProduct:%s", product));
            }
        }

        @Override
        public void onProductChanged(BaseProduct product) {
            if (product != null) {
                runOnUiThread(() -> {
                    //addLog("Product changed");
                    if (product.getModel() != null) {
                        productNameTextView.setText(getString(R.string.product_name, product.getModel().getDisplayName()));
                    } else if (product instanceof Aircraft) {
                        Aircraft aircraft = (Aircraft) product;
                        if (aircraft.getRemoteController() != null) {
                            productNameTextView.setText(getString(R.string.remote_controller));
                        }
                    }
                });
            }
        }

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key,
                                      BaseComponent oldComponent,
                                      BaseComponent newComponent) {
            Log.i(TAG, key.toString() + " changed");
        }

        @Override
        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int totalProcess) {
            Log.i(TAG, djisdkInitEvent.getInitializationState().toString());
        }

        @Override
        public void onDatabaseDownloadProgress(long current, long total) {
            runOnUiThread(() -> {
                int progress = (int) (100 * current / total);
                if (progress == lastProgress) {
                    return;
                }
                lastProgress = progress;
                //addLog("Fly safe database download progress: " + progress);
            });
        }
    };
    private List<String> missingPermission = new ArrayList<>();
    //endregion

    /**
     * Whether the app has started.
     *
     * @return `true` if the app has been started.
     */
    public static boolean isStarted() {
        return isAppStarted;
    }

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        isAppStarted = true;
        checkAndRequestPermissions();
        versionTextView.setText(getResources().getString(R.string.sdk_version,
                DJISDKManager.getInstance().getSDKVersion()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, TcpClientService.class));
        } else {
            startService(new Intent(this, TcpClientService.class));
        }

        cameraButton.setOnSlideCompleteListener(new SlideToActView.OnSlideCompleteListener() {
            @Override
            public void onSlideComplete(@NotNull SlideToActView slideToActView) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
                cameraButton.resetSlider();
            }
        });
    }
    //endregion

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            /*
            if("FROM_SERVER".equals(intent.getAction())){
                String message = intent.getStringExtra("speed_wp");
                addLog(message);
            }*/
            if ("IP".equals(intent.getAction())){
                String ip = intent.getStringExtra("server_ip");
                ipServerTextView.setText("Status: " + ip);
            }
        }
    };

    private void fromActivityToService(String action, String key, String msg){
        Intent intent = new Intent(action);
        intent.putExtra(key, msg);
        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("IP");
        filter.addAction("FROM_SERVER");
        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        // Prevent memory leak by releasing DJISDKManager's references to this activity
        if (DJISDKManager.getInstance() != null) {
            DJISDKManager.getInstance().destroy();
        }
        isAppStarted = false;
        super.onDestroy();
        stopService(new Intent(this, TcpClientService.class));
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(broadcastReceiver);
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            ToastUtils.setResultToToast("Missing permissions! Will not register SDK to connect to aircraft.");
        }
    }

    /**
     * Start the SDK registration
     */
    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            Log.i(TAG, "registering product");
            //addLog("Registering product");
            AsyncTask.execute(() -> DJISDKManager.getInstance().registerApp(getApplicationContext(), registrationCallback));
        }
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
