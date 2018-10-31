/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.R;

/**
 * Fragment class providing simple UI interface for showing single value on screen.
 */
public class SingleValueFragment extends Fragment {

    private TextView mValueText;
    private TextView mValueTitle;
    /**
     * @param mTitle title text to appear on top of fragment
     * @return new instance of fragment with given title
     */
    public static SingleValueFragment newInstance(String mTitle) {
        SingleValueFragment mFragment = new SingleValueFragment();

        Bundle args = new Bundle();
        args.putString("title", mTitle);
        mFragment.setArguments(args);

        return mFragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.value_layout, container, false);
        mValueText = (TextView) view.findViewById(R.id.single_value);
        mValueTitle = (TextView) view.findViewById(R.id.page_title);
        return view;
    }
    /**
     * When fragment is created or recreated, set title given.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setTitle(getArguments().getString("title"));
    }
    /**
     * Sets the fragment title text.
     * @param text title text
     */
    public void setTitle(final String text){
        if(mValueTitle != null)
           mValueTitle.setText(text);
    }
    /**
     * Sets single value shown in fragment.
     * @param text single value text
     */
    public void setValue(final String text) {
        if(mValueText != null)
            mValueText.setText(text);
    }
    /**
     * Method overload giving ability to set integer as text.
     * @param i single value number
     */
    public void setValue(int i) {
        setValue(String.valueOf(i));
    }
    /**
     * Sets elements according to existence of ambient mode.
     * @param isAmbient boolean value indicating ambient mode
     */
    public void updateAmbient(boolean isAmbient){
        mValueText.getPaint().setAntiAlias(!isAmbient);
    }
}
