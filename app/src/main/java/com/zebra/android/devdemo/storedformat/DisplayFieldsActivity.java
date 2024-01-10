package com.zebra.android.devdemo.storedformat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
public class DisplayFieldsActivity extends Activity {

    private List<FieldDescriptionData> variablesList = new ArrayList<>();
    private List<EditText> variableValues = new ArrayList<>();
    private boolean bluetoothSelected;
    private String macAddress;
    private String tcpAddress;
    private String tcpPort;
    private String formatName;
    private UIHelper helper = new UIHelper(this);
    private Connection connection;

    // ... (existing code)

    private FileObserver fileObserver;

    // Broadcast receiver for detecting power connection
    private BroadcastReceiver powerConnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(Intent.ACTION_POWER_CONNECTED)) {
                // Phone is plugged in, trigger file transfer
                transferFileToComputer();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stored_format_variables); // Replace with your actual layout file

        // Read CSV values once during onCreate
        readCsvFile();
        // Initialize CSV values
        initializeCsvValues();

//        final EditText barcodeField = findViewById(R.id.barcodeField);
        Button scanBarcodeButton = findViewById(R.id.scanBarcodeButton);

        scanBarcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBarcodeScanner();
            }
        });



        final Button printButton = findViewById(R.id.printFormatButton);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PrintFormatTask() {
                    @Override
                    protected void onPostExecute(Void result) {
                        transferFileToComputer();
                    }
                }.execute();

            }
        });

        new GetVariablesTask().execute();

        // Automatically select the first EditText field and show the keyboard
        if (!variableValues.isEmpty()) {
            EditText firstEditText = variableValues.get(0);
            firstEditText.requestFocus();
            helper.showKeyboard(firstEditText);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop FileObserver and unregister the receiver when the activity is destroyed
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        unregisterReceiver(powerConnectedReceiver);
    }
    private void startBarcodeScanner() {
        Log.d("BarcodeScanner", "Starting barcode scanner...");
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Scan a barcode");
        integrator.setOrientationLocked(false);
        integrator.setBeepEnabled(true);
        integrator.setCaptureActivity(CustomScannerActivity.class); // CustomScannerActivity is a placeholder, replace with your implementation
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("BarcodeScanner", "onActivityResult called");
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                // Handle the scanned barcode result
                String scannedBarcode = result.getContents();
                Log.d("BarcodeScanner", "Scanned Barcode: " + scannedBarcode);

                // Set the scanned barcode to the currently focused EditText field
                View focusedView = getCurrentFocus();
                if (focusedView instanceof EditText) {
                    EditText editText = (EditText) focusedView;
                    editText.setText(scannedBarcode);
                }
            } else {
                // Handle when the scanning is canceled or failed
                Log.d("BarcodeScanner", "Scanning canceled");
                Toast.makeText(this, "Scanning canceled", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void initializeCsvValues() {
        // Initialize your CSV values here
        tcpAddress = readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 0);
        tcpPort = readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 1);
        formatName = readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 2);

        // Log the values for debugging
        Log.d("file", readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 0));
        Log.d("file", readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 1));
        Log.d("file", readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 2));


        Log.d("CSV", "TCP Address: " + tcpAddress);
        Log.d("CSV", "TCP Port: " + tcpPort);
        Log.d("CSV", "Format Name: " + formatName);
    }

    private void transferFileToComputer() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String todaysDate = dateFormat.format(new Date());
        String csvFilePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/history" + todaysDate + ".txt";

        File sourceFile = new File(csvFilePath);

        // Specify the destination directory on the computer (can be modified based on your needs)
        File destinationFile = new File(getFilesDir(), "history" + todaysDate + ".txt");
        Log.d("filetransfer", "Source File: " + sourceFile.getAbsolutePath());
        Log.d("filetransfer", "Destination File: " + destinationFile.getAbsolutePath());
        Toast.makeText(this, "File transfer called", Toast.LENGTH_SHORT).show();
        try {
            // Create FileInputStream for the source file
            FileInputStream inputStream = new FileInputStream(sourceFile);

            // Create OutputStream for the destination file on the computer
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            // Transfer bytes from the source file to the destination file
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // Close streams
            inputStream.close();
            outputStream.close();

            // Optionally, you can delete the source file after transfer
            // sourceFile.delete();

            // Log success or perform additional actions as needed
            Toast.makeText(this, "File transfer successful", Toast.LENGTH_SHORT).show();
            Log.d("filetransfer", "File transfer successful. Destination File: " + destinationFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (e.g., show an error message)
            Toast.makeText(this, "File transfer unsuccessful: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("filetransfer", "File transfer unsuccessful: " + e.getMessage());
        }
    }

    //dumbass, this is the duplicator section
    @Override
    public void onBackPressed() {

        //this function keeps it from switching to the version that doesn't require a PIN.
        //not sure why that version opens when it's a different method from the one that displays
        //the formats, but it does.
        Intent newIntent = new Intent(this, LoadDevDemo.class);
        startActivity(newIntent);

        Log.d("switch", "switching from DisplayFieldsActivity.java");
    }

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
                // If there's only one field and the CheckBox is checked, execute PrintFormatTask
                if (variableValues.size() == 1 && isPrintOnScanChecked()) {
                    new PrintFormatTask() {
                        @SuppressLint("StaticFieldLeak")
                        @Override
                        protected void onPostExecute(Void result) {
                            transferFileToComputer();
                        }
                    }.execute();

                    return true; // consume the key event
                }
            }
        }

        // Clear the field so that stuff doesn't start doubling up
        if (variableValues.size() == 1 && isPrintOnScanChecked()) {
            EditText editTextToClear = variableValues.get(0);
            clearEditText(editTextToClear);

        }
        if (variableValues.size() == 1){
            EditText firstBox = variableValues.get(0);
            firstBox.requestFocus();
        }



        return super.onKeyDown(keyCode, event);
    }

    private boolean isPrintOnScanChecked() {
        CheckBox printOnScanCheckBox = findViewById(R.id.printOnScan);
        return printOnScanCheckBox.isChecked();
    }

    private void clearEditText(EditText editText) {
        if (editText != null) {
            editText.setText("");
        }
    }

    private void addToHistoryCsv(List<String> values) {
        Log.d("history", "add history called");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String todaysDate = dateFormat.format(new Date());
        String csvFilePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/history"+todaysDate+".txt";
        try {

            // Open CSV file in append mode
            FileWriter fileWriter = new FileWriter(csvFilePath, true);
            CSVWriter csvWriter = new CSVWriter(fileWriter);

            // Write a new row to the CSV file
            csvWriter.writeNext(values.toArray(new String[0]));

            // Close CSV writer
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("history", "Error writing to history.csv: " + e.getMessage());
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
                // Get the values from the EditText fields
                List<String> fieldValues = new ArrayList<>();
                for (EditText editText : variableValues) {
                    fieldValues.add(editText.getText().toString());
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentTime = dateFormat.format(new Date());
                fieldValues.add(currentTime);
                Log.d("history", "field values: " + fieldValues);
                // Create a new row in the CSV file with the values and timestamp
                addToHistoryCsv(fieldValues);
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
//    private void openConnection() {
//        connection = getPrinterConnection();
//        if (connection != null) {
//            try {
//                connection.open();
//            } catch (ConnectionException e) {
//                Log.e("ERROR", "Error opening connection: " + e.getMessage(), e);
//            }
//        }
//    }
//
//    private void closeConnection() {
//        if (connection != null) {
//            try {
//                connection.close();
//            } catch (ConnectionException e) {
//                Log.e("ERROR", "Error closing connection: " + e.getMessage(), e);
//            }
//        }
//    }

    private void readCsvFile() {
        String csvFilePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt";
        tcpAddress = readCsvValue(csvFilePath, 1, 0);
        tcpPort = readCsvValue(csvFilePath, 1, 1);
        formatName = readCsvValue(csvFilePath, 1, 2);
        Log.d("CSV", "TCP Address: " + tcpAddress);
        Log.d("CSV", "TCP Port: " + tcpPort);
        Log.d("CSV", "Format Name: " + formatName);
    }

    private Connection getPrinterConnection() {
        Log.d("CONNECTION", "ADDRESS: " + tcpAddress);
        Log.d("CONNECTION", "PORT: " + tcpPort);
        Log.d("CONNECTION", "FORMAT: " + formatName);
        if (!bluetoothSelected) {
            try {
                int port = Integer.parseInt(tcpPort);
                connection = new TcpConnection(tcpAddress, port);
                connection.open(); // Open the connection here
            } catch (NumberFormatException e) {
                helper.showErrorDialogOnGuiThread("Port number is invalid");
                return null;
            } catch (ConnectionException e) {
                Log.e("ERROR", "Error opening connection: " + e.getMessage(), e);
                helper.showErrorDialogOnGuiThread("Error opening connection: " + e.getMessage());
                return null;
            }
        } else {
            connection = new BluetoothConnection(macAddress);
            // Handle Bluetooth connection logic here if needed
        }
        return connection;
    }
    protected void getVariables() {
        Log.d("DEBUG", "getVariables: start");
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

                                // Debug logs to check variablesList size
                                Log.d("DEBUG", "VariablesList size: " + variablesList.size());

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
                TableLayout varTable = findViewById(R.id.variablesTable); // Replace with your actual TableLayout ID

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

                // CheckBox
                CheckBox printOnScanCheckBox = findViewById(R.id.printOnScan);

                if (variablesList.size() == 1) {
                    // If there's only one form, make the CheckBox visible
                    printOnScanCheckBox.setVisibility(View.VISIBLE);
                } else {
                    // If there are multiple forms, make the CheckBox invisible
                    printOnScanCheckBox.setVisibility(View.GONE);
                }

                // Select the first EditText field
                if (!variableValues.isEmpty()) {
                    EditText firstEditText = variableValues.get(0);
                    firstEditText.requestFocus();
                    helper.showKeyboard(firstEditText);
                }
            }
        });
    }

}
