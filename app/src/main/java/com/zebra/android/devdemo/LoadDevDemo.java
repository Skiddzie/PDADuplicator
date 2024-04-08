package com.zebra.android.devdemo;

import static com.zebra.android.devdemo.storedformat.VariablesScreen.readFieldsFromCSV;

import com.zebra.android.devdemo.ChangePIN.ChangePIN;
import com.zebra.android.devdemo.connectionbuilder.ConnectionBuilderDemo;
import com.zebra.android.devdemo.connectivity.ConnectivityDemo;
import com.zebra.android.devdemo.discovery.DiscoveryDemo;
import com.zebra.android.devdemo.imageprint.ImagePrintDemo;
import com.zebra.android.devdemo.listformats.ListFormatsDemo;
import com.zebra.android.devdemo.magcard.MagCardDemo;
import com.zebra.android.devdemo.multichannel.MultiChannelDemo;
import com.zebra.android.devdemo.receipt.ReceiptDemo;
import com.zebra.android.devdemo.sendfile.FromPhone;
import com.zebra.android.devdemo.sendfile.SendFileDemo;
import com.zebra.android.devdemo.sigcapture.SigCaptureDemo;
import com.zebra.android.devdemo.smartcard.SmartCardDemo;
import com.zebra.android.devdemo.status.PrintStatusDemo;
import com.zebra.android.devdemo.statuschannel.StatusChannelDemo;
import com.zebra.android.devdemo.storedformat.DisplayFieldsActivity;
import com.zebra.android.devdemo.storedformat.StoredFormatDemo;
import com.zebra.android.devdemo.util.Options;

import com.zebra.sdk.printer.FieldDescriptionData;

import android.Manifest;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LoadDevDemo extends ListActivity {

    private static final int SNDFILE_ID = 0;
    private static final int CONNECT_ID = 1;
    private static final int PIN_ID = 2;
    private static final int OPTIONS_ID = 3;
    private static final int PHONE_ID = 4;
    private static final int MAGCARD_ID = 11;
    private static final int PRNTSTATUS_ID = 6;
    private static final int SMRTCARD_ID = 7;
    private static final int SIGCAP_ID = 8;

    private static final int STRDFRMT_ID = 9;
    private static final int STATUSCHANNEL_ID = 10;
    private static final int CONNECTIONBUILDER_ID = 5;
    private static final int RECEIPT_ID = 12;
    private static final int MULTICHANNEL_ID = 13;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 0;

    final boolean[] showSysOps = {false};
    private int clickCounter = 0;
    Options options;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("LoadDevDemo", "onCreate");
        setContentView(R.layout.main);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            }, REQUEST_BLUETOOTH_PERMISSION);
        }

        options = Options.getInstance(this.getApplicationContext());

        // Initialize showSysOps based on Options
        if (options != null) {
            if (options.isHideSetupChecked()) {
                showSysOps[0] = false;
            } else {
                showSysOps[0] = true;
            }
        } else {
            // Handle the case where getting the Options instance failed
            showSysOps[0] = false; // or any other default value
        }

        updateUI();
        updateListView(showSysOps[0]); // Update list view after updating UI
    }

    @Override
    public void onBackPressed() {
        //nothing here means it's disabled
        //done to prevent accidentally making it back to the setup screen
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            // Check if the permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with Bluetooth scanning
            } else {
                // Permission denied, handle accordingly
            }
        }
        // Handle other permission requests if needed
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void updateUI() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        String ipDisplay = sharedPreferences.getString("tcpAddress", "NULL");
        String portDisplay = sharedPreferences.getString("tcpPortNumber", "NULL");
        String formatDisplay = sharedPreferences.getString("formatName", "NULL");

        TextView bottomText = findViewById(R.id.bottomText);
        bottomText.setText("IP: " + ipDisplay + "\n" +"PORT: "+ portDisplay + "\n" + "FORMAT: " +formatDisplay);
        bottomText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickCounter++;
                if (clickCounter == 7) {
                    clickCounter = 0;
                    showSysOps[0] = !showSysOps[0];
                    updateListView(showSysOps[0]);
                    if (showSysOps[0]) {
                        Toast.makeText(getApplicationContext(), "System Settings Visible", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "System Settings Hidden", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = null;

        // ... (existing code)

        // Proceed with the intent based on the selected item
        switch (position) {
            case SNDFILE_ID:
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                String ipDisplay = sharedPreferences.getString("ip", "");
                if (!ipDisplay.isEmpty()) {
                    intent = new Intent(this, DisplayFieldsActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "No printer connected. Please select Printer Setup", Toast.LENGTH_SHORT).show();
                }
                break;
            case CONNECT_ID:
                Log.d("intentscreen", "switching to connectivitydemo");

                intent = new Intent(this, ConnectivityDemo.class);
                startActivity(intent);
                break;
            case PIN_ID:
                intent = new Intent(this, ChangePIN.class);
                startActivity(intent);
                break;
            case OPTIONS_ID:
                intent = new Intent(this, Options.class);
                startActivity(intent);
                break;
            case PHONE_ID:
                intent = new Intent(this, FromPhone.class);
                startActivity(intent);
                break;
            default:
                return; // not possible
        }

    }

    // Update the adapter to dynamically hide/show items
    private void updateListView(boolean includeSysOpsItems) {
        String[] items;

        if (includeSysOpsItems) {
            // Include all items
            items = new String[] {
                    "Duplicate",
                    "Printer Setup",
                    "Change PIN",
                    "System Options",
                    "Send Format to Printer"
            };
        } else {
            // Exclude CONNECT_ID and PIN_ID from the items list
            items = new String[] {
                    "Duplicate",
                    "Printer Setup"
            };
        }

        // Set the updated adapter
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
    }
}
