/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.R;
import com.hlavackamartin.fitnessapp.activity.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment class providing basic UI settings showing list type interface.
 */
public class ListViewFragment extends Fragment implements WearableListView.ClickListener, WearableListView.OnScrollListener {

    private WearableListView mWearableListView;
    private WearableListView.Adapter mAdapter;
    private int mActivePosition = 0;
    private TextView mHeader;
    /**
     * @param titles string array with list item titles
     * @return new instance of list interface with given items
     */
    public static ListViewFragment newInstance(String[] titles) {
        ListViewFragment mFragment = new ListViewFragment();

        Bundle args = new Bundle();
        args.putStringArray("titles", titles);
        mFragment.setArguments(args);

        return mFragment;
    }
    /**
     * Create list with dismiss overlay availability and allows to scroll between items.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.list_view_layout, container, false);
        mHeader = (TextView) view.findViewById(R.id.header);
        mWearableListView = (WearableListView) view.findViewById(R.id.list);
        // Getting list of menu items from argument
        // Creating and setting the adapter used to show the available items
        mAdapter = new Adapter(getArguments().getStringArray("titles"));
        mWearableListView.setAdapter(mAdapter);
        mWearableListView.setClickListener(this);
        mWearableListView.addOnScrollListener(this);
        // Used to allow the WearableListView to intercept all the touches
        // preventing the parent to handle them
        mWearableListView.setGreedyTouchMode(true);
        return view;
    }
    /**
     * Registers click on item, which is then sent to activity to handle it.
     */
    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {
        ((MainActivity) getActivity()).swipeToActionFragment();
    }
    /**
     * Saves actual position in list
     * @param position actual selected item position
     */
    @Override
    public void onCentralPositionChanged(int position) {
        mActivePosition = position;
    }
    /**
     * @return selected item position
     */
    public int getSelectedItem(){
        return mActivePosition;
    }
    /**
     * Moves app title accordingly
     * @param i vertical position of list
     */
    @Override
    public void onAbsoluteScrollChange(int i) {
        if (i > 0) {
            mHeader.setY(-i);
        }
    }
    /**
     * Class handling each menu item separately.
     */
    private static class Holder extends WearableListView.ViewHolder {

        private TextView mTextView;
        //private ImageView mImageView;

        private Holder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.text);
            //mImageView = (ImageView) itemView.findViewById(R.id.circle);
        }
    }
    /**
     * Class providing functionality to create whole list of menu items.
     */
    private static class Adapter extends WearableListView.Adapter {

        private List<String> mData = new ArrayList<>();

        public Adapter(String[] data) {
            this.mData = Arrays.asList(data);
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
            return new Holder(view);
        }
        /**
         * Sets each menu item separately. If title starts with special character, method groups
         * items accordingly.
         * @param viewHolder holder of menu item
         * @param position position in list
         */
        @Override
        public void onBindViewHolder(WearableListView.ViewHolder viewHolder, int position) {
            final Holder holder = (Holder) viewHolder;
            String mTitle = mData.get(position);
            if( mTitle.startsWith("!") ){
                holder.mTextView.setText(mTitle.substring(1));
                holder.itemView.setPadding(0,0,0,25);
            }else{
                holder.mTextView.setText(mTitle);
            }

        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    public void setAmbient(boolean isAmbient){    }
    @Override
    public void onTopEmptyRegionClick() {    }
    @Override
    public void onScroll(int i) {    }
    @Override
    public void onScrollStateChanged(int i) {    }
}
