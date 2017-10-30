package com.devsil.cameravisualizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.devsil.cameravisualizer.Audio.AudioCallback;
import com.devsil.cameravisualizer.Audio.AudioSampler;
import com.devsil.cameravisualizer.Camera.CameraSurfaceRenderer;
import com.devsil.cameravisualizer.Camera.CameraSurfaceView;
import com.devsil.cameravisualizer.Camera.CameraUtils;
import com.devsil.cameravisualizer.Camera.Handlers.CameraActivityHandler;
import com.devsil.cameravisualizer.Camera.Interfaces.ICameraActivity;
import com.devsil.cameravisualizer.Camera.Threads.CameraThread;

/**
 * Created by devsil on 10/28/2017.
 */

public class CameraActivity extends AppCompatActivity implements ICameraActivity, SurfaceTexture.OnFrameAvailableListener, AudioCallback{
    private static final String TAG = ".Debug.CameraActivity";

    private final int REQUEST_CODE_CAMERA = 40001;

    private final int REQUEST_CODE_AUDIO = 50001;


    private FrameLayout flMain;

    private CameraSurfaceView mCameraSurface;

    private TextView mEffectText;

    protected CameraActivityHandler mActivityHandler;
    private CameraThread mCameraThread;
    protected CameraThread.CameraThreadHandler mCameraHandler;

    protected CameraSurfaceRenderer mRenderer;

    private AudioSampler mAudioSampler;

