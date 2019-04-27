package com.hlavackamartin.fitnessapp.recognition.provider;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import java.util.Collections;
import java.util.Map;

public class InputStreamVolleyRequest extends Request<byte[]> {

  private final Response.Listener<byte[]> mListener;

  private Map<String, String> responseHeaders;

  public InputStreamVolleyRequest(int method, String mUrl, Listener<byte[]> listener,
      ErrorListener errorListener) {
    super(method, mUrl, errorListener);
    // this request would never use cache.
    setShouldCache(false);
    mListener = listener;
  }

  @Override
  protected Map<String, String> getParams() {
    return Collections.emptyMap();
  }

  public Map<String, String> getResponseHeaders() {
    return responseHeaders;
  }

  @Override
  protected void deliverResponse(byte[] response) {
    mListener.onResponse(response);
  }

  @Override
  protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
    //Initialise local responseHeaders map with response headers received
    responseHeaders = response.headers;
    //Pass the response data here
    return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
  }
}
