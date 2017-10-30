package com.devsil.cameravisualizer.Imaging.GLTools;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by devsil on 10/30/2017.
 */

public class Line {



    private static final int COORDS_PER_VERTEX = 2;

    // Program from rendering the vertices of a shape
    private final String vertexShaderCode =
            "attribute vec4 vPosition; "+
                    "void main() {" +
                    "    gl_Position = vPosition;" +
                    "}";

    // Program for renderer the face of a shape with color or textures
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main(){" +
                    "   gl_FragColor = vColor;"+
                    "}";


    private FloatBuffer mVertexBuffer;
    private final ShortBuffer mDrawListBuffer;
    private float mTriangleBuffer;

    private int mProgram;
    private int mTextureTarget;

    private int mPositionHandle;
    private int mColorHandle;

    private final int mVertexStride = COORDS_PER_VERTEX * 4;


    float color[] = {1.0f, 1.0f, 1.0f, 1.0f};

    private static final float LINE_TEX_COORDS[] = {
            0.0f, 0.4f,     // 0 bottom left
            1.0f, 0.4f,     // 1 bottom right
            0.0f, 0.6f,     // 2 top left
            1.0f, 0.6f      // 3 top right
    };

    
    private final float[] mLinePathCoords = {
            0.0f, 0.5f,
            1.0f, 0.5f
    };
    private short[] pathDrawOrder = {0,1};



    public Line(){
        
        ByteBuffer bb = ByteBuffer.allocateDirect(mLinePathCoords.length *4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(mLinePathCoords);
        mVertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(pathDrawOrder.length *2);
        dlb.order(ByteOrder.nativeOrder());
        mDrawListBuffer = dlb.asShortBuffer();
        mDrawListBuffer.put(pathDrawOrder);
        mDrawListBuffer.position(0);

        
        
        mTextureTarget = GLES20.GL_TEXTURE_2D;
        mProgram = GLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
    }

    public void draw(int textureId){
        GLES20.glUseProgram(mProgram);
        GLUtil.checkGlError("draw start");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, mVertexStride, mVertexBuffer);

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        GLES20.glDrawElements(GLES20.GL_LINES, pathDrawOrder.length, GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);


        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisable(mColorHandle);

    }


    public void release() {
        GLES20.glDeleteProgram(mProgram);
        mProgram = -1;
    }



}
