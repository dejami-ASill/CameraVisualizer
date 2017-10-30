package com.devsil.cameravisualizer.Camera.Interfaces;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

/**
 * Created by devsi on 10/28/2017.
 */

public interface ICameraActivity {

    void handleSetSurfaceTexture(SurfaceTexture texture);
    void handleCameraOpened(boolean backFacing, int width, int height);
    void handleSurfaceTexture(boolean isPortrait, int width, int height);
    void onThreadStarted();
    void onImageCapture(); // TODO pass through an object representing the image and its data
    void updateRenderOrientation(int orientation, int bitrate);
    void onCameraFailed();

    void setPreviewSize(Camera.Size size);
}
