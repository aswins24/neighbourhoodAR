package com.example.ashwin.neighbourhoodAR;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by ashwin on 7/1/2018.
 */
public class CameraPreview extends SurfaceView implements
        SurfaceHolder.Callback {

    public static final String DEBUG_TAG = "ArDisplayView Log";
    public Camera mCamera;
    SurfaceHolder mHolder;
    Activity mActivity;


    public CameraPreview(Context context, Activity activity) {
        super(context);
        mActivity = activity;
        mHolder = getHolder();
        try {
            mCamera = Camera.open(0);
        } catch (Exception e) {
            Log.d("Camera", "Camera error");
        }
        // This value is supposedly deprecated and set "automatically" when
        // needed.
        // Without this, the application crashes.
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // callbacks implemented by ArDisplayView
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(DEBUG_TAG, "surfaceCreated");
        // Grab the camera
//         Set Display orientation
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);

        int rotation = mActivity.getWindowManager().getDefaultDisplay()
                .getRotation();
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
        mCamera.setDisplayOrientation((info.orientation - degrees + 360) % 360);
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "surfaceCreated exception: ", e);
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d(DEBUG_TAG, "surfaceChanged");
//        Camera.Parameters params = mCamera.getParameters();
        // Find an appropriate preview size that fits the surface
//        List<Camera.Size> prevSizes = params.getSupportedPreviewSizes();
//        for (Camera.Size s : prevSizes) {
//            if ((s.height <= height) && (s.width <= width)) {
//                params.setPreviewSize(s.width, s.height);
//                Log.d(DEBUG_TAG, "setPreviewSize: width"+s.width + ", height" +s.height);
//                break;
//            }
//        }
        // Set the preview format
        //params.setPreviewFormat(ImageFormat.JPEG);
        // Consider adjusting frame rate to appropriate rate for AR
        // Confirm the parameters
        //mCamera.setParameters(params); // fails in latest API 28
        // Begin previewing
        mCamera.startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(DEBUG_TAG, "surfaceDestroyed");
        // Shut down camera preview
        mCamera.stopPreview();
        mCamera.release();
    }
}
