package com.example.ashwin.neighbourhoodAR;

import android.content.Context;

/**
 * Created by ashwin on 7/14/2018.
 */
public class LandmarkDetails {
    private Context context;
    private double latitude;
    private double longitude;
    private String name;

    public LandmarkDetails(Context context) {
        this.context = context;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
