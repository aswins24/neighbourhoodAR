package com.example.ashwin.neighbourhoodAR;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by ashwin on 7/20/2018.
 */
public class RealTimePositioning extends View implements SensorEventListener {
    private static final String TAG = "RealTimePositioning";
    private Context c;
    private LandmarkDetails landmarkDetails;
    private Location location = new Location("");
    private Location target = new Location("");
    private LinearLayout cardLayout;
    private ArrayList<LandmarkDetails> landmarks;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private float rotation[] = new float[9];
    private float identity[] = new float[9];
    private float orientation[] = new float[3];
    private float cameraRotation[] = new float[9];
    private boolean gotRotation;
    private float last_azimuth = 0;
    private float[] accelerometerArray = new float[3];
    private float[] magnetometerArray = new float[3];
    private float[] gravity = new float[3];
    private float[] gravityNew = new float[3];
    private float[] linearAccelerometer = new float[3];
    private float[] magnetic = new float[3];
    private float accelerometerTimeStamp = 0;
    private float magnetometerTimeStamp = 0;
    private float alpha;
    private float dx;
    private float dy;
    private float verticalFOV;
    private float horizontalFOV;
    private boolean hasAccelerometer = false;
    private boolean hasMagnetometer = false;
    private boolean accelAccuracy = false;
    private boolean magAccuracy = false;
    private boolean flag = true;
    private TextPaint textPaint;
    private Paint targetPaint;

    public RealTimePositioning(Context context, ArrayList<LandmarkDetails> landmarkDetails, float horizontalFOV, float verticalFOV, TextPaint textPaint, Paint paint) {
        super(context);
        this.c = context;
        this.landmarks = landmarkDetails;
        this.horizontalFOV = horizontalFOV;
        this.verticalFOV = verticalFOV;
        this.textPaint = textPaint;
        this.targetPaint = paint;
        registerSensors();
//        LayoutInflater Inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        cardLayout = (LinearLayout) Inflater.inflate(R.layout.landmarks_in_card, null);
    }

    public void registerSensors() {
        sensorManager = (SensorManager) c.getSystemService(c.SENSOR_SERVICE);

        if ((accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) != null) {
            Log.d(TAG, "registerSensors: Accelerometer is available - " + accelerometer);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            hasAccelerometer = true;
        } else {
            hasAccelerometer = false;
            Toast.makeText(c, "Accelerometer is not available ", Toast.LENGTH_SHORT).show();
        }

        if ((gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)) != null) {
            Log.d(TAG, "registerSensors: Gyroscope is available - " + gyroscope);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(c, "Gyroscope is not available ", Toast.LENGTH_SHORT).show();
        }

        if ((magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) != null) {
            Log.d(TAG, "registerSensors: Magnetometer is available - " + magnetometer);
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            hasMagnetometer = true;
        } else {
            hasMagnetometer = false;
            Toast.makeText(c, "Compass is not available ", Toast.LENGTH_SHORT).show();
        }

        if (!hasAccelerometer || !hasMagnetometer) {
            Toast.makeText(c, " Doesn't meet the hardware requirements for running the application ", Toast.LENGTH_SHORT).show();
        }
    }

    public void setCurrentLocation(Location location) {
        this.location = location;
    }

    public void unregister() {
        if (sensorManager != null)
            sensorManager.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged: Sensor: " + sensor + ", accuracy: " + accuracy);
        switch (sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                if (accuracy == 2 || accuracy == 3) {
                    accelAccuracy = true;
                }
            case Sensor.TYPE_MAGNETIC_FIELD:
                if (accuracy == 2 || accuracy == 3) {
                    magAccuracy = true;
                }
        }
    }

    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                this.accelerometerArray = event.values.clone();
                if (accelerometerTimeStamp != 0) {
                    alpha = (accelerometerTimeStamp / event.timestamp);

                    gravity[0] = (alpha * gravity[0]) + (1 - alpha) * accelerometerArray[0];
                    gravity[1] = (alpha * gravity[1]) + (1 - alpha) * accelerometerArray[1];
                    gravity[2] = (alpha * gravity[2]) + (1 - alpha) * accelerometerArray[2];

                    accelerometerArray[0] = accelerometerArray[0] - gravity[0];
                    accelerometerArray[1] = accelerometerArray[1] - gravity[1];
                    accelerometerArray[2] = accelerometerArray[2] - gravity[2];

                    linearAccelerometer[0] = (alpha * accelerometerArray[0]) + (1 - alpha) * linearAccelerometer[0];
                    linearAccelerometer[1] = (alpha * accelerometerArray[1]) + (1 - alpha) * linearAccelerometer[1];
                    linearAccelerometer[2] = (alpha * accelerometerArray[2]) + (1 - alpha) * linearAccelerometer[2];

                    accelerometerTimeStamp = event.timestamp;
                } else {
                    gravity[0] = (float) (0.85 * gravity[0]) + (1 - alpha) * accelerometerArray[0];
                    gravity[1] = (float) (0.85 * gravity[1]) + (1 - alpha) * accelerometerArray[1];
                    gravity[2] = (float) (0.85 * gravity[2]) + (1 - alpha) * accelerometerArray[2];

                    linearAccelerometer[0] = accelerometerArray[0] - gravity[0];
                    linearAccelerometer[1] = accelerometerArray[1] - gravity[1];
                    linearAccelerometer[2] = accelerometerArray[2] - gravity[2];

                    accelerometerTimeStamp = event.timestamp;
                }
                break;

