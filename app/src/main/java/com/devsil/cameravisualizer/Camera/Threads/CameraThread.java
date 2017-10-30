package com.devsil.cameravisualizer.Camera.Threads;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.devsil.cameravisualizer.Camera.CameraUtils;
import com.devsil.cameravisualizer.Camera.Handlers.CameraActivityHandler;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by devsil on 10/29/2017.
 *
 *
 * This is using the Singleton pattern to avoid substantiating more then one instance of this thread.
 */

public class CameraThread implements Runnable {
    private static final String TAG = "Debug.CameraThread";


    public static final int OPEN_CAMERA = 1;
    public static final int STOP_PREVIEW = 21;
    public static final int START_PREVIEW = 23;
    public static final int SWITCH_CAMERA = 2;
    public static final int SET_PREVIEW_SURFACE = 3;
    public static final int TAKE_PICTURE = 6;
    public static final int HANDLE_TEXTURE_CHANGE = 7;
    public static final int MSG_RELEASE_CAMERA = 8;
    public static final int CHANGE_FLASH = 9;
    public static final int CHANGE_RECORDING_STATE = 11;
    public static final int HANDLE_ZOOM = 12;
    public static final int HANDLE_FOCUS = 13;



    protected Camera mCamera;
    public CameraThreadHandler mCameraHandler;
    protected CameraActivityHandler mActivityHandler;
    protected WindowManager mWindowManager;

    protected boolean mAutoFocusSupported;
    protected boolean mZoomSupported;
    protected int mCurrentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    protected int mPreviewWidth = 0;
    protected int mPreviewHeight = 0;
    protected float mImageAspectRatio = 0;


    protected int mCameraPreviewWidth;
    protected int mCameraPreviewHeight;

    protected int mRotationResult;

    protected boolean mCanCapture = false;

    protected boolean mRecording = false;

    private static CameraThread INSTANCE = null;


    private CameraThread(CameraActivityHandler cameraActivityHandler, WindowManager windowManager) {
        mActivityHandler = cameraActivityHandler;
        this.mWindowManager = windowManager;
    }


    /**
     *
     * Returns the single instance of the Camera Thread.
     *
     * @param activityHandler
     * @param windowManager
     */
    public static CameraThread getInstance(CameraActivityHandler activityHandler, WindowManager windowManager){
        if(INSTANCE == null){
           return INSTANCE = new CameraThread(activityHandler, windowManager);
        }

        INSTANCE.mActivityHandler = activityHandler;
        INSTANCE.mWindowManager = windowManager;

        return INSTANCE;
    }


    @Override
    public void run() {
        Looper.prepare();

        mCameraHandler = new CameraThreadHandler(this);


        Thread.currentThread().setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Log.i(TAG, "Thread ID: "+ Thread.currentThread().getId());



        Message readyMessage = mActivityHandler.obtainMessage(CameraActivityHandler.MSG_CAMERA_THREAD_READY);

        readyMessage.sendToTarget();

        Looper.loop();

    }


