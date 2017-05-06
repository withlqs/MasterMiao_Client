package master.miao.locationlogger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.tls.OkHostnameVerifier;

/**
 * Created by lqs on 5/6/17.
 */

public class LocationLooper {
    private Activity activity = null;
    private boolean started = false;
    private LocationManager locationManager = null;
    ExecutorService fixedThreadPool;

    private synchronized boolean isStarted() {
        return started;
    }

    private synchronized void setStarted(boolean started) {
        this.started = started;
    }

    public LocationLooper(Activity activity) {
        this.activity = activity;

        this.locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        this.fixedThreadPool = Executors.newFixedThreadPool(3);
    }

    public synchronized boolean start() {
        if (isStarted()) {
            return false;
        }
        setStarted(true);
        List<String> providers = locationManager.getProviders(true);
        String provider = null;
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
            return false;
        }
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MasterMiao", "Need permissions");
            return false;
        }
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            sendToServer(location);
        }
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                sendToServer(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("MasterMiao", "Need permissions");
                    return;
                }
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null) {
                    sendToServer(location);
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d("MasterMiao", "Provider disabled.");
            }
        };
        locationManager.requestLocationUpdates(provider, 1000, 1, listener);

        return true;
    }

    private void sendToServer(final Location location) {
        fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "http://"+Constants.serverAddress+"/record?latitude="+String.format("%.8f", location.getLatitude())+"&longitude="+String.format("%.8f", location.getLongitude());
                    Log.d("MasterMiao", "Request url: "+url);
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(url).build();
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        Log.d("MasterMiao", "Request body: "+response.body().string());
                    } else {
                        Log.d("MasterMiao", "Request failed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
