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
    public Response response = null;
    Context c;
    Location locations;
    double latitude;
    double longitude;
    String responseJSON;
    String type;
    ArrayList<LandmarkDetails> landmark_details = new ArrayList<>();

    public NearbyLandmarks(Context context, Location location, String type, Response response) {
        this.c = context;
        locations = location;
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
            Log.v("Json stat", "" + jsonObject);
            String status = jsonObject.getString("status");

            if (status.equals("OK")) {
                Toast.makeText(c, "Nearby landmarks found", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(c, "Nearby landmarks not found", Toast.LENGTH_LONG).show();
            }
            Log.v("Parsing", "Time to parse");
            Toast.makeText(c, "Extracting Data", Toast.LENGTH_SHORT).show();
            parseJSON(jsonObject);
            response.processComplete(landmark_details);


        } catch (final JSONException e) {
            Log.e("Json error", "Json parsing error: " + e.getMessage());
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
//                Log.v("Connection", "Successful");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()), 128);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                bufferedReader.close();
            } else {
                Log.v("Connection", "Error in URL");
                Toast.makeText(c, "Failed to connect. Please Check Internet connection", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("ConnectionError", "Failed to connect: " + e.getMessage(), e);
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
                    landmarks.put_latitude(latitude);
                    landmarks.put_longitude(longitude);
                    landmarks.put_name(name);

                    landmark_details.add(landmarks);

                }
//                Toast.makeText(c,"Size of landmark list is "+landmark_details.size(),Toast.LENGTH_SHORT).show();
            }
        } catch (final JSONException e) {
            Log.e("Json error", "parseJSON error: " + e.getMessage());
        }

    }

    public interface Response {
        void processComplete(ArrayList<LandmarkDetails> LDetails);
    }
}

