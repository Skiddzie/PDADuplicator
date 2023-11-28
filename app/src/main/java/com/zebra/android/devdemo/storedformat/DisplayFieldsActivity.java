package com.zebra.android.devdemo.storedformat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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
                new PrintFormatTask().execute();
            }
        });

        // Move the retrieval of variables outside the button click listener
        new GetVariablesTask().execute();

        // Automatically select the first EditText field and show the keyboard
        if (!variableValues.isEmpty()) {
            EditText firstEditText = variableValues.get(0);
            firstEditText.requestFocus();
            helper.showKeyboard(firstEditText);
        }
    }

    //dumbass, this is the duplicator section
//    @Override
//    public void onBackPressed() {
//
//        //this function keeps it from switching to the version that doesn't require a PIN.
//        //not sure why that version opens when it's a different method from the one that displays
//        //the formats, but it does.
//        Intent newIntent = new Intent(this, ConnectivityDemo.class);
//        startActivity(newIntent);
//
//        Log.d("switch", "switching from DisplayFieldsActivity.java");
//    }

    // AsyncTask for retrieving variables
    private class GetVariablesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            getVariables();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Update UI if needed after retrieving variables
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_TAB) {
            // Check if the current focused view is an EditText
            View focusedView = getCurrentFocus();
            if (focusedView instanceof EditText) {
                // If there's only one field, execute PrintFormatTask
                if (variableValues.size() == 1) {
                    new PrintFormatTask().execute();
                    return true; // consume the key event

                }

            }
        }
        //clears the field so that stuff doesn't start doubling up
        //not sure why placing it right after PrintFormatTask() breaks it, but it does
        EditText editTextToClear = variableValues.get(0);
        clearEditText(editTextToClear);
        return super.onKeyDown(keyCode, event);
    }
    private void clearEditText(EditText editText) {
        if (editText != null) {
            editText.setText("");
        }
    }
    private class PrintFormatTask extends AsyncTask<Void, Void, Void> {
        @SuppressLint("WrongThread")
        @Override
        protected Void doInBackground(Void... params) {
            try {
                connection = getPrinterConnection();
                if (connection != null) {
                    connection.open();
                    ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);

                    if (!formatName.isEmpty()) {
                        Map<Integer, String> vars = new HashMap<>();

                        for (int i = 0; i < variablesList.size(); i++) {
                            FieldDescriptionData var = variablesList.get(i);
                            vars.put(var.fieldNumber, variableValues.get(i).getText().toString());
                        }

                        printer.printStoredFormat(formatName, vars, "utf8");
                    } else {
                        Log.e("print", "format name from csv is empty");
                    }

                    connection.close();
                }
            } catch (ConnectionException e) {
                Log.e("print", "Error printing: " + e.getMessage(), e);
                helper.showErrorDialogOnGuiThread(e.getMessage());
            } catch (ZebraPrinterLanguageUnknownException | UnsupportedEncodingException e) {
                Log.e("print", "Error printing: " + e.getMessage(), e);
                helper.showErrorDialogOnGuiThread(e.getMessage());
            } finally {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        helper.dismissLoadingDialog();
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // This method is executed on the UI thread after the background operation is complete.
            // You can perform any UI updates here if needed.
        }
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
        formatName = readCsvValue(csvFilePath, 1, 2); // Update formatName from CSV
        Log.d("CSV", "TCP Address: " + tcpAddress);
        Log.d("CSV", "TCP Port: " + tcpPort);
        Log.d("CSV", "Format Name: " + formatName); // Log the updated formatName
    }

    private Connection getPrinterConnection() {
        Log.d("CONNECTION", "ADDRESS: "+tcpAddress);
        Log.d("CONNECTION", "PORT: "+tcpPort);
        Log.d("CONNECTION", "FORMAT: "+formatName);
        if (!bluetoothSelected) {
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
                //this is here to select the first field so the scanner can work without tapping on
                //the screen
                //needs to be in this function so that the fields are already loaded when it's called
                if (!variableValues.isEmpty()) {
                    EditText firstEditText = variableValues.get(0);
                    firstEditText.requestFocus();
                    helper.showKeyboard(firstEditText);
                }
            }
        });
    }
}
