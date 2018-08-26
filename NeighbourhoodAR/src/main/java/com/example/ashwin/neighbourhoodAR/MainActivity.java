package com.example.ashwin.neighbourhoodAR;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        LocationListener {

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    protected static final int MY_PERMISSIONS_ACCESS_CAMERA = 101;
    protected static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 99;
    private static Status status;
    FrameLayout camera_view;
    RealTimePositioning positioning = null;
    Criteria criteria = new Criteria();
    ArrayList<LandmarkDetails> landmarkDetails = new ArrayList<LandmarkDetails>();
    private LocationManager locationManager = null;
    private Location lastLocation = null;
    private Location currentLocation;
    private float verticalFOV;
    private float horizontalFOV;
    private Paint targetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint contentPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private CameraPreview camera;
    private boolean flag = true;
    private double currnetDistance = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        contentPaint.setTextAlign(Paint.Align.LEFT);
        contentPaint.setTextSize(30);
        contentPaint.setColor(Color.RED);
        targetPaint.setColor(Color.GREEN);
        checkInternetConnection();
    }

    private boolean checkInternetConnection() {
        ConnectivityManager check = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] info = check.getAllNetworkInfo();

        boolean connectionFlag = false;
        for (int i = 0; i < info.length; i++) {
            if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                Toast.makeText(this, "Internet is connected", Toast.LENGTH_SHORT).show();
                connectionFlag = true;
                break;
            } else if (i == info.length - 1) {
                Toast.makeText(this, "Please check internet connection", Toast.LENGTH_SHORT).show();
                connectionFlag = false;
            }
        }
        return connectionFlag;
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_ACCESS_CAMERA);
        }
    }

    private void cameraInstance() {
        if (camera == null) {
            try {
                camera = new CameraPreview(getApplicationContext(), this);
                camera_view = (FrameLayout) findViewById(R.id.camera_view);
                camera_view.addView(camera);
            } catch (Exception e) {
                Log.v("MainActivity", "Camera error");
            }
        } else {
            camera_view = (FrameLayout) findViewById(R.id.camera_view);
            camera_view.addView(camera);
        }
    }

    private void releaseCameraInstance() {
        if (camera != null) {
            camera_view = (FrameLayout) findViewById(R.id.camera_view);
            camera_view.removeView(camera);
            camera = null;
        }
    }

    private void startGPS() {
        // criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // while we want fine accuracy, it's unlikely to work indoors where we
        // do our testing. :)
        Log.d("GPS", "Its called");
        criteria.setAccuracy(Criteria.NO_REQUIREMENT);
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
//        String best = locationManager.getBestProvider(criteria, true);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            String bestProvider = locationManager.getBestProvider(criteria, true);
            Log.d("GPS", "Best provider: " + bestProvider);
            locationManager.requestLocationUpdates(bestProvider, 50, 0, this);
        }
        Toast.makeText(this, "Fetching GPS", Toast.LENGTH_SHORT).show();
    }

    public void onCheckboxClicked(View view) {
        CheckBox checkBox = (CheckBox) view;
        boolean checked = checkBox.isChecked();

        CheckBox restaurant = (CheckBox) findViewById(R.id.Restaurant);
        CheckBox hospital = (CheckBox) findViewById(R.id.Hospital);
        CheckBox bakery = (CheckBox) findViewById(R.id.Bakery);
        CheckBox carRepair = (CheckBox) findViewById(R.id.CarRepair);

        String type = null;

        if (lastLocation != null) {
            switch (view.getId()) {
                case R.id.Restaurant:
                    toggleIfChecked(checked, hospital, bakery, carRepair);
                    type = "restaurant";
                    break;
                case R.id.Hospital:
                    toggleIfChecked(checked, restaurant, bakery, carRepair);
                    type = "hospital";
                    break;
                case R.id.Bakery:
                    toggleIfChecked(checked, restaurant, hospital, carRepair);
                    type = "bakery";
                    break;
                case R.id.CarRepair:
                    toggleIfChecked(checked, restaurant, hospital, bakery);
                    type = "car_repair";
                    break;
            }
        } else {
            checkBox.toggle();
            type = null;
            Toast.makeText(this, "GPS is not reliable", Toast.LENGTH_SHORT).show();
        }
        if (type != null) {
            executeNearbyLandMarks(type);
        }

        if (!checkBox.isChecked() && lastLocation != null) {
            executeNearbyLandMarks(null);
        }
    }

    private void executeNearbyLandMarks(String type) {
        NearbyLandmarks nearbyLandmarks = new NearbyLandmarks(this, lastLocation, type, new NearbyLandmarks.Response() {
            @Override
            public void processComplete(ArrayList<LandmarkDetails> LDetails) {
                landmarkDetails = LDetails;
                if (positioning != null) {
                    camera_view.removeView(positioning);
                }
                positioning = new RealTimePositioning(getApplicationContext(), landmarkDetails, horizontalFOV, verticalFOV, contentPaint, targetPaint);
                camera_view.addView(positioning);
            }
        });
        nearbyLandmarks.execute(lastLocation);
    }

    private void toggleIfChecked(boolean checked, CheckBox hospital, CheckBox bakery, CheckBox carRepair) {
        if (checked) {
            if (hospital.isChecked())
                hospital.toggle();
            if (bakery.isChecked())
                bakery.toggle();
            if (carRepair.isChecked())
                carRepair.toggle();
        }
    }

    private boolean getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE_LOCATION);
        }
        return flag;
    }

    private PendingResult<LocationSettingsResult> displayLocationSettingsRequest(final Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i("Location Settings", "All location settings are satisfied.");
                        startGPS();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i("Location Settings", "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i("Location Settings", "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i("Location Settings", "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
        return result;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    //Request location updates:
//                        cameraInstance();
//                        displayLocationSettingsRequest(this);
//                        startGPS();
                } else {
//                    cameraInstance();
                    Toast.makeText(this, "This app requires GPS", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the functionality that depends on this permission.
                }
                break;
            }
            case MY_PERMISSIONS_ACCESS_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    cameraInstance();
                    getLocationPermission();
                } else {
                    Toast.makeText(this, "This application requires Camera", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS && resultCode == RESULT_OK) {
            startGPS();
        } else if (requestCode == REQUEST_CHECK_SETTINGS && resultCode == RESULT_CANCELED) {
            flag = false;
            Toast.makeText(this, "This application requires GPS ", Toast.LENGTH_SHORT).show();
        }
    }

    public void onLocationChanged(Location location) {
        // store it off for use when we need it
        currentLocation = location;
        if (lastLocation != null) {
            currnetDistance = location.distanceTo(lastLocation);
        }
        if (positioning != null) {
            positioning.setCurrentLocation(currentLocation);
        }
        if (lastLocation == null || currnetDistance > 250.00 && checkInternetConnection()) {
            Log.d("Last location", "Its getting updated");
            lastLocation = location;
            verticalFOV = camera.mCamera.getParameters().getVerticalViewAngle();
            horizontalFOV = camera.mCamera.getParameters().getHorizontalViewAngle();
            NearbyLandmarks nearbyLandmarks = new NearbyLandmarks(this, lastLocation, null, new NearbyLandmarks.Response() {
                @Override
                public void processComplete(ArrayList<LandmarkDetails> LDetails) {
                    landmarkDetails = LDetails;
                    if (positioning != null)
                        camera_view.removeView(positioning);
                    positioning = new RealTimePositioning(getApplicationContext(), landmarkDetails, horizontalFOV, verticalFOV, contentPaint, targetPaint);
                    camera_view.addView(positioning);
                }
            });
            nearbyLandmarks.execute(lastLocation);
        }
    }

    public void onProviderDisabled(String provider) {
        displayLocationSettingsRequest(this);
    }

    public void onProviderEnabled(String provider) {
        Log.v("Provider Enabled", "Its something else");
        startGPS();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case 1:
                Toast.makeText(this, "Not reliable GPS connection. Please move around for better GPS connection", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                startGPS();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (positioning != null)
            positioning.unregister();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        releaseCameraInstance();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraInstance();
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && flag) {
            displayLocationSettingsRequest(this);
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.v("Location manager", "Yup");
            flag = true;
        }

        startGPS();
        if (positioning != null)
            positioning.registerSensors();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            checkCameraPermission();
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            flag = false;
            getLocationPermission();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}