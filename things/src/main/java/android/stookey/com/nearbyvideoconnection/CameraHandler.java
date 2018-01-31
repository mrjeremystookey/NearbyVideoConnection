package android.stookey.com.nearbyvideoconnection;

import android.content.Context;
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
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;

import java.util.Collections;

/**
 * Created by Stookey on 1/26/18.
 */

public class CameraHandler {
    private static final String TAG = "CameraHandler";




    //Camera
    private static String mCamID;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private final CameraDevice.StateCallback cameraOpenedCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            setupRecorder();
            updateStatus("Camera opened");
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            updateStatus("Camera disconnected, closing");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            updateStatus("Camera device error, closing");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
            updateStatus("Camera closed");
            camera = null;
        }
    };
    private final CameraCaptureSession.StateCallback sessionConfiguredCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mCameraCaptureSession = session;
            updateStatus("capture session created");
            setupCaptureRequest();
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            updateStatus("failed to configure the capture session");
        }
    };
    private final CameraCaptureSession.CaptureCallback captureCompleteCallback = new CameraCaptureSession.CaptureCallback(){
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            updateStatus("image capture complete");
            super.onCaptureCompleted(session, request, result);
            if(mCameraCaptureSession != null){
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
                updateStatus("capture session closed");
            }
        }

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            updateStatus("image capture started");
        }

        @Override
        public void onCaptureProgressed( CameraCaptureSession session,  CaptureRequest request,  CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.d(TAG, "Partial Result");
        }
        @Override
        public void onCaptureFailed( CameraCaptureSession session,  CaptureRequest request,  CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.d(TAG, "capture failed");
        }
    };


    //Recorder
    private static MediaRecorder mMediaRecorder = new MediaRecorder();
    private static Size mPreviewSize;

    //ImageReader
    private static final int MAX_IMAGES = 1;
    private ImageReader mImageReader;


    //Constructor
    private CameraHandler(){}
    private static class InstanceHolder{
        private static CameraHandler mCamera = new CameraHandler();
    }
    public static CameraHandler getInstance(){
        Log.d(TAG, "camera object created");
        return InstanceHolder.mCamera;

    }



    //Methods
    public void openCamera(Context context, Handler handler, int width, int height, ImageReader.OnImageAvailableListener imageAvailableListener){
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraID : manager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraID);
                updateStatus("Camera Characteristics: " + cameraCharacteristics.toString());
                if(manager.getCameraIdList().length == 1){
                    mCamID = cameraID;
                    mPreviewSize = new Size(width, height);
                    mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, MAX_IMAGES);
                    mImageReader.setOnImageAvailableListener(imageAvailableListener, handler);
                    connectCamera(context, handler);
                } else{
                    updateStatus("multiple cameras found");
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void connectCamera(Context context, Handler handler){
        updateStatus("connecting to the camera...");
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraManager.openCamera(mCamID, cameraOpenedCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //Todo finish setting up Recorder
    private void setupRecorder() {
        updateStatus("Configuring recorder...");
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //mMediaRecorder.setOutputFile();
        //mMediaRecorder.setVideoSize(recorderSize.getWidth(), recorderSize.getHeight());
//      mMediaRecorder.setVideoEncodingBitRate();
          setupCaptureSession();
    }

    private void setupCaptureSession(){
        if(mCameraDevice == null){
            updateStatus("Cannot capture image. Camera not initialized");
            return;
        }
        try {
            updateStatus("creating capture session");
            mCameraDevice.createCaptureSession(Collections.singletonList(
                    mImageReader.getSurface()),
                    sessionConfiguredCallback,
                    null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setupCaptureRequest(){
        try {
            updateStatus("creating capture request");
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            startCapture();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void startCapture(){
        try {
            updateStatus("starting image capture...");
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), captureCompleteCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void shutDown(){
        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if(mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    private void updateStatus(String string){
        Log.d(TAG, string);
    }
}
