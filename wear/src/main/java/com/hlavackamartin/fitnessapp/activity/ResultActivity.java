/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.DismissOverlayView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.Common;
import com.hlavackamartin.fitnessapp.R;
import com.hlavackamartin.fitnessapp.extend.WearableActivityExt;
import com.hlavackamartin.fitnessapp.service.SendDataService;

import java.util.ArrayList;

/**
 * Class providing statistics and result after exercise tracking was finished.
 */
public class ResultActivity extends WearableActivityExt {

    private GestureDetectorCompat mGestureDetector;
    private DismissOverlayView mDismissOverlayView;

    /**
     * Method creating basic layout for showing and sending results obtained as intent message from
     * activity that it launched.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.result_layout);
        TextView title = (TextView) findViewById(R.id.result_page_title);
        TextView resultView = (TextView) findViewById(R.id.result_view);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setAmbientEnabled();

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            String type = extras.getString(Common.ExerciseResult.TYPE);

            ArrayList<String> result = extras.getStringArrayList(Common.ExerciseResult.DATA);
            Intent intent = new Intent(this, SendDataService.class);
            intent.putExtra(Common.ExerciseResult.TYPE, type);
            intent.putExtra(Common.ExerciseResult.DATA, result);
            startService(intent);

            title.setText(type);
            if (result != null)
                for(String item : result)
                    resultView.append(item + "\n");
        }
        else
            resultView.setText("No collected data to show...");

        mDismissOverlayView= (DismissOverlayView)findViewById(R.id.dismiss_overlay);
        mGestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public void onLongPress(MotionEvent event) {
                mDismissOverlayView.show();
            }
        });
    }
    /**
     * Method allowing to close activity through long press using dismiss overlay view.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.dispatchTouchEvent(event);
    }
}
