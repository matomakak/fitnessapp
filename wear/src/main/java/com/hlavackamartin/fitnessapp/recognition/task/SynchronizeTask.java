package com.hlavackamartin.fitnessapp.recognition.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.PowerManager;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import com.hlavackamartin.fitnessapp.recognition.provider.InputStreamVolleyRequest;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

/**
 * Class providing functionality for synchronization purposes including upload and download
 * possibility
 */
public class SynchronizeTask {

  private Context context;
  private PowerManager.WakeLock mWakeLock;
  private ProgressDialog dialog;
  private SyncActionType type;
  /**
   * Queue storing current requests made
   */
  private RequestQueue mRequestQueue;
  private int mRequestQueueCount;


  public SynchronizeTask(Context activity, ProgressDialog dialog, SyncActionType type) {
    this.context = activity;
    this.dialog = dialog;
    this.type = type;
  }

  /**
   * Triggering of execution process for either upload or download functionality
   */
  public void execute() {
    onPreExecute();
    switch (type) {
      case UPLOAD:
        File file;
        try {
          file = Utilities.getFile(context, context.getString(R.string.upload_file));
        } catch (FileNotFoundException e) {
          onPostExecute("No file to upload found");
          return;
        }
        if (file.length() == 0) {
          onPostExecute("No data to upload");
          return;
        }
        uploadData(file);
        break;
      case DOWNLOAD:
        downloadData();
        break;
      default:
        onPostExecute("Bad action type");
        break;
    }
  }

  /**
   * Connects to endpoint specified in string resources and uploads recorded data from learning
   * module
   *
   * @param file recorded data from learning module
   */
  private void uploadData(File file) {
    mRequestQueue = Volley.newRequestQueue(context);
    StringRequest postRequest = new StringRequest(Request.Method.POST,
        context.getString(R.string.upload_url),
        response -> onPostExecute(null),
        error -> onPostExecute(error.toString())) {
      @Override
      public byte[] getBody() {
        try {
          return FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
          return null;
        }
      }
    };
    mRequestQueueCount = 1;
    mRequestQueue.add(postRequest);
  }

  /**
   * Connects to endpoint specified in string resources and download data. Download first support
   * file for neural network and then trained neural network file itself.
   */
  private void downloadData() {
    InputStreamVolleyRequest labelsRequest = new InputStreamVolleyRequest(Request.Method.GET,
        context.getString(R.string.download_url_labels),
        response -> {
          String error = null;
          try {
            if (response != null) {
              FileOutputStream outputStream = new FileOutputStream(
                  Utilities.getFile(context, context.getString(R.string.download_labels)));
              outputStream.write(response);
              outputStream.close();
            }
          } catch (Exception e) {
            error = e.toString();
          }
          onPostExecute(error);
        }, error -> onPostExecute(error.toString()));

    //DETECTION TF FILE
    InputStreamVolleyRequest tfRequest = new InputStreamVolleyRequest(Request.Method.GET,
        context.getString(R.string.download_url_data),
        response -> {
          String error = null;
          try {
            if (response != null) {
              FileOutputStream outputStream = new FileOutputStream(
                  Utilities.getFile(context, context.getString(R.string.download_file)));
              outputStream.write(response);
              outputStream.close();
            }
          } catch (Exception e) {
            error = e.toString();
          }
          onPostExecute(error);
        }, error -> onPostExecute(error.toString()));
    tfRequest.setRetryPolicy(new RetryPolicy() {
      @Override
      public int getCurrentTimeout() {
        return 50000;
      }

      @Override
      public int getCurrentRetryCount() {
        return 50000;
      }

      @Override
      public void retry(VolleyError error) throws VolleyError {

      }
    });

    mRequestQueue = Volley.newRequestQueue(context, new HurlStack());
    mRequestQueueCount = 2;
    mRequestQueue.add(labelsRequest);
    mRequestQueue.add(tfRequest);
  }

  /**
   * For synchronization purposes requires wake lock to prevent device going to sleep
   */
  private void onPreExecute() {
    mRequestQueueCount = 0;
    // take CPU lock to prevent CPU from going off if the user
    // presses the power button during download
    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
    mWakeLock.acquire();
    dialog.show();
  }

  /**
   * releases wake lock and finishes whole synchronization process
   *
   * @param errorMsg empty if successful
   */
  private void onPostExecute(String errorMsg) {
    if (--mRequestQueueCount < 1 || errorMsg != null) {
      if (mRequestQueueCount > 0) {
        mRequestQueue.cancelAll(request -> true);
      }

      mWakeLock.release();
      dialog.dismiss();
      if (errorMsg != null) {
        Toast.makeText(context, context.getString(R.string.sync_fail) + errorMsg,
            Toast.LENGTH_LONG)
            .show();
      } else {
        Toast.makeText(context, R.string.sync_succes, Toast.LENGTH_SHORT).show();
      }
    }
  }

  /**
   * Type of synchronization process
   */
  public enum SyncActionType {
    DOWNLOAD,
    UPLOAD
  }
}
