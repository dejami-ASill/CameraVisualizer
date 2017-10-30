package com.devsil.cameravisualizer.Camera;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Created by devsil on 10/28/2017.
 */

public class CameraSurfaceView extends GLSurfaceView {

    public CameraSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
//        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }
}
