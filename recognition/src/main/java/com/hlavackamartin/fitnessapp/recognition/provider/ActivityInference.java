package com.hlavackamartin.fitnessapp.recognition.provider;

import android.content.Context;
import android.os.Environment;

import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class ActivityInference {
	
	static {
		System.loadLibrary("tensorflow_inference");
	}

	private static ActivityInference activityInferenceInstance;
	
	private TensorFlowInferenceInterface inferenceInterface;
	private static final String INPUT_NODE = "input";
	private static final String[] OUTPUT_NODES = {"y_"};
	private static final String OUTPUT_NODE = "y_";
	private static final long[] INPUT_SIZE = {1,270};
	
	private final String MODEL_FILE;
	private final int OUTPUT_SIZE;

	public static ActivityInference getInstance(final Context context) {
		if (activityInferenceInstance == null) {
			activityInferenceInstance = new ActivityInference(context);
		}
		return activityInferenceInstance;
	}

	private ActivityInference(final Context context) {
		MODEL_FILE = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) +
			context.getString(R.string.download_file);
		OUTPUT_SIZE = Utilities.readRecognitionLabels(context).size();
		inferenceInterface = new TensorFlowInferenceInterface(context.getAssets(), MODEL_FILE);
	}

	public float[] getActivityProb(float[] inputSignal) {
		float[] result = new float[OUTPUT_SIZE];
		inferenceInterface.feed(INPUT_NODE,inputSignal,INPUT_SIZE);
		inferenceInterface.run(OUTPUT_NODES);
		inferenceInterface.fetch(OUTPUT_NODE,result);
		return result;
	}
}
