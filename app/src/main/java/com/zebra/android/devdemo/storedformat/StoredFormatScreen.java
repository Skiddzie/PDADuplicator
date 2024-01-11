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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.zebra.android.devdemo.R;
import com.zebra.android.devdemo.connectivity.ConnectivityDemo;
import com.zebra.android.devdemo.util.SettingsHelper;
import com.zebra.android.devdemo.util.UIHelper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class StoredFormatScreen extends ListActivity {

    private boolean bluetoothSelected;
    private String macAddress;
    private String tcpAddress;
    private String tcpPort;
    private ArrayAdapter<String> statusListAdapter;
    private List<String> formatsList = new ArrayList<String>();
    private UIHelper helper = new UIHelper(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.stored_format_demo);
        Bundle b = getIntent().getExtras();
        bluetoothSelected = b.getBoolean("bluetooth selected");
        macAddress = b.getString("mac address");
        //change this shit to the shit sent from connectivity demo
        tcpAddress = b.getString("tcpAddress");
        tcpPort = b.getString("tcpPortNumber");
        statusListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, formatsList);

        setListAdapter(statusListAdapter);
        new Thread(new Runnable() {

            public void run() {
                Looper.prepare();
                getFileList();
                Looper.loop();
                Looper.myLooper().quit();
            }
        }).start();

    }
    @Override
    public void onBackPressed() {

        //this function keeps it from switching to the version that doesn't require a PIN.
        //not sure why that version opens when it's a different method from the one that displays
        //the formats, but it does.
        Log.d("intentscreen", "switching to storedformatdemo");
        Intent newIntent = new Intent(this, ConnectivityDemo.class);
        Log.d("intentscreen", "switching to connectivitydemo");
        startActivity(newIntent);

        Log.d("switch", "switching from StoredFormatScreen.java");
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent intent;
        intent = new Intent(this, VariablesScreen.class);
        intent.putExtra("bluetooth selected", bluetoothSelected);
        intent.putExtra("mac address", macAddress);
        intent.putExtra("tcp address", tcpAddress);
        intent.putExtra("tcp port", tcpPort);
        intent.putExtra("format name", (String) l.getItemAtPosition(position));

        // Add the following lines to write to CSV
        writeDataToCsv(tcpAddress, tcpPort, (String) l.getItemAtPosition(position));

        startActivity(intent);
    }

    // Add this method to write the selected data to CSV
    private void writeDataToCsv(String tcpAddress, String tcpPort, String formatName) {
        Log.e("formatwrite", formatName);
        try {
            String csvFilePath = String.valueOf(new File(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt"));

            Log.e("path", csvFilePath);
            File csvFile = new File(csvFilePath);

            // Read existing data
            List<String[]> existingData;
            if (csvFile.exists() && csvFile.length() > 0) {
                CSVReader reader = new CSVReader(new FileReader(csvFilePath));
                existingData = reader.readAll();
                reader.close();

                // Check if the second row exists
                if (existingData.size() >= 2) {
                    // Modify the format name in the second row
                    existingData.get(1)[2] = formatName;
                }
            } else {
                // If the file is empty or doesn't exist, create an empty list
                existingData = new ArrayList<>();
            }

            // If the second row doesn't exist, add a new row with the format name
            if (existingData.size() < 2) {
                // Add a new row with the format name
                String[] newRow = {tcpAddress, tcpPort, formatName};
                existingData.add(newRow);
            }

            // Write the updated data to the CSV file
            FileWriter fileWriter = new FileWriter(csvFilePath);
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            csvWriter.writeAll(existingData);

            // Close the writers
            csvWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private void getFileList() {
        Connection connection = null;
        if (bluetoothSelected == true) {
            Log.d("connection", "bluetooth");
            connection = new BluetoothConnection(macAddress);
        } else {
            try {
                Log.d("connection", "IP network");
                int port = Integer.parseInt(tcpPort);
                connection = new TcpConnection(tcpAddress, port);
            } catch (NumberFormatException e) {
                helper.showErrorDialogOnGuiThread("Port number is invalid");
                return;
            }

        }
        try {
            helper.showLoadingDialog("Retrieving Formats...");
            connection.open();
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
            PrinterLanguage pl = printer.getPrinterControlLanguage();
            String[] formatExtensions;

            if (pl == PrinterLanguage.ZPL) {
                formatExtensions = new String[] { "ZPL" };
            } else {
                formatExtensions = new String[] { "FMT", "LBL" };
            }

            String[] formats = printer.retrieveFileNames(formatExtensions);
            for (int i = 0; i < formats.length; i++) {
                formatsList.add(formats[i]);
            }
            connection.close();
            saveSettings();
            updateGuiWithFormats();
        } catch (ConnectionException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ZebraPrinterLanguageUnknownException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ZebraIllegalArgumentException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } finally {
            helper.dismissLoadingDialog();
        }
    }

    private void saveSettings() {
        SettingsHelper.saveBluetoothAddress(StoredFormatScreen.this, macAddress);
        SettingsHelper.saveIp(StoredFormatScreen.this, tcpAddress);
        SettingsHelper.savePort(StoredFormatScreen.this, tcpPort);
    }

    private void updateGuiWithFormats() {
        runOnUiThread(new Runnable() {
            public void run() {
                statusListAdapter.notifyDataSetChanged();
                Toast.makeText(StoredFormatScreen.this, "Found " + formatsList.size() + " Formats", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
