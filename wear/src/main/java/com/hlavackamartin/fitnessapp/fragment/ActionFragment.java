/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.fragment;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.hlavackamartin.fitnessapp.R;
import com.hlavackamartin.fitnessapp.extend.WearableActivityExt;

/**
 * Fragment class providing action button interface settings.
 */
public class ActionFragment extends Fragment {

    private static final String ACTION_TYPE_BUNDLE_KEY = "type";
    private static final String BUTTON_TYPE_BUNDLE_KEY = "button";
    private View mView;
    private Button mButton;
    private ProgressBar mProgressBar;
    private type mButtonType;
    /**
     * Action type which defines button appearance and behaviour trigger
     */
    public enum type {
        START_ACTION,
        PAUSE_ACTION,
        RESET_ACTION,
        END_ACTION
    }
    /**
     * @param actionType type of fragment
     * @see com.hlavackamartin.fitnessapp.fragment.ActionFragment.type
     * @return new instance of fragment according to type
     */
    public static ActionFragment newInstance(type actionType) {
        ActionFragment mFragment = new ActionFragment();

        Bundle args = new Bundle();
        args.putSerializable(ACTION_TYPE_BUNDLE_KEY, actionType);
        mFragment.setArguments(args);

        return mFragment;
    }
    /**
     * Saving button type when fragment goes in backstack.
     */
    @Override
    public void onPause() {
        super.onPause();
        getArguments().putSerializable(BUTTON_TYPE_BUNDLE_KEY, mButtonType);
    }
    /**
     * Method creates View and sets appearance and onclick listeners according to defined type.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.action_layout, container, false);
        mButton = (Button) mView.findViewById(R.id.btn);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.progressBar1);
        mProgressBar.getIndeterminateDrawable().setColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN);
        type mType = (type)getArguments().getSerializable(ACTION_TYPE_BUNDLE_KEY);
        if(mType != null)
            switch (mType) {
                case START_ACTION:
                    mButton.setBackgroundResource(R.drawable.start_button);
                    mButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((WearableActivityExt) getActivity()).startActionTask();
                        }
                    });
                    break;
                case PAUSE_ACTION:
                    mButton.setBackgroundResource(R.drawable.pause_button);
                    mButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((WearableActivityExt) getActivity()).pauseActionTask();
                        }
                    });
                    break;
                case RESET_ACTION:
                    mButton.setBackgroundResource(R.drawable.reset_button);
                    mButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((WearableActivityExt) getActivity()).resetActionTask();
                        }
                    });
                    break;
                case END_ACTION:
                    mButton.setBackgroundResource(R.drawable.end_button);
                    mButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            setProgressBarVisibility(true);
                            ((WearableActivityExt) getActivity()).endActionTask();
                        }
                    });
                    break;
            }
        type buttonType = (type)getArguments().getSerializable(BUTTON_TYPE_BUNDLE_KEY);
        if(buttonType != null) setButtonBackground(buttonType);
        return mView;
    }
    /**
     * @param setVisible boolean value for setting visibility of progressbar around button
     */
    public void setProgressBarVisibility(boolean setVisible){
        if(mProgressBar != null)
            mProgressBar.setVisibility(setVisible ? View.VISIBLE : View.GONE);
    }
    /**
     * @param mType type for setting right type of button appearance
     * @see com.hlavackamartin.fitnessapp.fragment.ActionFragment.type
     */
    public void setButtonBackground(type mType){
        switch (mType) {
            case START_ACTION:
                mButton.setBackgroundResource(R.drawable.start_button);
                break;
            case PAUSE_ACTION:
                mButton.setBackgroundResource(R.drawable.pause_button);
                break;
            case RESET_ACTION:
                mButton.setBackgroundResource(R.drawable.reset_button);
                break;
            case END_ACTION:
                mButton.setBackgroundResource(R.drawable.end_button);
                break;
        }
        mButtonType = mType;
    }

    public void setAmbient(boolean isAmbient){
    }
}