//            case Sensor.TYPE_GYROSCOPE:


            case Sensor.TYPE_MAGNETIC_FIELD:
                magnetometerArray = event.values.clone();
                if (magnetometerTimeStamp != 0) {
                    alpha = magnetometerTimeStamp / event.timestamp;

                    magnetic[0] = (alpha * magnetometerArray[0]) + (1 - alpha) * magnetic[0];
                    magnetic[1] = (alpha * magnetometerArray[1]) + (1 - alpha) * magnetic[1];
                    magnetic[2] = (alpha * magnetometerArray[2]) + (1 - alpha) * magnetic[2];


                    magnetometerTimeStamp = event.timestamp;
                } else {
                    magnetic = magnetometerArray;
                    magnetometerTimeStamp = event.timestamp;
                }

                break;
        }


//            if (last_azimuth == 0 || Math.abs(last_azimuth - orientation[0]) > 3) {
//                last_azimuth = orientation[0];
        if (hasAccelerometer && hasMagnetometer && accelAccuracy && magAccuracy) {
            this.invalidate();
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < landmarks.size(); i++) {
            flag = false;
            landmarkDetails = landmarks.get(i);

            target.setLatitude(landmarkDetails.getLatitude());
            target.setLongitude(landmarkDetails.getLongitude());
//            Log.v("My Location", "Current location is "+location);
            double bearingTo = location.bearingTo(target);
            float distance = location.distanceTo(target);
            gotRotation = SensorManager.getRotationMatrix(rotation,
                    identity, linearAccelerometer, magnetic);


            if (gotRotation) {
                // remap such that the camera is pointing straight down the Y axis
                SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, cameraRotation);
                // orientation vector
                SensorManager.getOrientation(cameraRotation, orientation);

                if (Math.abs(Math.toDegrees(orientation[0]) - bearingTo) <= 3 && distance <= 250) {
                    canvas.save();
                    this.dx = (float) ((canvas.getWidth() / horizontalFOV) * (Math.toDegrees(orientation[0]) - bearingTo));
                    this.dy = (float) ((canvas.getHeight() / verticalFOV) * (Math.toDegrees(orientation[1])));

                    canvas.translate(0.0f, 0.0f - this.dy);
                    //canvas.drawLine(0f - canvas.getHeight(), canvas.getHeight()/2, canvas.getWidth()+canvas.getHeight(), canvas.getHeight()/2, targetPaint);
                    canvas.translate(0.0f - dx, 0.0f);
                    canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 8.0f, targetPaint);
                    canvas.drawText(landmarkDetails.getName(), canvas.getWidth() / 2, canvas.getHeight() / 2, textPaint);
                    canvas.drawText("" + distance + "m", canvas.getWidth() / 2 + 4, (canvas.getHeight() / 2 + 25), textPaint);
                    //canvas.translate(canvas.getWidth()/2, canvas.getHeight()/2);

//                    TextView landmarktext = (TextView) cardLayout.findViewById(R.id.name_text);
//                    TextView landmarkDistance = (TextView) cardLayout.findViewById(R.id.dist_text);
//
//                    landmarktext.setText(landmarkDetails.getName());
//                    landmarkDistance.setText("" + distance);
//
//                    cardLayout.draw(canvas);
                    canvas.restore();
                }
            }
        }
        flag = true;
    }
}