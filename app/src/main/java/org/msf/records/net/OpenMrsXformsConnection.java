package org.msf.records.net;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static org.msf.records.net.Constants.API_BASE;

/**
 * A connection to the Xform handling module we are adding to OpenMRS to provide xforms.
 * This is not part of OpenMrsServer as it has entirely it's own interface, but may be merged in
 * future.
 *
 * @author nfortescue@google.com
 */
public class OpenMrsXformsConnection {
    private static final String TAG = "OpenMrsXformsConnection";
    private static final String KNOWN_UUID = "d5bbf64a-69ba-11e4-8a42-47ebc7225440";

    private final VolleySingleton mVolley;

    public OpenMrsXformsConnection(Context context) {
        this.mVolley = VolleySingleton.getInstance(context.getApplicationContext());
    }

    /**
     * Get a single (full) Xform from the OpenMRS server
     * @param uuid the uuid of the form to fetch
     * @param resultListener the listener to be informed of the form asynchronously
     * @param errorListener a listener to be informed of any errors
     */
    public void getXform(String uuid, final Response.Listener<String> resultListener,
                          Response.ErrorListener errorListener) {
        Request request = new OpenMrsJsonRequest(
                Constants.LOCAL_ADMIN_USERNAME, Constants.LOCAL_ADMIN_PASSWORD,
                "http://"+ Constants.LOCALHOST_EMULATOR +":8080"+ API_BASE +"/xform/"+uuid+"?v=full",
                null, // null implies GET
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String xml = response.getString("xml");
                            resultListener.onResponse(xml);
                        } catch (JSONException e) {
                            // The result was not in the expected format. Just log, and return
                            // results so far.
                            Log.e(TAG, "response was in bad format: " + response, e);
                        }
                    }
                }, errorListener
        );
        mVolley.addToRequestQueue(request, TAG);
    }

    /**
     * List all xforms on the server, but not their contents.
     * @param listener a listener to be told about the index entries for all forms asynchronously.
     * @param errorListener a listener to be told about any errors.
     */
    public void listXforms(final Response.Listener<List<OpenMrsXformIndexEntry>> listener,
                           final Response.ErrorListener errorListener) {
        Request request = new OpenMrsJsonRequest(
                Constants.LOCAL_ADMIN_USERNAME, Constants.LOCAL_ADMIN_PASSWORD,
                "http://"+ Constants.LOCALHOST_EMULATOR +":8080"+ API_BASE +"/xform", // list all forms
                null, // null implies GET
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "got forms: " + response);
                        ArrayList<OpenMrsXformIndexEntry> result = new ArrayList<>();
                        try {
                            // This seems quite code heavy (parsing manually), but is reasonably
                            // efficient as we only look at the fields we need, so we are robust to
                            // changes in the rest of the object.
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject entry = results.getJSONObject(i);
                                OpenMrsXformIndexEntry indexEntry = new OpenMrsXformIndexEntry(
                                        entry.getString("uuid"),
                                        entry.getString("name"),
                                        entry.getLong("date_changed"));
                                result.add(indexEntry);
                            }
                        } catch (JSONException e) {
                            // The result was not in the expected format. Just log, and return
                            // results so far.
                            Log.e(TAG, "response was in bad format: " + response, e);
                        }
                        Log.i(TAG, "returning response: " + response);
                        listener.onResponse(result);
                    }
                },
                errorListener
        );
        mVolley.addToRequestQueue(request, TAG);
    }

    /**
     * Get a single (full) Xform from the OpenMRS server
     * @param uuid the uuid of the form to fetch
     * @param resultListener the listener to be informed of the form asynchronously
     * @param errorListener a listener to be informed of any errors
     */
    public void postXformInstance(
            String uuid,
            String xform,
            final Response.Listener<String> resultListener,
            Response.ErrorListener errorListener) {

        // The JsonObject members in the API as written at the moment.
        // int "patient_id"
        // int "enterer_id"
        // String "date_entered"
        // String "xml" the form.
        JsonObject post = new JsonObject();


        OpenMrsJsonRequest request = new OpenMrsJsonRequest(
                Constants.LOCAL_ADMIN_USERNAME, Constants.LOCAL_ADMIN_PASSWORD,
                "http://"+ Constants.LOCALHOST_EMULATOR +":8080"+ API_BASE +"/xform/"+uuid+"?v=full",
                null, // null implies GET
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String xml = response.getString("xml");
                            resultListener.onResponse(xml);
                        } catch (JSONException e) {
                            // The result was not in the expected format. Just log, and return
                            // results so far.
                            Log.e(TAG, "response was in bad format: " + response, e);
                        }
                    }
                }, errorListener
        );
        mVolley.addToRequestQueue(request, TAG);
    }

}