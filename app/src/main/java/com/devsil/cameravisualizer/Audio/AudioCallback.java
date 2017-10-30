package com.devsil.cameravisualizer.Audio;

/**
 * Created by devsil on 10/30/2017.
 */

public interface AudioCallback {

    void onAudioSampled(int amp, double db, double freq);
}
