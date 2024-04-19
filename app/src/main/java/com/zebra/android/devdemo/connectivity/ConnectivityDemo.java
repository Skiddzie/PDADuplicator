/***********************************************
 * CONFIDENTIAL AND PROPRIETARY
 *
 * The source code and other information contained herein is the confidential and the exclusive property of
 * ZIH Corp. and is subject to the terms and conditions in your end user license agreement.
 * This source code, and any other information contained herein, shall not be copied, reproduced, published,
 * displayed or distributed, in whole or in part, in any medium, by any means, for any purpose except as
 * expressly permitted under such license agreement.
 *
 * Copyright ZIH Corp. 2012
 *
 * ALL RIGHTS RESERVED
 ***********************************************/



package com.zebra.android.devdemo.connectivity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.MacAddress;
import android.os.Environment;
import android.util.Log;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.zebra.android.devdemo.LoadDevDemo;
import com.zebra.android.devdemo.R;
import com.zebra.android.devdemo.sendfile.SendFileDemo;
import com.zebra.android.devdemo.storedformat.StoredFormatDemo;
import com.zebra.android.devdemo.storedformat.StoredFormatScreen;
import com.zebra.android.devdemo.storedformat.VariablesScreen;
import com.zebra.android.devdemo.util.DemoSleeper;
import com.zebra.android.devdemo.util.SettingsHelper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class ConnectivityDemo extends Activity {

    private Connection printerConnection;
    private RadioButton btRadioButton;
    private ZebraPrinter printer;
    private TextView statusField;
    private EditText macAddress, ipDNSAddress, portNumber;
    private Button testButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.connection_screen_with_status);


        ipDNSAddress = (EditText) this.findViewById(R.id.ipAddressInput);
        ipDNSAddress.setText(SettingsHelper.getIp(this));

        portNumber = (EditText) this.findViewById(R.id.portInput);
        portNumber.setText(SettingsHelper.getPort(this));

        macAddress = (EditText) this.findViewById(R.id.macInput);
        macAddress.setText(SettingsHelper.getBluetoothAddress(this));

        statusField = (TextView) this.findViewById(R.id.statusText);
        btRadioButton = (RadioButton) this.findViewById(R.id.bluetoothRadio);

        testButton = (Button) this.findViewById(R.id.testButton);
        testButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String pinText = ((EditText) findViewById(R.id.pinInput)).getText().toString();
                String savedPIN = getPIN();

                if (pinText.equals(savedPIN)) { // Assuming the default PIN is "1234"
                    // PIN is correct, proceed with the connection
                    new Thread(new Runnable() {
                        public void run() {
                            navigateToSendFileActivity();
                            connect();
                            enableTestButton(false);
                            doConnectionTest();
                        }
                    }).start();
                } else {
                    // PIN is incorrect, show error message
                    setStatus("Incorrect PIN", Color.RED);
                }
            }

        });
        RadioGroup radioGroup = (RadioGroup) this.findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.bluetoothRadio) {
                    toggleEditField(macAddress, true);
                    toggleEditField(portNumber, false);
                    toggleEditField(ipDNSAddress, false);
                } else {
                    toggleEditField(portNumber, true);
                    toggleEditField(ipDNSAddress, true);
                    toggleEditField(macAddress, false);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {

        //because i'm coding like a crazy person this also has to go in here
        //otherwise it'll infinitely switch back and forth between them
        Intent newIntent = new Intent(this, LoadDevDemo.class);
        startActivity(newIntent);

        Log.d("switch", "switching from ConnectivityDemo.java");
    }
    private String getPIN() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("PIN", "1234"); // "" is the default value if PIN is not found
    }

    private void savePIN(String pin) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("PIN", pin);
        editor.apply();
    }

    private void toggleEditField(EditText editText, boolean set) {
        /*
         * Note: Disabled EditText fields may still get focus by some other means, and allow text input.
         *       See http://code.google.com/p/android/issues/detail?id=2771
         */
        editText.setEnabled(set);
        editText.setFocusable(set);
        editText.setFocusableInTouchMode(set);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (printerConnection != null && printerConnection.isConnected()) {
            disconnect();
        }
    }

    private void enableTestButton(final boolean enabled) {
        runOnUiThread(new Runnable() {
            public void run() {
                testButton.setEnabled(enabled);
            }
        });
    }

    private boolean isBluetoothSelected() {
        return btRadioButton.isChecked();

    }

    public ZebraPrinter connect() {
        Log.d("ConnectivityDemo", "Connecting...");
        Log.d("crashlooker", "connect");
        //based on the comment below i'm pretty sure I accidentally deleted some code here
        //when copying from chatGPT
        // existing code...
        if (btRadioButton.isChecked()){
            printerConnection = new BluetoothConnection(getMacAddress());
        } else {
            int port = Integer.parseInt(getTcpPortNumber());
            printerConnection = new TcpConnection(getTcpAddress(), port);
            SettingsHelper.saveIp(this, getTcpAddress());
            SettingsHelper.savePort(this, getTcpPortNumber());
        }




        try {
            printerConnection.open();
            setStatus("Connected", Color.GREEN);
            Log.d("ConnectivityDemo", "Connected successfully");
        } catch (ConnectionException e) {
            setStatus("Comm Error! Disconnecting", Color.RED);
            DemoSleeper.sleep(1000);
            disconnect();
            Log.e("ConnectivityDemo", "ConnectionException: " + e.getMessage());
        }

        ZebraPrinter printer = null;

        if (printerConnection.isConnected()) {
            try {
                printer = ZebraPrinterFactory.getInstance(printerConnection);
                // existing code...
            } catch (ConnectionException e) {
                setStatus("Unknown Printer Language", Color.RED);
                printer = null;
                DemoSleeper.sleep(1000);
                disconnect();
                Log.e("ConnectivityDemo", "ConnectionException: " + e.getMessage());
            } catch (ZebraPrinterLanguageUnknownException e) {
                setStatus("Unknown Printer Language", Color.RED);
                printer = null;
                DemoSleeper.sleep(1000);
                disconnect();
                Log.e("ConnectivityDemo", "ZebraPrinterLanguageUnknownException: " + e.getMessage());
            }
        }

        return printer;
    }

    public void disconnect() {
        Log.d("crashlooker", "disconnect");
        try {
            setStatus("Disconnecting", Color.RED);
            if (printerConnection != null) {
                printerConnection.close();
            }
            setStatus("Not Connected", Color.RED);
        } catch (ConnectionException e) {
            setStatus("COMM Error! Disconnected", Color.RED);
        } finally {
            enableTestButton(true);
        }
    }

    private void setStatus(final String statusMessage, final int color) {
        Log.d("crashlooker", "setStatus");
        runOnUiThread(new Runnable() {
            public void run() {
                statusField.setBackgroundColor(color);
                statusField.setText(statusMessage);
            }
        });
        DemoSleeper.sleep(1000);
    }

    private String getMacAddressFieldText() {
        return macAddress.getText().toString();
    }

    //put this into a variable that can be shared with the other scripts
    public String getTcpAddress() {
        return ipDNSAddress.getText().toString();
    }

    public String getTcpPortNumber() {
        return portNumber.getText().toString();
    }
    public String getMacAddress() {
        return macAddress.getText().toString();
    }

    //added code for sending the port number and ip address over to sendfiledemo
    public void saveTcpAddress(String tcpAddress) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("tcpAddress", tcpAddress);
        editor.apply();
    }
    public void saveTcpPortNumber(String tcpPortNumber) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("tcpPortNumber", tcpPortNumber);
        editor.apply();
    }

    public void saveMacAddress(String macAddress) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("macAddress", macAddress);
        editor.apply();
    }

    protected String connectIP() {
        String IP = getTcpAddress();
        return IP;
    }
    protected String connectPort() {
        String port = getTcpPortNumber();
        return port;
    }



    private void navigateToSendFileActivity() {
        String tcpAddress = getTcpAddress();
        saveTcpAddress(tcpAddress);

        String tcpPortNumber = getTcpPortNumber();
        saveTcpPortNumber(tcpPortNumber);

        String macAddress = getMacAddress();
        saveMacAddress(macAddress);
        Log.d("preferences", "port: " + tcpPortNumber);
        Log.d("preferences", "ip: " + tcpAddress);
//        Log.d("preferences", "format: " + formatName);

        // Save PIN to SharedPreferences
        String pinText = ((EditText) findViewById(R.id.pinInput)).getText().toString();
//        savePINToPreferences(pinText);

        Intent storedFormatIntent = new Intent(this, StoredFormatDemo.class);
        storedFormatIntent.putExtra("tcpAddress", tcpAddress);
        storedFormatIntent.putExtra("tcpPortNumber", tcpPortNumber);
        storedFormatIntent.putExtra("macAddress", macAddress);
        startActivity(storedFormatIntent);
    }

    private void doConnectionTest() {
        printer = connect();
        if (printer != null) {
//            sendTestLabel();
//            idk if getting rid of these functions breaks anything, so it's getting a log!
            Log.d("connection","CONNECTED");
        } else {
            Log.d("connection","CONNECTION FAILED");
            disconnect();
        }
    }

    private void sendTestLabel() {
        try {
            byte[] configLabel = getConfigLabel();
            printerConnection.write(configLabel);
            setStatus("Sending Data", Color.BLUE);
            DemoSleeper.sleep(1500);
            if (printerConnection instanceof BluetoothConnection) {
                String friendlyName = ((BluetoothConnection) printerConnection).getFriendlyName();
                setStatus(friendlyName, Color.MAGENTA);
                DemoSleeper.sleep(500);
            }
        } catch (ConnectionException e) {
            setStatus(e.getMessage(), Color.RED);
        } finally {
            disconnect();
        }
    }

    /*
    * Returns the command for a test label depending on the printer control language
    * The test label is a box with the word "TEST" inside of it
    *
    * _________________________
    * |                       |
    * |                       |
    * |        TEST           |
    * |                       |
    * |                       |
    * |_______________________|
    *
    *
    */
    private byte[] getConfigLabel() {
        PrinterLanguage printerLanguage = printer.getPrinterControlLanguage();

        byte[] configLabel = null;
        if (printerLanguage == PrinterLanguage.ZPL) {
            configLabel = "^XA^FO17,16^GB379,371,8^FS^FT65,255^A0N,135,134^FDTEST^FS^XZ".getBytes();
        } else if (printerLanguage == PrinterLanguage.CPCL) {
            String cpclConfigLabel = "! 0 200 200 406 1\r\n" + "ON-FEED IGNORE\r\n" + "BOX 20 20 380 380 8\r\n" + "T 0 6 137 177 TEST\r\n" + "PRINT\r\n";
            configLabel = cpclConfigLabel.getBytes();
        }
        return configLabel;
    }


}
