package com.hlavackamartin.fitnessapp.recognition.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.PowerManager;
import android.widget.Toast;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.hlavackamartin.fitnessapp.recognition.R;
import com.hlavackamartin.fitnessapp.recognition.Utilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.FileUtils;


public class SynchronizeTask {

  private Context context;
  private PowerManager.WakeLock mWakeLock;
  private ProgressDialog dialog;
  private SyncActionType type;


  public SynchronizeTask(Context activity, ProgressDialog dialog, SyncActionType type) {
    this.context = activity;
    this.dialog = dialog;
    this.type = type;
  }

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

  private void uploadData(File file) {
    RequestQueue queue = Volley.newRequestQueue(context);
    StringRequest postRequest = new StringRequest(Request.Method.POST,
        context.getString(R.string.upload_url),
        response -> onPostExecute(null),
        error -> onPostExecute(error.toString())) {
      @Override
      public byte[] getBody() throws AuthFailureError {
        try {
          return FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
          return null;
        }
      }
    };
    queue.add(postRequest);
  }

  private void downloadData() {
    return;
  }

  private void onPreExecute() {
    // take CPU lock to prevent CPU from going off if the user 
    // presses the power button during download
    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
    mWakeLock.acquire();
    dialog.show();
  }

  private void onPostExecute(String errorMsg) {
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

  public enum SyncActionType {
    DOWNLOAD,
    UPLOAD
  }
}
