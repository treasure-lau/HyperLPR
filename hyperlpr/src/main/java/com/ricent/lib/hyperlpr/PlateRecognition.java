package com.ricent.lib.hyperlpr;

import android.util.Log;

import org.opencv.android.OpenCVLoader;

/**
 * Created by yujinke on 24/10/2017.
 */

public class PlateRecognition {
    private static boolean isLoaded;

    public static boolean isLoaded() {
        return isLoaded;
    }

    static {

        try {
            isLoaded = OpenCVLoader.initDebug();
            if (isLoaded) {
                System.loadLibrary("ricenthyperlpr");
            }
        } catch (UnsatisfiedLinkError error) {
            Log.w("PlateRecognition", error);
            isLoaded = false;
        }
    }

    static native long InitPlateRecognizer(String casacde_detection,
                                           String finemapping_prototxt, String finemapping_caffemodel,
                                           String segmentation_prototxt, String segmentation_caffemodel,
                                           String charRecognization_proto, String charRecognization_caffemodel,
                                           String segmentationfree_proto, String segmentationfree_caffemodel);

    static native void ReleasePlateRecognizer(long object);

    static native String SimpleRecognization(long inputMat, long object);

    static native PlateInfo PlateInfoRecognization(long inputMat, long object);


}
