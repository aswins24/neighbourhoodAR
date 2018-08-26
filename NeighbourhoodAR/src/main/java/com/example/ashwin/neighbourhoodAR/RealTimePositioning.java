package com.example.ashwin.neighbourhoodAR;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by ashwin on 7/20/2018.
 */
public class RealTimePositioning extends View implements SensorEventListener {
    Context c;
    Camera.Parameters Params;
    float accel_time_stamp = 0;
    float mag_time_stamp = 0;
    float alpha;
    LandmarkDetails landmarkDetails;
    Location location = new Location("");
    Location target = new Location("");
    LinearLayout cardLayout;
    ArrayList<LandmarkDetails> landmarks;
    private SensorManager sensor;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private Sensor magSensor;
    private float[] accel = new float[3];
    private float[] compass = new float[3];
    private float[] gravity = new float[3];
    private float[] linear_accel = new float[3];
    private float[] magnetic = new float[3];
    private float[] gravityNew = new float[3];
    private float dx, dy;
    private float verticalFOV;
    private float horizontalFOV;
    private boolean isAccel, isCompass;
    private boolean accelAccuracy = false;
    private boolean magAccuracy = false;
    private boolean flag = true;
    private Toast t1, t2, t3;
    private TextPaint contentPaint;
    private Paint targetPaint;

    public RealTimePositioning(Context context, ArrayList<LandmarkDetails> landmarkDetails, float horizontalFOV, float verticalFOV, TextPaint textPaint, Paint paint) {
        super(context);
        this.c = context;
        this.landmarks = landmarkDetails;
        this.horizontalFOV = horizontalFOV;
        this.verticalFOV = verticalFOV;
        this.contentPaint = textPaint;
        this.targetPaint = paint;
        registerSensors();
        LayoutInflater Inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        cardLayout = (LinearLayout) Inflater.inflate(R.layout.landmarks_in_card, null);
    }

    public void registerSensors() {
        sensor = (SensorManager) c.getSystemService(c.SENSOR_SERVICE);

        if ((accelSensor = sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) != null) {
            Log.d("Sensor availabilty", "Accelerometer is available - " + accelSensor);
            sensor.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isAccel = true;
        } else {
            isAccel = false;
            if (t1 == null) {
                t1 = Toast.makeText(c, " Accelerometer is not available ", Toast.LENGTH_SHORT);
                t1.show();
            }
        }

        if ((gyroSensor = sensor.getDefaultSensor(Sensor.TYPE_GYROSCOPE)) != null) {
            Log.d("Sensor availabilty", "Gyroscope is available - " + gyroSensor);
            sensor.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        if ((magSensor = sensor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) != null) {
            Log.d("Sensor availabilty", "compass is available - " + magSensor);
            sensor.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isCompass = true;
        } else {
            isCompass = false;
            if (t2 == null) {
                t2 = Toast.makeText(c, " compass is not available ", Toast.LENGTH_SHORT);
                t2.show();
            }
        }

        if (!isAccel || !isCompass) {
            if (t3 == null) {
                t3 = Toast.makeText(c, " Doesn't meet the  hardware requirements for running the application ", Toast.LENGTH_SHORT);
                t3.show();
            }
        }
    }

    public void setCurrentLocation(Location location) {
        this.location = location;
    }

    public void unregister() {
        if (sensor != null)
            sensor.unregisterListener(this);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("onAccuracyChanged", "Sensor: " + sensor + ", accuracy: " + accuracy);
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
//        Log.d("onSensorChanged", "SensorEvent: " + event);
//        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && flag) {
//            Log.v("Sensor Changed", "Reached");
//            flag = false;
//            startGPS();
//        }
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                this.accel = event.values.clone();
                if (accel_time_stamp != 0) {
                    alpha = (accel_time_stamp / event.timestamp);

                    gravity[0] = alpha * gravity[0] + (1 - alpha) * accel[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * accel[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * accel[2];

                    linear_accel[0] = accel[0] - gravity[0];
                    linear_accel[1] = accel[1] - gravity[1];
                    linear_accel[2] = accel[2] - gravity[2];
                } else {
                    linear_accel = accel;
                    accel_time_stamp = event.timestamp;
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                compass = event.values.clone();
                if (mag_time_stamp != 0) {
                    alpha = mag_time_stamp / event.timestamp;

                    gravity[0] = alpha * gravityNew[0] + (1 - alpha) * compass[0];
                    gravity[1] = alpha * gravityNew[1] + (1 - alpha) * compass[1];
                    gravity[2] = alpha * gravityNew[2] + (1 - alpha) * compass[2];

                    magnetic[0] = compass[0] - gravityNew[0];
                    magnetic[1] = compass[1] - gravityNew[1];
                    magnetic[2] = compass[2] - gravityNew[2];
                } else {
                    magnetic = compass;
                    mag_time_stamp = event.timestamp;
                }
//              compassData = msg.toString();
                break;
        }
        if (isAccel && isCompass && accelAccuracy && magAccuracy && flag)
            this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < landmarks.size(); i++) {
            flag = false;
            landmarkDetails = landmarks.get(i);

            target.setLatitude(landmarkDetails.get_latitude());
            target.setLongitude(landmarkDetails.get_longitude());

            double bearingTo = location.bearingTo(target);
            float distance = location.distanceTo(target);
            float rotation[] = new float[9];
            float identity[] = new float[9];
            boolean gotRotation = SensorManager.getRotationMatrix(rotation,
                    identity, linear_accel, magnetic);
            float orientation[] = new float[3];
            if (gotRotation) {
                // remap such that the camera is pointing straight down the Y
                // axis
                float cameraRotation[] = new float[9];
                // orientation vector
                SensorManager.remapCoordinateSystem(rotation,
                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
                        cameraRotation);
                SensorManager.getOrientation(cameraRotation, orientation);

                if (Math.abs(Math.toDegrees(orientation[0]) - bearingTo) <= 5 && distance <= 500) {
                    canvas.save();
//            float bearig_difference = (float) (Math.toDegrees(orientation[0]) - bearingTo);
                    this.dx = (float) ((canvas.getWidth() / horizontalFOV) * (Math.toDegrees(orientation[0]) - bearingTo));
                    this.dy = (float) ((canvas.getHeight() / verticalFOV) * (Math.toDegrees(orientation[1])));

                    canvas.translate(0.0f, 0.0f - this.dy);
//                        canvas.drawLine(0f - canvas.getHeight(), canvas.getHeight()/2, canvas.getWidth()+canvas.getHeight(), canvas.getHeight()/2, targetPaint);
                    canvas.translate(0.0f - dx, 0.0f);
                    canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, 8.0f, targetPaint);
                    canvas.drawText(landmarkDetails.get_name(), canvas.getWidth() / 2, canvas.getHeight() / 2, contentPaint);
                    canvas.drawText("" + distance + "m", canvas.getWidth() / 2 + 4, (canvas.getHeight() / 2 + 25), contentPaint);
//                canvas.translate(canvas.getWidth()/2, canvas.getHeight()/2);

                    TextView landmarktext = (TextView) cardLayout.findViewById(R.id.name_text);
                    TextView landmarkDistance = (TextView) cardLayout.findViewById(R.id.dist_text);

                    landmarktext.setText(landmarkDetails.get_name());
                    landmarkDistance.setText("" + distance);

                    cardLayout.draw(canvas);
                    Log.v("Canvas", "Card_Layout is " + cardLayout);

                    canvas.restore();
                }
            }
        }
        flag = true;
    }
}