    private void stopPreview(){
        if(mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                Log.e(TAG, "Stop camera preview failed", e);
            }
        }
    }

    private void startPreview(){
        if(mCamera != null) {
            try {
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Start camera preview failed", e);
            }
        }
    }


    private void openCamera() {
        // Try to find a Back facing
        try {
            if (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            } else {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            }


            if (mCamera == null) {
                Log.d(TAG, "No back-facing camera found; opening default");
                mCamera = Camera.open();  // opens first back-facing camera
            }
        }
        catch (Exception e){
            Log.e(TAG, "Catching an already open camera instance... ");
        }
        if (mCamera == null) {
            mActivityHandler.sendMessage(mActivityHandler.obtainMessage(CameraActivityHandler.MSG_CANT_OPEN_CAMERA));
            return;
            //throw new RuntimeException("Unable to open camera");
        }

        Camera.Parameters parms = mCamera.getParameters();

        mImageAspectRatio = CameraUtils.setPictureSize(parms);

        //CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);
        CameraUtils.getOptimalPreviewSize(parms, CameraUtils.MAX_PREVIEW_WIDTH, CameraUtils.MAX_PREVIEW_HEIGHT);
        CamcorderProfile camProfile = CameraUtils.printRecorderProfile();

        mAutoFocusSupported = CameraUtils.canCameraFocus(parms);
        mZoomSupported = CameraUtils.canZoom(parms);

        setCameraDisplayOrientation(parms, mWindowManager);

        // Give the camera a hint that we're recording video.  This can have a big
        // impact on frame rate for the video but hardly any effect for Image capturing.
        parms.setRecordingHint(true);

        if(parms.getSupportedFlashModes() == null){
            mActivityHandler.sendMessage(mActivityHandler.obtainMessage(CameraActivityHandler.MSG_SET_FLASH, -1));
        }
//        else if(mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
//            switch (CURRENT_FLASH_MODE) {
//                case 0:
//                    parms.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
//                    mCameraActivityHandler.sendMessage(mCameraActivityHandler.obtainMessage(CameraActivityHandler.MSG_SET_FLASH, 0));
//                    break;
//                case 1:
//                    parms.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
//                    mCameraActivityHandler.sendMessage(mCameraActivityHandler.obtainMessage(CameraActivityHandler.MSG_SET_FLASH, 1));
//                    break;
//                case 2:
//                    parms.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//                    mCameraActivityHandler.sendMessage(mCameraActivityHandler.obtainMessage(CameraActivityHandler.MSG_SET_FLASH, 2));
//                    break;
//                default:
//                    parms.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//                    mCameraActivityHandler.sendMessage(mCameraActivityHandler.obtainMessage(CameraActivityHandler.MSG_SET_FLASH, 2));
//            }
//        }
//        else {
//            parms.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//            mCameraActivityHandler.sendMessage(mCameraActivityHandler.obtainMessage(CameraActivityHandler.MSG_SET_FLASH, -1));
//        }


        // leave the frame rate set to default
        try {
            mCamera.setParameters(parms);
        }
        catch (Exception e){
            Log.e(TAG, "Set Params failed", e);
        }



        int[] fpsRange = new int[2];
        Camera.Size mCameraPreviewSize = parms.getPreviewSize();
        parms.getPreviewFpsRange(fpsRange);
        String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
        if (fpsRange[0] == fpsRange[1]) {
            previewFacts += " @" + (fpsRange[0] / 1000.0) + "fps";
        } else {
            previewFacts += " @[" + (fpsRange[0] / 1000.0) +
                    " - " + (fpsRange[1] / 1000.0) + "] fps";
        }

        Log.i(TAG, "Camera Preview Stats:: " + previewFacts);

        mCameraPreviewWidth = mCameraPreviewSize.width;
        mCameraPreviewHeight = mCameraPreviewSize.height;

        mActivityHandler.sendMessage(mActivityHandler.obtainMessage(CameraActivityHandler.MSG_CAMERA_OPENED, mCameraPreviewWidth, mCameraPreviewHeight, (mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)));
//        startFaceDetection();
    }


    private void setCameraDisplayOrientation(Camera.Parameters params, WindowManager windowManager) {

        Camera.CameraInfo info = new Camera.CameraInfo();

        Camera.getCameraInfo(mCurrentCameraId, info);

        boolean isPortrait = false;

        int rotation = windowManager.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                isPortrait = true;
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                isPortrait = true;
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        boolean isFrontFacing = false;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // Correct the mirror
            isFrontFacing = true;
        } else {
            result = (info.orientation - degrees + 360) % 360;
            isFrontFacing = false;
        }

        mCamera.setDisplayOrientation(result);

        params.setRotation(result);

        mRotationResult = result;
        try{
            CamcorderProfile profile = CamcorderProfile.get(isFrontFacing ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK, CamcorderProfile.QUALITY_HIGH);

            if(isPortrait){
                mActivityHandler.sendMessage(mActivityHandler.obtainMessage(CameraActivityHandler.MSG_UPDATE_ORIENT, 0, profile.videoBitRate));
            }
            else{
                mActivityHandler.sendMessage(mActivityHandler.obtainMessage(CameraActivityHandler.MSG_UPDATE_ORIENT, 1, profile.videoBitRate));
            }
        } catch (Exception e){
            Log.e(TAG, "Failed at camcorder profile", e);
        }
    }

    public void handleSetPreviewSurface(SurfaceTexture st) {
        if(mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewTexture(st);
            } catch (IOException e) {
                Log.e(TAG, "Failed to set preview surface", e);
            }

            try {
                mCamera.startPreview();
            }
            catch (Exception e){
                Log.e(TAG, "Failed to start preview", e);
            }

            mCanCapture = true;
        }
        else {
            mCanCapture = false;
        }

    }

    private void handleAutoFocus(){
        try {
//            if(CURRENT_FLASH_MODE > 0) {
                mCamera.autoFocus(AUTOFOCUS_CALLBACK);
//            }
        } catch (Exception e) {
            Log.e(TAG, "Failed setting autofocus", e);
        }
    }


    private void handleZoom(float zoom, float distance){

        try {
            Camera.Parameters params = mCamera.getParameters();
            if (params == null || !params.isZoomSupported()) {
                return;
            }

            mCamera.cancelAutoFocus();

            int maxSupportedZoom = params.getMaxZoom();


            int currentZoom = params.getZoom();

            if (zoom > distance) {
                // zoom in
                if (currentZoom < maxSupportedZoom) {
                    currentZoom++;
                }
            } else if (zoom < distance) {
                // zoom out
                if (currentZoom > 0) {
                    currentZoom--;
                }
            }

            params.setZoom(currentZoom);
            mCamera.setParameters(params);
        }
        catch (Exception e){
            Log.e(TAG, " Failed handling zoom", e);
        }

    }


    private Camera.AutoFocusCallback AUTOFOCUS_CALLBACK = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            mActivityHandler.sendMessage(mActivityHandler.obtainMessage(CameraActivityHandler.MSG_ON_AUTO_FOCUS, success));
        }
    };


    private void textureChange(int screenWidth, int screenHeight) {

        mPreviewWidth = screenWidth;
        mPreviewHeight = screenHeight;

        if(mCamera != null){
            Camera.Parameters parms = null;
            try {
                parms = mCamera.getParameters();
            }
            catch (Exception e) {
                Log.e(TAG, "Failed Texture Change", e);
            }

            if(parms != null) {
                setCameraDisplayOrientation(parms, mWindowManager);

                // Give the camera a hint that we're recording video.  This can have a big
                // impact on frame rate for the video but hardly any effect for Image capturing.
                parms.setRecordingHint(true);

                Camera.Size size = null;
                if(mCamera != null) {
                    size = CameraUtils.getOptimalPreviewSize(mCamera.getParameters(), mPreviewWidth, mPreviewHeight);
                }

                try {
                    mCamera.setParameters(parms);
                } catch (Exception e) {
                    Log.e(TAG, "Failed Set Params",e);
                }

                try {
                    mCamera.startPreview();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to Start Camera Preview",e);
                }

                mCanCapture = true;
                mActivityHandler.sendMessage(mActivityHandler.obtainMessage(CameraActivityHandler.MSG_SET_PREVIEW_SIZE, size));
            }
        }
    }


    private void releaseCamera() {
        Log.i(TAG, "Release Camera Thread ID: "+ Thread.currentThread().getId());
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            }
            catch (Exception e){
                Log.e(TAG,"Release Camera Failed", e);
            }
            mCamera.release();
            mCamera = null;
        }

        try {
            Looper.myLooper().quitSafely();

        }
        catch (Exception e){
            Log.e(TAG,"Looper Failed to quit safley", e);
        }

    }


    public static class CameraThreadHandler extends Handler {

        private final WeakReference<CameraThread> mWeakThread;

        public CameraThreadHandler (CameraThread cameraManagementThread) {
            mWeakThread = new WeakReference<CameraThread>(cameraManagementThread);
        }

        @Override
        public void handleMessage(Message msg) {
            CameraThread cameraThread = (CameraThread) mWeakThread.get();

            int what = msg.what;
            Object obj = msg.obj;

            Log.d(TAG, "CameraActivityHandler : what= " + what);


            switch(what) {
                case OPEN_CAMERA: {
                    cameraThread.openCamera();
                    break;
                }
                case SWITCH_CAMERA: {
//                    cameraThread.switchCamera((SurfaceTexture) obj);
                    break;
                }
                case SET_PREVIEW_SURFACE: {
                    cameraThread.handleSetPreviewSurface((SurfaceTexture) obj);
                    break;
                }
                case TAKE_PICTURE: {
//                    cameraThread.takePicture();
                    break;
                }
                case MSG_RELEASE_CAMERA: {
                    cameraThread.releaseCamera();
                    break;
                }
                case HANDLE_TEXTURE_CHANGE: {
                    cameraThread.textureChange(msg.arg1, msg.arg2);
                    break;
                }
                case CHANGE_FLASH: {
//                    cameraThread.changeFlash();
                    break;
                }
                case STOP_PREVIEW:
                    cameraThread.stopPreview();
                    break;
                case START_PREVIEW:
                    cameraThread.startPreview();
                    break;
                case CHANGE_RECORDING_STATE:
//                    cameraThread.changeRecordingState();
                    break;
                case HANDLE_ZOOM:
                    cameraThread.handleZoom(msg.arg1, msg.arg2);
                    break;
                case HANDLE_FOCUS:
                    cameraThread.handleAutoFocus();
                    break;
                default: {
                    throw new RuntimeException("Unknown msg = " + what);
                }
            }
        }
    }
}
