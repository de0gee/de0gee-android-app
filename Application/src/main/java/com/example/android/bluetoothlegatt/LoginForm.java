package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class LoginForm extends AppCompatActivity {
    private final static String TAG = LoginForm.class.getSimpleName();

    private Button mLoginButton;
    private EditText mLoginEmail;
    private EditText mLoginPassword;
    private RequestQueue queue;
    private boolean mLoginSuccess;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_form);

        //request permissions
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        } else {
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }

        ActionBar ab = getSupportActionBar();
        queue = Volley.newRequestQueue(this);


        Log.d(TAG,"creating alarm");
        Intent ll24 = new Intent(this, AlarmReceiver.class);
        PendingIntent recurringLl24 = PendingIntent.getBroadcast(this, 0, ll24, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarms.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.currentThreadTimeMillis(),15000,recurringLl24);

        mLoginEmail = (EditText) findViewById(R.id.login_email);
        mLoginPassword = (EditText) findViewById(R.id.login_password);

        mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoginSuccess = false;
                Log.v(TAG, mLoginEmail.getText().toString());
                try {
                    String URL = Globals.SERVER_ADDRESS + "/login";
                    Log.d(TAG, "Attempting to connect to " + URL);
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("u", mLoginEmail.getText().toString());
                    jsonBody.put("p", mLoginPassword.getText().toString());
                    final String mRequestBody = jsonBody.toString();

                    StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if (response == "") {
                                response = "Incorrect login";
                            } else {
                                Intent intent = DeviceScanActivity.newIntent(LoginForm.this, mLoginEmail.getText().toString(), response);
                                response = "Welcome " + mLoginEmail.getText().toString();
                                startActivity(intent);
                            }
                            Toast.makeText(LoginForm.this, response, Toast.LENGTH_LONG).show();
                            Log.i("LOG_VOLLEY", response);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(LoginForm.this, error.toString(), Toast.LENGTH_LONG).show();
                            Log.e("LOG_VOLLEY", error.toString());
                        }
                    }) {
                        @Override
                        public String getBodyContentType() {
                            return "application/json; charset=utf-8";
                        }

                        @Override
                        public byte[] getBody() throws AuthFailureError {
                            try {
                                return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                            } catch (UnsupportedEncodingException uee) {
                                VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                                return null;
                            }
                        }

                        @Override
                        protected Response<String> parseNetworkResponse(NetworkResponse response) {
                            String responseString = "";
                            try {
                                JSONObject myObject = new JSONObject(new String(response.data));
                                Log.d(TAG, myObject.toString());
                                mLoginSuccess = ((boolean) myObject.get("success"));
                                if (mLoginSuccess == true) {
                                    responseString = myObject.get("message").toString();
                                    Log.d(TAG, myObject.get("message").toString());
                                } else {
                                    Log.w(TAG, myObject.get("message").toString());
                                }
                            } catch (Exception e) {
                                Log.w(TAG, e.toString());
                            }
                            return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                        }
                    };

                    queue.add(stringRequest);
                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        });
    }
}
