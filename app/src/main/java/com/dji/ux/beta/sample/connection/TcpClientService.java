package com.dji.ux.beta.sample.connection;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dji.ux.beta.sample.R;
import com.dji.ux.beta.sample.cameraview.CameraActivity;
import com.dji.ux.beta.sample.mission.GPSPlancia;
import com.dji.ux.beta.sample.utils.DroneState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpClientService extends Service {
    private final AtomicBoolean working = new AtomicBoolean(true);
    private final AtomicBoolean workingJetson = new AtomicBoolean(true);
    private Socket socket;
    private Thread connectThread;
    private Handler handler = new Handler();
    BufferedReader bufferedReader;//Declare the input stream object
    OutputStream outputStream;//Declare the output stream object
    public final String _ip = "192.168.1.103"; //console TP-link
//    public final String _ip = "192.168.2.5"; //console RUBICON
//    public final String _ip = "192.168.200.185"; //console livorno
//    public final String _ip = "213.82.97.234"; //console livorno remoto
    //public final String _ip = "192.168.1.105"; //localhost plancia
    private final String port = "11000";
//    private final String port = "8089"; //porta console livorno
//    private final String TAG = TcpClientService.class.getSimpleName();
    private final String TAG = "TcpClientService";
    Boolean isconnectBoolean = false;

    Boolean isconnectBooleanJetson = false;
    private Socket socketJetson;
    private Thread connectThreadJetson;
    BufferedReader bufferedReaderJetson; //Declare the input stream object
    OutputStream outputStreamJetson; //Declare the output stream object
    public final String _ipJetson = "192.168.1.100"; //python jetson TP-link
//    public final String _ipJetson = "192.168.2.8"; //python jetson RUBICON
//    public final String _ipJetson = "192.168.200.22"; //python jetson livorno
//    public final String _ipJetson = "146.48.53.41"; //python jetson remoto
    private final String portJetson = "65432"; //port python

    private String[] request;
    private String[] requestJetson;
    private String message = "Hello Server";
    private String messageJetson = "Hello Jetson";
    private static final String ACTION = "FROM_SERVER";
    private static final String KEY = "mission";
    SharedPreferences sharedPreferences;

    private final Runnable runnable = new Runnable() {
        //TODO: connettere tramite pulsante

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            try {
                sharedPreferences = getSharedPreferences(getString(R.string.my_pref), Context.MODE_PRIVATE);
                socket = new Socket(_ip, Integer.parseInt(port));

                bufferedReader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream=socket.getOutputStream();

                //char[] buffer=new char[256];//Define the array to receive the input stream data
                StringBuilder bufferString= new StringBuilder();//Define a character to receive array data
                //int tag=0;//First know where to write to the array

                while (working.get()){
                    Log.i(TAG, "Connected to " + _ip);
                    fromServiceToActivity("IP", "server_ip", "Connected to " + _ip);

//                    outputStream.write((message +"\n").getBytes(StandardCharsets.UTF_8));
//                    //The output stream is sent to the server
//                    outputStream.flush();

                    //while (bufferedReader.read(buffer) >0){
                    String line;

                    while ((line = bufferedReader.readLine()) != null){
                        bufferString.append(line);

                        handleRequest(bufferString.toString());

                        //Log.i(TAG, message);

                        if(bufferString.toString().equals("status")){
                            boolean disableConsole = sharedPreferences.getBoolean(getString(R.string.disableConsole), true);
                            StringBuilder bufferStatus = DroneState.getDroneStatePlancia(disableConsole);
                            //StringBuilder bufferStatus = DroneState.getDroneState();
                            //bufferStatus.append("\n").append(DroneState.getStreamInfo());
                            //TODO status string
//                            Log.i(TAG, "State: " + bufferStatus.toString());
                            outputStream.write((bufferStatus.toString()).getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                        }

                        if(bufferString.toString().equals("closing connection")){
                            //Log.i(TAG, "Status connection --> " + bufferStatus.toString());
                            fromServiceToActivity("IP", "server_ip", getString(R.string.server_disc));
                            break;
                        }

                        //fromServiceToActivity("FROM_SERVER", "server_msg", bufferString.toString());
                        bufferString.setLength(0);
                    }
                    break;
                }
                socket.close();
                bufferedReader.close();
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private final Runnable runnableJetson = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {

            try {
                socketJetson = new Socket(_ipJetson, Integer.parseInt(portJetson));

                bufferedReaderJetson=new BufferedReader(new InputStreamReader(socketJetson.getInputStream()));
                outputStreamJetson=socketJetson.getOutputStream();

                StringBuilder bufferStringJetson= new StringBuilder();//Define a character to receive array data

                while (workingJetson.get()){

                    Log.i(TAG, "Connected to " + _ipJetson);
                    fromServiceToActivity("IP", "jetson_ip", "Jetson connected");
//                    outputStreamJetson.write((messageJetson +"\n").getBytes(StandardCharsets.UTF_8));
//                    outputStreamJetson.flush();

                    String line;
                    while ((line=bufferedReaderJetson.readLine()) != null){
                        bufferStringJetson.append(line);
                        handleRequest(bufferStringJetson.toString());

                        if(bufferStringJetson.toString().equals("gps")){
                            StringBuilder bufferStatus = DroneState.getGPSJetson();
//                            Log.i(TAG, "Jetson gps: " + bufferStatus.toString());
                            //outputStreamJetson.write((bufferStatus.toString()).getBytes(StandardCharsets.UTF_8));
                            //outputStreamJetson.flush();
                            sendMessageJetson(bufferStatus.toString());
                        }
                        bufferStringJetson.setLength(0);
                    }
                    break;
                }
                socketJetson.close();
                bufferedReaderJetson.close();
                outputStreamJetson.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        IntentFilter filter = new IntentFilter("FROM CAMERA");
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);

        if(!isconnectBoolean){
            connectThread = new Thread(runnable);
            connectThread.start();
            Log.d(TAG, "Connected to the server successfully");
            isconnectBoolean=true;
        }

        if(!isconnectBooleanJetson){
            connectThreadJetson = new Thread(runnableJetson);
            connectThreadJetson.start();
            Log.d(TAG, "Connected to the jetson successfully");
            isconnectBooleanJetson=true;
        }
        startMeForeground();
    }

    @Override
    public void onDestroy() {
        if (isconnectBoolean){
            working.set(false);
            //connectThread.interrupt();
            if(socket!=null && !socket.isClosed()){
                try{
                    if(outputStream!=null){
                        outputStream.close();
                    }
                    if(bufferedReader!=null){
                        bufferedReader.close();
                    }
                    socket.close();
                    socket=null;

                } catch (Exception e) {
                    e.printStackTrace();
                    Logger.getLogger(TcpClientService.class.getName()).log(Level.SEVERE, "Can't close a System.in based BufferedReader", e);
                    }
            }
            connectThread.interrupt();
            Log.i(TAG, "Closing connection!");
            isconnectBoolean=false;
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void startMeForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            String NOTIFICATION_CHANNEL_ID = this.getPackageName();
            String channelName = "Tcp Client Background Service";
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setLightColor(ResourcesCompat.getColor(getResources(), R.color.dark_gray, null));
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);

            Notification builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.icon_slider) //TODO:cambiare icona notifica (mettere mini-drone)
                    .setContentTitle("Tcp Client is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MAX)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();

            startForeground(2, builder);
        } else {startForeground(1, new Notification());}
    }

    private void fromServiceToActivity(String action, String key, String msg){
        Intent intent = new Intent(action);
        intent.putExtra(key, msg);
        LocalBroadcastManager.getInstance(TcpClientService.this).sendBroadcast(intent);
    }

    private void handleRequest(String req){
        sharedPreferences = this.getSharedPreferences(getString(R.string.my_pref), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean disableConsole = sharedPreferences.getBoolean(getString(R.string.disableConsole), true);

        if(!req.equals("gps") && !req.equals("") && !req.equals("status"))
            Log.i(TAG, "Received " + req);
        request = req.split("-");

        if(disableConsole && !request[0].startsWith("hotpoint_jetson") && !request[0].startsWith("warning") && !req.equals("gps") && !req.equals("") && !req.equals("status")) {
            Log.i(TAG, "Ignoring command because the console is disabled");
            return;
        }

        switch (request[0]){
            case "waypoint_speed":
                setSpeed(editor);
                break;
            case "interdiction_area":
                setInterdictionRadius(editor);
                break;
            case "warning":
                setWarning(editor);
                break;
            case "next_target":
                //TODO: skip current target and resume search
                break;
            case "waypoint_coordinates":
                //setCoordinate(editor);
                request[1] = request[1].replace(",", ".");
                request[2] = request[2].replace(",", ".");
                GPSPlancia.populateGPSPlancia(request[1], request[2], request[3]);
                //fromServiceToActivity(ACTION, KEY, "coordinates_wp");
                break;
            case "follow_coordinates":
                request[1] = request[1].replace(",", ".");
                request[2] = request[2].replace(",", ".");
                setFMCoordinate(editor);
                fromServiceToActivity(ACTION, KEY, "start_follow");
                break;
            case "update_coordinates":
                request[1] = request[1].replace(",", ".");
                request[2] = request[2].replace(",", ".");
                updateFMCoordinate(editor);
                //fromServiceToActivity(ACTION, KEY, "start_follow"); //TODO: cambiare MSG, serve un msg?
                break;
            case "hotpoint_coordinates": //from command NADSearchAtPos
                request[1] = request[1].replace(",", ".");
                request[2] = request[2].replace(",", ".");
                setHotCoordinate(editor);
                fromServiceToActivity(ACTION, KEY, "start_hotpoint");
                break;
            case "hotpoint_jetson":
                setHotCoordinate(editor);
                //TODO: check if it is needed fromServiceToActivity() here too
                break;
            default:
                fromServiceToActivity(ACTION, KEY, request[0]);
        }
    }

    private void setSpeed(SharedPreferences.Editor editor){
        Log.i(TAG, "Setting speed to: " + request[1]);
        float speed = Float.parseFloat(request[1]);
        if(checkSpeed(speed, request[1])){
            editor.putFloat(getString(R.string.speed_waypoint), speed);
            editor.apply();
            fromServiceToActivity(ACTION, KEY, "speed_wp");
        }
    }

    private void setInterdictionRadius(SharedPreferences.Editor editor){
        Log.i(TAG, "New interdiction radius: " + request[1]);
        if(isNumeric(request[1])){
//            CameraActivity.setInterdictionRadius(Float.parseFloat(request[1]));

            float interdictionArea = Float.parseFloat(request[1]);
            editor.putFloat(getString(R.string.interdiction_area), interdictionArea);
            editor.apply();
            fromServiceToActivity(ACTION, KEY, "interdiction_radius");
        }
        else {
            try {
                outputStream.write(("Incorrect or null interdiction_area value! Please send a numeric value\n").getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setWarning(SharedPreferences.Editor editor){
        Log.i(TAG, "warning " + request[1]);
        editor.putString(getString(R.string.warning), request[1]);
        editor.apply();
        fromServiceToActivity(ACTION, KEY, "warning");
    }

    private void setCoordinate(SharedPreferences.Editor editor){
        editor.putString(getString(R.string.latitude), request[1]);
        editor.putString(getString(R.string.longitude), request[2]);
        editor.putString(getString(R.string.altitude), request[3]);
        editor.apply();
    }

    private void setHotCoordinate(SharedPreferences.Editor editor){
        editor.putString(getString(R.string.latitude_pos), request[1]);
        editor.putString(getString(R.string.longitude_pos), request[2]);
        editor.putString(getString(R.string.altitude_pos), request[3]);
        editor.putString(getString(R.string.radius_pos), request[4]);
        editor.apply();
    }

    private void setFMCoordinate(SharedPreferences.Editor editor){
        editor.putString(getString(R.string.latitude_fm), request[1]);
        editor.putString(getString(R.string.longitude_fm), request[2]);
        editor.putString(getString(R.string.altitude_fm), request[3]);
        editor.apply();
    }

    private void updateFMCoordinate(SharedPreferences.Editor editor){
        //TODO check if the replace is needed or not
        request[1] = request[1].replace(",", ".");
        request[2] = request[2].replace(",", ".");
        editor.putString(getString(R.string.latitude_fm), request[1]);
        editor.putString(getString(R.string.longitude_fm), request[2]);
        editor.apply();
    }

    private boolean checkSpeed(float speed, String msg){
        if (speed <= 0.0f || speed > 15.0f || !isNumeric(msg)){
            try {
                outputStream.write(("Incorrect or null speed value! Please send a value in the range [0.0f-15.0f] " + "\n").getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        return true;
    }

    private static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Float.parseFloat(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static boolean checkGpsCoordinates(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i(TAG, "Broadcaster: " + intent.getStringExtra("state"));
            String msg= intent.getStringExtra("state");
            if(msg.equals("isStreaming")){
                sendMessageJetson(msg);
            }
            if(msg.equals("mob_mission")){
                sendMessagePlancia(msg);
            }
        }
    };

    private void sendMessageJetson(String msg) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    if(outputStreamJetson!=null){
//                        Log.i(TAG, "sending to jetson" + msg);
                        outputStreamJetson.write((msg + "\n").getBytes(StandardCharsets.UTF_8));
                        outputStreamJetson.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        thread.start();
    }

    private void sendMessagePlancia(String msg) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    if(outputStream!=null){
                        Log.i(TAG, "sending to ship" + msg);
                        outputStream.write((msg).getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    //TODO: cambiare thread (ogni nuovo messaggio apre un thread-no buono)
}