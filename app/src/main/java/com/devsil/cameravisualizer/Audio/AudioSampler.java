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

public class AudioSampler implements Runnable{

    private static final String TAG = ".Debug.AudioSampler";

    private static final int SAMPLING_RATE = 44100;

    private AudioRecord mAudioRecord;
    private int mBufferSize;

    private int mSamplingInterval = 100;

    private boolean mRecording = false;

    private Timer mTimer; // Temporary - Easy to implement and test this without messing with the calling classes much.

    int numberOfBytesRead;

    private final AudioCalculator mAudioCalculator;
    private final AudioCallback mCallback;


    /**
     *
     * This Class will be used to list to the microphone on the device. This will eventually feed
     * audio data to the Renderer to draw geometric shapes, patterns and distortions onto the Surface ontop of the
     * live camera preview;
     *
     *
     **
     * @param callback - call back to calling thread
     */
    public AudioSampler(AudioCallback callback){
        init();
        mAudioCalculator = new AudioCalculator();
        this.mCallback = callback;
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

        mRecording = true;
        runRecording();
    }

    public void stopRecording(){
        mTimer.cancel();
        mAudioRecord.stop();
        mAudioRecord.release();

        mRecording = false;

    }


    private void runRecording() {
        final byte buffer[] = new byte[mBufferSize];
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!mRecording) {
                    return;
                }
                numberOfBytesRead = mAudioRecord.read(buffer, 0, mBufferSize);

                mAudioCalculator.setBytes(buffer);
                int amplitude = mAudioCalculator.getAmplitude();
                double decibelLevel = mAudioCalculator.getDecibel();
                double frequency = mAudioCalculator.getFrequency();

//                Log.d(TAG, "Calculated Audio Results: Amplitude: " + amplitude + " Decibels: " + decibelLevel + " Frequency: " + frequency);
            }
        }, 0, mSamplingInterval);
    }

    @Override
    public void run() {
        /// IMPLEMENT ALL THIS IN A BACKGROUND THREAD SO AS TO NOT TYE UP THE UI THREAD WITH THESE SHANNANIGANS
    }
}
