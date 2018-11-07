package com.hlavackamartin.fitnessapp.recognition.fragment.impl;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.NumberPicker;


public class NumberPickerDialog extends DialogFragment {
	private NumberPicker.OnValueChangeListener valueChangeListener;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final NumberPicker numberPicker = new NumberPicker(getActivity());

		numberPicker.setMinValue(1);
		numberPicker.setMaxValue(20);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("How many reps");

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				valueChangeListener.onValueChange(numberPicker,
					numberPicker.getValue(), numberPicker.getValue());
			}
		});

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
