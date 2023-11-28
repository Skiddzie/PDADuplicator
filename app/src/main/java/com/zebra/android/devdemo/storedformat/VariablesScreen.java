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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.zebra.android.devdemo.LoadDevDemo;
import com.zebra.android.devdemo.R;
import com.zebra.android.devdemo.connectivity.ConnectivityDemo;
import com.zebra.android.devdemo.util.UIHelper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.FieldDescriptionData;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class VariablesScreen extends Activity {

    private boolean bluetoothSelected;
    private String macAddress;
    private String tcpAddress;
    private String tcpPort;
    private String formatName;
    private List<FieldDescriptionData> variablesList = new ArrayList<FieldDescriptionData>();
    private List<EditText> variableValues = new ArrayList<EditText>();
    private UIHelper helper = new UIHelper(this);
    private Connection connection;

    @Override
    //this is being ran when the format is selected, NOT when the page is printed
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Log.d("lol", "onCreate called");
//        setContentView(R.layout.notification);
//        Bundle b = getIntent().getExtras();
//        bluetoothSelected = b.getBoolean("bluetooth selected");
//        macAddress = b.getString("mac address");
//        tcpAddress = b.getString("tcp address");
//        tcpPort = b.getString("tcp port");
//        formatName = b.getString("format name");
//
//        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/csv.txt";
//        String stringPath = filePath.toString();
//        Log.d("ZPL","Format name: " + formatName);
//
//        Log.d("ZPL",filePath+" "+stringPath);
//        //this one changes the csv
//        modifyFormatValue(filePath,1,2,formatName);
//        TextView formatNameTextView = (TextView) this.findViewById(R.id.formatName);
//        formatNameTextView.setText(formatName);
//        Log.d("ZPL", "reading: "+readFieldsFromCSV(filePath));
//
//        showDialogAfterSelectingFormat();
//
//
////        final Button printButton = (Button) this.findViewById(R.id.printFormatButton);
//
////        printButton.setOnClickListener(new OnClickListener() {
////            public void onClick(View v) {
////                printButton.setEnabled(false);
////
////                new Thread(new Runnable() {
////                    public void run() {
////                        printFormat();
////
////                        runOnUiThread(new Runnable() {
////                            public void run() {
////                                printButton.setEnabled(true);
////                            }
////                        });
////                    }
////                }).start();
////            }
////        });
//
////        new Thread(new Runnable() {
////            public void run() {
////                Looper.prepare();
//////                getVariables();
////                Looper.loop();
////                Looper.myLooper().quit();
////            }
////        }).start();
//
//    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification);
        Bundle b = getIntent().getExtras();
        bluetoothSelected = b.getBoolean("bluetooth selected");
        macAddress = b.getString("mac address");
        tcpAddress = b.getString("tcp address");
        tcpPort = b.getString("tcp port");
        formatName = b.getString("format name");


        // Find the OK button in the layout
        Button okButton = (Button) findViewById(R.id.okButton);

        // Set a click listener for the OK button
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle OK button click (e.g., navigate to another activity)

                Intent intent = new Intent(VariablesScreen.this, LoadDevDemo.class);
                startActivity(intent); // Call the method to start the next activity

                finish(); // Close the current activity
            }
        });
    }
