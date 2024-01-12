package com.zebra.android.devdemo.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.zebra.android.devdemo.R;

public class Options extends Activity {
    private static Options instance;
    private SharedPreferences sharedPreferences;
    private CheckBox hideSetupCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options);

        hideSetupCheckbox = findViewById(R.id.hideSetupCheckbox);

        // Set the checkbox state from Options class
        setCheckboxState();

        // Handle checkbox changes
        handleCheckboxChanges();
    }

    public static Options getInstance(Context context) {
        if (instance == null) {
            instance = new Options();
            instance.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
        return instance;
    }

    public boolean isHideSetupChecked() {
        // Retrieve the checkbox state, default is false if not found
        return getSharedPreferences().getBoolean("isHidden", false);
    }

    public void setHideSetupChecked(boolean isChecked) {
        // Save the checkbox state
        getSharedPreferences().edit().putBoolean("isHidden", isChecked).apply();
    }

    // Function to set checkbox state based on XML layout
    public void setCheckboxState() {
        hideSetupCheckbox.setChecked(isHideSetupChecked());
    }

    // Function to handle checkbox changes and update the state
    public void handleCheckboxChanges() {
        hideSetupCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the checkbox state in Options class
            setHideSetupChecked(isChecked);
        });
    }

    private SharedPreferences getSharedPreferences() {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        }
        return sharedPreferences;
    }
}
