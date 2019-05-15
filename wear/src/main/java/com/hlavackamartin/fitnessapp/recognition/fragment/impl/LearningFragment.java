package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import static android.app.Activity.RESULT_OK;
import static com.hlavackamartin.fitnessapp.recognition.Utilities.fileExist;

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


/**
 * Implementation for data collecting within learning module
 */
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
  /**
   * Connection between activity and data recording service {@link MotionRecordingService}
   */
  private ServiceConnection mServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      MotionRecordingService.LocalBinder binder = (MotionRecordingService.LocalBinder) service;
      mService = binder.getService();
      mServiceBound = true;
      if ((selectedExercise != null) && !selectedExercise.isEmpty()) {
        mButton.setEnabled(true);
        mTitle.setText(selectedExercise);
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      mButton.setEnabled(false);
      mServiceBound = false;
    }
  };

  /**
   * Providing functionality for storing selected exercise for data collection while app is paused
   */
  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putString(EXERCISE_BUNDLE_KEY, selectedExercise);
    super.onSaveInstanceState(outState);
  }

  /**
   * Retrieval of last selected exercise before app was paused
   */
  @Override
  public void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    selectedExercise = state != null ? state.getString(EXERCISE_BUNDLE_KEY, "") : "";
  }

  /**
   * Creates UI components and checks possibility for external application dir write access
   */
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

  /**
   * Repopulates UI when app is resumed
   */
  @Override
  public void onResume() {
    super.onResume();
    mButton.setEnabled(false);
    if (Utilities.isExternalStorageWritable()) {
      if (!mServiceBound) {
        if (fileExist(getContext(), getString(R.string.download_file)) && fileExist(getContext(),
            getString(R.string.download_labels))) {
          Intent intent = new Intent(getActivity(), MotionRecordingService.class);
          getActivity().bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } else if (mTitle != null) {
          mTitle.setText(R.string.error__no_data);
        }
      } else {
        enableSelectedExercise(selectedExercise);
      }
    }
  }

  /**
   * When exercise name for collection of data purposes selected allowing user to click to start
   * learning/collection process
   *
   * @param name name of selected exercise
   */
  private void enableSelectedExercise(String name) {
    selectedExercise = name;
    if (selectedExercise != null) {
      mTitle.setText(selectedExercise);
      if (mButton != null) {
        mButton.setEnabled(Utilities.isExternalStorageWritable() && mServiceBound);
      }
    } else {
      mButton.setEnabled(false);
    }
  }

  /**
   * Click on button provides start/stop functionality for learning/collection process
   */
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

  /**
   * Showing simple count down dialog for user preparation purposes before starting collecting data
   * via {@link MotionRecordingService}
   */
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

  /**
   * Adds vibration within start of collection/learning process to inform user Removes UI indication
   * when process stopped
   */
  public void startRecording() {
    this.indicateRepProcessing(true); //To show animation as soon as possible
    if (mService.startRepRecording(selectedExercise)) {
      Utilities.vibrate(getActivity(), null);
    } else {
      this.indicateRepProcessing(false);
    }
  }

  /**
   * Uses simple indeterminate circular progress bar around button to indicate ongoing process
   *
   * @param processing show/hide indication
   */
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

  /**
   * finished recording process and updates UID
   *
   * @param showRepCountInput possibility to use deprecated {@link NumberPickerDialog} for number of
   * reps input
   */
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

  /**
   * Implementation for value from {@link NumberPickerDialog}
   */
  @Override
  public void onValueChange(NumberPicker numberPicker, int i, int i1) {
    //AFTER NUMBER PICKER DIALOG
    if (mServiceBound) {
      mService.setRepsForFinishedRecording(numberPicker.getValue());
    }
  }

  /**
   * Provides types of exercise within bottom menu
   *
   * @return list of types of exercises
   */
  @Override
  public List<String> getActionMenu(Resources resources) {
    List<String> list =
        new ArrayList<>(Arrays.asList(resources.getStringArray(R.array.movements)));
    list.add(resources.getString(R.string.custom));
    return list;
  }

  /**
   * Implementation for bottom menu and its' functionality
   *
   * @param menuItem selected menu item
   * @return consumed event
   */
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

  /**
   * Implementation for supporting speach recognition within custom menu item allowing users'
   * personalized exercise name
   */
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

  /**
   * Functionality for destroying connection between application and {@link MotionRecordingService}
   */
  private void endTask() {
    if (mServiceBound) {
      getActivity().unbindService(mServiceConnection);
      mServiceBound = false;
    }
  }

  /**
   * Support for ambient mode
   */
  @Override
  public void onEnterAmbient(Bundle bundle) {
    mTitle.getPaint().setAntiAlias(false);
  }

  /**
   * Support for ambient mode
   */
  @Override
  public void onExitAmbient() {
    mTitle.getPaint().setAntiAlias(true);
  }

  /**
   * Support for ambient mode
   */
  @Override
  public void onUpdateAmbient() {
  }
}
