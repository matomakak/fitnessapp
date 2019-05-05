package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.fragment.FitnessAppFragment;
import com.hlavackamartin.fitnessapp.recognition.task.SynchronizeTask;
import java.util.List;

/**
 * Implementation for synchronization module for easy upload/download of data for training and
 * recognition purposes
 */
public class SyncFragment extends FitnessAppFragment implements
    View.OnClickListener, View.OnLongClickListener {

  private TextView mTitle;
  private ImageButton mButton;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_sync, container, false);

    mTitle = rootView.findViewById(R.id.sync_title);
    mButton = rootView.findViewById(R.id.sync_btn);
    mButton.setOnClickListener(this);
    mButton.setOnLongClickListener(this);

    return rootView;
  }

  /**
   * After selection of module checks possibility to write to application external dir and provides
   * feedback to user viac text field
   */
  @Override
  public void onResume() {
    super.onResume();
    if (Utilities.isExternalStorageWritable()) {
      mTitle.setText(R.string.sync);
      mButton.setEnabled(true);
    } else {
      mTitle.setText(R.string.error__no_storage);
      mButton.setEnabled(false);
    }
  }

  /**
   * Execution of uploading process within single tap on button
   */
  @Override
  public void onClick(View view) {
    if (mButton.isEnabled()) {
      executeUploadTask();
    }
  }

  /**
   * Execution of downloading process within long hold on button
   */
  @Override
  public boolean onLongClick(View v) {
    if (mButton.isEnabled()) {
      executeDownloadTask();
    }
    return true;
  }

  /**
   * Creates progress dialog indicating ongoing upload/download process
   *
   * @param type type of executed process (upload/download)
   * @return progress dialog with proper naming
   */
  private ProgressDialog createSyncDialog(SynchronizeTask.SyncActionType type) {
    ProgressDialog mProgressDialog;
    mProgressDialog = new ProgressDialog(getActivity());
    mProgressDialog.setMessage(getString(
        type == SynchronizeTask.SyncActionType.UPLOAD ? R.string.upload_message
            : R.string.download_message
    ));
    mProgressDialog.setIndeterminate(true);
    mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    mProgressDialog.setCancelable(false);
    return mProgressDialog;
  }

  /**
   * Execution of uploading process via {@link SynchronizeTask}
   */
  private void executeUploadTask() {
    ProgressDialog mProgressDialog;

    mProgressDialog = createSyncDialog(SynchronizeTask.SyncActionType.UPLOAD);
    final SynchronizeTask uploadTask =
        new SynchronizeTask(getActivity(), mProgressDialog, SynchronizeTask.SyncActionType.UPLOAD);
    uploadTask.execute();
  }

  /**
   * Execution of downloading process via {@link SynchronizeTask}
   */
  private void executeDownloadTask() {
    ProgressDialog mProgressDialog;

    mProgressDialog = createSyncDialog(SynchronizeTask.SyncActionType.DOWNLOAD);
    final SynchronizeTask downloadTask =
        new SynchronizeTask(getActivity(), mProgressDialog,
            SynchronizeTask.SyncActionType.DOWNLOAD);
    downloadTask.execute();
  }

  @Override
  public List<String> getActionMenu(Resources resources) {
    return null;
  }

  @Override
  public boolean onMenuItemClick(MenuItem menuItem) {
    return false;
  }

  @Override
  public void onEnterAmbient(Bundle bundle) {
    mTitle.getPaint().setAntiAlias(false);
  }

  @Override
  public void onExitAmbient() {
    mTitle.getPaint().setAntiAlias(true);
  }

  @Override
  public void onUpdateAmbient() {
  }

}
