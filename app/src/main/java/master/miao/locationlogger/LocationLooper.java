package master.miao.locationlogger;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by lqs on 5/6/17.
 */

public class LocationLooper {
    private ExecutorService fixedThreadPool;
    private MainActivity activity = null;
    private boolean started = false;
    private LocationManager locationManager = null;

    public LocationLooper(MainActivity mainActivity) {
        this.activity = mainActivity;

        this.locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        this.fixedThreadPool = Executors.newFixedThreadPool(3);
    }

    private synchronized boolean isStarted() {
        return started;
    }

    private synchronized void setStarted(boolean started) {
        this.started = started;
    }

    public synchronized boolean start() {
        if (isStarted()) {
            return false;
        }
        setStarted(true);
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
        } else {
            Log.d("MasterMiao", "Provider has no member");
            return false;
        }
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MasterMiao", "Need permissions");
            return false;
        }
        fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                Location location;
                while (true) {
                    try {
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location == null) {
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }
                        sendToServer(location);
                    } catch (SecurityException e) {
                        Log.d("MasterMiao", "circle exception");
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500, 1, listener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500, 1, listener);

        return true;
    }

    private void sendToServer(final Location location) {
        if (location == null) {
            Log.d("MasterMiao", "location is null");
            return;
        }
        activity.setLocation(location);
        Log.d("MasterMiao", "altitude:" + location.getAltitude() + " longitude:" + location.getLongitude());
        fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = "http://"+Constants.serverAddress+"/record?latitude="+String.format("%.11f", location.getLatitude())+"&longitude="+String.format("%.11f", location.getLongitude());
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
                    Log.d("MasterMiao", "http send exception");
                }
            }
        });
    }
}
