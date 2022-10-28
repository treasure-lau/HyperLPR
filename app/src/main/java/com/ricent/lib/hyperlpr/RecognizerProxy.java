package com.ricent.lib.hyperlpr;

import android.graphics.Bitmap;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

public class RecognizerProxy {

    private static final String TAG = "RecognizerProxy";
    private Class<?> clazz;
    private Object object;
    private boolean isRelease = false;

    public RecognizerProxy(String cascade_filename,
                           String finemapping_prototxt, String finemapping_caffemodel,
                           String segmentation_prototxt, String segmentation_caffemodel,
                           String character_prototxt, String character_caffemodel,
                           String segmentationfree_prototxt, String segmentationfree_caffemodel) {
        try {
            clazz = Class.forName("com.ricent.lib.hyperlpr.Recognizer");
            Constructor<?> cons = clazz.getConstructor(String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class);
            object = cons.newInstance(cascade_filename,
                    finemapping_prototxt, finemapping_caffemodel,
                    segmentation_prototxt, segmentation_caffemodel,
                    character_prototxt, character_caffemodel,
                    segmentationfree_prototxt, segmentationfree_caffemodel);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public Map<String, Object> simple(Bitmap bmp, int dp) {
        if (isInitError()) {
            return null;
        }
        try {
            Method method = clazz.getMethod("simple", Bitmap.class, int.class);
            Object result = method.invoke(object, bmp, dp);
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    public void release() {
        if (isInitError()) {
            return;
        }
        if (isRelease) {
            return;
        }
        try {
            Log.d(TAG, "release");
            Method method = clazz.getMethod("release");
            method.invoke(object);
            isRelease = true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public int isSimple() {
        if (isInitError()) {
            return -1;
        }
        try {
            Method method = clazz.getMethod("isSimple");
            Object result = method.invoke(object);
            if (result instanceof Boolean) {
                Boolean b = (Boolean) result;

                return b ? 1 : 0;
            }
            return -1;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return -1;
        }
    }

    public void setSimple(boolean isSimple) {
        if (isInitError()) {
            return;
        }
        try {
            Method method = clazz.getMethod("setSimple", boolean.class);
            method.invoke(object, isSimple);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public boolean isError() {
        if (isInitError()) {
            return true;
        }
        try {
            Method method = clazz.getMethod("isError");
            Object result = method.invoke(object);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return true;
        }
    }

    private boolean isInitError() {
        return clazz == null || object == null;
    }
}
