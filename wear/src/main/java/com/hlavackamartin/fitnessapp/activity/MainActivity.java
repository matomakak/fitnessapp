/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.GridViewPager;
import android.view.WindowManager;
import android.widget.Toast;

import com.hlavackamartin.fitnessapp.R;
import com.hlavackamartin.fitnessapp.adapter.FragmentGridAdapter;
import com.hlavackamartin.fitnessapp.extend.WearableActivityExt;
import com.hlavackamartin.fitnessapp.fragment.ActionFragment;
import com.hlavackamartin.fitnessapp.fragment.ListViewFragment;

import java.util.ArrayList;

/**
 * Main class creating basic menu for activity tracking selection.
 */
public class MainActivity extends WearableActivityExt {

    private GridViewPager mPager;
    private ActionFragment mActionPage;
    private ListViewFragment mListViewPage;
    private ArrayList<Class> mMenuActivities;
    /**
     * Method which creates basic information for menu creating such as title and activity type
     * to run when selected.
     * @return string array of menu item titles
     */
    private String[] getMenuItems(){
        ArrayList<String> mTitles = new ArrayList<>();
        mMenuActivities = new ArrayList<>();
        mTitles.add("Jumping Jacks");mMenuActivities.add(JumpJacksActivity.class);
        mTitles.add("!Squats");      mMenuActivities.add(SquatsActivity.class);
        mTitles.add("Running");      mMenuActivities.add(RunningActivity.class);
        mTitles.add("!Cycling");     mMenuActivities.add(CyclingActivity.class);
        mTitles.add("MapView");      mMenuActivities.add(MapActivity.class);

        return mTitles.toArray(new String[mTitles.size()]);
    }
    /**
     * Method using GridView to create simple UI containing list of available exercises and
     * action button triggering tracking of it.
     */
    private void setupViews() {
        mPager = (GridViewPager) findViewById(R.id.pager);

        FragmentGridAdapter mAdapter = new FragmentGridAdapter(this, getFragmentManager());

        mListViewPage = ListViewFragment.newInstance(getMenuItems());
        mActionPage =  ActionFragment.newInstance(ActionFragment.type.START_ACTION);
        mAdapter.addFragmentRow(
                mListViewPage,
                mActionPage
        );

        mPager.setAdapter(mAdapter);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.grid_pager_layout);
        setupViews();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setAmbientEnabled();
    }
    /**
     * After activity resuming, set the position of GridView back to exercise list.
     */
    @Override
    protected void onResume() {
        super.onResume();
        mPager.setCurrentItem(0, 0);
        mActionPage.setProgressBarVisibility(false);
    }
    /**
     * Method providing behaviour for action button which starts exercise tracking.
     */
    public void startActionTask(){
        final Class mClass = mMenuActivities.get(mListViewPage.getSelectedItem());
        final Context mThis = this;

        if( mClass != null ) {
            mActionPage.setProgressBarVisibility(true);
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    mThis.startActivity(new Intent(mThis, mClass));
                }
            }, 1500);
        }else {
            Toast.makeText(this, "Tracking of this activity is not available yet", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * When activity stops, doesn't destroy as implemented in WearableActivityExt.
     * Do nothing, so activity doesn't get killed.
     * @see WearableActivityExt
     */
    @Override
    protected void activityFinish() {  }
    /**
     * UI method allowing clicking on exercise title and swiping to action button.
     */
    public void swipeToActionFragment(){
        mPager.setCurrentItem(0,1);
    }

}
