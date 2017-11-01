package com.devsil.cameravisualizer.Camera;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.Log;

import com.devsil.cameravisualizer.Camera.Handlers.CameraActivityHandler;
import com.devsil.cameravisualizer.Imaging.GLTools.FullFrameRect;
import com.devsil.cameravisualizer.Imaging.GLTools.Line;
import com.devsil.cameravisualizer.Imaging.GLTools.Rectangle;
import com.devsil.cameravisualizer.Imaging.GLTools.Texture2DProgram;
import com.devsil.cameravisualizer.Imaging.GLTools.Triangle;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraSurfaceRenderer implements GLSurfaceView.Renderer{

    public enum MODE {
        NONE, RECT, TRIANGLE // MORE COMING SOON
    }

    private static final String TAG = ".Debug.GLRenderer";

    private boolean VERBOSE = false;

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    private CameraActivityHandler mCameraHandler;
//    private TextureMovieEncoder mVideoEncoder;
    private File mOutputFile;

    private FullFrameRect mFullScreen;

    private final float[] mSTMatrix = new float[16];
    private int mTextureId;

    private SurfaceTexture mSurfaceTexture;
    private boolean mRecordingEnabled;
    private int mRecordingStatus;
    private int mFrameCount;

    // width/height of the incoming camera preview frames
    private boolean mIncomingSizeUpdated;
    private int mIncomingWidth;
    private int mIncomingHeight;

    // width/height of the outgoing camera frames.
    private int mOutputWidth;
    private int mOutputHeight;
    private int mOutputBitrate;

    private int mCurrentFilter;
    private int mNewFilter;

    private final boolean mSupportsVideo;

    // Shape Declarations
    private Line mLine;
    private Triangle mTriangle;
    private Rectangle mRect;

    private MODE mWhichMode = MODE.NONE;

    // TODO ADD IN Video Stuff
//    private VideoMuxer mMuxer;

    /**
     * Constructs CameraSurfaceRenderer.
     * <p>
     * @param cameraHandler Handler for communicating with UI thread
     * @params audioEncoder audio Encoder Obect
     */
    public CameraSurfaceRenderer(CameraActivityHandler cameraHandler) {
        mCameraHandler = cameraHandler;

        mTextureId = -1;

        mRecordingStatus = -1;
        mRecordingEnabled = false;
        mFrameCount = -1;

        mIncomingSizeUpdated = false;
        mIncomingWidth = mIncomingHeight = -1;

        // Just setting these to default in case they are never set otherwise.
        mIncomingWidth = 640;
        mIncomingHeight = 480;
        mOutputBitrate = 1000000;

        // We could preserve the old filter mode, but currently not bothering.
        mCurrentFilter = -1;
        mNewFilter = CameraUtils.FILTER_NONE;


        mSupportsVideo = true;
    }

    public void setOutputFile(File file){
        this.mOutputFile = file;
    }

//    public void setMuxer(VideoMuxer muxer){
//        mMuxer = muxer;
//    }

//    public void setTextureEncoder(TextureMovieEncoder encoder){
//        mVideoEncoder = encoder;
//    }


    public File getOutputFile(){
        return mOutputFile;
    }

    /**
     * Notifies the renderer thread that the activity is pausing.
     * <p>
     * For best results, call this *after* disabling Camera preview.
     */
    public void notifyPausing() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false);     // assume the GLSurfaceView EGL context is about
            mFullScreen = null;             //  to be destroyed
        }

        mIncomingWidth = mIncomingHeight = -1;
    }

    /**
     * Notifies the renderer that we want to stop or start recording.
     */
    public void changeRecordingState(boolean isRecording) {
        mRecordingEnabled = isRecording;
    }

    /**
     * Changes the filter that we're applying to the camera preview.
     */
    public void changeFilterMode(int filter) {
        mNewFilter = filter;
    }

    public void updateOutputFormat(int width, int height, int bitrate){
        mOutputWidth = width;
        mOutputHeight = height;
        mOutputBitrate = bitrate;
    }

    /**
     * Updates the filter program.
     * This wont be used for the time being unless we decide we want the filter feature.
     */
    public void updateFilter() {
        Texture2DProgram.ProgramType programType;
        float[] kernel = null;
        float colorAdj = 0.0f;

        switch (mNewFilter) {
            case CameraUtils.FILTER_NONE:
                programType = Texture2DProgram.ProgramType.TEXTURE_EXT;
                break;
            case CameraUtils.FILTER_BLACK_WHITE:
                programType = Texture2DProgram.ProgramType.TEXTURE_EXT_BW;
                break;
            case CameraUtils.FILTER_BLUR:
                programType = Texture2DProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        1f/16f, 2f/16f, 1f/16f,
                        2f/16f, 4f/16f, 2f/16f,
                        1f/16f, 2f/16f, 1f/16f };
                break;
            case CameraUtils.FILTER_SHARPEN:
                programType = Texture2DProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        0f, -1f, 0f,
                        -1f, 5f, -1f,
                        0f, -1f, 0f };
                break;
            case CameraUtils.FILTER_EDGE_DETECT:
                programType = Texture2DProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        -1f, -1f, -1f,
                        -1f, 8f, -1f,
                        -1f, -1f, -1f };
                break;
            case CameraUtils.FILTER_EMBOSS:
                programType = Texture2DProgram.ProgramType.TEXTURE_EXT_FILT;
                kernel = new float[] {
                        2f, 0f, 0f,
                        0f, -1f, 0f,
                        0f, 0f, -1f };
                colorAdj = 0.5f;
                break;
            case CameraUtils.FILTER_SEPIA:
                programType = Texture2DProgram.ProgramType.TEXTURE_EXT_SEPIA;
                break;
            case CameraUtils.FILTER_SOMETHING:
                programType = Texture2DProgram.ProgramType.TEXTURE_EXT_SOMETHING;
                break;
            case CameraUtils.FILTER_SOMETHING_2:
                programType = Texture2DProgram.ProgramType.TEXTURE_EXT_SOMETHING_2;
                break;
            default:
                throw new RuntimeException("Unknown filter mode " + mNewFilter);
        }

        // Do we need a whole new program?  (We want to avoid doing this if we don't have
        // too -- compiling a program could be expensive.)
        if (programType != mFullScreen.getProgram().getProgramType()) {
            mFullScreen.changeProgram(new Texture2DProgram(programType));
            // If we created a new program, we need to initialize the texture width/height.
            mIncomingSizeUpdated = true;
        }

        // Update the filter kernel (if any).
        if (kernel != null) {
            mFullScreen.getProgram().setKernel(kernel, colorAdj);
        }

        mCurrentFilter = mNewFilter;
    }

    /**
     * Records the size of the incoming camera preview frames.
     * <p>
     * It's not clear whether this is guaranteed to execute before or after onSurfaceCreated(),
     * so we assume it could go either way.  (Fortunately they both run on the same thread,
     * so we at least know that they won't execute concurrently.)
     */
    public void setCameraPreviewSize(int width, int height) {
        mIncomingWidth = width;
        mIncomingHeight = height;
        mIncomingSizeUpdated = true;
    }

    public void setEffect(MODE effect){
        this.mWhichMode = effect;
    }

    public SurfaceTexture getSurfaceTexture(){
        return mSurfaceTexture;
    }

    public void setSoundData(int amp, double db, double freq){
//        Log.d(TAG, "Calculated Audio Results: Amplitude: " + amp + " Decibels: " + db + " Frequency: " + freq);

        if(mRect != null && mWhichMode == MODE.RECT){
            mRect.setColor(amp, db, freq);
        }
        else if(mTriangle != null && mWhichMode== MODE.TRIANGLE){
            mTriangle.setTriangleColor(amp, db, freq);
        }

    }


    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {

        // We're starting up or coming back.  Either way we've got a new EGLContext that will
        // need to be shared with the video encoder, so figure out if a recording is already
        // in progress.
        mRecordingEnabled = false;
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Enable alpha blending to allow the alpha to be rendered correctly.
        GLES20.glEnable(GLES20.GL_BLEND);
        // Allows the alpha bit in the "Overlay" bitmap to be multplied by the background images creating the transparent (window) effect.
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Set up the texture blitter that will be used for on-screen display.  This
        // is *not* applied to the recording, because that uses a separate shader.
        mFullScreen = new FullFrameRect(
                new Texture2DProgram(Texture2DProgram.ProgramType.TEXTURE_EXT));

        mLine = new Line();

        mTriangle = new Triangle();

        mRect = new Rectangle();

        mTextureId = mFullScreen.createTextureObject();

        // Create a SurfaceTexture, with an external texture, in this EGL context.  We don't
        // have a Looper in this thread -- GLSurfaceView doesn't create one -- so the frame
        // available messages will arrive on the main thread.
        mSurfaceTexture = new SurfaceTexture(mTextureId);

        // Tell the UI thread to enable the camera preview.
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                CameraActivityHandler.MSG_SET_SURFACE_TEXTURE, mSurfaceTexture));
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        // All we are doing here is adjusting the view port width and height;
        gl10.glViewport(0,0,width,height);
        GLES20.glViewport(0,0, width, height);

        if(mTriangle != null){
            mTriangle.defineProjections(width, height);
        }

        if(mRect != null){
            mRect.defineProjections(width, height);
        }

        // Now we send a comment back to the UI thread to let the camera know of the needed changes.
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                CameraActivityHandler.MSG_CHANGE_SURFACE_TEXTURE, width,height, (height > width) /*isPortrait*/));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onDrawFrame(GL10 gl10) {
        boolean showBox = false;

        // Latch the latest frame.  If there isn't anything new, we'll just re-use whatever
        // was there before.
        try{
            mSurfaceTexture.updateTexImage();
        }
        catch (Exception e){
            Log.e(TAG,"Error onDrawFrame", e);
        }


        // If the recording state is changing, take care of it here.  Ideally we wouldn't
        // be doing all this in onDrawFrame(), but the EGLContext sharing with GLSurfaceView
        // makes it hard to do elsewhere.
        if(mSupportsVideo) {
            if (mRecordingEnabled) {
//                switch (mRecordingStatus) {
//                    case RECORDING_OFF:
//                        // start recording
//                        mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(
//                                mOutputFile, mOutputWidth, mOutputHeight, mOutputBitrate, EGL14.eglGetCurrentContext()), mMuxer);
//
//                        mRecordingStatus = RECORDING_ON;
//                        break;
//                    case RECORDING_RESUMED:
//                        mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
//                        mRecordingStatus = RECORDING_ON;
//                        break;
//                    case RECORDING_ON:
//                        // yay
//                        break;
//                    default:
//                        throw new RuntimeException("unknown status " + mRecordingStatus);
//                }
            } else {
//                switch (mRecordingStatus) {
//                    case RECORDING_ON:
//                    case RECORDING_RESUMED:
//                        // stop recording
//                        mVideoEncoder.stopRecording();
//                        mMuxer.release();
//                        mRecordingStatus = RECORDING_OFF;
//                        break;
//                    case RECORDING_OFF:
//                        // yay
//                        break;
//                    default:
//                        throw new RuntimeException("unknown status " + mRecordingStatus);
//                }
            }
        }

        // Set the video encoder's texture name.  We only need to do this once, but in the
        // current implementation it has to happen after the video encoder is started, so
        // we just do it here.
//        if(mVideoEncoder != null) {
//            mVideoEncoder.setTextureId(mTextureId);
//
//            // Tell the video encoder thread that a new frame is available.
//            // This will be ignored if we're not actually recording.
//            mVideoEncoder.frameAvailable(mSurfaceTexture);
//        }
        // Using just this and not the loop with mRecording results in jump, presumbly because of the timestamps
        if (mIncomingWidth <= 0 || mIncomingHeight <= 0) {
            // Texture size isn't set yet.  This is only used for the filters, but to be
            // safe we can just skip drawing while we wait for the various races to resolve.
            // (This seems to happen if you toggle the screen off/on with power button.)
            if(VERBOSE) {
                Log.i(TAG, "Drawing before incoming texture size set; skipping");
            }
        }
        // Update the filter, if necessary.
        if (mCurrentFilter != mNewFilter) {
            updateFilter();
        }
        if (mIncomingSizeUpdated) {
            mFullScreen.getProgram().setTexSize(mIncomingWidth, mIncomingHeight);
            mIncomingSizeUpdated = false;
        }

        // Draw the video frame.
        if(mSurfaceTexture != null) {
            mSurfaceTexture.getTransformMatrix(mSTMatrix);
        }
        if(mFullScreen != null){
                mFullScreen.drawFrame(mTextureId, mSTMatrix);
        }


        switch (mWhichMode){
            case RECT:
                mRect.draw();
                break;
            case TRIANGLE:
                mTriangle.draw();
                break;
            case NONE:
            default:
                // DO NOTHING HERE WE DONT HAVE TO DRAW ANYTHING EXTRA
        }

        /// End Frame Rendering ///
    }


}