    @Override
    public void onResume(){
        super.onResume();

        // CHECK PERMISSIONS
        if(checkSelfPermission(Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED){
            // We evidently don't have permissions. So lets determine in what way we ask the user.

            // Should we show the user the rationale for this? Maybe because they denied it before but is needed.
            if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                // TODO ALERT DIALOG HERE or something.

            } else{
                // Make the request for the permissions
              requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
            }
        }
        else {
            // We have permission to use the camera! Whoooo!
            startCameraThread();
        }


        // CHECK PERMISSIONS
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO ) != PackageManager.PERMISSION_GRANTED){
            // We evidently don't have permissions. So lets determine in what way we ask the user.

            // Should we show the user the rationale for this? Maybe because they denied it before but is needed.
            if(shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)){
                // TODO ALERT DIALOG HERE or something.

            } else{
                // Make the request for the permissions
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_AUDIO);
            }
        }
        else {
            // We have permission to use the camera! Whoooo!
            startAudioRecording();
        }


        mCameraSurface.onResume();

    }


    @Override
    public void onPause(){
        super.onPause();

        releaseCamera();

        mCameraSurface.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.notifyPausing();
            }
        });

        mCameraSurface.onPause();

        if(isFinishing()){
            if(mCameraHandler != null) {
                mCameraHandler.removeCallbacks(mCameraThread);
            }

            if (mActivityHandler != null) {
                mActivityHandler.invalidateHandler();
            }

            mAudioSampler.stopRecording();
        }


    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        Log.i(TAG, "on Create: UI THREAD ID: " + Thread.currentThread().getId());// Just for informational purposes

        mActivityHandler = new CameraActivityHandler(this);

        mRenderer = new CameraSurfaceRenderer(mActivityHandler);

        setContentView(R.layout.activity_camera);

        flMain = (FrameLayout) findViewById(R.id.main_frame);

        mCameraSurface = (CameraSurfaceView)findViewById(R.id.camera_surface_view);


        mEffectText = (TextView) findViewById(R.id.effect_textview);

        mCameraSurface.setRenderer(mRenderer);
    }

    private void releaseCamera(){
        if(mCameraHandler != null ){
            Log.d(TAG, "Release Camera");
            mCameraHandler.sendMessage(mCameraHandler.obtainMessage(CameraThread.MSG_RELEASE_CAMERA));
        }
    }


    private void startCameraThread(){

        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            Snackbar.make(flMain, R.string.action_camera, Snackbar.LENGTH_SHORT);
            return;
        }


        mCameraThread = CameraThread.getInstance(mActivityHandler, getWindowManager());

        Thread cameraThread = new Thread(mCameraThread, "CameraManagementThread");
        cameraThread.start();


        mCameraHandler = mCameraThread.mCameraHandler;
    }



    private void startAudioRecording(){
        mAudioSampler = new AudioSampler(this);
        mAudioSampler.startRecording();
    }


    public boolean isPortrait(){
        int tempOrientation = getResources().getConfiguration().orientation;
        switch (tempOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return false;
            case Configuration.ORIENTATION_PORTRAIT:
                return true;
        }
        return false;
    }

    @Override
    public void handleSetSurfaceTexture(SurfaceTexture texture) {
        texture.setOnFrameAvailableListener(this);

        if(mCameraHandler != null) {
            mCameraHandler.sendMessage(mCameraHandler.obtainMessage(CameraThread.SET_PREVIEW_SURFACE, texture));
        }
    }

    @Override
    public void handleCameraOpened(boolean backFacing, int width, int height) {
        if(isPortrait()) {
            mRenderer.setCameraPreviewSize(width, height);
        }
        else{
            mRenderer.setCameraPreviewSize(height, width);
        }
    }

    @Override
    public void handleSurfaceTexture(boolean isPortrait, int width, int height) {
        Log.d(TAG, "Surface Texture size : " + width + "x" + height);
        if (isPortrait) {
            mRenderer.setCameraPreviewSize(height,width);
        } else {
            mRenderer.setCameraPreviewSize(width, height);
        }
        if(mCameraHandler != null) {
            mCameraHandler.sendMessage(mCameraHandler.obtainMessage(CameraThread.HANDLE_TEXTURE_CHANGE, width,height));
        }
    }

    @Override
    public void onThreadStarted() {
        mCameraHandler = mCameraThread.mCameraHandler;
        if(mCameraHandler != null) {
            mCameraHandler.sendMessage(mCameraHandler.obtainMessage(CameraThread.OPEN_CAMERA));
            Log.d(TAG, "Camera Handler obtained");
        }

    }

    @Override
    public void onImageCapture() {
        // TODO
    }

    @Override
    public void updateRenderOrientation(int orientation, int bitrate) {
        if(orientation == 0){
            // Portrait
            mRenderer.updateOutputFormat(CameraUtils.MAX_PREVIEW_HEIGHT, CameraUtils.MAX_PREVIEW_WIDTH,bitrate);
        }
        else{
            // Landscape
            mRenderer.updateOutputFormat(CameraUtils.MAX_PREVIEW_WIDTH, CameraUtils.MAX_PREVIEW_HEIGHT, bitrate);
        }
    }

    @Override
    public void onCameraFailed() {
        Snackbar.make(flMain, R.string.camera_failed, Snackbar.LENGTH_SHORT);

    }

    @Override
    public void setPreviewSize(Camera.Size size) {
        final int width = size.width;
        final int height = size.height;
        Log.d(TAG, "Camera Preview Size: " + size.width + "x" + size.height);
        if(isPortrait()){
            mCameraSurface.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.setCameraPreviewSize(height,width);
                }
            });
        }
        else{
            mCameraSurface.queueEvent(new Runnable() {
                @Override
                public void run() {
                    mRenderer.setCameraPreviewSize(width, height);
                }
            });
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch (requestCode){
            case REQUEST_CODE_CAMERA:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startCameraThread();
                }
                else{
                    Snackbar.make(flMain, R.string.camera_permissions_required, Snackbar.LENGTH_SHORT);
                }
                break;
            case REQUEST_CODE_AUDIO:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    startAudioRecording();
                }
                else{
                    Snackbar.make(flMain, R.string.camera_permissions_required, Snackbar.LENGTH_SHORT);
                }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mCameraSurface.requestRender();
    }


    float mDistance, mFirstTouch, mCurrentTouch;
    static final int SWIPE_THRESHOLD = 450;
    private int mCurrentEffect = 0;
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        int action = motionEvent.getAction();

        // TWO FINGER TOUCHING IS HAPPENING i.e Zoom
        if(motionEvent.getPointerCount() > 1){
            if(action == MotionEvent.ACTION_POINTER_DOWN){
                mDistance = getFingerSpacing(motionEvent);
            }
            else if(action == MotionEvent.ACTION_MOVE){
                handleZoom(motionEvent);
            }
            return super.onTouchEvent(motionEvent);
        }

        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                mFirstTouch = motionEvent.getX();
                break;
            case MotionEvent.ACTION_UP:
                mCurrentTouch = motionEvent.getX();
                float delta = mCurrentTouch - mFirstTouch;
                if(( mCurrentTouch < mFirstTouch ) && Math.abs(delta) > SWIPE_THRESHOLD){
                    // This will be a right to left kind of swipe
                    incrementEffect();
                }
                else if((mCurrentTouch > mFirstTouch) && Math.abs(delta) > SWIPE_THRESHOLD){
                    decrementEffect();
                }
        }
        return super.onTouchEvent(motionEvent);
    }

    private void incrementEffect(){
        if(mCurrentEffect == 8){
            mCurrentEffect = 0;
        }else{
            mCurrentEffect++;
        }
        changePreviewEffect();

    }

    private void decrementEffect(){
        if(mCurrentEffect == 0){
            mCurrentEffect = 8;
        }
        else{
            mCurrentEffect--;
        }
        changePreviewEffect();
    }

    private void changePreviewEffect(){
        mCameraSurface.queueEvent(new Runnable() {
            @Override
            public void run() {
                mRenderer.changeFilterMode(mCurrentEffect);
            }
        });

        showLabel();
    }

    private void showLabel(){
        String which = null;
        switch (mCurrentEffect){
            case CameraUtils.FILTER_NONE:
                which = getString(R.string.camera_effect_normal);
                break;
            case CameraUtils.FILTER_BLACK_WHITE:
                which = getString(R.string.camera_effect_bw);
                break;
            case CameraUtils.FILTER_BLUR:
                which = getString(R.string.camera_effect_blur);
                break;
            case CameraUtils.FILTER_EDGE_DETECT:
                which = getString(R.string.camera_effect_edge_detection);
                break;
            case CameraUtils.FILTER_EMBOSS:
                which = getString(R.string.camera_effect_emboss);
                break;
            case CameraUtils.FILTER_SEPIA:
                which = getString(R.string.camera_effect_emboss);
                break;
            case CameraUtils.FILTER_SOMETHING:
                which = getString(R.string.camera_effect_something_weird);
                break;
            case CameraUtils.FILTER_SOMETHING_2:
                which = getString(R.string.camera_effect_something_stranger);
                break;
            default:
                Log.e(TAG, "Unknown filter");
        }

        mEffectText.setText(which);


        final Animation in = new AlphaAnimation(0.0f, 1.0f);
        in.setDuration(1000);

        final Animation out = new AlphaAnimation(1.0f, 0.0f);
        out.setDuration(1000);

        in.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mEffectText.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mEffectText.startAnimation(out);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                mEffectText.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        mEffectText.startAnimation(in);

    }


    private float getFingerSpacing(MotionEvent event){
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    private void handleZoom(MotionEvent event){
        float newDistance = getFingerSpacing(event);

        if(mCameraHandler != null) {
            mCameraHandler.handleMessage(mCameraHandler.obtainMessage(CameraThread.HANDLE_ZOOM, (int) newDistance, (int) mDistance));
            mDistance = newDistance;
        }
    }

    @Override
    public void onAudioSampled(int amp, double db, double freq) {
//        Log.d(TAG, "Calculated Audio Results: Amplitude: " + amp + " Decibels: " + db + " Frequency: " + freq);
    }
}
