package com.devsil.cameravisualizer.Camera;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by devsi on 10/28/2017.
 */

public class CameraUtils {

    public static final String VIDEO_MIME_TYPE = "video/avc";
    public static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";

    public static final int MAX_PREVIEW_WIDTH = 1280;
    public static final int MAX_PREVIEW_HEIGHT = 720;

    public static final int FILTER_NONE = 0;
    public static final int FILTER_BLACK_WHITE = 1;
    public static final int FILTER_BLUR = 2;
    public static final int FILTER_SHARPEN = 3;
    public static final int FILTER_EDGE_DETECT = 4;
    public static final int FILTER_EMBOSS = 5;
    public static final int FILTER_SEPIA = 6;
    public static final int FILTER_SOMETHING = 7;
    public static final int FILTER_SOMETHING_2 = 8;

    public static final int IMAGE_MODE = 100;
    public static final int VIDEO_MODE = 200;
    public static final int LIBRARY_MODE = 300;


    private static final String TAG = ".Debug.CameraUtils";
    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video
     */
    public static void choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            Log.d(TAG, "Camera preferred preview size for video is " +
                    ppsfv.width + "x" + ppsfv.height);
        }

        //for (Camera.Size size : parms.getSupportedPreviewSizes()) {
        //    Log.d(TAG, "supported: " + size.width + "x" + size.height);
        //}

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                return;
            }
        }

        Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
        }
        // else use whatever the default size is
    }

    public static float setPictureSize(Camera.Parameters params){
        // FIRST WE ATTEMPT TO FIND A SUITABLE 4:3
        int MAX_WIDTH = 4160;
        int MAX_HEIGHT = 3120;
        float ASPECT_RATIO = MAX_WIDTH/ (float)MAX_HEIGHT;

//        Log.d(TAG, "Aspect ratio: "+ ASPECT_RATIO);

//        for(Camera.Size size : params.getSupportedPictureSizes()){
//            Log.d(TAG, "SUPPORTED PICTURE SIZES: "+ size.width + " x "+ size.height);
//        }

        for(Camera.Size size : params.getSupportedPictureSizes()) {
//            Log.d(TAG, "Size supported: " + size.width + "x" + size.height);
            if ((size.width <= MAX_WIDTH || size.height <= MAX_HEIGHT) && Math.abs(size.width / (float) size.height - ASPECT_RATIO) < 0.00001) {//size.width /(float)size.height == ASPRECT_RATIO)){
                params.setPictureSize(size.width, size.height);
//                Log.d(TAG, "Picture Size: " + size.width + "x" + size.height);
                return ASPECT_RATIO;
            }
        }

        // NEXT WE ATTEMPT TO FIND A SUITABLE 16:9
        MAX_WIDTH = 4160;
        MAX_HEIGHT = 2340;
        ASPECT_RATIO = MAX_WIDTH /(float)MAX_HEIGHT;

//        Log.d(TAG, "Aspect ratio: "+ ASPECT_RATIO);

        for(Camera.Size size : params.getSupportedPictureSizes()){
//            Log.d(TAG, "Size supported: "+ size.width+"x"+size.height);
            if((size.width <= MAX_WIDTH || size.height <= MAX_HEIGHT) && Math.abs(size.width /(float)size.height - ASPECT_RATIO) < 0.00001){//size.width /(float)size.height == ASPRECT_RATIO)){
                params.setPictureSize(size.width, size.height);
//                Log.d(TAG, "Picture Size: "+ size.width +"x"+ size.height);
                return ASPECT_RATIO;
            }
        }

        return params.getPictureSize().width / (float)params.getPictureSize().height;
    }


    public static boolean canCameraFocus(Camera.Parameters params){

        for(String str : params.getSupportedFocusModes()){
            Log.d(TAG, "Supported focus mode: "+ str);
            if(str.equals(Camera.Parameters.FOCUS_MODE_AUTO)){
                return true;
            }
        }

        return false;
    }

    public static boolean canZoom(Camera.Parameters params){
        return params.isZoomSupported();
    }

    public static int getMaxFaceDetection(Camera.Parameters params){
        return params.getMaxNumDetectedFaces();
    }

    public static Camera.Size getOptimalPreviewSize(Camera.Parameters params, int width, int height){
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = width/height;

        List<Camera.Size> cameraSizes = params.getSupportedPreviewSizes();

        if(cameraSizes == null){
            return null;
        }

        Camera.Size optimalSize = null;
        float minDiff = Float.MAX_VALUE;

        int targetHeight = height;

        for(Camera.Size size : cameraSizes){
            float ratio = (float) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Float.MAX_VALUE;
            for (Camera.Size size : cameraSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        if(optimalSize != null) {
            Log.d(TAG, "Set Preview Size : " + optimalSize.width + "x" + optimalSize.height);
            params.setPreviewSize(optimalSize.width, optimalSize.height);
        }

        return optimalSize;
    }

    public static CamcorderProfile printRecorderProfile(){
        CamcorderProfile profile = CamcorderProfile.get(Camera.CameraInfo.CAMERA_FACING_BACK, CamcorderProfile.QUALITY_HIGH);
        Log.d(TAG, String.format("Camcorder default profile: audio sample rate: %d, Audio BitRate: %d, audio codec: %d\n"+
                        "Video frame rate:%d , Video bitrate: %d ",
                profile.audioSampleRate, profile.audioBitRate, profile.audioCodec, profile.videoFrameRate, profile.videoBitRate));
        return profile;
    }

    public static MediaCodecInfo selectCodec(String mime){
        int numCodecs = MediaCodecList.getCodecCount();

        for(int i = 0; i < numCodecs;  i++){
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if(!codecInfo.isEncoder()){
                continue;
            }

            String[] types = codecInfo.getSupportedTypes();
            for(int j = 0; j < types.length; j++){
                if(types[j].equalsIgnoreCase(mime)){
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public static SimpleDateFormat getExifDateTime(){
        SimpleDateFormat sFormat = new SimpleDateFormat("YYYY:MM:DD HH:mm:ss");
        sFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sFormat;
    }
}
