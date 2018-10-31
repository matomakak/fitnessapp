/*
 * Copyright (c) 2016 Martin Hlavačka
 */

package com.hlavackamartin.fitnessapp.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.wearable.view.FragmentGridPagerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Class providing functionality for GridView containing all fragments of given instance.
 */
public class FragmentGridAdapter extends FragmentGridPagerAdapter {

    private final Context mContext;
    private List<FragmentRow> mRows;
    /**
     * Constructor creating array of fragment rows.
     * @param context application context
     * @param fm FragmentManager
     */
    public FragmentGridAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
        mRows = new ArrayList<>();
    }
    /**
     * Method add complete row of fragments to GridView.
     * @param fragments array of fragments for row creating
     */
    public void addFragmentRow(Fragment... fragments) {
        FragmentRow mRow = new FragmentRow();
        for (Fragment f : fragments) {
            mRow.add(f);
        }
        mRows.add(mRow);
    }

    @Override
    public Fragment getFragment(int row, int col) {
        return mRows.get(row).getColumn(col);
    }

    @Override
    public int getRowCount() {
        return mRows.size();
    }

    @Override
    public int getColumnCount(int row) {
        return mRows.get(row).getColumnCount();
    }
    /**
     * Helper class implementing row of fragments.
     */
    private class FragmentRow {
        final ArrayList<Fragment> columns;

        public FragmentRow() {
            columns = new ArrayList<>();
        }
        public void add(Fragment f) {
            columns.add(f);
        }
        public Fragment getColumn(int i) {
            return columns.get(i);
        }
        public int getColumnCount() {
            return columns.size();
        }
    }
}
