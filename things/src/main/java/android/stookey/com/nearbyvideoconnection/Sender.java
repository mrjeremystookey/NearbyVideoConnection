package android.stookey.com.nearbyvideoconnection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */


//Concept: using the flexibility of Android for embedded systems and mobile devices, one app written to stream video from a raspberry pi to a phone.


public class Sender extends Activity {

    private static final String TAG = Sender.class.getSimpleName();


    private SurfaceHolder mHolder;
    private Handler mCameraHandler;
    private HandlerThread cameraThread;

    private CameraHandler camera;
    private SurfaceView mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.d(TAG, "No permission");
            return;
        }



        //Background duties
        cameraThread = new HandlerThread("CameraBackground");
        cameraThread.start();
        mCameraHandler = new Handler(cameraThread.getLooper());

        //Surface duties
        mSurface = (SurfaceView) findViewById(R.id.surfaceView);
        mSurface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                Log.d(TAG, "surface created");
                //Set the holder to the size of the image returned. 2509,1492?
                //Size returned byt getOutputSizes(SurfaceHolder.class)?
                holder.setFixedSize();
                holder.getSurface()
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


        //Camera will be initialized here
        camera = CameraHandler.getInstance();
        camera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);
        camera.recordVideo(mSurface);

    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    //Image from Camera
                    Image image = reader.acquireLatestImage();
                }
            };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraThread.quitSafely();
        //Shut down camera here
    }


    //On Image Available Listener. https://developer.android.com/things/training/doorbell/camera-input.html
    //Assign imageBytes to a byteArray to be processed.


}
