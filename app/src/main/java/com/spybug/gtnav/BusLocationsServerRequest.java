package com.spybug.gtnav;

import android.content.Context;
import android.os.AsyncTask;

import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


import static com.spybug.gtnav.HelperUtil.haveNetworkConnection;


/**
 * Background task to communicate with the map server
 */

public class BusLocationsServerRequest extends AsyncTask<Object, Void, List<LatLng>> {

    private static final String REQUEST_METHOD = "GET";
    private static final int READ_TIMEOUT = 15000;
    private static final int CONNECTION_TIMEOUT = 15000;
    private WeakReference<Context> contextRef;
    private boolean hasNetwork = true;
    private int errorCode = 0;
    private OnEventListener<List<LatLng>, String> mCallBack;

    BusLocationsServerRequest(Context context, OnEventListener<List<LatLng>, String> callback) {
        contextRef = new WeakReference<>(context);
        mCallBack = callback;
    }

    protected void onPreExecute() {
        hasNetwork = haveNetworkConnection(contextRef.get());
    }

    @Override
    protected List<LatLng> doInBackground(Object[] objects) {
        List<LatLng> pointsList = new ArrayList<>();
        String routeTag = (String)objects[0];

        if (!hasNetwork) {
            errorCode = 1;
            return pointsList;
        }

        String inputLine;
        String stringUrl;
        String result;

        stringUrl = String.format("%sbuses?route=%s",
                BuildConfig.API_URL,
                routeTag);

        try {
            //Create a URL object holding our url
            URL myUrl = new URL(stringUrl);
            //Create a connection
            HttpURLConnection connection =(HttpURLConnection)
                    myUrl.openConnection();
            //Set methods and timeouts
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);

            //Connect to our url
            connection.connect();
            //Create a new InputStreamReader
            InputStreamReader streamReader = new
                    InputStreamReader(connection.getInputStream());
            //Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            //Check if the line we are reading is not null
            while((inputLine = reader.readLine()) != null){
                stringBuilder.append(inputLine);
            }
            //Close our InputStream and Buffered reader
            reader.close();
            streamReader.close();
            //Set our result equal to our stringBuilder
            result = stringBuilder.toString();
        }
        catch(IOException e){
            e.printStackTrace();
            result = null;
        }

        if (result != null) {
            try {
                JSONArray vehicles = new JSONArray(result);

                for (int i = 0; i < vehicles.length(); i++) {
                    JSONArray vehicle = vehicles.getJSONArray(i);
                    pointsList.add(new LatLng(vehicle.getDouble(2),
                            vehicle.getDouble(3)));

                }
            }
            catch(JSONException ex) {
                ex.printStackTrace();
            }
        }

        return pointsList;
    }

    protected void onPostExecute(List<LatLng> result) {
        if (mCallBack != null) {
            if (errorCode != 0) {
                if (errorCode == 1) {
                    String info = "You are not connected to the internet. Please try again later.";
                    mCallBack.onFailure(info);
                }
            }
            else {
                mCallBack.onSuccess(result);
            }
        }
    }
}
