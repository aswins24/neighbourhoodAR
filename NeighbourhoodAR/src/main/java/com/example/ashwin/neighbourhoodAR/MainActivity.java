package com.example.ashwin.neighbourhoodAR;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
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

    public static final String TAG = "MainActivity";
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    protected static final int MY_PERMISSIONS_ACCESS_CAMERA = 101;
    protected static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 99;
    private static Status status;
    public LocationManager locationManager = null;
    FrameLayout cameraView;
    RealTimePositioning positioning = null;
    Criteria criteria = new Criteria();
    ArrayList<LandmarkDetails> landmarkDetails = new ArrayList<LandmarkDetails>();
    NearbyLandmarks nearbyLandmarks;
    private LandmarkDetails landmarks;
    private String type = null;
    private String next_page = null;
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
            Log.v("Camera Instance", "Calling camera object");
            try {
                camera = new CameraPreview(getApplicationContext(), this);
                cameraView = (FrameLayout) findViewById(R.id.camera_view);
                cameraView.addView(camera);
                Camera myCamera = camera.getMyCamera();
                if (myCamera != null) {
                    verticalFOV = myCamera.getParameters().getVerticalViewAngle();
                    horizontalFOV = myCamera.getParameters().getHorizontalViewAngle();

                }
            } catch (Exception e) {
                Log.v(TAG, "cameraInstance: Camera error");
                throw e;
            }
        } else {
            cameraView = (FrameLayout) findViewById(R.id.camera_view);
            cameraView.addView(camera);
        }
    }

    private void releaseCameraInstance() {
        if (camera != null) {
            cameraView = (FrameLayout) findViewById(R.id.camera_view);
            cameraView.removeView(camera);
            camera = null;
        }
    }

    private void startGPS() {
        // criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // while we want fine accuracy, it's unlikely to work indoors where we
        // do our testing. :)
        Log.d(TAG, "startGPS: It is called");
        criteria.setAccuracy(Criteria.NO_REQUIREMENT);
        criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
//        String best = locationManager.getBestProvider(criteria, true);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            String bestProvider = locationManager.getBestProvider(criteria, true);
            Log.d(TAG, "startGPS: Best provider: " + bestProvider);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 50, 0, this);
            if (bestProvider.equals("gps")) {
                Toast.makeText(this, "Fetching GPS", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(this, "Please check GPS", Toast.LENGTH_SHORT).show();
        }

    }

    public void onCheckboxClicked(View view) {
        CheckBox checkBox = (CheckBox) view;
        boolean checked = checkBox.isChecked();
        next_page = null;
        CheckBox restaurant = (CheckBox) findViewById(R.id.Restaurant);
        CheckBox hospital = (CheckBox) findViewById(R.id.Hospital);
        CheckBox bakery = (CheckBox) findViewById(R.id.Bakery);
        CheckBox carRepair = (CheckBox) findViewById(R.id.CarRepair);


        if (lastLocation != null) {
            switch (view.getId()) {
                case R.id.Restaurant:
                    type = "restaurant";
                    if (hospital.isChecked())
                        hospital.toggle();
                    if (bakery.isChecked())
                        bakery.toggle();
                    if (carRepair.isChecked())
                        carRepair.toggle();
                    break;
                case R.id.Hospital:
                    type = "hospital";
                    if (restaurant.isChecked())
                        restaurant.toggle();
                    if (bakery.isChecked())
                        bakery.toggle();
                    if (carRepair.isChecked())
                        carRepair.toggle();
                    break;
                case R.id.Bakery:
                    type = "bakery";
                    if (hospital.isChecked())
                        hospital.toggle();
                    if (restaurant.isChecked())
                        restaurant.toggle();
                    if (carRepair.isChecked())
                        carRepair.toggle();
                    break;
                case R.id.CarRepair:
                    type = "car_repair";
                    if (hospital.isChecked())
                        hospital.toggle();
                    if (bakery.isChecked())
                        bakery.toggle();
                    if (restaurant.isChecked())
                        restaurant.toggle();
                    break;
            }

            if (!checkBox.isChecked()) {
                type = null;
            }

            executeNearbyLandMarks(type);
        } else {
            checkBox.toggle();
            type = null;
            Toast.makeText(this, "GPS is not reliable", Toast.LENGTH_SHORT).show();
        }



    }

    private void executeNearbyLandMarks(final String type) {
        // A request can give upto 60 results, but they are divided into 3 pages with 20 results in each page.
        // In order to retrieve all the results, we have to use page_token from the JSON response(if present) and
        // request again using the page token. Hence it takes 3 requests (maximum) to retrieve all results.
        nearbyLandmarks = new NearbyLandmarks(this, type, null, new NearbyLandmarks.Response() {
            // Requesting first page
            @Override
            public void processComplete(ArrayList<LandmarkDetails> LDetails, String Page_Token) {
                landmarkDetails = LDetails;
                if (LDetails.isEmpty() || LDetails.size() == 0)
                    lastLocation = null;
                next_page = Page_Token; // If there are more than 20 results "Page_Token" will have a value, else "null".

                if (next_page != null) {
                    for (int i = 0; i < 25000; i++) {
                        for (int j = 0; j < 25000; j++) {
                            // There is a small delay (2 sec) before page_token will be valid, requesting next page without a delay will
                            // cause an INVALID RESPONSE.
                        }
                    }
                    nearbyLandmarks = new NearbyLandmarks(getApplicationContext(), type, next_page, new NearbyLandmarks.Response() {
                        // Requesting second page (if page_token isn't "null").
                        @Override
                        public void processComplete(ArrayList<LandmarkDetails> LDetails, String Page_Token) {
                            next_page = null;
                            for (int i = 0; i < LDetails.size(); i++) {
                                landmarks = LDetails.get(i);
                                landmarkDetails.add(landmarks);
                            }
                            next_page = Page_Token;
                            if (next_page != null) {
                                for (int i = 0; i < 25000; i++) {
                                    for (int j = 0; j < 25000; j++) {
                                        // There is a small delay (2 sec) before page_token will be valid, requesting next page without a delay will
                                        // cause an INVALID RESPONSE.
                                    }
                                }
                                nearbyLandmarks = new NearbyLandmarks(getApplicationContext(), type, next_page, new NearbyLandmarks.Response() {
                                    //Requesting 3rd page (if page token isn't "null")
                                    @Override
                                    public void processComplete(ArrayList<LandmarkDetails> LDetails, String Page_Token) {
                                        for (int i = 0; i < LDetails.size(); i++) {
                                            landmarks = LDetails.get(i);
                                            landmarkDetails.add(landmarks);
                                        }
                                        if (positioning != null)
                                            cameraView.removeView(positioning);
                                        positioning = new RealTimePositioning(getApplicationContext(), landmarkDetails, horizontalFOV, verticalFOV, contentPaint, targetPaint);
                                        cameraView.addView(positioning);
                                        Toast.makeText(getApplicationContext(), "Landmark size is " + landmarkDetails.size(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                nearbyLandmarks.execute(lastLocation);
                            } else {
                                if (positioning != null)
                                    cameraView.removeView(positioning);
                                positioning = new RealTimePositioning(getApplicationContext(), landmarkDetails, horizontalFOV, verticalFOV, contentPaint, targetPaint);
                                cameraView.addView(positioning);
                                Toast.makeText(getApplicationContext(), "Landmark size is " + landmarkDetails.size(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    nearbyLandmarks.execute(lastLocation);

                } else {
                    if (positioning != null) {
                        cameraView.removeView(positioning);
                    }
                    positioning = new RealTimePositioning(getApplicationContext(), landmarkDetails, horizontalFOV, verticalFOV, contentPaint, targetPaint);
                    cameraView.addView(positioning);
                    Toast.makeText(getApplicationContext(), "Landmark size is " + landmarkDetails.size(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        nearbyLandmarks.execute(lastLocation);
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
                        Log.i(TAG, "displayLocationSettingsRequest: All location settings are satisfied.");
                        startGPS();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "displayLocationSettingsRequest: Location settings are not satisfied. Show the user a dialog to upgrade location settings ");
                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "displayLocationSettingsRequest: PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "displayLocationSettingsRequest: Location settings are inadequate, and cannot be fixed here. Dialog not created.");
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
                    Toast.makeText(this, " This app requires GPS", Toast.LENGTH_SHORT).show();
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
            currnetDistance = currentLocation.distanceTo(lastLocation);
        }

        if (lastLocation == null || currnetDistance > 250.00 && checkInternetConnection()) {
            Log.d(TAG, "onLocationChanged: Its getting updated");
            lastLocation = location;


            executeNearbyLandMarks(type);

        }
        if (positioning != null) {
            positioning.setCurrentLocation(currentLocation);

        }
    }

    public void onProviderDisabled(String provider) {
        displayLocationSettingsRequest(this);
    }

    public void onProviderEnabled(String provider) {
        Log.v(TAG, "onProviderEnabled: It is something else: " + provider);
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

    }

    @Override
    protected void onResume() {
        super.onResume();
        releaseCameraInstance();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraInstance();
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && flag) {
//            displayLocationSettingsRequest(this);
            flag = false;
            startGPS();
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.v(TAG, "onResume: Yup");
            flag = true;
        }


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
            getLocationPermission();
        }
//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && flag) {
//            displayLocationSettingsRequest(this);
//        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCameraInstance();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            locationManager.removeUpdates(this);
        positioning = null;

    }
}