package com.hlavackamartin.fitnessapp.smartphone.provider;

import android.content.Context;
import com.hlavackamartin.fitnessapp.smartphone.R;
import com.hlavackamartin.fitnessapp.smartphone.data.Recognition;
import com.hlavackamartin.fitnessapp.smartphone.utils.Utilities;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.tensorflow.lite.Interpreter;

public class ActivityInference {

  private static ActivityInference activityInferenceInstance;
  private Interpreter tflite;
  private List<String> OUTPUT_LABELS;


  private ActivityInference(final Context context) {
    OUTPUT_LABELS = Utilities.readRecognitionLabels(context);
    File model;
    try {
      model = Utilities.getFile(context, context.getString(R.string.download_file));
    } catch (Exception ignored) {
      return;
    }
    tflite = new Interpreter(model);
  }

  public static ActivityInference getInstance(final Context context) {
    if (activityInferenceInstance == null) {
      activityInferenceInstance = new ActivityInference(context);
    }
    return activityInferenceInstance;
  }

  public List<Recognition> getActivityProb(float[][][][] inputSignal) {
    float[][] result = new float[1][OUTPUT_LABELS.size()];
    tflite.run(inputSignal, result);
    return getSortedResult(result);
  }

  private List<Recognition> getSortedResult(float[][] labelProbArray) {
    final ArrayList<Recognition> recognitions = new ArrayList<>();

    for (int i = 0; i < OUTPUT_LABELS.size(); ++i) {
      float confidence = labelProbArray[0][i];
      recognitions.add(new Recognition("" + i,
          OUTPUT_LABELS.size() > i ? OUTPUT_LABELS.get(i) : "unknown",
          confidence));
    }

    return recognitions;
  }
}
