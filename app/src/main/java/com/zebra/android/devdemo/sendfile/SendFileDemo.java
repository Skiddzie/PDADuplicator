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

package com.zebra.android.devdemo.sendfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.zebra.android.devdemo.ConnectionScreen;
import com.zebra.android.devdemo.connectivity.ConnectivityDemo;
import com.zebra.android.devdemo.storedformat.StoredFormatDemo;
import com.zebra.android.devdemo.storedformat.VariablesScreen;
import com.zebra.android.devdemo.util.SettingsHelper;
import com.zebra.android.devdemo.util.UIHelper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

public class SendFileDemo extends ConnectionScreen {

    private UIHelper helper = new UIHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        code written by chatgpt need to fix this shit later
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_send_file_demo);

        // Retrieve the values from the Intent
        Log.d("intent", "SendFileDemo.java");
        Intent intent = getIntent();
        if (intent != null) {
            String tcpAddress = intent.getStringExtra("tcpAddress");
            String tcpPortNumber = intent.getStringExtra("tcpPortNumber");

            // Now you can use tcpAddress and tcpPortNumber as needed
            if (tcpAddress != null && tcpPortNumber != null) {
                // Use the tcpAddress and tcpPortNumber values here in your SendFileDemo class
                // For example, log or display these values
                Log.d("SendFileDemo", "Received TCP Address: " + tcpAddress);
                Log.d("SendFileDemo", "Received TCP Port Number: " + tcpPortNumber);
            }
        } else {
            // Handle case where intent is null or extras are missing
        }

//        super.onCreate(savedInstanceState);
        testButton.setText("Send Test File");
    }

    public void onClick(View v) {
        // Redirect to VariablesScreen activity
        Log.d("lol", "onClick called");

    }

    public void performTest() {
        new Thread(new Runnable() {
            public void run() {
                sendFile();
                runOnUiThread(new Runnable() {
                    public void run() {
                        // Start the VariablesScreen activity

                    }
                });
            }
        }).start();
    }

    private String getStoredTcpAddress() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("tcpAddress", "default_value_if_not_found");
    }
    // Example usage:
    private void saveTcpAddressFromConnectivityDemo() {
        ConnectivityDemo connectivityDemo = new ConnectivityDemo(); // Create an instance of ConnectivityDemo
        String tcpAddress = connectivityDemo.getTcpAddress(); // Retrieve the TCP address
        connectivityDemo.saveTcpAddress(tcpAddress); // Save the TCP address to SharedPreferences
    }

    private void sendFile() {
        Connection connection = null;
        if (isBluetoothSelected() == true) {
            connection = new BluetoothConnection(getMacAddressFieldText());
        } else {
            try {
                int port = Integer.parseInt(getTcpPortNumber());
                connection = new TcpConnection(getStoredTcpAddress(), port);
            } catch (NumberFormatException e) {
                helper.showErrorDialogOnGuiThread("Port number is invalid");
                return;
            }
        }
        try {
            helper.showLoadingDialog("Sending file to printer ...");
            connection.open();
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
            testSendFile(printer);
            connection.close();
        } catch (ConnectionException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ZebraPrinterLanguageUnknownException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } finally {
            helper.dismissLoadingDialog();
        }
    }

    private void testSendFile(ZebraPrinter printer) {
        try {
            File filepath = getFileStreamPath("ABC.ZPL");
            createDemoFile(printer, "ABC.ZPL");
            printer.sendFileContents(filepath.getAbsolutePath());
            SettingsHelper.saveBluetoothAddress(this, getMacAddressFieldText());
            SettingsHelper.saveIp(this, getStoredTcpAddress());
            SettingsHelper.savePort(this, getTcpPortNumber());

        } catch (ConnectionException e1) {
            helper.showErrorDialogOnGuiThread("Error sending file to printer");
        } catch (IOException e) {
            helper.showErrorDialogOnGuiThread("Error creating file");
        }
    }

    private void createDemoFile(ZebraPrinter printer, String fileName) throws IOException {

        FileOutputStream os = this.openFileOutput(fileName, Context.MODE_PRIVATE);

        byte[] configLabel = null;

        PrinterLanguage pl = printer.getPrinterControlLanguage();
        if (pl == PrinterLanguage.ZPL) {
            configLabel = ("^XA^DFE:ABC.ZPL^FS\n" +
                                "\n" +
                                "~SD20\n" +
                                "\n" +
                                "^BY2,3,\n" +
                                "\n" +
                                "^FO60,20\n" +
                                "\n" +
                                "^BC,140,N,N,N,^FN1^FS\n" +
                                "^FO85,170\n" +
                                "^A0N,30,50^FN1^FS\n" +
                                "^XZ\n" +
                                "\n" +
                                "^XA^XFE:ABC.ZPL^FS\n" +
                                "^FN1^FD34^FS\n" +
                                "\n" +
                                "^PQ1^XZ\n").getBytes();
        } else if (pl == PrinterLanguage.CPCL) {
            String cpclConfigLabel = "! 0 200 200 406 1\r\n" + "ON-FEED IGNORE\r\n" + "BOX 20 20 380 380 8\r\n" + "T 0 6 137 177 TEST\r\n" + "PRINT\r\n";
            configLabel = cpclConfigLabel.getBytes();
        }
        os.write(configLabel);
        os.flush();
        os.close();
    }

}
