package android.stookey.com.nearbyvideoconnection;

import android.Manifest;
import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.nio.ByteBuffer;


//Concept: using the flexibility of Android for embedded systems and mobile devices, one app written to stream video from a raspberry pi to a phone.


public class Sender extends Activity {

    private static final String TAG = "Sender";
    private static final String SERVICE_ID = "Rover";


    //Camera
    private Handler mCameraHandler;
    private HandlerThread cameraThread;
    private CameraHandler camera;


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
                    //Check for SurfaceSize Payload, if so, initialize openCamera(context,handler,width, height)
                }

                @Override
                public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {
                    //update both the Advertiser and Discoverer on payload progress
                }
            };

    //Image Handling
    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
                  updateStatus("New Image Available");
                  Image image = reader.acquireLatestImage();
                  //get image bytes
            ByteBuffer imageBuffer = image.getPlanes()[0].getBuffer();
            final byte[] imageBytes = new byte[imageBuffer.remaining()];
            //Todo Send to do Discoverer via Payload
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        startBackgroundThread();
        init();
    }

    private void init(){
        camera = CameraHandler.getInstance();
        connectionsClient = Nearby.getConnectionsClient(this);
        camera.openCamera(this, mCameraHandler, 640, 480, onImageAvailableListener);
    }



    private void startAdvertising(){
        updateStatus("startAdvertising(): starting advertising...");
        connectionsClient.startAdvertising("Rover", SERVICE_ID,
                connectionLifecycleCallback, new AdvertisingOptions(Strategy.P2P_CLUSTER)).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateStatus("We are advertising");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                updateStatus("Failed to start Advertising");
            }
        });
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
