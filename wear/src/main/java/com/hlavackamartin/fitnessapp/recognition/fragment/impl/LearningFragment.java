package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.fragment.FitnessAppFragment;
import com.hlavackamartin.fitnessapp.recognition.service.MotionRecordingService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class LearningFragment extends FitnessAppFragment implements
    View.OnClickListener,
    NumberPicker.OnValueChangeListener {

  private static final int SPEECH_REQUEST_CODE = 1;
  private static final String EXERCISE_BUNDLE_KEY = "EXERCISE_BUNDLE_KEY";

  private TextView mTitle;
  private Button mButton;
  private ProgressBar mProgressBar;

  private String selectedExercise;

  private boolean mServiceBound = false;
  private MotionRecordingService mService;
  private ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      MotionRecordingService.LocalBinder binder = (MotionRecordingService.LocalBinder) service;
      mService = binder.getService();
      mServiceBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      mServiceBound = false;
    }
  };

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putString(EXERCISE_BUNDLE_KEY, selectedExercise);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    selectedExercise = state != null ? state.getString(EXERCISE_BUNDLE_KEY, "") : "";
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_learn, container, false);

    mTitle = rootView.findViewById(R.id.learn_title);
    mButton = rootView.findViewById(R.id.learn_btn);
    mProgressBar = rootView.findViewById(R.id.learn_progressBar);
    mProgressBar.getIndeterminateDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
    mProgressBar.setVisibility(View.GONE);

    mTitle.setText(Utilities.isExternalStorageWritable() ?
        R.string.select_exercise : R.string.error__no_storage);
    mButton.setOnClickListener(this);

    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    mButton.setEnabled(false);
    if (Utilities.isExternalStorageWritable()) {
      if (selectedExercise != null && !selectedExercise.isEmpty()) {
        mButton.setEnabled(mServiceBound);
        mTitle.setText(selectedExercise);
      }
      if (!mServiceBound) {
        Intent intent = new Intent(getActivity(), MotionRecordingService.class);
        getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
      }
    }
  }

  private void enableSelectedExercise(String name) {
    selectedExercise = name;
    mTitle.setText(selectedExercise);
    if (mButton != null) {
      mButton.setEnabled(Utilities.isExternalStorageWritable() && mServiceBound);
    }
  }

  @Override
  public void onClick(View view) {
    if (mButton.isEnabled() && mServiceBound) {
      MotionRecordingService.RecordingStatus status = mService.getRecordingStatus();
      if (status == MotionRecordingService.RecordingStatus.STOPPED) {
        showStartCountDownDialog();
      } else if (status == MotionRecordingService.RecordingStatus.RECORDING) {
        stopRecordingData(false);
      }
    }
  }

  public void showStartCountDownDialog() {
    AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
    alertDialog.setTitle("Start in");
    alertDialog.setMessage("6");
    alertDialog.show();

    new CountDownTimer(6000, 100) {
      @Override
      public void onTick(long millisUntilFinished) {
        alertDialog.setMessage(String.format(Locale.ENGLISH, "%.1f", millisUntilFinished / 1000f));
      }

      @Override
      public void onFinish() {
        if (alertDialog.isShowing()) {
          alertDialog.dismiss();
          startRecording();
        }
      }
    }.start();
  }

  public void startRecording() {
    this.indicateRepProcessing(true); //To show animation as soon as possible
    if (mService.startRepRecording(selectedExercise)) {
      Utilities.vibrate(getActivity(), null);
    } else {
      this.indicateRepProcessing(false);
    }
  }

  public void indicateRepProcessing(boolean processing) {
    if (mProgressBar != null) {
      mProgressBar.setVisibility(processing ? View.VISIBLE : View.GONE);
    }
    if (mButton != null) {
      mButton.setSelected(processing);
      mButton.setPressed(processing);
    }
    if (mTitle != null) {
      mTitle.setText(processing ? getString(R.string.complete_rep) : selectedExercise);
    }
  }

  public void stopRecordingData(boolean showRepCountInput) {
    this.indicateRepProcessing(false);
    mService.stopRepRecording(showRepCountInput);
    if (showRepCountInput) {
      showRepCountPickerDialog();
    }
  }

  public void showRepCountPickerDialog() {
    NumberPickerDialog newFragment = new NumberPickerDialog();
    newFragment.setValueChangeListener(this);
    newFragment.show(getFragmentManager(), "Rep count");
  }

  @Override
  public void onValueChange(NumberPicker numberPicker, int i, int i1) {
    //AFTER NUMBER PICKER DIALOG
    if (mServiceBound) {
      mService.setRepsForFinishedRecording(numberPicker.getValue());
    }
  }

  @Override
  public List<String> getActionMenu(Resources resources) {
    List<String> list =
        new ArrayList<>(Arrays.asList(resources.getStringArray(R.array.movements)));
    list.add(resources.getString(R.string.custom));
    return list;
  }

  @Override
  public boolean onMenuItemClick(MenuItem menuItem) {
    if (menuItem.getTitle().equals(getString(R.string.restart_all))) {
      if (mServiceBound) {
        if (mService.deleteData()) {
          Toast.makeText(getActivity(), R.string.del_succes, Toast.LENGTH_SHORT).show();
          return true;
        }
      }
      Toast.makeText(getActivity(), R.string.del_fail, Toast.LENGTH_SHORT).show();
    } else if (menuItem.getTitle().equals(getString(R.string.custom))) {
      Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
      intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
          RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
      startActivityForResult(intent, SPEECH_REQUEST_CODE);
    } else {
      this.enableSelectedExercise(menuItem.getTitle().toString());
    }
    return true;
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
      try {
        List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        this.enableSelectedExercise(results.get(0));
      } catch (NullPointerException ignored) {
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void onDestroy() {
    endTask();
    super.onDestroy();
  }

  private void endTask() {
    if (mServiceBound) {
      getActivity().unbindService(mServiceConnection);
      mServiceBound = false;
    }
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
