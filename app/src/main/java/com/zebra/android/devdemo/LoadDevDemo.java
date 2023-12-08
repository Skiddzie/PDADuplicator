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
import com.zebra.android.devdemo.sendfile.SendFileDemo;
import com.zebra.android.devdemo.sigcapture.SigCaptureDemo;
import com.zebra.android.devdemo.smartcard.SmartCardDemo;
import com.zebra.android.devdemo.status.PrintStatusDemo;
import com.zebra.android.devdemo.statuschannel.StatusChannelDemo;
import com.zebra.android.devdemo.storedformat.DisplayFieldsActivity;
import com.zebra.android.devdemo.storedformat.StoredFormatDemo;
import com.zebra.sdk.printer.FieldDescriptionData;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

public class LoadDevDemo extends ListActivity {
    private static final int SNDFILE_ID = 0;
    private static final int CONNECT_ID = 1;
    private static final int PIN_ID = 2;
    private static final int IMGPRNT_ID = 3;
    private static final int LSTFORMATS_ID = 4;
    private static final int MAGCARD_ID = 5;
    private static final int PRNTSTATUS_ID = 6;
    private static final int SMRTCARD_ID = 7;
    private static final int SIGCAP_ID = 8;

    private static final int STRDFRMT_ID = 9;
    private static final int STATUSCHANNEL_ID = 10;
    private static final int CONNECTIONBUILDER_ID = 11;
    private static final int RECEIPT_ID = 12;
    private static final int MULTICHANNEL_ID = 13;

    final boolean[] showSysOps = {false};
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("LoadDevDemo", "onCreate");
        setContentView(R.layout.main);

        createHistoryCsv();

        updateUI();
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        Log.d("LoadDevDemo", "onResume");
//        updateUI();
//    }

    @Override
    public void onBackPressed() {
        //nothing here means it's disabled
        //done to prevent accidentally making it back to the setup screen
    }

    private void updateUI() {
        String filePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt";
        Log.e("CSV", filePath);
        List<FieldDescriptionData> fieldsFromCSV = readFieldsFromCSV(filePath);
        Log.e("CSV", "updateUI");
        TextView bottomText = (TextView) findViewById(R.id.bottomText);
        int[] counter = {0};

        // Initialize the ListView with the appropriate adapter based on the initial state of showSysOps[0]
        updateListView(showSysOps[0]);

        // Set an OnClickListener for bottomText
        bottomText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter[0]++;
                if (counter[0] == 7) {
                    counter[0] = 0;
                    if (showSysOps[0]) {
                        showSysOps[0] = false;
                        updateListView(showSysOps[0]);
                        Toast.makeText(getApplicationContext(), "System Settings Hidden", Toast.LENGTH_SHORT).show();
                    } else {
                        showSysOps[0] = true;
                        updateListView(showSysOps[0]);
                        Toast.makeText(getApplicationContext(), "System Settings Visible", Toast.LENGTH_SHORT).show();
                    }
                }
                Log.d("hidden", String.valueOf(showSysOps[0]));
            }
        });

        // Check if there is CSV data available
        if (!fieldsFromCSV.isEmpty()) {
            FieldDescriptionData firstRow = fieldsFromCSV.get(0);

            // Read values from the first row
            String ipDisplay = readValueFromSecondRow(filePath, 0);
            String portDisplay = readValueFromSecondRow(filePath, 1);
            String formatDisplay = readValueFromSecondRow(filePath, 2);

            Log.d("CSV", "IP: " + ipDisplay + ", PORT: " + portDisplay + ", FORMAT: " + formatDisplay);

            // Update the TextView with the fetched values
            bottomText.setText("IP: " + ipDisplay + "\n" +"PORT: "+ portDisplay + "\n" + "FORMAT: " +formatDisplay);
        } else {
            Log.d("CSV", "No stored connection data.");
            bottomText.setText("SEI PDA Duplicator\nNo Format to Display");
        }
    }

    private void createHistoryCsv() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String todaysDate = dateFormat.format(new Date());
        String historyFilePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/history"+todaysDate+".txt";
        File historyCsvFile = new File(historyFilePath);

        // Check if the "history.txt" file already exists
        if (!historyCsvFile.exists()) {
            try {
                // Create a blank CSV file if it doesn't exist
                boolean created = historyCsvFile.createNewFile();

                if (created) {
                    Log.d("CSV", "Blank history.csv created successfully.");
                } else {
                    Log.d("CSV", "Failed to create blank history.csv.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String readValueFromSecondRow(String filePath, int columnIndex) {
        String value = null;
        try {
            FileReader fileReader = new FileReader(filePath);
            CSVReader csvReader = new CSVReader(fileReader);

            // Skip the first row
            csvReader.readNext();

            // Read the second row
            String[] secondRow = csvReader.readNext();

            // Check if the column index is valid
            if (secondRow != null && columnIndex >= 0 && columnIndex < secondRow.length) {
                value = secondRow[columnIndex];
            }

            csvReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static void main(String[] args) {
        // Replace "yourFilePath.csv" with the actual path to your CSV file
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/csv.txt";

        // Replace columnIndex with the index of the column you want to retrieve
        int columnIndex = 2;

        String value = readValueFromSecondRow(filePath, columnIndex);

        if (value != null) {
            System.out.println("Selected value: " + value);
        } else {
            System.out.println("Failed to read the value from the second row.");
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent;


        if (position == SNDFILE_ID) {
            String filePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt";
            File csvFile = new File(filePath);

            if (!csvFile.exists()) {
                // CSV file doesn't exist, show toast and break
                Toast.makeText(this, "No printer connected", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // Proceed with the intent based on the selected item
        switch (position) {
            case SNDFILE_ID:
                intent = new Intent(this, DisplayFieldsActivity.class);
                break;
            case CONNECT_ID:
                intent = new Intent(this, ConnectivityDemo.class);
                break;

            case PIN_ID:
                intent = new Intent(this, ChangePIN.class);
                break;
            // ... (remaining cases)
            default:
                return; // not possible
        }

        startActivity(intent);
    }

    // Update the adapter to dynamically hide/show items
    private void updateListView(boolean includeSysOpsItems) {
        String[] items;

        if (includeSysOpsItems) {
            // Include all items
            items = new String[] {
                    "Duplicate",
                    "Printer Setup",
                    "Change PIN"
            };
        } else {
            // Exclude CONNECT_ID and PIN_ID from the items list
            items = new String[] {
                    "Duplicate"
            };
        }

        // Set the updated adapter
        setListAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
    }

}
