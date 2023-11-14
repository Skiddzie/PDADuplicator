package com.zebra.android.devdemo.storedformat;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.zebra.android.devdemo.R;
import com.zebra.android.devdemo.util.UIHelper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.FieldDescriptionData;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;

public class DisplayFieldsActivity extends Activity {

    private List<FieldDescriptionData> variablesList = new ArrayList<>();
    private List<EditText> variableValues = new ArrayList<>();
    private boolean bluetoothSelected;
    private String macAddress;
    private String tcpAddress = readCsvValue(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/csv.txt", 1, 0);
    private String tcpPort = readCsvValue(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/csv.txt", 1, 1);
    private String formatName = readCsvValue(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/csv.txt", 1, 2);
    private UIHelper helper = new UIHelper(this);
    private Connection connection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stored_format_variables); // Replace with your actual layout file

        // Read CSV values once during onCreate
        readCsvFile();

        final Button printButton = (Button) findViewById(R.id.printFormatButton);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    public void run() {
                        Looper.prepare();
                        openConnection(); // Open the connection

                        // Print and handle any exceptions
                        try {
                            printFormat();
                        } catch (Exception e) {
                            Log.e("ERROR", "Error printing: " + e.getMessage(), e);
                        } finally {
                            closeConnection(); // Close the connection regardless of success or failure
                            Looper.loop();
                            Looper.myLooper().quit();
                        }
                    }
                }).start();
            }
        });

        // Move the retrieval of variables outside the button click listener
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                getVariables();
                Looper.loop();
                Looper.myLooper().quit();
            }
        }).start();
    }


    private void openConnection() {
        connection = getPrinterConnection();
        if (connection != null) {
            try {
                connection.open();
            } catch (ConnectionException e) {
                Log.e("ERROR", "Error opening connection: " + e.getMessage(), e);
            }
        }
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (ConnectionException e) {
                Log.e("ERROR", "Error closing connection: " + e.getMessage(), e);
            }
        }
    }

    private void readCsvFile() {
        String csvFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/csv.txt";
        tcpAddress = readCsvValue(csvFilePath, 1, 0);
        tcpPort = readCsvValue(csvFilePath, 1, 1);
        formatName = readCsvValue(csvFilePath, 1, 2);
        Log.d("CSV", "TCP Address: " + tcpAddress);
        Log.d("CSV", "TCP Port: " + tcpPort);
        Log.d("CSV", "Format Name: " + formatName);
    }
    private Connection getPrinterConnection() {
        Log.d("CONNECTION", "ADDRESS: "+tcpAddress);
        Log.d("CONNECTION", "PORT: "+tcpPort);
        Log.d("CONNECTION", "FORMAT: "+formatName);
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
    protected void getVariables() {
        helper.showLoadingDialog("Retrieving variables...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    connection = getPrinterConnection();

                    if (connection != null) {
                        connection.open();

                        ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
                        if (printer != null) {
                            byte[] formatContents = printer.retrieveFormatFromPrinter(formatName);
                            if (formatContents != null) {
                                FieldDescriptionData[] variables = printer.getVariableFields(new String(formatContents, "utf8"));

                                for (int i = 0; i < variables.length; i++) {
                                    variablesList.add(variables[i]);
                                }
                                updateGuiWithFormats();
                            } else {
                                Log.e("ERROR", "Format contents are null");
                            }
                        } else {
                            Log.e("ERROR", "Zebra printer instance is null");
                        }

                        connection.close();
                    } else {
                        Log.e("ERROR", "Connection is null");
                    }
                } catch (ConnectionException e) {
                    Log.e("ERROR", "ConnectionException: " + e.getMessage(), e);
                    helper.showErrorDialogOnGuiThread(e.getMessage());
                } catch (ZebraPrinterLanguageUnknownException e) {
                    Log.e("ERROR", "ZebraPrinterLanguageUnknownException: " + e.getMessage(), e);
                    helper.showErrorDialogOnGuiThread(e.getMessage());
                } catch (UnsupportedEncodingException e) {
                    Log.e("ERROR", "UnsupportedEncodingException: " + e.getMessage(), e);
                    helper.showErrorDialogOnGuiThread(e.getMessage());
                } catch (Exception e) {
                    Log.e("ERROR", "Unexpected exception: " + e.getMessage(), e);
                } finally {
                    helper.dismissLoadingDialog();
                }
            }
        }).start();
    }

    public static String readCsvValue(String filePath, int rowIndex, int columnIndex) {
        try {
            FileReader fileReader = new FileReader(filePath);
            CSVReader csvReader = new CSVReader(fileReader);

            // Read all rows from the CSV file
            String[] nextRecord = new String[0];
            for (int i = 0; i <= rowIndex; i++) {
                nextRecord = csvReader.readNext();
                if (nextRecord == null) {
                    // The CSV file doesn't have enough rows
                    return null;
                }
            }

            // Check if the specified column exists
            if (columnIndex < nextRecord.length) {
                return nextRecord[columnIndex];
            } else {
                // The CSV file doesn't have enough columns in the specified row
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    protected void printFormat() {
        helper.showLoadingDialog("Printing...");
        connection = getPrinterConnection();

        if (connection != null) {
            try {
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
            } catch (ConnectionException e) {
                helper.showErrorDialogOnGuiThread(e.getMessage());
            } catch (ZebraPrinterLanguageUnknownException | UnsupportedEncodingException e) {
                helper.showErrorDialogOnGuiThread(e.getMessage());
            }
        }

        helper.dismissLoadingDialog();
    }
    private void updateGuiWithFormats() {
        runOnUiThread(new Runnable() {
            public void run() {
                TableLayout varTable = (TableLayout) findViewById(R.id.variablesTable); // Replace with your actual TableLayout ID

                for (int i = 0; i < variablesList.size(); i++) {
                    TableRow aRow = new TableRow(DisplayFieldsActivity.this);
                    aRow.setLayoutParams(new TableRow.LayoutParams(
                            ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    TextView varName = new TextView(DisplayFieldsActivity.this);

                    FieldDescriptionData var = variablesList.get(i);
                    varName.setText(var.fieldName == null ? "Field " + var.fieldNumber : var.fieldName);
                    varName.setLayoutParams(new TableRow.LayoutParams(
                            ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    aRow.addView(varName);

                    EditText value = new EditText(DisplayFieldsActivity.this);
                    value.setLayoutParams(new TableRow.LayoutParams(
                            ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    variableValues.add(value);
                    aRow.addView(value);

                    varTable.addView(aRow, new TableLayout.LayoutParams(
                            ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
            }
        });
    }
}
