package com.zebra.android.devdemo.ChangePIN;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.zebra.android.devdemo.R;

public class ChangePIN extends Activity {
    private EditText oldPinInput;
    private EditText reenterPinInput;
    private EditText newPinInput;
    private Button OKButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_change);

        // Initialize UI elements
        oldPinInput = (EditText) findViewById(R.id.oldPinInput);
        reenterPinInput = (EditText) findViewById(R.id.reenterPinInput);
        newPinInput = (EditText) findViewById(R.id.newPinInput);
        OKButton = (Button) findViewById(R.id.OKButton);

        // Set onClickListener for the OKButton
        OKButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Get values from the input fields
                String oldPin = oldPinInput.getText().toString();
                String reenterPin = reenterPinInput.getText().toString();
                String newPin = newPinInput.getText().toString();

                Log.d("pin", oldPin);
            }
        });
    }
}