package android.stookey.com.nearbyvideoconnection;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Collections;

/**
 * Created by Stookey on 1/26/18.
 */

public class CameraHandler {

    /**Useful stuff:
     * Camera API
     * https://developer.android.com/reference/android/hardware/camera2/package-summary.html


     **/

    private static final String TAG = "CameraHandler";

    //Used for camera manipulation
    private MediaRecorder recorder;
    private ImageReader imageReader;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;

    private static final int IMAGE_WIDTH = 2592;
    private static final int IMAGE_HEIGHT = 1944;
    private static final int MAX_IMAGES = 3;

    private SurfaceHolder mHolder;

    //Needs a constructor
    //Lazy Loaded Singleton ensures only one instance of camera is open at a time.
    private CameraHandler(){

    }
    private static class InstanceHolder{
        private static CameraHandler mCamera = new CameraHandler();
    }
    public static CameraHandler getInstance(){
        Log.d(TAG, "camera object opened");
        return InstanceHolder.mCamera;

    }


    //Staring up the camera and initializing our camera object.
    public void initializeCamera(Context context,
                                 Handler backgroundHandler,
                                 ImageReader.OnImageAvailableListener imageAvailableListener){
        Log.d(TAG, "beginning camera initiialization");

         //Lets find them cams
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String[] camIds = {};
        try {
            camIds = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception: ", e);
        }
        if(camIds.length < 1) {
            Log.d(TAG, "No cams");
            return;
        }
        String id = camIds[0];
        Log.d(TAG, "CamID selected: " + id);
        //Storing the image
        //This is for an image reader, not video, probably won't work
        //Preconfigured with correct size and format
        //imageReader surfaces will be used to hold camera capture
        //imageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT, ImageFormat.JPEG, MAX_IMAGES);
        //imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);
        //Configure recorder
        recorder.setVideoSize(IMAGE_WIDTH, IMAGE_HEIGHT);
        try {
            manager.openCamera(id, cameraOpenedCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.d(TAG, "Camera Access Exception: ", e);
        }
    }


    //Begin creating session for image capture
    //once created, capture image
    public void recordVideo(SurfaceView surfaceView) {
        //Check to see if the camera has started
        if (mCameraDevice == null) {
            Log.w(TAG, "Cannot capture image. Camera not initialized.");
            return;
        }

        //Creating a new capture
        try {
            Log.d(TAG, "creating capture session");
            mCameraDevice.createCaptureSession(

                    //ImageReader Surface and MediaRecorder surface.
                    //MediaRecorder surface not configured above.
                    Collections.singletonList(recorder.getSurface()),
                    //Collections.singletonList(imageReader.getSurface()),
                    sessionConfigured,
                    null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "access exception while preparing video", e);
        }
    }

    //Building the Capture
    //Captures information from the image sensor
    private void triggerImageCapture(){
        Log.d(TAG, "starting image capture");
        //creating the capture that the data will be housed in
        final CaptureRequest.Builder captureBuilder;
        try {
            //Factory Method to create an image capture
            //TEMPLATE_STILL_CAPTURE will get you still images.
            //TEMPLATE_PREVIEW may work for preview windows.
            //TEMPLATE_RECORD works best for recorded video
            captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            //Setting the surface for the image to rest on

            captureBuilder.addTarget(recorder.getSurface());
            //Not sure what these do.
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            //Starts the capture process and alerts when capture is completed.
            mCameraCaptureSession.setRepeatingRequest(captureBuilder.build(), captureCompleteCallback, null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "camera capture exception: ",e);
        }
    }



    //pops when camera is opened, or errors
    private final CameraDevice.StateCallback cameraOpenedCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            Log.d(TAG, "Camera opened");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {

        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };




    //pops when session is configured, or fails
    private final CameraCaptureSession.StateCallback sessionConfigured = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if(mCameraDevice == null)
                return;
            //Setting up the session
            //Capture image data from the camera
            mCameraCaptureSession = session;
            Log.d(TAG, "capture session created");
            //begins the processing of capturing the image data from the camera sensor
            triggerImageCapture();
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.w(TAG, "Failed to configure the camera");
        }
    };




    //Pops on Capture events -  complete, failure,
    private final CameraCaptureSession.CaptureCallback captureCompleteCallback =
            new CameraCaptureSession.CaptureCallback(){
                @Override
                //If capture is completed
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request,
                                               TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    if(session != null){
                        //Closes the Capture session after the capture is complete
                        session.close();
                        mCameraCaptureSession.close();
                        mCameraCaptureSession = null;
                        Log.d(TAG, "CaptureSession Closed");
                    }
                }
            };


    public void shutDown(){
        if(mCameraDevice != null){
            mCameraDevice.close();
        }
    }
}
