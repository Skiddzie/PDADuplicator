package com.zebra.android.devdemo.sendfile;


import static com.zebra.android.devdemo.storedformat.StoredFormatScreen.PICK_FILE_REQUEST_CODE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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
import com.zebra.android.devdemo.storedformat.CustomScannerActivity;
import com.zebra.android.devdemo.storedformat.StoredFormatScreen;
import com.zebra.android.devdemo.util.SettingsHelper;
import com.zebra.android.devdemo.util.UIHelper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.FieldDescriptionData;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
public class FromPhone extends Activity {

    private static final int PICK_FILE_REQUEST_CODE = 1;
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
        setContentView(R.layout.send_format);
        Log.d("crashlooker", "FromPhone load");
        // Read CSV values once during onCreate
        readCsvFile();
        // Initialize CSV values
        initializeCsvValues();




        final Button printButton = findViewById(R.id.printFormatButton);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*"); // You can specify a MIME type here to filter the types of files to display
                final int yourRequestCode = 1;

                startActivityForResult(intent, yourRequestCode);

            }
        });

        //new com.zebra.android.devdemo.sendfile.FromPhone.GetVariablesTask().execute();

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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                String fileData = readFile(selectedFileUri);
                if (fileData != null) {
                    // Now you have the file data, send it to the printer
                    sendFileToPrinter(fileData);
                } else {
                    Toast.makeText(this, "Failed to read file data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Selected file URI is null", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private String getStoredTcpAddress() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("tcpAddress", "default_value_if_not_found");
    }
    private void sendFileContents(ZebraPrinter printer, String fileData) {
        try {
            // Create a temporary file with the file data
            File tempFile = createTempFile(fileData);

            // Send the file contents to the printer
            printer.sendFileContents(tempFile.getAbsolutePath());

        } catch (ConnectionException | IOException e) {
            helper.showErrorDialogOnGuiThread("Error sending file to printer: " + e.getMessage());
        }
    }

    private File createTempFile(String fileData) throws IOException {
        // Create a temporary file
        File tempFile = File.createTempFile("temp_file_", ".tmp", getCacheDir());

        // Write file data to the temporary file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(fileData);
        }

        return tempFile;
    }

    @SuppressLint("StaticFieldLeak")
    private void sendFileToPrinter(final String fileData) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                helper.showLoadingDialog("Sending file to printer ...");
            }


            @Override
            protected Void doInBackground(Void... params) {
                Connection connection = null;
                EditText ipAddressInput = findViewById(R.id.ipAddressInput);
                EditText portInput = findViewById(R.id.portInput);

                String ipAddress = ipAddressInput.getText().toString().trim();
                if (TextUtils.isEmpty(ipAddress)) {
                    try {
                        int port = Integer.parseInt(tcpPort);
                        connection = new TcpConnection(getStoredTcpAddress(), port);
                        connection.open();
                        ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
                        sendFileContents(printer, fileData);
                    } catch (NumberFormatException e) {
                        helper.showErrorDialogOnGuiThread("Please check printer connection.");
                    } catch (ConnectionException e) {
                        helper.showErrorDialogOnGuiThread(e.getMessage());
                    } catch (ZebraPrinterLanguageUnknownException e) {
                        helper.showErrorDialogOnGuiThread(e.getMessage());
                    } finally {
                        if (connection != null) {
                            try {
                                connection.close();
                            } catch (ConnectionException ce) {
                                // Handle the exception if needed
                            }
                        }
                    }
                } else {

                    try {
                        int port = Integer.parseInt(portInput.getText().toString().trim());


                        connection = new TcpConnection(ipAddress, port);
                        connection.open();

                        ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
                        sendFileContents(printer, fileData);

                    } catch (NumberFormatException e) {
                        helper.showErrorDialogOnGuiThread("Port number is invalid");
                    } catch (ConnectionException e) {
                        helper.showErrorDialogOnGuiThread(e.getMessage());
                    } catch (ZebraPrinterLanguageUnknownException e) {
                        helper.showErrorDialogOnGuiThread(e.getMessage());
                    } finally {
                        if (connection != null) {
                            try {
                                connection.close();
                            } catch (ConnectionException ce) {
                                // Handle the exception if needed
                            }
                        }
                    }

                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                helper.dismissLoadingDialog();

                Toast.makeText(FromPhone.this, "File stored to printer. Now available to select.", Toast.LENGTH_LONG).show();
            }
        }.execute();
    }
    private String readFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                InputStreamReader reader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(reader);
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                inputStream.close();
                return stringBuilder.toString();
            } else {
                Log.e("Read File", "InputStream is null");
                return null;
            }
        } catch (IOException e) {
            Log.e("Read File", "Error reading data from file: " + e.getMessage());
            return null;
        }
    }

    private void initializeCsvValues() {
        // Initialize your CSV values here
        tcpAddress = readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 0);
        tcpPort = readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 1);
        formatName = readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 2);

        // Log the values for debugging
