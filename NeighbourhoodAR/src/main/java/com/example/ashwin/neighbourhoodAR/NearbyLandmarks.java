package com.example.ashwin.neighbourhoodAR;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by ashwin on 7/3/2018.
 */
public class NearbyLandmarks extends AsyncTask<Location, Void, ArrayList<LandmarkDetails>> {
    private static final String TAG = "NearbyLandmarks";
    public Response response = null;
    private Context c;
    private double latitude;
    private double longitude;
    private String responseJSON;
    private String type;
    private ArrayList<LandmarkDetails> landmarkDetails = new ArrayList<>();

    public NearbyLandmarks(Context context, String type, Response response) {
        this.c = context;
        this.response = response;
        this.type = type;
    }

    @Override
    protected ArrayList<LandmarkDetails> doInBackground(Location... locations) {
        if (locations.length != 0) {
            StringBuilder stringBuilder = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?&location=");

            latitude = locations[0].getLatitude();
            stringBuilder.append(Double.toString(latitude));
            stringBuilder.append(",");

            longitude = locations[0].getLongitude();
            stringBuilder.append(Double.toString(longitude));

            stringBuilder.append("&radius=500");
            if (type != null) {
                stringBuilder.append("&type=");
                stringBuilder.append(type);
            }
            stringBuilder.append("&key=" + "AIzaSyDqQkopSvrDR5yciMZan7Rl7guooG5vpf8");
            String requestURL = stringBuilder.toString();
            responseJSON = executeAndGetResponseAsString(requestURL);
        }
        return null;
    }

    @Override
    protected void onPostExecute(ArrayList<LandmarkDetails> landmarks) {
        super.onPostExecute(landmarks);
        try {
            JSONObject jsonObject = new JSONObject(responseJSON);
            Log.v(TAG, "onPostExecute: JSON stat: " + jsonObject);
            String status = jsonObject.getString("status");

            if (status.equals("OK")) {
                Toast.makeText(c, "Nearby landmarks found", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(c, "Nearby landmarks not found", Toast.LENGTH_LONG).show();
            }

            Log.v(TAG, "onPostExecute: Time to parse");
            Toast.makeText(c, "Extracting Data", Toast.LENGTH_SHORT).show();
            parseJSON(jsonObject);
            response.processComplete(landmarkDetails);


        } catch (final JSONException e) {
            Log.e(TAG, "onPostExecute: Error: " + e.getMessage());
        }
    }

    private String executeAndGetResponseAsString(String uri) {
        StringBuilder stringBuilder = new StringBuilder();
        int responseCode = -1;

        try {
            URL url = new URL(uri);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            responseCode = httpURLConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.v(TAG, "executeAndGetResponseAsString: Connection successful");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()), 128);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                bufferedReader.close();
            } else {
                Log.v(TAG, "executeAndGetResponseAsString: Error in URL: " + url.toString());
                Toast.makeText(c, "Failed to connect. Please Check Internet connection", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "executeAndGetResponseAsString: Failed to connect: " + e.getMessage(), e);
        }
        return stringBuilder.toString();
    }

    private void parseJSON(JSONObject jsonobj) {
        try {
            JSONArray results = jsonobj.getJSONArray("results");
            if (results.length() == 0) {
                Toast.makeText(c, "No results found", Toast.LENGTH_SHORT).show();
            } else {
                int len = results.length();
                for (int i = 0; i < len; i++) {
                    JSONObject geometry = results.getJSONObject(i).getJSONObject("geometry");
                    JSONObject location = geometry.getJSONObject("location");
                    double latitude = location.getDouble("lat");
                    double longitude = location.getDouble("lng");
                    String name = results.getJSONObject(i).getString("name");

                    LandmarkDetails landmarks = new LandmarkDetails(c);
                    landmarks.setLatitude(latitude);
                    landmarks.setLongitude(longitude);
                    landmarks.setName(name);

                    landmarkDetails.add(landmarks);

                }
                Toast.makeText(c, "Landmarks found: " + landmarkDetails.size(), Toast.LENGTH_SHORT).show();
            }
        } catch (final JSONException e) {
            Log.e(TAG, "parseJSON: Error: " + e.getMessage());
        }

    }

    // Sample JSON format
    //    {
    //        "html_attributions":[
    //
    //        ],
    //        "results":[
    //        {
    //            "geometry":{
    //            "location":{
    //                "lat":10.1482017,
    //                 "lng":76.24500549999999
    //            },
    //            "viewport":{
    //                "northeast":{
    //                    "lat":10.1495506802915,
    //                    "lng":76.24635448029149
    //                },
    //                "southwest":{
    //                    "lat":10.1468527197085,
    //                    "lng":76.24365651970848
    //                }
    //            }
    //        },
    //            "icon":"https:\/\/maps.gstatic.com\/mapfiles\/place_api\/icons\/restaurant-71.png",
    //                "id":"dbabe3c3a3cbec6f0c590286c02f4522123b7bf9",
    //                "name":"ന്യൂ സഫയർ ബക്കാല",
    //                "photos":[
    //            {
    //                "height":2340,
    //                    "html_attributions":[
    //                "<a href=\"https:\/\/maps.google.com\/maps\/contrib\/107325718459425827250\/photos\">Vishnu Santhosh<\/a>"
    //                ],
    //                "photo_reference":"CmRaAAAA2oGeYSSwo4Z79sXV-EpbNuqNx0520MDsZlO2tiAvKPl_5MR6mizC98cYkioCUPRfI9Bz6XFaWrPwVgmwetXD3pey_9gyow4sPuCooYhi0YqSFNCPR_l_gc_qjfmXTo7lEhBcdJPJdN4BUvbGQLl1Yq2oGhR0Hu3KFOBEwjGReWBkNTrcvmIpCA",
    //                    "width":4160
    //            }
    //            ],
    //            "place_id":"ChIJpQDN5IAbCDsR2QcpuuiAAaM",
    //                "plus_code":{
    //                    "compound_code":"46XW+72 North Paravur, Kerala, India",
    //                    "global_code":"7J2R46XW+72"
    //        },
    //            "rating":5,
    //                "reference":"CmRSAAAAIZ0rBNL2HajVn-I_V_mmyxzJ3S_IBUlfMq5HSKBjYpBFLjiP8TeEXhqKgQD2VYKyXWkNy-SQbk1Jp66PqDWLD8PnEw3Uv62l7PBM2g6hSEV3LxbHl5ivk5_-GX-fZccVEhC0lR4Gmft1RDsr9JHzIkd8GhSCWOqlzWtQLuqkeSm4Oz2q9Fe0PA",
    //                "scope":"GOOGLE",
    //                "types":[
    //                    "bakery",
    //                    "store",
    //                    "food",
    //                    "point_of_interest",
    //                    "establishment"
    //            ],
    //            "vicinity":"North Paravur"
    //        }
    //        ],
    //        "status":"OK"
    //    }

    public interface Response {
        void processComplete(ArrayList<LandmarkDetails> LDetails);
    }
}

