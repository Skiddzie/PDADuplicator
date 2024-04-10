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
import android.support.v4.app.Fragment;
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
import android.content.SharedPreferences;

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
//    private BroadcastReceiver powerConnectedReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (action != null && action.equals(Intent.ACTION_POWER_CONNECTED)) {
//                // Phone is plugged in, trigger file transfer
//                transferFileToComputer();
//            }
//        }
//    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_format);
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        String storedTcpAddress = sharedPreferences.getString("tcpAddress", "defaultTcpAddress");
        String storedTcpPortNumber = sharedPreferences.getString("tcpPortNumber", "defaultTcpPortNumber");
        String storedMacAddress = sharedPreferences.getString("macAddress", "defaultMacAddress");

        Log.d("crashlooker", "FromPhone load");
        // Read CSV values once during onCreate
//        readCsvFile();
//        // Initialize CSV values
//        initializeCsvValues();




        final Button printButton = findViewById(R.id.printFormatButton);
        printButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Set the MIME types to filter only ".ZPL" and ".TXT" files
                String[] mimeTypes = {"text/plain", "application/vnd.zebra.raw"};
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

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
//        unregisterReceiver(powerConnectedReceiver);
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


//    private void transferFileToComputer() {
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        String todaysDate = dateFormat.format(new Date());
////        String csvFilePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/history" + todaysDate + ".txt";
//
//        File sourceFile = new File(csvFilePath);
//
//        // Specify the destination directory on the computer (can be modified based on your needs)
//        File destinationFile = new File(getFilesDir(), "history" + todaysDate + ".txt");
//        Log.d("filetransfer", "Source File: " + sourceFile.getAbsolutePath());
//        Log.d("filetransfer", "Destination File: " + destinationFile.getAbsolutePath());
//        Toast.makeText(this, "File transfer called", Toast.LENGTH_SHORT).show();
//        try {
//            // Create FileInputStream for the source file
//            FileInputStream inputStream = new FileInputStream(sourceFile);
//
//            // Create OutputStream for the destination file on the computer
//            FileOutputStream outputStream = new FileOutputStream(destinationFile);
//
//            // Transfer bytes from the source file to the destination file
//            byte[] buffer = new byte[1024];
//            int length;
//            while ((length = inputStream.read(buffer)) > 0) {
//                outputStream.write(buffer, 0, length);
//            }
//
//            // Close streams
//            inputStream.close();
//            outputStream.close();
//
//            // Optionally, you can delete the source file after transfer
//            // sourceFile.delete();
//
//            // Log success or perform additional actions as needed
//            Toast.makeText(this, "File transfer successful", Toast.LENGTH_SHORT).show();
//            Log.d("filetransfer", "File transfer successful. Destination File: " + destinationFile.getAbsolutePath());
//        } catch (IOException e) {
//            e.printStackTrace();
//            // Handle the exception (e.g., show an error message)
//            Toast.makeText(this, "File transfer unsuccessful: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//            Log.e("filetransfer", "File transfer unsuccessful: " + e.getMessage());
//        }
//    }

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
//                            transferFileToComputer();
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

//    private void addToHistoryCsv(List<String> values) {
//        Log.d("history", "add history called");
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        String todaysDate = dateFormat.format(new Date());
//        String csvFilePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/history"+todaysDate+".txt";
//        try {
//
//            // Open CSV file in append mode
//            FileWriter fileWriter = new FileWriter(csvFilePath, true);
//            CSVWriter csvWriter = new CSVWriter(fileWriter);
//
//            // Write a new row to the CSV file
//            csvWriter.writeNext(values.toArray(new String[0]));
//
//            // Close CSV writer
//            csvWriter.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            Log.e("history", "Error writing to history.csv: " + e.getMessage());
//        }
//    }

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
//                addToHistoryCsv(fieldValues);
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

//    private void readCsvFile() {
//        String csvFilePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt";
//        tcpAddress = readCsvValue(csvFilePath, 1, 0);
//        tcpPort = readCsvValue(csvFilePath, 1, 1);
//        formatName = readCsvValue(csvFilePath, 1, 2);
//        Log.d("CSV", "TCP Address: " + tcpAddress);
//        Log.d("CSV", "TCP Port: " + tcpPort);
//        Log.d("CSV", "Format Name: " + formatName);
//    }

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

}
