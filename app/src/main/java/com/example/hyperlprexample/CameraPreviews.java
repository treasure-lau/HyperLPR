package com.example.hyperlprexample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.ricent.lib.hyperlpr.RecognizerProxy;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author by hs-johnny
 * Created on 2019/6/17
 */
public class CameraPreviews extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraPreview";
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private byte[] lock = new byte[0];
    private List<String> mResultList = new ArrayList<>();
    private String currentPlate = "";
    private Paint mPaint;
    private float oldDist = 1f;
    /**
     * 停止识别
     */
    private boolean isStopReg;
    private RecognizerProxy recognizer;

    public CameraPreviews(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(ContextCompat.getColor(context, R.color.purple_500));
    }

    public Camera getCameraInstance() {
        if (mCamera == null) {
            try {
                CameraHandlerThread mThread = new CameraHandlerThread("camera thread");
                synchronized (mThread) {
                    mThread.openCamera();
                }
            } catch (Exception e) {
                Log.e(TAG, "camera is not available");
            }
        }
        return mCamera;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = getCameraInstance();
        mCamera.setPreviewCallback(this);
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            setPreviewFocus(mCamera);
            initRecognizer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.e(TAG, "surfaceChanged");
        int rotation = getDisplayOrientation();
        mCamera.setDisplayOrientation(rotation);
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setRotation(rotation);
        mCamera.setParameters(parameters);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfaceDestroyed");
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        getHandler().postDelayed(() -> {
            if (recognizer != null) {
                recognizer.release();
                recognizer = null;
            }
        }, 1000);
    }

    public int getDisplayOrientation() {
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int result = (info.orientation - degrees + 360) % 360;
        return result;
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        if (recognizer == null) {
            return;
        }
        synchronized (lock) {

            //处理data
            Camera.Size previewSize = camera.getParameters().getPreviewSize();
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inJustDecodeBounds = true;
            YuvImage yuvimage = new YuvImage(
                    data,
                    ImageFormat.NV21,
                    previewSize.width,
                    previewSize.height,
                    null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);
            byte[] rawImage = baos.toByteArray();
            //将rawImage转换成bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
            Map<String, Object> result = recognizer.simple(rotateBitmap(bitmap), 8);
            bitmap.recycle();
            if (!isStopReg && result != null) {
                Log.e(TAG, "onPreviewFrame: " + result.get("plateName") + "----time: " + System.currentTimeMillis());
                isStopReg = true;
                sendPlate(result);
            }
        }
    }

    private void sendPlate(Map<String, Object> plate) {
        EventBus.getDefault().post(plate);
    }

    private Bitmap rotateBitmap(Bitmap bmp) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedBitMap = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        return rotatedBitMap;
    }

    private void openCameraOriginal() {
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "camera is not available");
        }
    }

    private class CameraHandlerThread extends HandlerThread {
        Handler handler;

        public CameraHandlerThread(String name) {
            super(name);
            start();
            handler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    openCameraOriginal();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            } catch (Exception e) {
                Log.e(TAG, "wait was interrupted");
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = getFingerSpacing(event);
                    if (newDist > oldDist) {
                        handleZoom(true, mCamera);
                    } else if (newDist < oldDist) {
                        handleZoom(false, mCamera);
                    }
                    oldDist = newDist;
                    break;
            }
        }
        return true;
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        if (parameters.isZoomSupported()) {
            int maxZoom = parameters.getMaxZoom();
            int zoom = parameters.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            parameters.setZoom(zoom);
            camera.setParameters(parameters);
        } else {
            Log.e(TAG, "handleZoom: " + "the device is not support zoom");
        }
    }

    private void setPreviewFocus(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<String> focusList = parameters.getSupportedFocusModes();
        if (focusList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        camera.setParameters(parameters);
    }

    private void copyFilesFromAssets(Context context, String oldPath, String newPath) throws Exception {
        String[] fileNames = context.getAssets().list(oldPath);
        if (fileNames != null && fileNames.length > 0) {
            // directory
            File file = new File(newPath);
            if (!file.mkdir()) {
                Log.d("mkdir", "can't make folder");

            }
//                    return false;                // copy recursively
            for (String fileName : fileNames) {
                copyFilesFromAssets(context, oldPath + "/" + fileName,
                        newPath + "/" + fileName);
            }
        } else {
            // file
            InputStream is = context.getAssets().open(oldPath);
            FileOutputStream fos = new FileOutputStream(new File(newPath));
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
            is.close();
            fos.close();
        }
    }


    private void initRecognizer() {
        try {
            String assetPath = "pr";
            String sdcardPath = Environment.getExternalStorageDirectory()
                    + File.separator + assetPath;
            copyFilesFromAssets(getContext(), assetPath, sdcardPath);
            String cascade_filename = sdcardPath
                    + File.separator + "cascade.xml";
            String finemapping_prototxt = sdcardPath
                    + File.separator + "HorizonalFinemapping.prototxt";
            String finemapping_caffemodel = sdcardPath
                    + File.separator + "HorizonalFinemapping.caffemodel";
            String segmentation_prototxt = sdcardPath
                    + File.separator + "Segmentation.prototxt";
            String segmentation_caffemodel = sdcardPath
                    + File.separator + "Segmentation.caffemodel";
            String character_prototxt = sdcardPath
                    + File.separator + "CharacterRecognization.prototxt";
            String character_caffemodel = sdcardPath
                    + File.separator + "CharacterRecognization.caffemodel";
            String segmentationfree_prototxt = sdcardPath
                    + File.separator + "SegmenationFree-Inception.prototxt";
            String segmentationfree_caffemodel = sdcardPath
                    + File.separator + "SegmenationFree-Inception.caffemodel";
            recognizer = new RecognizerProxy(cascade_filename,
                    finemapping_prototxt, finemapping_caffemodel,
                    segmentation_prototxt, segmentation_caffemodel,
                    character_prototxt, character_caffemodel,
                    segmentationfree_prototxt, segmentationfree_caffemodel);
            recognizer.setSimple(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
