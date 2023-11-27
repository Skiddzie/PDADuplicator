package com.zebra.android.devdemo.ChangePIN;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.zebra.android.devdemo.R;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class ChangePIN extends Activity {
    private EditText oldPinInput;
    private EditText reenterPinInput;
    private EditText newPinInput;
    private Button OKButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_change);

        // Initialize UI elements
        oldPinInput = (EditText) findViewById(R.id.oldPinInput);
        reenterPinInput = (EditText) findViewById(R.id.reenterPinInput);
        newPinInput = (EditText) findViewById(R.id.newPinInput);
        OKButton = (Button) findViewById(R.id.OKButton);

        // Set onClickListener for the OKButton
        OKButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Get values from the input fields
                String oldPin = oldPinInput.getText().toString();
                String reenterPin = reenterPinInput.getText().toString();
                String newPin = newPinInput.getText().toString();

                // Check if oldPin and reenterPin match the third row value in the CSV
                if (checkPinMatch(oldPin, reenterPin)) {
                    // If they match, update the third row value with newPin
                    updatePinInCSV(newPin);
                    Toast.makeText(ChangePIN.this, "PIN updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    // If they don't match, show an error message
                    Toast.makeText(ChangePIN.this, "Incorrect or non-matching PIN", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean checkPinMatch(String oldPin, String reenterPin) {
        // Retrieve the third row value from the CSV
        String csvThirdRowValue = readThirdRowFromCSV(); // Implement this method

        // Check if oldPin and reenterPin match the third row value
        return oldPin.equals(csvThirdRowValue) && reenterPin.equals(csvThirdRowValue);
    }

    private void updatePinInCSV(String newPin) {
        // Implement the logic to update the third row value in the CSV with newPin
        // You can use the modifyFormatValue method you provided earlier
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/csv.txt";
        modifyFormatValue(filePath, 2, 0, newPin); // Assuming the third row is at index 2
    }

    // Implement the logic to read the third row value from the CSV
    private String readThirdRowFromCSV() {
        // Replace this with the actual logic to read the third row value from the CSV
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/csv.txt";
        return readValueFromCSV(filePath, 2, 0); // Assuming the third row is at index 2
    }

    // Implement the logic to read a specific value from the CSV file
    private String readValueFromCSV(String file, int rowIndex, int columnIndex) {
        try {
            FileReader fileReader = new FileReader(file);
            CSVReader csvReader = new CSVReader(fileReader);
            List<String[]> csvData = csvReader.readAll();

            if (rowIndex < csvData.size() && columnIndex < csvData.get(rowIndex).length) {
                return csvData.get(rowIndex)[columnIndex];
            } else {
                Log.e("ERROR", "Invalid row or column index provided.");
                return "";
            }
        } catch (IOException e) {
            Log.e("ERROR", "Error reading value from CSV: " + e.getMessage(), e);
            return "";
        }
    }

    // Implement the logic to modify a specific value in the CSV file
    private void modifyFormatValue(String file, int rowIndex, int columnIndex, String newValue) {
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
                Log.e("ERROR", "Invalid row or column index provided.");
            }
        } catch (IOException e) {
            Log.e("ERROR", "Error modifying value in CSV: " + e.getMessage(), e);
        }
    }
}