package android.stookey.com.nearbyvideoconnection;

import android.app.Activity;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;


//Concept: using the flexibility of Android for embedded systems and mobile devices, one app written to stream video from a raspberry pi to a phone.


public class Sender extends Activity {

    private static final String TAG = Sender.class.getSimpleName();


    private SurfaceHolder mHolder;
    private Handler mCameraHandler;
    private HandlerThread cameraThread;

    private CameraHandler camera;
    private SurfaceView mSurface;

    //Nearby
    private ConnectionsClient connectionsClient;
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String s, DiscoveredEndpointInfo discoveredEndpointInfo) {

                }

                @Override
                public void onEndpointLost(String s) {

                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        //Background duties
        startBackgroundThread();
        camera = CameraHandler.getInstance();
        mSurface = (SurfaceView) findViewById(R.id.surfaceView);
        mSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surface created");
                Log.d(TAG, "holder info:" + holder.getSurface().toString());
                camera.openCamera(getBaseContext(), mCameraHandler, mOnImageAvailableListener, holder);
                camera.takePicture();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surface has been updated");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surface destroyed");
            }
        });

        connectionsClient = Nearby.getConnectionsClient(this);
        connectionsClient.startAdvertising(TAG, getPackageName(), );



    }


    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //Image from Camera
                    Image image = reader.acquireLatestImage();
                }
            };


    private void startBackgroundThread(){
        //Setting up background thread
        Log.d(TAG, "startBackGroundThread: background thread started");
        cameraThread = new HandlerThread("CameraBackground");
        cameraThread.start();
        mCameraHandler = new Handler(cameraThread.getLooper());
    }

    private void stopBackgroundThread(){
        cameraThread.quitSafely();
        cameraThread = null;
        mCameraHandler = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBackgroundThread();
        //Shut down camera here
    }



}
