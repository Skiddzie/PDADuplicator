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

//    public static void modifyFormatValue(String file, int rowIndex, int columnIndex, String newValue) {
//        try {
//            FileReader fileReader = new FileReader(file);
//            CSVReader csvReader = new CSVReader(fileReader);
//            List<String[]> csvData = csvReader.readAll();
//
//            if (rowIndex < csvData.size() && columnIndex < csvData.get(rowIndex).length) {
//                csvData.get(rowIndex)[columnIndex] = newValue;
//
//                csvReader.close();
//
//                // Write back the modified content to the CSV file
//                FileWriter fileWriter = new FileWriter(file);
//                CSVWriter csvWriter = new CSVWriter(fileWriter);
//                csvWriter.writeAll(csvData);
//                csvWriter.close();
//            } else {
//                System.out.println("Invalid row or column index provided.");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Override
    public void onBackPressed() {
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
                    String fieldNumberStr = nextRecord[1];

                    // Check if the fieldNumberStr is not empty before parsing
                    if (!fieldNumberStr.isEmpty()) {
                        try {
                            int fieldNumber = Integer.parseInt(fieldNumberStr); // Assuming it's an integer

                            // Create FieldDescriptionData object and add to the list
                            FieldDescriptionData field = new FieldDescriptionData(fieldNumber, fieldName);
                            fieldsFromCSV.add(field);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid field number format: " + fieldNumberStr);
                        }
                    } else {
                        System.out.println("Field number is empty in row: " + Arrays.toString(nextRecord));
                    }
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

}