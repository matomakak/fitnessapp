package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.fragment.FitnessAppFragment;
import com.hlavackamartin.fitnessapp.recognition.task.SynchronizeTask;

import java.util.List;


public class SyncFragment extends FitnessAppFragment implements
	View.OnClickListener {

	private TextView mTitle;
	private Button mButton;

	@Override
	public View onCreateView(
		LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_sync, container, false);

		mTitle = rootView.findViewById(R.id.sync_title);
		mButton = rootView.findViewById(R.id.sync_btn);

		if (Utilities.isExternalStorageWritable()) {
			mTitle.setText(R.string.sync);
			mButton.setOnClickListener(this);
			mButton.setEnabled(true);
		} else {
			mTitle.setText(R.string.error__no_storage);
			mButton.setEnabled(false);
		}

		return rootView;
	}

	@Override
	public void onClick(View view) {
		if (mButton.isEnabled()) {
			executeSyncTask();
		}
	}

	private ProgressDialog createSyncDialog(SynchronizeTask.SyncActionType type) {
		ProgressDialog mProgressDialog;
		mProgressDialog = new ProgressDialog(getActivity());
		mProgressDialog.setMessage(getString(
			type == SynchronizeTask.SyncActionType.UPLOAD ? R.string.upload_message : R.string.download_message
		));
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(true);
		return mProgressDialog;
	}

	private void executeSyncTask() {
		ProgressDialog mProgressDialog;

		//mProgressDialog = createSyncDialog(SynchronizeTask.SyncActionType.DOWNLOAD);
		//final SynchronizeTask downloadTask =
		//	new SynchronizeTask(getActivity(), mProgressDialog, SynchronizeTask.SyncActionType.DOWNLOAD);
		//downloadTask.execute();

		mProgressDialog = createSyncDialog(SynchronizeTask.SyncActionType.UPLOAD);
		final SynchronizeTask uploadTask =
			new SynchronizeTask(getActivity(), mProgressDialog, SynchronizeTask.SyncActionType.UPLOAD);
		uploadTask.execute();
	}


	@Override
	public List<String> getActionMenu(Resources resources) {
		return null;
	}

	@Override
	public void onEnterAmbient(Bundle bundle) {

	}

	@Override
	public void onUpdateAmbient() {

	}

	@Override
	public void onExitAmbient() {

	}

	@Override
	public boolean onMenuItemClick(MenuItem menuItem) {
		return false;
	}
}
