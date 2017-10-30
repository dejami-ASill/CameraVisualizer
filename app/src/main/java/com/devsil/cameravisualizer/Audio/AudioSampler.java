package com.devsil.cameravisualizer.Audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by devsil on 10/30/2017.
 */

public class AudioSampler {

    private static final String TAG = ".Debug.AudioSampler";

    private static final int SAMPLING_RATE = 44100;

    private AudioRecord mAudioRecord;
    private int mBufferSize;

    private int mSamplingInterval = 100;

    private Visualizer mVisualizer;

    private boolean mRecording = false;

    private Timer mTimer;

    public AudioSampler(){
        init();
    }

    private void init(){
        int bs = AudioRecord.getMinBufferSize(SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bs);

        if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            mBufferSize = bs;
        }


    }


    public void startRecording(){
        mTimer = new Timer();

        mAudioRecord.startRecording();

        Log.d(TAG, "AudioRecorder ID: "+ mAudioRecord.getAudioSessionId());


        mVisualizer = new Visualizer(mAudioRecord.getAudioSource());
        mVisualizer.setMeasurementMode(Visualizer.MEASUREMENT_MODE_PEAK_RMS);

        mVisualizer.setDataCaptureListener(VISUALIZER_LISTENER,SAMPLING_RATE, true, true);
        mVisualizer.setEnabled(true);
        mRecording = true;
        runRecording();
    }

    public void stopRecording(){
        mAudioRecord.stop();
        mAudioRecord.release();

        mRecording = false;

        mVisualizer.setEnabled(false);
        mVisualizer.release();
    }


    private void runRecording(){
        final byte buffer[] = new byte[mBufferSize];
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mAudioRecord.read(buffer, 0, mBufferSize);
            }
        },0,mSamplingInterval);
    }


    private Visualizer.OnDataCaptureListener VISUALIZER_LISTENER = new Visualizer.OnDataCaptureListener() {
        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int i) {
            Log.d(TAG, "On Wave Form Data Capture : " + bytes.length  + " : " + i);
        }

        @Override
        public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int i) {
            Log.d(TAG, "On Fft Data Capture : " + bytes.length  + " : " + i);
        }
    };


}
