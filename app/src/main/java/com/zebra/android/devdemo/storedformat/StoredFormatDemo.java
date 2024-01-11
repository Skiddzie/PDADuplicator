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

package com.zebra.android.devdemo.storedformat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.content.SharedPreferences;
import com.zebra.android.devdemo.ConnectionScreen;
import com.zebra.android.devdemo.connectivity.ConnectivityDemo;

public class StoredFormatDemo extends ConnectivityDemo {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use the appropriate context (in this case, 'this' refers to the activity)
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        // Retrieve values from SharedPreferences
        String storedTcpAddress = sharedPreferences.getString("tcpAddress", "defaultTcpAddress");
        String storedTcpPortNumber = sharedPreferences.getString("tcpPortNumber", "defaultTcpPortNumber");
        String storedMacAddress = sharedPreferences.getString("macAddress", "defaultMacAddress");
        Log.d("IP", storedMacAddress);
        Log.d("IP", "Retrieved TCP Address: " + storedTcpAddress);
        Log.d("IP", "Retrieved TCP Port Number: " + storedTcpPortNumber);

        // Rest of your code
        performTest();
//        testButton.setText("Retrieve Formats");
    }
    @Override
    public void onBackPressed() {

        //this function keeps it from switching to the version that doesn't require a PIN.
        //not sure why that version opens when it's a different method from the one that displays
        //the formats, but it does.
        Intent newIntent = new Intent(this, ConnectivityDemo.class);
        startActivity(newIntent);

        Log.d("switch", "switching from StoredFormatDemo.java");

    }

    @Override
    public String getTcpAddress() {
        // Call the getTcpAddress method from the superclass (ConnectionScreen)
        return super.getTcpAddress();
    }
    @Override
    public String getTcpPortNumber(){
        return super.getTcpPortNumber();
    }

    //    @Override
    public void performTest() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        // this is pulling the ip and port number from connectivity demo
        String tcpAddress = sharedPreferences.getString("tcpAddress", "defaultTcpAddress");
        String tcpPortNumber = sharedPreferences.getString("tcpPortNumber", "defaultTcpPortNumber");
        String macAddress = sharedPreferences.getString("storedMacAddress", "defaultMacAddress");
        //this is where it's using the old ip and port
        Log.d("IP", tcpAddress);
        Log.d("IP", tcpPortNumber);

        Intent intent = new Intent(this, StoredFormatScreen.class);

//
//        intent.putExtra("bluetooth selected", isBluetoothSelected());
//        intent.putExtra("mac address", getMacAddressFieldText());
        intent.putExtra("tcpAddress", tcpAddress);
        intent.putExtra("tcpPortNumber", tcpPortNumber);
        intent.putExtra("macAddress", macAddress);

        Log.d("storedformat", "Received TCP Address: " + tcpAddress);
        Log.d("storedformat", "Received TCP Port Number: " + tcpPortNumber);
        Log.d("intentscreen", "switching to storedformatscreen");
        startActivity(intent);
    }



}
