package com.zebra.android.devdemo.ChangePIN;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
        oldPinInput = findViewById(R.id.oldPinInput);
        reenterPinInput = findViewById(R.id.reenterPinInput);
        newPinInput = findViewById(R.id.newPinInput);
        OKButton = findViewById(R.id.OKButton);

        // Set onClickListener for the OKButton
        OKButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Get values from the input fields
                String oldPin = oldPinInput.getText().toString();
                String reenterPin = reenterPinInput.getText().toString();
                String newPin = newPinInput.getText().toString();

                // Check if oldPin and reenterPin match
                Log.d("pin", oldPin + reenterPin);
                if (checkPinMatch(oldPin, reenterPin)) {
                    // If they match, update the PIN in SharedPreferences
                    savePIN(newPin);
                    Toast.makeText(ChangePIN.this, "PIN updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    // If they don't match, show an error message
                    Toast.makeText(ChangePIN.this, "Incorrect or non-matching PIN", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void savePIN(String pin) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("PIN", pin);
        editor.apply();
    }

    private boolean checkPinMatch(String oldPin, String reenterPin) {
        // Retrieve saved PIN from SharedPreferences
        String savedPIN = getSavedPIN();
        // Check if oldPin and reenterPin match the saved PIN
        Log.d("pin", "saved pin: " + savedPIN);
        Log.d("pin", String.valueOf(oldPin.equals(savedPIN))+" "+String.valueOf(reenterPin.equals(savedPIN)));
        return oldPin.equals(savedPIN) && reenterPin.equals(savedPIN);
    }

    private String getSavedPIN() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("PIN", "1234");
    }
}
