package com.devsil.cameravisualizer.Camera.Handlers;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.devsil.cameravisualizer.CameraActivity;

import java.lang.ref.WeakReference;

/**
 * Created by devsil on 10/28/2017.
 */

public class CameraActivityHandler extends Handler {
    private static final String HANDLER_TAG = ".Debug.CameraActivityHandler";

    public static final int MSG_SET_SURFACE_TEXTURE = 0;
    public static final int MSG_CHANGE_SURFACE_TEXTURE = 1;
    public static final int MSG_CAMERA_OPENED = 2;
    public static final int MSG_PICTURE_TAKEN = 3;
    public static final int MSG_CAMERA_CLOSED = 4;
    public static final int MSG_CAMERA_THREAD_READY = 5;
    public static final int MSG_ON_IMAGE_CAPTURE = 6;
    public static final int MSG_DRAW_FACES = 7;
    public static final int MSG_ORIENT_FACEVIEW= 8;
    public static final int MSG_FOCUS_FRAME = 9;
    public static final int MSG_REMOVE_FOCUS_FRAME = 10;
    public static final int MSG_UPDATE_ORIENT =11;
    public static final int MSG_CANT_OPEN_CAMERA = 12;
    public static final int MSG_SET_PREVIEW_SIZE = 13;
    public static final int MSG_SET_FLASH = 14;
    public static final int MSG_START_RECORDING = 15;
    public static final int MSG_STOP_RECORDING = 16;
    public static final int MSG_SET_VIDEO_FILE = 17;
    public static final int MSG_VIDEO_LIMIT_REACHED = 18;
    public static final int MSG_ON_SHUTTER = 19;
    public static final int MSG_ON_AUTO_FOCUS = 20;

    private WeakReference<CameraActivity> mWeakActivity;

    public CameraActivityHandler(CameraActivity activity){
        mWeakActivity = new WeakReference<CameraActivity>(activity);
    }

    public void invalidateHandler(){
        mWeakActivity.clear();
    }

    @Override
    public void handleMessage(Message inputMessage){
        int what = inputMessage.what;

        CameraActivity captureActivity = mWeakActivity.get();

        if(captureActivity == null){
            Log.w(HANDLER_TAG, "CameraActivityHandler.handleMessage() activity is null");
            return;
        }

        switch (what){
            case MSG_SET_SURFACE_TEXTURE:
                captureActivity.handleSetSurfaceTexture((SurfaceTexture) inputMessage.obj);
                break;
            case MSG_CAMERA_OPENED:
                captureActivity.handleCameraOpened((boolean)inputMessage.obj, inputMessage.arg1 , inputMessage.arg2 );
                break;
            case MSG_CHANGE_SURFACE_TEXTURE:
                captureActivity.handleSurfaceTexture((boolean) inputMessage.obj, inputMessage.arg1, inputMessage.arg2);
                break;
            case MSG_CAMERA_THREAD_READY:
                captureActivity.onThreadStarted();
                break;
            case MSG_ON_IMAGE_CAPTURE:
//                captureActivity.onImageCapture((ImageFile) inputMessage.obj);
                break;
            case MSG_UPDATE_ORIENT:
                captureActivity.updateRenderOrientation((int)inputMessage.arg1, (int)inputMessage.arg2);
                break;
            case MSG_CANT_OPEN_CAMERA:
                captureActivity.onCameraFailed();
                break;
            case MSG_SET_PREVIEW_SIZE:
                captureActivity.setPreviewSize((Camera.Size) inputMessage.obj);
                break;
            case MSG_SET_FLASH:
//                captureActivity.setFlashMode((int) inputMessage.obj);
                break;
            case MSG_START_RECORDING:
//                captureActivity.startRecording();
                break;
            case MSG_STOP_RECORDING:
//                captureActivity.stopRecording();
                break;
            case MSG_SET_VIDEO_FILE:
//                captureActivity.setVideoFile((File) inputMessage.obj);
                break;
            case MSG_VIDEO_LIMIT_REACHED:
//                captureActivity.onVideoLimitReached();
                break;
            case MSG_ON_SHUTTER:
//                captureActivity.onShutter();
                break;
            case MSG_ON_AUTO_FOCUS:
//                captureActivity.onAutoFocus();
                break;
            default:
                throw new RuntimeException("unknown msg= "+ what);
        }

    }
}