//        Log.d("file", readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 0));
//        Log.d("file", readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 1));
//        Log.d("file", readCsvValue(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt", 1, 2));
//
//
//        Log.d("CSV", "TCP Address: " + tcpAddress);
//        Log.d("CSV", "TCP Port: " + tcpPort);
//        Log.d("CSV", "Format Name: " + formatName);
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
//    private class GetVariablesTask extends AsyncTask<Void, Void, Void> {
//        @Override
//        protected Void doInBackground(Void... voids) {
//            try {
//                connection = getPrinterConnection();
//                if (connection != null) {
//                    connection.open();
//                    // Log connection details for debugging
//                    Log.d("CONNECTION", "Connected: " + connection.isConnected());
//                    Log.d("CONNECTION", "Type: " + connection.getClass().getSimpleName());
//
//                    getVariables("^XA^DFE:A.ZPL^FS\n" +
//                            "\n" +
//                            "~SD20\n" +
//                            "\n" +
//                            "^BY2,3,\n" +
//                            "\n" +
//                            "^FO60,20\n" +
//                            "\n" +
//                            "^BC,140,N,N,N,^FN1^FS\n" +
//                            "^FO85,170\n" +
//                            "^A0N,30,50^FN1^FS\n" +
//                            "^XZ\n" +
//                            "\n" +
//                            "^XA^XFE:A.ZPL^FS\n" +
//                            "^FN1^FD1234567890^FS\n" +
//                            "\n" +
//                            "^PQ1^XZ\n");
//                }
//            } catch (ConnectionException e) {
//                Log.e("ERROR", "Error in connection: " + e.getMessage(), e);
//                // Handle ConnectionException (e.g., show an error message)
//            } finally {
//                if (connection != null) {
//                    try {
//                        connection.close();
//                    } catch (ConnectionException e) {
//                        Log.e("ERROR", "Error closing connection: " + e.getMessage(), e);
//                    }
//                }
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void aVoid) {
//            // Update UI if needed after retrieving variables
//        }
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_TAB) {
            // Check if the current focused view is an EditText
            View focusedView = getCurrentFocus();
            if (focusedView instanceof EditText) {
                // If there's only one field and the CheckBox is checked, execute PrintFormatTask
                if (variableValues.size() == 1 && isPrintOnScanChecked()) {
                    new com.zebra.android.devdemo.sendfile.FromPhone.PrintFormatTask() {
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
                helper.showErrorDialogOnGuiThread("Port number is invalid, please connect to your printer from the printer setup.");
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
    protected void getVariables(String zplString) {
        try {
            byte[] formatContents = zplString.getBytes("utf8");
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection); // Use the connection to get an instance
            FieldDescriptionData[] variables = printer.getVariableFields(new String(formatContents, "utf8"));

            for (int i = 0; i < variables.length; i++) {
                variablesList.add(variables[i]);
            }

            // Debug logs to check variablesList size
            Log.d("DEBUG", "VariablesList size: " + variablesList.size());

            updateGuiWithFormats();
        } catch (ZebraPrinterLanguageUnknownException | UnsupportedEncodingException e) {
            Log.e("ERROR", "Error parsing ZPL string: " + e.getMessage(), e);
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ConnectionException e) {
            throw new RuntimeException(e);
        }
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
                    TableRow aRow = new TableRow(com.zebra.android.devdemo.sendfile.FromPhone.this);
                    aRow.setLayoutParams(new TableRow.LayoutParams(
                            ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                    TextView varName = new TextView(com.zebra.android.devdemo.sendfile.FromPhone.this);

                    FieldDescriptionData var = variablesList.get(i);
                    varName.setText(var.fieldName == null ? "Field " + var.fieldNumber : var.fieldName);
                    varName.setLayoutParams(new TableRow.LayoutParams(
                            ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    aRow.addView(varName);

                    EditText value = new EditText(com.zebra.android.devdemo.sendfile.FromPhone.this);
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
