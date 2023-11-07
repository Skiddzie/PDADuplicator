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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.zebra.android.devdemo.ConnectionScreen;

public class StoredFormatDemo extends ConnectionScreen {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        performTest();
        testButton.setText("Retrieve Formats");
    }

    @Override
    public void performTest() {
        String tcpAddress = getTcpAddress();
        String tcpPortNumber = getTcpPortNumber();

        Intent intent = new Intent(this, StoredFormatScreen.class);

        intent.putExtra("bluetooth selected", isBluetoothSelected());
        intent.putExtra("mac address", getMacAddressFieldText());
        intent.putExtra("tcpAddress", tcpAddress);
        intent.putExtra("tcpPortNumber", tcpPortNumber);

        Log.d("storedformat", "Received TCP Address: " + tcpAddress);
        Log.d("storedformat", "Received TCP Port Number: " + tcpPortNumber);

        startActivity(intent);
    }



}
