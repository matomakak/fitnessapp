/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.fragment;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.hlavackamartin.fitnessapp.R;
import com.hlavackamartin.fitnessapp.Utilities;
import com.hlavackamartin.fitnessapp.service.LocationService;

/**
 * Fragment class providing UI interface and settings allowing to show Google map with current
 * position
 */
public class SimpleMapFragment extends Fragment {

    private TextView mBottomInfo;
    private TextView mWaitingForGpsText;
    private MapFragment mMapFragment;
    private LocationService mLocationSourceService;
    private boolean serviceBounded = false;

    private static GoogleMap mMap;
    /**
     * Connection to location service which allows to acquire position for map.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mLocationSourceService = binder.getService();
            serviceBounded = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBounded = true;
        }
    };
    /**
     * When activity containing this fragment has started, starts location service and obtain reference to it.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().bindService(
                new Intent(getActivity(), LocationService.class),
                mServiceConnection,
                Context.BIND_AUTO_CREATE
        );
    }
    /**
     * Method creates View and sets parameters and location service source for Google map. Also
     * dim map to notice user of active searching for position.
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.map_layout, container, false);
        mMapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mWaitingForGpsText = (TextView) view.findViewById(R.id.waiting_gps_overlay);
        if(Utilities.hasGpsPermissions(getActivity()))
            mMapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    mMap = googleMap;
                    mMap.getUiSettings().setZoomControlsEnabled(true);
                    mMap.getUiSettings().setZoomGesturesEnabled(false);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    mMap.setLocationSource(mLocationSourceService);
                    mMap.setMyLocationEnabled(true);
                }
            });
        else
            mWaitingForGpsText.setText("GPS missing or not allowed to use");
        mBottomInfo = (TextView) view.findViewById(R.id.bottom_info);
        return view;
    }
    /**
     * Interface for activity to stop running location service.
     */
    public void stopLocationService(){
        mLocationSourceService.stopLocation();
    }
    /**
     * Interface for activity to start location service.
     */
    public void startLocationService(){
        mLocationSourceService.startLocation();
    }
    /**
     * When resumed, starts location service and show position on map.
     */
    @Override
    public void onResume() {
        if(mMap != null && serviceBounded) {
            mLocationSourceService.startLocation();
            mMap.setMyLocationEnabled(true);
        }
        super.onResume();
    }
    /**
     * When activity containing this fragment finishes, stops running service and hide position.
     */
    @Override
    public void onDestroy() {
        if(serviceBounded) getActivity().unbindService(mServiceConnection);
        if(mMap != null) mMap.setMyLocationEnabled(false);
        super.onDestroy();
    }
    /**
     * Method moves position on map accordingly.
     * @param position position to move view
     * @param bearing rotation of map.
     * @param resetZoom boolean indicating resetting zoom level
     */
    public void moveCameraTo(LatLng position,float bearing,boolean resetZoom){
        new AsyncTask<Object,Void,CameraUpdate>(){
            @Override
            protected CameraUpdate doInBackground(Object... objects) {
                return CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .target((LatLng) objects[0])
                        .bearing((Float) objects[1])
                        .zoom((boolean)objects[2] ? 16 : (Float)objects[3])
                        .build());
            }
            @Override
            protected void onPostExecute(CameraUpdate result) {
                mMap.animateCamera(result);
            }
        }.execute(position, bearing, resetZoom, mMap.getCameraPosition().zoom);
    }
    /**
     * Shows or hides notification of active location searching.
     * @param showInfo indicating show/hide operation
     */
    public void setAwaitGPS(boolean showInfo){
        if(showInfo){
            mWaitingForGpsText.setText(R.string.gps_waiting);
            mWaitingForGpsText.setVisibility(View.VISIBLE);
        }else{
            mWaitingForGpsText.setVisibility(View.GONE);
        }
    }
    /**
     * Shows or hides notification of paused position identifying.
     * @param showInfo indicating show/hide operation
     */
    public void setStartPause(boolean showInfo){
        if(showInfo){
            mWaitingForGpsText.setText(R.string.gps_paused);
            mWaitingForGpsText.setVisibility(View.VISIBLE);
        }else{
            mWaitingForGpsText.setVisibility(View.GONE);
        }
    }
    /**
     * Draws line on map.
     * @param startPosition start position of line
     * @param endPosition end position of line
     */
    public void drawPolyline(LatLng startPosition, LatLng endPosition){
        new AsyncTask<LatLng, Void, PolylineOptions>(){
            @Override
            protected PolylineOptions doInBackground(LatLng... positions) {
                return new PolylineOptions()
                        .add(positions[0],positions[1])
                        .color(Color.RED)
                        .width(5);
            }
            @Override
            protected void onPostExecute(PolylineOptions result) {
                mMap.addPolyline(result);
            }
        }.execute(startPosition, endPosition);
    }
    /**
     * Sets text under map.
     * @param text string text
     */
    public void setBottomInfo(String text){
        if(mBottomInfo != null)
           mBottomInfo.setText(text);
    }
    /**
     * Hides bottom info panel.
     */
    public void hideBottomInfo(){
        mBottomInfo.setVisibility(View.GONE);
    }
    /**
     * When ambient mode is entered set map style to ambient.
     * @param b bundle passed to fragment
     */
    public void onEnterAmbient(Bundle b) {
        mMapFragment.onEnterAmbient(b);
    }
    /**
     * When ambent mode is exited set map style to default.
     */
    public void onExitAmbient() {
        mMapFragment.onExitAmbient();
    }

}
