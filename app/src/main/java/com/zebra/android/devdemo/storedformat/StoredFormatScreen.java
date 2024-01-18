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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.zebra.android.devdemo.R;
import com.zebra.android.devdemo.connectivity.ConnectivityDemo;
import com.zebra.android.devdemo.sendfile.FromPhone;
import com.zebra.android.devdemo.util.SettingsHelper;
import com.zebra.android.devdemo.util.UIHelper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.printer.FieldDescriptionData;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class StoredFormatScreen extends ListActivity {

    public static final int PICK_FILE_REQUEST_CODE = 1;
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
        final Button sendButton = findViewById(R.id.sendFromAppButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
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


    }

    private String getStoredTcpAddress() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("tcpAddress", "default_value_if_not_found");
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
                    sendFileToPrinter("^XA^DFE:SEIExampleFormat.ZPL^FS\n" +
                            "\n" +
                            "~SD20\n" +
                            "\n" +
                            "^BY2,3,\n" +
                            "\n" +
                            "^FO60,20\n" +
                            "\n" +
                            "^BC,140,N,N,N,^FN1^FS\n" +
                            "^FO85,170\n" +
                            "^A0N,30,50^FN1^FS\n" +
                            "^XZ\n" +
                            "\n" +
                            "^XA^XFE:SEIExampleFormat.ZPL^FS\n" +
                            "^FN1^FD34^FS\n" +
                            "\n" +
                            "^PQ1^XZ\n");
                } else {
                    Toast.makeText(this, "Failed to read file data", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Selected file URI is null", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resetUIAndConnect() {
        // Clear the current formatsList and update the UI
        formatsList.clear();
        updateGuiWithFormats();

        // Establish a new connection
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                getFileList();
                Looper.loop();
                Looper.myLooper().quit();
            }
        }).start();
    }
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
                try {
                    int port = Integer.parseInt(tcpPort);
                    connection = new TcpConnection(getStoredTcpAddress(), port);
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
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                helper.dismissLoadingDialog();
                resetUIAndConnect();
                Toast.makeText(StoredFormatScreen.this, "File stored to printer. Now available to select.", Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    private void sendFileContents(ZebraPrinter printer, String fileData) {
        try {
            // Create a temporary file with the file data
            File tempFile = createTempFile(fileData);

            // Send the file contents to the printer
            printer.sendFileContents(tempFile.getAbsolutePath());

            // Save settings
            saveSettings();
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


    private String getPathFromURI(Uri uri) {
        String filePath = "";

        try {
            ContentResolver contentResolver = getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(uri);

            if (inputStream != null) {
                File file = createTemporalFileFrom(inputStream);

                if (file != null) {
                    filePath = file.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            Log.e("File Path", "Exception: " + e.getMessage());
        }

        return filePath;
    }

    private File createTemporalFileFrom(InputStream inputStream) throws IOException {
        File targetFile = null;

        if (inputStream != null) {
            int read;
            byte[] buffer = new byte[8 * 1024];
            targetFile = createTemporalFile();

            try (OutputStream outputStream = new FileOutputStream(targetFile)) {
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
        }

        return targetFile;
    }

    private File createTemporalFile() {
        String fileName = "temp_file_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

        try {
            return File.createTempFile(fileName, null, storageDir);
        } catch (IOException e) {
            Log.e("File Creation", "Error creating temporary file: " + e.getMessage());
            return null;
        }
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

        String clickedItem = (String) l.getItemAtPosition(position);

        // Check if the clicked item is "SEIExampleFormat.ZPL"
        if ("SEIExampleFormat.ZPL".equals(clickedItem)) {

            sendFileToPrinter("^XA^DFE:SEIEXAMPLEFORMAT.ZPL^FS\n" +
                    "\n" +
                    "~SD20\n" +
                    "\n" +
                    "^BY2,3,\n" +
                    "\n" +
                    "^FO60,20\n" +
                    "\n" +
                    "^BC,140,N,N,N,^FN1^FS\n" +
                    "^FO85,170\n" +
                    "^A0N,30,50^FN1^FS\n" +
                    "^XZ\n" +
                    "\n" +
                    "^XA^XFE:SEIEXAMPLEFORMAT.ZPL^FS\n" +
                    "^FN1^FD34^FS\n" +
                    "\n" +
                    "^PQ1^XZ\n");

            Toast.makeText(this, "Clicked on SEIExampleFormat.ZPL", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, VariablesScreen.class);
            intent.putExtra("bluetooth selected", bluetoothSelected);
            intent.putExtra("mac address", macAddress);
            intent.putExtra("tcp address", tcpAddress);
            intent.putExtra("tcp port", tcpPort);
            intent.putExtra("format name", "E:SEIEXAMPLEFORMAT.ZPL");

            // Add the following lines to write to CSV
            writeDataToCsv(tcpAddress, tcpPort, "E:SEIEXAMPLEFORMAT.ZPL");

            startActivity(intent);
        } else {
            // Handle the click on other items
            Intent intent = new Intent(this, VariablesScreen.class);
            intent.putExtra("bluetooth selected", bluetoothSelected);
            intent.putExtra("mac address", macAddress);
            intent.putExtra("tcp address", tcpAddress);
            intent.putExtra("tcp port", tcpPort);
            intent.putExtra("format name", clickedItem);

            // Add the following lines to write to CSV
            writeDataToCsv(tcpAddress, tcpPort, clickedItem);

            startActivity(intent);
        }
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

    private String readIpCsv() {
        try {
            String csvFilePath = String.valueOf(new File(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt"));

            Log.e("path", csvFilePath);
            File csvFile = new File(csvFilePath);

            if (csvFile.exists() && csvFile.length() > 0) {
                // Read existing data
                CSVReader reader = new CSVReader(new FileReader(csvFilePath));
                List<String[]> existingData = reader.readAll();
                reader.close();

                // Check if the second row exists
                if (existingData.size() >= 2) {

                    return existingData.get(1)[0];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return a default value or handle the case when the data is not found
        return "DefaultFormatName";
    }
    private String readMacCsv() {
        try {
            String csvFilePath = String.valueOf(new File(getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/csv.txt"));

            Log.e("path", csvFilePath);
            File csvFile = new File(csvFilePath);

            if (csvFile.exists() && csvFile.length() > 0) {
                // Read existing data
                CSVReader reader = new CSVReader(new FileReader(csvFilePath));
                List<String[]> existingData = reader.readAll();
                reader.close();

                // Check if the second row exists
                if (existingData.size() >= 2) {

                    return existingData.get(1)[3];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Return a default value or handle the case when the data is not found
        return "DefaultFormatName";
    }


    //it's over here lol
    //it's over here lol
    //it's over here lol
    //it's over here lol
    //it's over here lol
    //it's over here lol
    //it's over here lol
    //it's over here lol
    //it's over here lol
    private void getFileList() {
        Connection connection = null;

        if ("0".equals(readIpCsv())) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Log.d("bluetooth", "unavailable");
                return;
            } else {
                try {
                    String BTAddress = readMacCsv();
                    Log.d("bluetooth", "connecting to mac address " + BTAddress);
                    connection = new BluetoothConnection(BTAddress);
                } catch (NumberFormatException e) {
                    helper.showErrorDialogOnGuiThread("Mac address is invalid");
                    return;
                }
            }
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
                formatExtensions = new String[]{"ZPL"};
            } else {
                formatExtensions = new String[]{"FMT", "LBL"};
            }

            String[] formats = printer.retrieveFileNames(formatExtensions);

            // Add the default item "SEIExampleFormat.ZPL"
            formatsList.add("SEIExampleFormat.ZPL");

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
