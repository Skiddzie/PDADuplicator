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
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

public class LoadDevDemo extends ListActivity {

    private static final int CONNECT_ID = 0;
    private static final int SNDFILE_ID = 1;
    private static final int DISCO_ID = 2;
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

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
//        String dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
////        List<FieldDescriptionData> fieldsFromCSV = readFieldsFromCSV(dcimPath + "/csv.txt");
////        if (fieldsFromCSV.size() >= 2) {
//            // Get the second row values
////            FieldDescriptionData secondRowField1 = fieldsFromCSV.get(1);
////            String fieldName = secondRowField1.fieldName;
////            int fieldNumber = secondRowField1.fieldNumber;
//            String firstValue = readValueFromSecondRow(dcimPath, 1);
//            String secondValue = readValueFromSecondRow(dcimPath, 2);
//            Log.e("csv", "csv value: " + firstValue);
//            Log.e("csv", dcimPath);
//            // Update the TextView with the fetched values
//
//
//
//            TextView bottomText = (TextView) findViewById(R.id.bottomText);
//            bottomText.setText("Field Name: " + firstValue + "\nField Number: " + secondValue);
//        } else {
//            TextView bottomText = (TextView) findViewById(R.id.bottomText);
//            bottomText.setText("f");
//        }


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
        String filePath = "yourFilePath.csv";

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
        switch (position) {
        case CONNECT_ID:
            intent = new Intent(this, ConnectivityDemo.class);
            break;
        case DISCO_ID:
            intent = new Intent(this, DisplayFieldsActivity.class);
            break;
        case IMGPRNT_ID:
            intent = new Intent(this, ImagePrintDemo.class);
            break;
        case LSTFORMATS_ID:
            intent = new Intent(this, ListFormatsDemo.class);
            break;
        case MAGCARD_ID:
            intent = new Intent(this, MagCardDemo.class);
            break;
        case PRNTSTATUS_ID:
            intent = new Intent(this, PrintStatusDemo.class);
            break;
        case SMRTCARD_ID:
            intent = new Intent(this, SmartCardDemo.class);
            break;
        case SIGCAP_ID:
            intent = new Intent(this, SigCaptureDemo.class);
            break;
        case SNDFILE_ID:
            intent = new Intent(this, DisplayFieldsActivity.class);
            break;
        case STRDFRMT_ID:
            intent = new Intent(this, StoredFormatDemo.class);
            break;
        case STATUSCHANNEL_ID:
            intent = new Intent(this, StatusChannelDemo.class);
            break;
        case CONNECTIONBUILDER_ID:
            intent = new Intent(this, ConnectionBuilderDemo.class);
            break;
        case RECEIPT_ID:
            intent = new Intent(this, ReceiptDemo.class);
            break;
        case MULTICHANNEL_ID:
            intent = new Intent(this, MultiChannelDemo.class);
            break;
        default:
            return;// not possible
        }
        startActivity(intent);
    }

}
