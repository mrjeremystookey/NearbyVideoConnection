package android.stookey.com.nearbyvideoconnection;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
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
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private static String mCamID;
    private static SurfaceHolder mSurfaceHolder;

    private MediaRecorder mMediaRecorder;


    //Lazy Loaded Singleton ensures only one instance of camera is open at a time.
    private CameraHandler(){

    }
    private static class InstanceHolder{
        private static CameraHandler mCamera = new CameraHandler();
    }
    public static CameraHandler getInstance(){
        Log.d(TAG, "camera object created");
        return InstanceHolder.mCamera;

    }




    public void openCamera(Context context,
                           Handler backgroundHandler,
                           ImageReader.OnImageAvailableListener imageAvailableListener,
                           SurfaceHolder holder){

        Log.d(TAG, "beginning camera initialization");
        mSurfaceHolder = holder;
        try {
            CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] camIds = {};
            camIds = manager.getCameraIdList();

            //Get Camera Info
            CameraCharacteristics chars = manager.getCameraCharacteristics(camIds[0]);
            StreamConfigurationMap configs = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] mySizes = configs.getOutputSizes(SurfaceHolder.class);
            //mySizes[0] = 320,240
            //mySizes[1] = 640,480
            Size mySize = mySizes[1];
            Log.d(TAG, "width, height: " + mySize.getWidth()+", "+ mySize.getHeight());
            holder.setFixedSize(mySize.getWidth(), mySize.getHeight());
            mCamID = camIds[0];
            manager.openCamera(mCamID, cameraOpenedCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.d(TAG, "Cam access exception: ", e);
        }


    }

    //pops when camera is opened, or errors
    private final CameraDevice.StateCallback cameraOpenedCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            Log.d(TAG, "Camera opened, initialization complete");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "Camera Disconnected, closing");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "Camera device error, closing");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
            Log.d(TAG, "Closed camera, releasing");
            camera = null;
        }
    };









    public void takePicture() {
        Log.d(TAG, "takePicture called");
        if (mCameraDevice == null) {
            Log.w(TAG, "Cannot capture image. Camera not initialized.");
            return;
        }
        try {
            Log.d(TAG, "creating capture session");
            mCameraDevice.createCaptureSession(
                    Collections.singletonList(mSurfaceHolder.getSurface()),
                    sessionConfigured,
                    null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "access exception while preparing video", e);
        }
    }

    //pops when session is configured (and starts imagecapture), or fails
    private final CameraCaptureSession.StateCallback sessionConfigured = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            if(mCameraDevice == null)
                return;
            mCameraCaptureSession = session;
            Log.d(TAG, "capture session created");
            triggerImageCapture();
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.w(TAG, "Failed to configure the camera");
        }
    };


    private void triggerImageCapture(){
        Log.d(TAG, "starting image capture");
        try {
            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureBuilder.addTarget(mSurfaceHolder.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mCameraCaptureSession.setRepeatingRequest(captureBuilder.build(), captureCompleteCallback, null);
        } catch (CameraAccessException e) {
            Log.d(TAG, "camera capture exception: ",e);
        }
    }

    private final CameraCaptureSession.CaptureCallback captureCompleteCallback = new CameraCaptureSession.CaptureCallback(){
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    if(mCameraCaptureSession != null){
                        mCameraCaptureSession.close();
                        mCameraCaptureSession = null;
                        Log.d(TAG, "CaptureSession Closed");
                    }
                }
                @Override
                public void onCaptureProgressed( CameraCaptureSession session,  CaptureRequest request,  CaptureResult partialResult) {
                    super.onCaptureProgressed(session, request, partialResult);
                    Log.d(TAG, "Partial Result");
                }
                @Override
                public void onCaptureFailed( CameraCaptureSession session,  CaptureRequest request,  CaptureFailure failure) {
                    super.onCaptureFailed(session, request, failure);
                    Log.d(TAG, "Capture Failed");
                }
            };


    public void shutDown(){
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
}
