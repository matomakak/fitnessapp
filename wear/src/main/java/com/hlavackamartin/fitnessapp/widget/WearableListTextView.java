/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.widget;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.R;

/**
 * Helper class extending basic function of list view with animation of its items.
 */
public class WearableListTextView extends LinearLayout implements WearableListView.OnCenterProximityListener {

    private final float FADED_ALPHA = 0.5f;            /**partial transparency*/
    private ImageView mCircle;
    private TextView mName;

    public WearableListTextView(Context context) {
        super(context);
    }

    public WearableListTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WearableListTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCircle = (ImageView) findViewById(R.id.circle);
        mName = (TextView) findViewById(R.id.text);
    }
    /**
     * Animate selected list item with full visibility.
     */
    @Override
    public void onCenterPosition(boolean animate) {
        mName.setAlpha(1f);
        mCircle.setAlpha(1f);
    }
    /**
     * Animate non selected list item with faded effect.
     */
    @Override
    public void onNonCenterPosition(boolean animate) {
        mCircle.setAlpha(FADED_ALPHA);
        mName.setAlpha(FADED_ALPHA);
    }
}
