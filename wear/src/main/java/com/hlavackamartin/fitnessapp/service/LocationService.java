/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.wearable.Wearable;
import com.hlavackamartin.fitnessapp.R;
import com.hlavackamartin.fitnessapp.Utilities;

import java.util.concurrent.TimeUnit;

/**
 * Service class providing detection of location using gps module.
 */
public class LocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        LocationSource{

    GoogleApiClient mGoogleApiClient;
    private int badAccuracy;
    private OnLocationChangedListener mMapLocationListener;

    public static final String LOCATION_INTENT = "LOCATION";
    public static final String GPS_WAIT_INTENT = "GPS_WAIT";

    private final IBinder mBinder = new LocalBinder();
    /**
     * Interface allowing to return running reference to this service.
     */
    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    /**
     * When service starts, connecting to google api client.
     */
    public void startLocation(){
        if(mGoogleApiClient != null)
            mGoogleApiClient.connect();
        badAccuracy = 3;
    }
    /**
     * When service finishes, removing location listener and disconnecting from google api client.
     */
    public void stopLocation(){
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()){
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }
    /**
     * Method builds connection to google api client.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        if(!Utilities.hasGpsPermissions(getApplication()))
            return;

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS))
            Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_SHORT).show();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        if(mGoogleApiClient != null)
            mGoogleApiClient.connect();
        badAccuracy = 3;
    }
    /**
     * @see LocationService#stopLocation()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocation();
    }
    /**
     * When connection to google api client was successful, registers location updates from gps.
     */
    @Override
    public void onConnected(Bundle bundle) {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(TimeUnit.SECONDS.toMillis(10))
                .setFastestInterval(TimeUnit.SECONDS.toMillis(1))
                .setSmallestDisplacement(10);

        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, locationRequest, this);
    }
    /**
     * Method which gets called when new position was acquired. Sending it through
     * local broadcast to parent activity.
     * @param location current aquired position
     */
    @Override
    public void onLocationChanged(Location location) {
        if(location.getAccuracy() < 50 && location.getAccuracy() > 0){
            if(mMapLocationListener != null){
                mMapLocationListener.onLocationChanged(location);
            }
            Intent mIntent = new Intent(LOCATION_INTENT);
            mIntent.putExtra("latitude", location.getLatitude());
            mIntent.putExtra("longitude", location.getLongitude());
            mIntent.putExtra("speed", location.getSpeed());
            LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent);
            badAccuracy = 0;
        }else if(++badAccuracy > 3){
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(GPS_WAIT_INTENT));
            badAccuracy = 0;
        }
    }
    /**
     * when connection from google api client was lost, removing location update listener.
     */
    @Override
    public void onConnectionSuspended(int i) {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
    /**
     * Method providing interface for Google map to obtain current position.
     * @param onLocationChangedListener map location listener
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mMapLocationListener = onLocationChangedListener;
    }
    /**
     * Method providing interface for Google map to remove location updates subscription.
     */
    @Override
    public void deactivate() {
        mMapLocationListener = null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {    }
}
