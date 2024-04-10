package com.zebra.android.devdemo.storedformat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.zebra.android.devdemo.LoadDevDemo;
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

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayFieldsActivity extends Activity {

    private List<FieldDescriptionData> variablesList = new ArrayList<>();
    private List<EditText> variableValues = new ArrayList<>();
    private boolean bluetoothSelected;
    private String macAddress;
    private String tcpAddress;
    private String tcpPort;
    private String formatName;
    private String bluetoothAddress;
    private UIHelper helper = new UIHelper(this);
    private Connection connection;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stored_format_variables);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        // Read values from SharedPreferences
        readStoredValues();
        Log.d("preferences", "port: " + tcpPort);
        Log.d("preferences", "ip: " + tcpAddress);
        Log.d("preferences", "format: " + formatName);
        // Initialize values
        initializeValues();

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
                new PrintFormatTask().execute();
            }
        });

        new GetVariablesTask().execute();

        if (!variableValues.isEmpty()) {
            EditText firstEditText = variableValues.get(0);
            firstEditText.requestFocus();
            helper.showKeyboard(firstEditText);
        }
    }

    private void startBarcodeScanner() {
        Log.d("BarcodeScanner", "Starting barcode scanner...");
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Scan a barcode");
        integrator.setOrientationLocked(false);
        integrator.setBeepEnabled(true);
        integrator.setCaptureActivity(CustomScannerActivity.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("BarcodeScanner", "onActivityResult called");
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scannedBarcode = result.getContents();
                Log.d("BarcodeScanner", "Scanned Barcode: " + scannedBarcode);
                View focusedView = getCurrentFocus();
                if (focusedView instanceof EditText) {
                    EditText editText = (EditText) focusedView;
                    editText.setText(scannedBarcode);
                    if (variableValues.size() == 1 && isPrintOnScanChecked()) {
                        new PrintFormatTask().execute();
                    }
                }
            } else {
                Log.d("BarcodeScanner", "Scanning canceled");
                Toast.makeText(this, "Scanning canceled", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_TAB) {
            View focusedView = getCurrentFocus();
            if (focusedView instanceof EditText) {
                if (variableValues.size() == 1 && isPrintOnScanChecked()) {
                    new PrintFormatTask().execute();
                    return true;
                }
            }
        }

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
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        String todaysDate = dateFormat.format(new Date());
//        String csvFilePath = getExternalFilesDir(Environment.DIRECTORY_DCIM) + "/csv/history" + todaysDate + ".txt";
//        try {
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putString("history" + todaysDate, values.toString());
//            editor.apply();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e("history", "Error writing to SharedPreferences: " + e.getMessage());
//        }
//    }

    private void readStoredValues() {
        // Read stored values from SharedPreferences
        tcpAddress = sharedPreferences.getString("tcpAddress", "");
        tcpPort = sharedPreferences.getString("tcpPortNumber", "");
        formatName = sharedPreferences.getString("formatName", "");
        macAddress = sharedPreferences.getString("macAddress", "");

        // Determine Bluetooth selection based on stored values
        bluetoothSelected = tcpAddress.equals("0");
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
                        Log.e("print", "format name from shared preferences is empty");
                    }

                    connection.close();
                }

                List<String> fieldValues = new ArrayList<>();
                for (EditText editText : variableValues) {
                    fieldValues.add(editText.getText().toString());
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentTime = dateFormat.format(new Date());
                fieldValues.add(currentTime);
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

    private void initializeValues() {
        tcpAddress = sharedPreferences.getString("tcpAddress", "");
        tcpPort = sharedPreferences.getString("tcpPortNumber", "");
        formatName = sharedPreferences.getString("formatName", "");
        macAddress = sharedPreferences.getString("macAddress", "");

        if (tcpAddress.equals("0")){
            bluetoothSelected = true;
        } else {
            bluetoothSelected = false;
        }
    }

    private Connection getPrinterConnection() {
        if (!bluetoothSelected) {
            try {

                int port = Integer.parseInt(tcpPort);
                Log.d("preferences", "test");
                Log.d("preferences", "port int: "+port);
                connection = new TcpConnection(tcpAddress, port);
                connection.open();
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

    private void updateGuiWithFormats() {
        runOnUiThread(new Runnable() {
            public void run() {
                TableLayout varTable = findViewById(R.id.variablesTable);

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

                CheckBox printOnScanCheckBox = findViewById(R.id.printOnScan);

                if (variablesList.size() == 1) {
                    printOnScanCheckBox.setVisibility(View.VISIBLE);
                } else {
                    printOnScanCheckBox.setVisibility(View.GONE);
                }

                if (!variableValues.isEmpty()) {
                    EditText firstEditText = variableValues.get(0);
                    firstEditText.requestFocus();
                    helper.showKeyboard(firstEditText);
                }
            }
        });
    }
}
