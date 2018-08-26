package com.example.ashwin.neighbourhoodAR;

import android.content.Context;

/**
 * Created by ashwin on 7/14/2018.
 */
public class LandmarkDetails {
    Context c;
    double latitude;
    double longitude;
    String name;

    public LandmarkDetails(Context context) {

        c = context;
    }

    public void put_latitude(double lat) {
        this.latitude = lat;
    }

    public void put_longitude(double lng) {
        this.longitude = lng;
    }

    public void put_name(String name) {
        this.name = name;
    }

    public double get_latitude() {
        return this.latitude;
    }

    public double get_longitude() {
        return this.longitude;
    }

    public String get_name() {
        return this.name;
    }
}
