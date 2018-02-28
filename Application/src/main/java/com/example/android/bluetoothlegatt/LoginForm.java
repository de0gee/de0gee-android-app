package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginForm extends Activity {
    private final static String TAG = LoginForm.class.getSimpleName();

    private Button mLoginButton;
    private EditText mLoginEmail;
    private EditText mLoginPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_form);
        mLoginEmail   = (EditText)findViewById(R.id.login_email);
        mLoginPassword   = (EditText)findViewById(R.id.login_password);

        mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG,mLoginEmail.getText().toString());
                Toast.makeText(LoginForm.this,R.string.login_incorrect,Toast.LENGTH_SHORT).show();
                Intent intent = DeviceScanActivity.newIntent(LoginForm.this, mLoginEmail.getText().toString(), mLoginPassword.getText().toString());
                startActivity(intent);
            }
        });
    }
}
