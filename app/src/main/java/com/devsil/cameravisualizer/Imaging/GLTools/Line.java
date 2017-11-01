package com.devsil.cameravisualizer.Imaging.GLTools;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by devsil on 10/31/2017.
 */

public class Line {

    private final String vertexShaderCode =
                "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";



    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;


    private int mProgramHandle;

    private int mPositionHandle;

    private int mColorHandle;

    private int mMVPMatrixHandle;

    static final int COORDS_PER_VERTEX = 3;
    private final int vertexStride = COORDS_PER_VERTEX * 4;


    private float[] mPathCords =
            {
                    0.5f, 0.5f, 0.0f,

                    0.5f, 0.0f, 0.0f
            };


    private short[] mPathDrawOrder = {0,1};
    private float[] mColor = {1.0f, 0.0f, 0.0f, 1.0f};

    public Line(){
        ByteBuffer bb = ByteBuffer.allocateDirect(mPathCords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(mPathCords);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(mPathDrawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(mPathDrawOrder);
        drawListBuffer.position(0);

        int vertexShader = GLUtil.loadShader(GLES20.GL_VERTEX_SHADER,vertexShaderCode);
        int fragmentShader = GLUtil.loadShader(GLES20.GL_FRAGMENT_SHADER,fragmentShaderCode);

        mProgramHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgramHandle, vertexShader);
        GLES20.glAttachShader(mProgramHandle, fragmentShader);
        GLES20.glLinkProgram(mProgramHandle);
    }

    public int createTextureObject() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLUtil.checkGlError("glGenTextures");

        int texId = textures[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLUtil.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        GLUtil.checkGlError("glTexParameter");

        return texId;
    }


    public void drawFrame(int texId , float[] mvpMatrix){
        GLUtil.checkGlError("Before Draw");
        // Set Renderer to use the Line Program.
        GLES20.glUseProgram(mProgramHandle);

        GLUtil.checkGlError("glUseProgram");
//        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLUtil.checkGlError("gl Active ");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLUtil.checkGlError("gl Bind ");


        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLUtil.checkGlError("enable vertex poimter");
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);
        GLUtil.checkGlError("enable vertex poimter 2");

        mColorHandle = GLES20.glGetUniformLocation(mProgramHandle, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, mColor, 0);
        GLUtil.checkGlError("enable vertex poimter 3");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLUtil.checkGlError("enable vertex poimter 2");


        GLES20.glDrawElements(GLES20.GL_LINES, mPathDrawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLUtil.checkGlError("enable vertex poimter 2");

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLUtil.checkGlError("After Disable position");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLUtil.checkGlError("After Line Draw");


    }





}