//    private void showDialogAfterSelectingFormat() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Format Selected");
//        builder.setMessage("You selected the format: " + formatName);
//        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                // Continue with your activity or perform any other necessary action
//                Intent intent = new Intent(VariablesScreen.this, LoadDevDemo.class);
//                startActivity(intent); // Call the method to start the next activity
//            }
//        });
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }


    //thank you computer for writing this code for me lol
    public static void modifyFormatValue(String file, int rowIndex, int columnIndex, String newValue) {
        try {
            FileReader fileReader = new FileReader(file);
            CSVReader csvReader = new CSVReader(fileReader);
            List<String[]> csvData = csvReader.readAll();

            if (rowIndex < csvData.size() && columnIndex < csvData.get(rowIndex).length) {
                csvData.get(rowIndex)[columnIndex] = newValue;

                csvReader.close();

                // Write back the modified content to the CSV file
                FileWriter fileWriter = new FileWriter(file);
                CSVWriter csvWriter = new CSVWriter(fileWriter);
                csvWriter.writeAll(csvData);
                csvWriter.close();
            } else {
                System.out.println("Invalid row or column index provided.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onBackPressed() {

        //this function keeps it from switching to the version that doesn't require a PIN.
        //not sure why that version opens when it's a different method from the one that displays
        //the formats, but it does.
        Intent newIntent = new Intent(this, ConnectivityDemo.class);
        startActivity(newIntent);

        Log.d("switch", "switching from VariablesScreen.java");
    }
    public static List<FieldDescriptionData> readFieldsFromCSV(String file) {
        List<FieldDescriptionData> fieldsFromCSV = new ArrayList<>();

        try {
            FileReader fileReader = new FileReader(file);
            CSVReader csvReader = new CSVReader(fileReader);
            String[] nextRecord;

            // Skip header if it exists
            csvReader.readNext(); // Skip the header row if it exists

            // Read each row and assume specific columns contain field data
            while ((nextRecord = csvReader.readNext()) != null) {
                // Check if there are enough elements in the array
                if (nextRecord.length >= 2) {
                    // Assuming column 0 contains field names and column 1 contains field numbers (modify as per your CSV structure)
                    String fieldName = nextRecord[0];
                    int fieldNumber = Integer.parseInt(nextRecord[1]); // Assuming it's an integer

                    // Create FieldDescriptionData object and add to the list
                    FieldDescriptionData field = new FieldDescriptionData(fieldNumber, fieldName);
                    fieldsFromCSV.add(field);
                } else {
                    System.out.println("Invalid row format: " + Arrays.toString(nextRecord));
                }
            }
            csvReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fieldsFromCSV;
    }
//    protected void getVariables() {
//        helper.showLoadingDialog("Retrieving variables...");
//        connection = getPrinterConnection();
//
//        if (connection != null) {
//            try {
//                connection.open();
//                ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
//
//                byte[] formatContents = printer.retrieveFormatFromPrinter(formatName);
//                FieldDescriptionData[] variables = printer.getVariableFields(new String(formatContents, "utf8"));
//
//                for (int i = 0; i < variables.length; i++) {
//                    variablesList.add(variables[i]);
//                }
//                connection.close();
//                updateGuiWithFormats();
//            } catch (ConnectionException e) {
//                helper.showErrorDialogOnGuiThread(e.getMessage());
//            } catch (ZebraPrinterLanguageUnknownException e) {
//                helper.showErrorDialogOnGuiThread(e.getMessage());
//            } catch (UnsupportedEncodingException e) {
//                helper.showErrorDialogOnGuiThread(e.getMessage());
//            }
//        }
//        helper.dismissLoadingDialog();
//    }

    protected void printFormat() {
        try {
            helper.showLoadingDialog("Printing...");
            connection = getPrinterConnection();

            if (connection != null) {
                connection.open();
                ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);

                // Read the CSV file to get the value from the 1st row and 3rd column

                // Check if the value is not empty before printing
                if (!formatName.isEmpty()) {
                    Map<Integer, String> vars = new HashMap<>();

                    for (int i = 0; i < variablesList.size(); i++) {
                        FieldDescriptionData var = variablesList.get(i);
                        vars.put(var.fieldNumber, variableValues.get(i).getText().toString());
                    }

                    // Use the value from the CSV file as the formatName
                    printer.printStoredFormat(formatName, vars, "utf8");
                    Log.d("ZPL", "format name: " + formatName);
                } else {
                    Log.e("ZPL", "Format name from CSV is empty");
                }

                connection.close();
            }
        } catch (ConnectionException e) {
            Log.e("ERROR", "Error printing: " + e.getMessage(), e);
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ZebraPrinterLanguageUnknownException | UnsupportedEncodingException e) {
            Log.e("ERROR", "Error printing: " + e.getMessage(), e);
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } finally {
            helper.dismissLoadingDialog();
        }
    }

    // Helper method to read a specific value from the CSV file
    private String readValueFromCSV(String file, int rowIndex, int columnIndex) {
        try {
            FileReader fileReader = new FileReader(file);
            CSVReader csvReader = new CSVReader(fileReader);
            List<String[]> csvData = csvReader.readAll();

            if (rowIndex < csvData.size() && columnIndex < csvData.get(rowIndex).length) {
                return csvData.get(rowIndex)[columnIndex];
            } else {
                System.out.println("Invalid row or column index provided.");
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private Connection getPrinterConnection() {
        if (bluetoothSelected == false) {
            try {
                int port = Integer.parseInt(tcpPort);
                connection = new TcpConnection(tcpAddress, port);
            } catch (NumberFormatException e) {
                helper.showErrorDialogOnGuiThread("Port number is invalid");
                return null;
            }
        } else {
            connection = new BluetoothConnection(macAddress);
        }
        return connection;
    }

    private void updateGuiWithFormats() {
        runOnUiThread(new Runnable() {
            public void run() {

                TableLayout varTable = (TableLayout) findViewById(R.id.variablesTable);

                for (int i = 0; i < variablesList.size(); i++) {
                    TableRow aRow = new TableRow(VariablesScreen.this);
                    aRow.setLayoutParams(new android.widget.TableRow.LayoutParams(android.widget.TableRow.LayoutParams.FILL_PARENT, android.widget.TableRow.LayoutParams.WRAP_CONTENT));

                    TextView varName = new TextView(VariablesScreen.this);

                    FieldDescriptionData var = variablesList.get(i);
                    varName.setText(var.fieldName == null ? "Field " + var.fieldNumber : var.fieldName);
                    varName.setLayoutParams(new android.widget.TableRow.LayoutParams(android.widget.TableRow.LayoutParams.FILL_PARENT, android.widget.TableRow.LayoutParams.WRAP_CONTENT));
                    aRow.addView(varName);

                    EditText value = new EditText(VariablesScreen.this);
                    value.setInputType(InputType.TYPE_TEXT_VARIATION_NORMAL | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                    value.setLayoutParams(new android.widget.TableRow.LayoutParams(android.widget.TableRow.LayoutParams.FILL_PARENT, android.widget.TableRow.LayoutParams.WRAP_CONTENT));
                    variableValues.add(value);
                    aRow.addView(value);

                    varTable.addView(aRow, new android.widget.TableLayout.LayoutParams(android.widget.TableRow.LayoutParams.FILL_PARENT, android.widget.TableRow.LayoutParams.WRAP_CONTENT));
                }

            }
        });
    }
}
