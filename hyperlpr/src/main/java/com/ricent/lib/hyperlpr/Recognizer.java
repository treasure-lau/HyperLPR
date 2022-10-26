package com.ricent.lib.hyperlpr;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class Recognizer {
    private final long object;

    public boolean isError() {
        return object == -1;
    }

    public Recognizer(String cascade_filename,
                      String finemapping_prototxt, String finemapping_caffemodel,
                      String segmentation_prototxt, String segmentation_caffemodel,
                      String character_prototxt, String character_caffemodel,
                      String segmentationfree_prototxt, String segmentationfree_caffemodel) {
        if (PlateRecognition.isLoaded()) {
            object = PlateRecognition.InitPlateRecognizer(
                    cascade_filename,
                    finemapping_prototxt, finemapping_caffemodel,
                    segmentation_prototxt, segmentation_caffemodel,
                    character_prototxt, character_caffemodel,
                    segmentationfree_prototxt, segmentationfree_caffemodel
            );
        } else {
            object = -1;
        }
    }

    public PlateInfo simple(Bitmap bmp, int dp) {
        if (isError()) {
            return null;
        }
        float dp_asp = dp / 10.f;
        Mat mat_src = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC4);
        float new_w = bmp.getWidth() * dp_asp;
        float new_h = bmp.getHeight() * dp_asp;
        Size sz = new Size(new_w, new_h);
        Utils.bitmapToMat(bmp, mat_src);
        Imgproc.resize(mat_src, mat_src, sz);
        return PlateRecognition.PlateInfoRecognization(mat_src.getNativeObjAddr(), object);
    }

    public void release() {
        if (isError()) {
            return;
        }
        PlateRecognition.ReleasePlateRecognizer(object);
    }
}
