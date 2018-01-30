package android.stookey.com.nearbyvideoconnection;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;


//Concept: using the flexibility of Android for embedded systems and mobile devices, one app written to stream video from a raspberry pi to a phone.


public class Sender extends Activity {

    private static final String TAG = "Sender";
    private static final String SERVICE_ID = "Rover";


    //Camera
    private Handler mCameraHandler;
    private HandlerThread cameraThread;
    private CameraHandler camera;

    //Preview
    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;




    //Nearby
    private ConnectionsClient connectionsClient;
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String s, ConnectionInfo connectionInfo) {
                    updateStatus("onConnectionInitiated(): accepting connection");
                    connectionsClient.acceptConnection(s, payloadCallback);
                }
                @Override
                public void onConnectionResult(String s, ConnectionResolution connectionResolution) {
                    if(connectionResolution.getStatus().isSuccess()){
                        updateStatus("onConnectionResult(): connection successful");
                    } else {
                        updateStatus("onConnectionResult(): connection failed");
                    }
                    //Devices are now connected, do something here.
                }
                @Override
                public void onDisconnected(String s) {
                    updateStatus("onDisconnected(): connection disconnected");
                }
            };
    private final PayloadCallback payloadCallback = new PayloadCallback() {
                @Override
                public void onPayloadReceived(String s, Payload payload) {
                    Log.i(TAG, "onPayloadReceived(String s, Payload payload): payload received");
                    //Check for SurfaceSize Payload, if so, initialize openCamera(width, height)
                }

                @Override
                public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
                    //update both the Advertiser and Discoverer on payload progress
                }
            };


    //Image
    private SurfaceHolder.Callback mSurfaceHolderCallBack = new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    startAdvertising();
                    //set Holder size to size of SurfacePreview from Payload
                    camera.openCamera(getBaseContext(), mCameraHandler, holder);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {

                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        startBackgroundThread();
        startDisplay();
        init();
    }

    private void init(){
        camera = CameraHandler.getInstance();
        connectionsClient = Nearby.getConnectionsClient(this);
    }


    private void startDisplay(){
        updateStatus("starting display initialization");
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(mSurfaceHolderCallBack);
    }

    private void startAdvertising(){
        updateStatus("startAdvertising(): starting advertising...");
        connectionsClient.startAdvertising("Rover", SERVICE_ID,
                connectionLifecycleCallback, new AdvertisingOptions(Strategy.P2P_CLUSTER));
    }

    private void startBackgroundThread(){
        updateStatus("startBackGroundThread: background thread started");
        cameraThread = new HandlerThread("CameraBackground");
        cameraThread.start();
        mCameraHandler = new Handler(cameraThread.getLooper());
    }

    private void stopBackgroundThread(){
        updateStatus("stopBackgroundThread(): Stopping background thread");
        cameraThread.quitSafely();
        cameraThread = null;
        mCameraHandler = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBackgroundThread();
    }

    private void updateStatus(String string){
        Log.d(TAG, string);
    }



}
