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

package com.zebra.android.devdemo.imageprint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.zebra.android.devdemo.BuildConfig;
import com.zebra.android.devdemo.R;
import com.zebra.android.devdemo.util.SettingsHelper;
import com.zebra.android.devdemo.util.UIHelper;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

public class ImagePrintDemo extends Activity {

    private RadioButton btRadioButton;
    private EditText macAddressEditText;
    private EditText ipAddressEditText;
    private EditText portNumberEditText;
    private EditText printStoragePath;

    private EditText xStartBoxText;
    private EditText yStartBoxText;
    private EditText xSizeBoxText;
    private EditText ySizeBoxText;

    private static final String bluetoothAddressKey = "ZEBRA_DEMO_BLUETOOTH_ADDRESS";
    private static final String tcpAddressKey = "ZEBRA_DEMO_TCP_ADDRESS";
    private static final String tcpPortKey = "ZEBRA_DEMO_TCP_PORT";
    private static final String PREFS_NAME = "OurSavedAddress";
    private UIHelper helper = new UIHelper(this);
    private static int TAKE_PICTURE = 1;
    private static int PICTURE_FROM_GALLERY = 2;
    private static File file = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.image_print_demo);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        ipAddressEditText = (EditText) this.findViewById(R.id.ipAddressInput);
        String ip = settings.getString(tcpAddressKey, "");
        ipAddressEditText.setText(ip);

        portNumberEditText = (EditText) this.findViewById(R.id.portInput);
        String port = settings.getString(tcpPortKey, "");
        portNumberEditText.setText(port);

        macAddressEditText = (EditText) this.findViewById(R.id.macInput);
        String mac = settings.getString(bluetoothAddressKey, "");
        macAddressEditText.setText(mac);

        xStartBoxText = (EditText) this.findViewById(R.id.xStartBox);

        yStartBoxText = (EditText) this.findViewById(R.id.yStartBox);

        xSizeBoxText = (EditText) this.findViewById(R.id.xSizeBox);

        ySizeBoxText = (EditText) this.findViewById(R.id.ySizeBox);

        printStoragePath = (EditText) findViewById(R.id.printerStorePath);



        CheckBox cb = (CheckBox) findViewById(R.id.checkBox);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    printStoragePath.setVisibility(View.VISIBLE);
                } else {
                    printStoragePath.setVisibility(View.INVISIBLE);
                }
            }
        });

        btRadioButton = (RadioButton) this.findViewById(R.id.bluetoothRadio);

        Button cameraButton = (Button) this.findViewById(R.id.testButton);
        cameraButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                Log.d("CAMERABUTTON", "CAMERA BUTTON CLICKED");

                getPhotoFromCamera();
            }
        });

        Button galleryButton = (Button) this.findViewById(R.id.galleryButton);
        galleryButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                getPhotosFromGallery();
            }
        });

        RadioGroup radioGroup = (RadioGroup) this.findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.bluetoothRadio) {
                    toggleEditField(macAddressEditText, true);
                    toggleEditField(portNumberEditText, false);
                    toggleEditField(ipAddressEditText, false);
                } else {
                    toggleEditField(portNumberEditText, true);
                    toggleEditField(ipAddressEditText, true);
                    toggleEditField(macAddressEditText, false);
                }
            }
        });
    }

    private void getPhotosFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICTURE_FROM_GALLERY);
    }

    private void toggleEditField(EditText editText, boolean set) {
        /*
         * Note: Disabled EditText fields may still get focus by some other means, and allow text input.
         *       See http://code.google.com/p/android/issues/detail?id=2771
         */
        editText.setEnabled(set);
        editText.setFocusable(set);
        editText.setFocusableInTouchMode(set);
    }

    private boolean isBluetoothSelected() {
        return btRadioButton.isChecked();
    }

    private String getMacAddressFieldText() {
        return macAddressEditText.getText().toString();
    }

    private String getTcpAddress() {
        return ipAddressEditText.getText().toString();
    }

    private String getTcpPortNumber() {
        return portNumberEditText.getText().toString();
    }

    private void getPhotoFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            // Log that we're about to create the file
            file = new File(getExternalFilesDir(null), "tempPic.jpg");
            Log.d("ImagePrintDemo", "File path: " + file.getAbsolutePath());

            // Log the attempt to use FileProvider
            Uri photoURI = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
            Log.d("ImagePrintDemo", "Generated photo URI: " + photoURI.toString());

            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, TAKE_PICTURE);
        } catch (Exception e) {
            // Log any exceptions that occur
            Log.e("ImagePrintDemo", "Error while trying to open the camera", e);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == TAKE_PICTURE) {
                printPhotoFromExternal(BitmapFactory.decodeFile(file.getAbsolutePath()));
            }
            if (requestCode == PICTURE_FROM_GALLERY) {
                Uri imgPath = data.getData();
                Bitmap myBitmap = null;
                try {
                    myBitmap = Media.getBitmap(getContentResolver(), imgPath);
                } catch (FileNotFoundException e) {
                    helper.showErrorDialog(e.getMessage());
                } catch (IOException e) {
                    helper.showErrorDialog(e.getMessage());
                }
                printPhotoFromExternal(myBitmap);
            }
        }
    }

    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private int getIntFromEditText(EditText editText, int defaultValue) {
        String text = editText.getText().toString();
        if (text.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            Log.e("ImagePrintDemo", "Invalid number format", e);
            return defaultValue; // Return the default value if the input is invalid
        }
    }


    private void printPhotoFromExternal(final Bitmap bitmap) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    getAndSaveSettings();

                    Looper.prepare();
                    helper.showLoadingDialog("Sending image to printer");

                    // Rotate the bitmap 90 degrees
                    Bitmap rotatedBitmap = rotateBitmap(bitmap, 90);

                    Connection connection = getZebraPrinterConn();
                    connection.open();
                    ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);

                    if (((CheckBox) findViewById(R.id.checkBox)).isChecked()) {
                        printer.storeImage(printStoragePath.getText().toString(), new ZebraImageAndroid(rotatedBitmap), 550, 412);
                    } else {
                        int xStart = getIntFromEditText(xStartBoxText, 0);
                        int yStart = getIntFromEditText(yStartBoxText, 0);
                        int xSize = getIntFromEditText(xSizeBoxText, 1000);
                        int ySize = getIntFromEditText(ySizeBoxText, 1000);

                        printer.printImage(new ZebraImageAndroid(rotatedBitmap), xStart, yStart, xSize, ySize, false);

                    }
                    connection.close();

                    if (file != null) {
                        file.delete();
                        file = null;
                    }
                } catch (ConnectionException e) {
                    helper.showErrorDialogOnGuiThread(e.getMessage());
                } catch (ZebraPrinterLanguageUnknownException e) {
                    helper.showErrorDialogOnGuiThread(e.getMessage());
                } catch (ZebraIllegalArgumentException e) {
                    helper.showErrorDialogOnGuiThread(e.getMessage());
                } finally {
                    // Recycle the bitmaps
                    bitmap.recycle();
                    helper.dismissLoadingDialog();
                    Looper.myLooper().quit();
                }
            }
        }).start();
    }

    private Connection getZebraPrinterConn() {
        int portNumber;
        try {
            portNumber = Integer.parseInt(getTcpPortNumber());
        } catch (NumberFormatException e) {
            portNumber = 0;
        }
        return isBluetoothSelected() ? new BluetoothConnection(getMacAddressFieldText()) : new TcpConnection(getTcpAddress(), portNumber);
    }

    private void getAndSaveSettings() {
        SettingsHelper.saveBluetoothAddress(ImagePrintDemo.this, getMacAddressFieldText());
        SettingsHelper.saveIp(ImagePrintDemo.this, getTcpAddress());
        SettingsHelper.savePort(ImagePrintDemo.this, getTcpPortNumber());
    }

}
