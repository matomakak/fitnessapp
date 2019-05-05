package com.hlavackamartin.fitnessapp.recognition.provider;

import android.content.Context;
import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.data.Recognition;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import org.tensorflow.lite.Interpreter;

/**
 * Providing functionality for communication with neural network base on Tensorflow implementation.
 * Implementing initialization, feeding data and resolving result from CNN.
 */
public class ActivityInference {

  private static final float THRESHOLD = 0.1f;
  private static ActivityInference activityInferenceInstance;
  private Interpreter tflite;
  private List<String> OUTPUT_LABELS;

  /**
   * Initialize neural network. Reads available types of exercises.
   */
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

  /**
   * Supports singleton principles
   */
  public static ActivityInference getInstance(final Context context) {
    if (activityInferenceInstance == null) {
      activityInferenceInstance = new ActivityInference(context);
    }
    return activityInferenceInstance;
  }

  /**
   * Feeds data to neural network and parses results
   *
   * @param inputSignal dataset for recognition purposes
   * @return list of recognized exercises and confidence in their validity
   */
  public List<Recognition> getActivityProb(float[][][][] inputSignal) {
    float[][] result = new float[1][OUTPUT_LABELS.size()];
    tflite.run(inputSignal, result);
    return getSortedResult(result);
  }

  /**
   * Parses 2D array to readable list of {@link Recognition} data
   *
   * @param labelProbArray 2D array of results straight from CNN
   * @return sorted and parsed data
   */
  private List<Recognition> getSortedResult(float[][] labelProbArray) {
    PriorityQueue<Recognition> pq = new PriorityQueue<>(OUTPUT_LABELS.size(),
        (lhs, rhs) -> Float.compare(rhs.getConfidence(), lhs.getConfidence())
    );

    for (int i = 0; i < OUTPUT_LABELS.size(); ++i) {
      float confidence = labelProbArray[0][i];
      if (confidence > THRESHOLD) {
        pq.add(new Recognition("" + i,
            OUTPUT_LABELS.size() > i ? OUTPUT_LABELS.get(i) : "unknown",
            confidence));
      }
    }

    final ArrayList<Recognition> recognitions = new ArrayList<>();
    int recognitionsSize = Math.min(pq.size(), OUTPUT_LABELS.size());
    for (int i = 0; i < recognitionsSize; ++i) {
      recognitions.add(pq.poll());
    }

    return recognitions;
  }
}
