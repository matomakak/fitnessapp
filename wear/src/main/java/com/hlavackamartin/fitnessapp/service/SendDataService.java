/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.service;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.hlavackamartin.fitnessapp.Common;

import java.util.concurrent.TimeUnit;
/**
 * Service class providing behaviour for sending measured statistics to paired mobile device.
 */
public class SendDataService extends IntentService {

    public SendDataService() {
        super("SendDataToMobile");
    }
    /**
     * Method sends data through DataApi
     * @param intent containing measured statistics
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.blockingConnect(2, TimeUnit.SECONDS);

        Long time = System.currentTimeMillis();
        PutDataMapRequest mPutDataMapRequest = PutDataMapRequest
                .create(Common.SHARED_DATA_PATH + time);
        DataMap dataMap = mPutDataMapRequest.getDataMap();
        dataMap.putLong(Common.ExerciseResult.TIME, time);
        dataMap.putString(Common.ExerciseResult.TYPE, intent.getStringExtra(Common.ExerciseResult.TYPE));
        dataMap.putStringArrayList(Common.ExerciseResult.DATA, intent.getStringArrayListExtra(Common.ExerciseResult.DATA));
        Wearable.DataApi.putDataItem(mGoogleApiClient, mPutDataMapRequest.asPutDataRequest());
        if(mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }
}
