package com.hlavackamartin.fitnessapp.recognition.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.widget.Toast;

import com.hlavackamartin.fitnessapp.recognition.R;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class SynchronizeTask extends AsyncTask<Void, Integer, String> {
	
	private Context context;
	private PowerManager.WakeLock mWakeLock;
	private ProgressDialog dialog;
	private SyncActionType type;


	public SynchronizeTask(Context activity, ProgressDialog dialog, SyncActionType type) {
		this.context = activity;
		this.dialog = dialog;
		this.type = type;
	}

	@Override
	protected String doInBackground(Void... voids) {
		switch (type) {
			case UPLOAD:
				File file = new File(Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DOCUMENTS), context.getString(R.string.upload_file));
				dialog.setMax((int)file.length());
				return uploadData(file);
			case DOWNLOAD:
				dialog.setMax(100);
				return downloadData();
		}
		return "Bad type of action";
	}
	
	private String uploadData(File file) {
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection connection = null;
		String fileName = file.getName();
		try {
			connection = (HttpURLConnection) new URL(context.getString(R.string.upload_url)).openConnection();
			connection.setRequestMethod("POST");
			String boundary = "---------------------------boundary";
			String tail = "\r\n--" + boundary + "--\r\n";
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			connection.setDoOutput(true);

			String metadataPart = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"metadata\"\r\n\r\n"
				+ "" + "\r\n";
			String fileHeader1 = "--" + boundary + "\r\n"
				+ "Content-Disposition: form-data; name=\"uploadfile\"; filename=\""
				+ fileName + "\"\r\n"
				+ "Content-Type: application/octet-stream\r\n"
				+ "Content-Transfer-Encoding: binary\r\n";
			long fileLength = file.length() + tail.length();
			String fileHeader2 = "Content-length: " + fileLength + "\r\n";
			String fileHeader = fileHeader1 + fileHeader2 + "\r\n";
			String stringData = metadataPart + fileHeader;

			long requestLength = stringData.length() + fileLength;
			connection.setRequestProperty("Content-length", "" + requestLength);
			connection.setFixedLengthStreamingMode((int) requestLength);
			connection.connect();

			DataOutputStream out = new DataOutputStream(connection.getOutputStream());
			out.writeBytes(stringData);
			out.flush();

			int progress = 0;
			int bytesRead;
			byte buf[] = new byte[1024];
			BufferedInputStream bufInput = new BufferedInputStream(new FileInputStream(file));
			while ((bytesRead = bufInput.read(buf)) != -1) {
				// write output
				out.write(buf, 0, bytesRead);
				out.flush();
				progress += bytesRead;
				// update progress bar
				publishProgress(progress);
			}
			// Write closing boundary and close stream
			out.writeBytes(tail);
			out.flush();
			out.close();
			// Get server response
			/*BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line = "";
			StringBuilder builder = new StringBuilder();
			while((line = reader.readLine()) != null) {
				builder.append(line);
			}*/

		} catch (Exception e) {
			// Exception
		} finally {
			if (connection != null) connection.disconnect();
		}

		return null;
	}
	
	private String downloadData() {
		HttpURLConnection connection = null;
		try {
			URL url = new URL(context.getString(R.string.download_url_labels));
			connection = (HttpURLConnection) url.openConnection();
			connection.connect();
			OutputStream output1 = new FileOutputStream(new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
				context.getString(R.string.download_labels)));
			downloadProcess(connection,output1);
			
			url = new URL(context.getString(R.string.download_url_data));
			connection = (HttpURLConnection) url.openConnection();
			OutputStream output2 = new FileOutputStream(new File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
				context.getString(R.string.download_file)));
			downloadProcess(connection,output2);
			
		} catch (Exception e) {
			return e.toString();
		} finally {
			if (connection != null)
				connection.disconnect();
		}
		return null;
	}
	
	private void downloadProcess(HttpURLConnection connection, OutputStream output) throws Exception{
		if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
			throw new Exception("Server returned HTTP " + connection.getResponseCode()
				+ " " + connection.getResponseMessage());
		}
		// this will be useful to display download percentage
		// might be -1: server did not report the length
		int fileLength = connection.getContentLength();
		// download the file
		InputStream input = connection.getInputStream();

		byte data[] = new byte[4096];
		long total = 0;
		int count;
		while ((count = input.read(data)) != -1) {
			// allow canceling with back button
			if (isCancelled()) {
				input.close();
			}
			total += count;
			// publishing the progress....
			if (fileLength > 0) // only if total length is known
				publishProgress((int) (total * 100 / fileLength));
			output.write(data, 0, count);
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		// take CPU lock to prevent CPU from going off if the user 
		// presses the power button during download
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
			getClass().getName());
		mWakeLock.acquire();
		dialog.show();
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		super.onProgressUpdate(progress);
		// if we get here, length is known, now set indeterminate to false
		dialog.setIndeterminate(false);
		dialog.setProgress(progress[0]);
	}

	@Override
	protected void onPostExecute(String result) {
		mWakeLock.release();
		dialog.dismiss();
		if (result != null)
			Toast.makeText(context,context.getString(R.string.sync_fail)+result, Toast.LENGTH_LONG).show();
		else
			Toast.makeText(context, R.string.sync_succes, Toast.LENGTH_SHORT).show();
	}

	public enum SyncActionType {
		DOWNLOAD,
		UPLOAD
	}
}
