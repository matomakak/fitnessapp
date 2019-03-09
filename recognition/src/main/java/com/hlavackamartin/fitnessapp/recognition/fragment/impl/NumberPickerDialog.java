package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.NumberPicker;
import com.hlavackamartin.fitnessapp.recognition.R;


public class NumberPickerDialog extends DialogFragment {

  private NumberPicker.OnValueChangeListener valueChangeListener;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {

    final NumberPicker numberPicker = new NumberPicker(getActivity());

    numberPicker.setMinValue(1);
    numberPicker.setMaxValue(20);

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(R.string.rep_number);

    builder.setPositiveButton(R.string.ok, (dialog, which) -> valueChangeListener
        .onValueChange(numberPicker, numberPicker.getValue(), numberPicker.getValue()));

    builder.setView(numberPicker);
    return builder.create();
  }

  public NumberPicker.OnValueChangeListener getValueChangeListener() {
    return valueChangeListener;
  }

  public void setValueChangeListener(NumberPicker.OnValueChangeListener valueChangeListener) {
    this.valueChangeListener = valueChangeListener;
  }
}
