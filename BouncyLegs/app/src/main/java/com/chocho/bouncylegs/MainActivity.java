package com.chocho.bouncylegs;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.chocho.bouncylegs.sensorProc.DataInstance;
import com.chocho.bouncylegs.sensorProc.DataInstanceList;
import com.chocho.bouncylegs.sensorProc.SlidingWindow;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;
import static com.chocho.bouncylegs.Constants.PREFIX_FEATURES;
import static com.chocho.bouncylegs.Constants.STEP_SIZE;
import static com.chocho.bouncylegs.Constants.WINDOW_SIZE;
import static com.chocho.bouncylegs.Constants.clientUUID;
import static com.chocho.bouncylegs.Constants.leftAddress;
import static com.chocho.bouncylegs.Constants.recvUUID;
import static com.chocho.bouncylegs.Constants.rightAddress;
import static com.chocho.bouncylegs.Constants.sendUUID;
import static com.chocho.bouncylegs.Constants.serviceUUID;
import static com.chocho.bouncylegs.DataClassifier.TAG;

public class MainActivity extends AppCompatActivity {
    final String TAG = "BOUNCY";
    final int REQUEST_ENABLE_BT = 1;
    BluetoothDevice leftDevice;
//    BluetoothDevice rightDevice;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt gattLeft;
//    private BluetoothGatt gattRight;

    private Button btnStartScanLeft;
    private Button btnStopScanLeft;
    private Button btnCollectTrainingLeft;
    private Button btnStopCollectingLeft;
    private TextView tvStatusLeft;
//
//    private Button btnStartScanRight;
//    private Button btnStopScanRight;
//    private Button btnCollectTrainingRight;
//    private Button btnStopCollectingRight;
//    private TextView tvStatusRight;

    private Spinner spModeLeft;
//    private Spinner spModeRight;

    private Button btnStartTesting;

    private TextView tvResult;
//    private TextView tvResultRight;

    Boolean isLeftConnected = false;
//    Boolean isRightConnected = false;
    Boolean isTesting = false;

    BluetoothGattCharacteristic txLeft;
    BluetoothGattCharacteristic rxLeft;
//
//    BluetoothGattCharacteristic txRight;
//    BluetoothGattCharacteristic rxRight;


    // Classifier
    private String classLabel;  // Optional value, only necessary for data collection
    private DataInstanceList dlPressure;   // For raw data save purpose (left)
//    private DataInstanceList dlPressureRight;
    private SlidingWindow slidingWindowPressure;    // For extracting samples by window
//    private SlidingWindow slidingWindowPressureRight;    // For extracting samples by window

    private DataClassifier sensorDataClassifier;
//    private DataClassifier sensorDataClassifierRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = (TextView) findViewById(R.id.tvResult);
//        tvResultRight = (TextView) findViewById(R.id.tvResultRight);

        btnStartScanLeft = (Button) findViewById(R.id.btnStartScanLeft);
        btnStopScanLeft = (Button) findViewById(R.id.btnStopScanLeft);
        btnCollectTrainingLeft = (Button) findViewById(R.id.btnCollectTrainingLeft);
        btnStopCollectingLeft = (Button) findViewById(R.id.btnStopCollectingLeft);
        tvStatusLeft = (TextView) findViewById(R.id.tvStatusLeft);
//
//        btnStartScanRight = (Button) findViewById(R.id.btnStartScanRight);
//        btnStopScanRight = (Button) findViewById(R.id.btnStopScanRight);
//        btnCollectTrainingRight = (Button) findViewById(R.id.btnCollectTrainingRight);
//        btnStopCollectingRight = (Button) findViewById(R.id.btnStopCollectingRight);
//        tvStatusRight = (TextView) findViewById(R.id.tvStatusRight);

        btnStartTesting = (Button) findViewById(R.id.btnStartTesting);

        spModeLeft = (Spinner) findViewById(R.id.spModeLeft);
//        spModeRight = (Spinner) findViewById(R.id.spModeRight);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.mode_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spModeLeft.setAdapter(adapter);
//        spModeRight.setAdapter(adapter);

        spModeLeft.setOnItemSelectedListener(spinnerListener);
//        spModeRight.setOnItemSelectedListener(spinnerListener);

        btnStopCollectingLeft.setEnabled(false);
//        btnStopCollectingRight.setEnabled(false);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        btnStartScanLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothAdapter.startLeScan(scanCallbackLeft);
            }
        });
        btnStopScanLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothAdapter.cancelDiscovery();
                bluetoothAdapter.stopLeScan(scanCallbackLeft);
                if (gattLeft != null) {
                    gattLeft.disconnect();
                }
            }
        });

        btnCollectTrainingLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDataCollection("left");
            }
        });

        btnStopCollectingLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishDataCollection("left");
            }
        });

//
//        btnStartScanRight.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                bluetoothAdapter.startLeScan(scanCallbackRight);
//            }
//        });
//        btnStopScanRight.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                bluetoothAdapter.cancelDiscovery();
//                bluetoothAdapter.stopLeScan(scanCallbackRight);
//                if (gattRight != null) {
//                    gattRight.disconnect();
//                }
//            }
//        });
//
//        btnCollectTrainingRight.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startDataCollection("right");
//            }
//        });
//
//        btnStopCollectingRight.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finishDataCollection("right");
//            }
//        });

        dlPressure = new DataInstanceList();

        // For classifier
        slidingWindowPressure = new SlidingWindow(WINDOW_SIZE, STEP_SIZE);
        sensorDataClassifier = new DataClassifier();
        this.dataAdaptor = this.sensorDataClassifier;

        this.sensorDataClassifier.eventHandler = new DataClassifier.EventHandler() {
            @Override
            public void onClassified(final String resultClass) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(System.currentTimeMillis());
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String formattedDate = sdf.format(cal.getTime());
                        tvResult.setText(resultClass);

                        // Notify Lilypad to ring buzzer when bouncing
                        if (resultClass.equals("bouncing")) {
                            txLeft.setValue("B".getBytes(Charset.forName("UTF-8")));
                            if (gattLeft.writeCharacteristic(txLeft)) {
                                Log.d(TAG,"Sent to left: B");
                            } else {
                                Log.d(TAG,"Couldn't write B to left!");
                            }
                        } else {
                            txLeft.setValue("X".getBytes(Charset.forName("UTF-8")));
                            if (gattLeft.writeCharacteristic(txLeft)) {
                                Log.d(TAG,"Sent to left: X");
                            } else {
                                Log.d(TAG,"Couldn't write X to left!");
                            }
                        }
                    }
                });
            }
        };
//
//        dlPressureRight = new DataInstanceList();
//
//        // For classifier
//        slidingWindowPressureRight = new SlidingWindow(WINDOW_SIZE, STEP_SIZE);
//        sensorDataClassifierRight = new DataClassifier();
//        this.dataAdaptorRight = this.sensorDataClassifierRight;
//
//        this.sensorDataClassifierRight.eventHandler = new DataClassifier.EventHandler() {
//            @Override
//            public void onClassified(final String resultClass) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Calendar cal = Calendar.getInstance();
//                        cal.setTimeInMillis(System.currentTimeMillis());
//                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                        String formattedDate = sdf.format(cal.getTime());
//                        tvResultRight.setText(resultClass);
//
//                        // Notify Lilypad to ring buzzer when bouncing
//                        if (resultClass.equals("bouncing")) {
//                            txLeft.setValue("B".getBytes(Charset.forName("UTF-8")));
//                            if (gattLeft.writeCharacteristic(txLeft)) {
//                                Log.d(TAG,"Sent to left: B");
//                            } else {
//                                Log.d(TAG,"Couldn't write B to left!");
//                            }
//                        } else {
//                            txLeft.setValue("X".getBytes(Charset.forName("UTF-8")));
//                            if (gattLeft.writeCharacteristic(txLeft)) {
//                                Log.d(TAG,"Sent to left: X");
//                            } else {
//                                Log.d(TAG,"Couldn't write X to left!");
//                            }
//                        }
//                    }
//                });
//            }
//        };
//

        btnStartTesting = (Button) findViewById(R.id.btnStartTesting);
        btnStartTesting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String outputFileName = "result.txt";
                if (!isTesting) {
                    if (outputFileName == null || outputFileName.equals("")) {
                        Toast.makeText(MainActivity.this, "Output file name is required for testing a model", Toast.LENGTH_SHORT).show();
                    } else {
                        startTesting(Constants.ARFF_FILE_NAMES);
                        isTesting = !isTesting;
                        btnStartTesting.setText("FINISH");
                    }
                } else {
                    finishTesting(outputFileName);
                    isTesting = !isTesting;
                    btnStartTesting.setText("START");
                }


            }
        });

    }

    AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            switch(position) {
                case 0: // Still
                    classLabel = "still";
                    break;
                case 1: // Bouncing
                    classLabel = "bouncing";
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            classLabel = "still";
        }
    };

    BluetoothAdapter.LeScanCallback scanCallbackLeft = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getAddress().equals(leftAddress)) {
                bluetoothAdapter.stopLeScan(scanCallbackLeft);
                Log.d(TAG, "Found SIMBLEE LEFT!");

                leftDevice = device;
                Log.d(TAG, "Bond State: " + leftDevice.getBondState());
                if (!isLeftConnected) {
                    gattLeft = leftDevice.connectGatt(getApplicationContext(), false, gattCallbackLeft);
                }
            }
        }
    };

//
//    BluetoothAdapter.LeScanCallback scanCallbackRight = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            if (device.getAddress().equals(rightAddress)) {
//                bluetoothAdapter.stopLeScan(scanCallbackRight);
//                Log.d(TAG, "Found SIMBLEE RIGHT!");
//
//                rightDevice = device;
//                if (!isRightConnected) {
//                    gattRight = device.connectGatt(getApplicationContext(), false, gattCallbackRight);
//                }
//            }
//        }
//    };

    BluetoothGattCallback gattCallbackLeft = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == STATE_CONNECTED) {
                Log.d(TAG, "Connected!");
                isLeftConnected = true;
                if (!gatt.discoverServices()) {
                    Log.d(TAG, "Failed to start discovering services!");
                }
            } else if (newState == STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected!");
                isLeftConnected = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatusLeft.setText(R.string.statusDisconnected);
                        tvStatusLeft.setTextColor(getResources().getColor(R.color.colorRed));
                    }
                });
            } else {
                Log.d(TAG, "Connection state changed: " + newState);
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Service discovery complete");
            } else {
                Log.d(TAG, "Service discovery failed with status: " + status);
            }


            txLeft = gatt.getService(serviceUUID).getCharacteristic(sendUUID);
            rxLeft = gatt.getService(serviceUUID).getCharacteristic(recvUUID);

            // Setup notifications on RX characteristic changes (i.e. data received)
            if (!gatt.setCharacteristicNotification(rxLeft, true)) {    // Enable notification
                Log.d(TAG, "Couldn't set notifications for rx characteristic");
            }

            // Update the rx characteristic's client descriptor to enable notifications
            if (rxLeft.getDescriptor(clientUUID) != null) {
                BluetoothGattDescriptor desc = rxLeft.getDescriptor(clientUUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc)) {
                    Log.d(TAG, "Couldn't write rx client descriptor value");
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatusLeft.setText(R.string.statusConnected);
                            tvStatusLeft.setTextColor(getResources().getColor(R.color.colorGreen));
                        }
                    });
                }
            } else {
                Log.d(TAG, "Couldn't get RX client descriptor");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            // @TODO handle received data
            float[] values = new float[1];
            float p = (float) characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);

            Log.d(TAG, "Received: " + Float.toString(p));
            values[0] = p;

            DataInstance diPressure = new DataInstance(System.currentTimeMillis(), values);
            diPressure.setLabel(classLabel);
            dlPressure.add(diPressure);
            slidingWindowPressure.input(diPressure);

            processWindowBuffer();
        }
    };

//
//    BluetoothGattCallback gattCallbackRight = new BluetoothGattCallback() {
//        @Override
//        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            super.onConnectionStateChange(gatt, status, newState);
//            if (newState == STATE_CONNECTED) {
//                Log.d(TAG, "Connected!");
//                isRightConnected = true;
//                if (!gatt.discoverServices()) {
//                    Log.d(TAG, "Failed to start discovering services!");
//                }
//            } else if (newState == STATE_DISCONNECTED) {
//                Log.d(TAG, "Disconnected!");
//                isRightConnected = false;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        tvStatusRight.setText(R.string.statusDisconnected);
//                        tvStatusRight.setTextColor(getResources().getColor(R.color.colorRed));
//                    }
//                });
//            } else {
//                Log.d(TAG, "Connection state changed: " + newState);
//            }
//        }
//
//        @Override
//        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
//            super.onServicesDiscovered(gatt, status);
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Log.d(TAG, "Service discovery complete");
//            } else {
//                Log.d(TAG, "Service discovery failed with status: " + status);
//            }
//
//
//            txRight = gatt.getService(serviceUUID).getCharacteristic(sendUUID);
//            rxRight = gatt.getService(serviceUUID).getCharacteristic(recvUUID);
//
//            // Setup notifications on RX characteristic changes (i.e. data received)
//            if (!gatt.setCharacteristicNotification(rxRight, true)) {    // Enable notification
//                Log.d(TAG, "Couldn't set notifications for rx characteristic");
//            }
//
//            // Update the rx characteristic's client descriptor to enable notifications
//            if (rxRight.getDescriptor(clientUUID) != null) {
//                BluetoothGattDescriptor desc = rxRight.getDescriptor(clientUUID);
//                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                if (!gatt.writeDescriptor(desc)) {
//                    Log.d(TAG, "Couldn't write rx client descriptor value");
//                } else {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            tvStatusRight.setText(R.string.statusConnected);
//                            tvStatusRight.setTextColor(getResources().getColor(R.color.colorGreen));
//                        }
//                    });
//                }
//            } else {
//                Log.d(TAG, "Couldn't get RX client descriptor");
//            }
//        }
//
//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            super.onCharacteristicChanged(gatt, characteristic);
//
//            float[] values = new float[1];
//            float p = (float) characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
//
//            Log.d(TAG, "Received: " + Float.toString(p));
//            values[0] = p;
//
//            DataInstance diPressure = new DataInstance(System.currentTimeMillis(), values);
//            diPressure.setLabel(classLabel);
//            dlPressureRight.add(diPressure);
//            slidingWindowPressureRight.input(diPressure);
//
//            processWindowBufferRight();
//        }
//    };

    public void highlightButton(String btnLabel) {
        switch(btnLabel) {
            case "":    // Initial states
                btnCollectTrainingLeft.setEnabled(true);
//                btnCollectTrainingRight.setEnabled(true);
                btnStopCollectingLeft.setEnabled(false);
//                btnStopCollectingRight.setEnabled(false);
                break;
            case "start_collect_left":
                btnCollectTrainingLeft.setEnabled(false);
                btnStopCollectingLeft.setEnabled(true);
                break;
//            case "start_collect_right":
//                btnCollectTrainingRight.setEnabled(false);
//                btnStopCollectingRight.setEnabled(true);
//                break;
            case "stop_collect_left":
                btnCollectTrainingLeft.setEnabled(true);
                btnStopCollectingLeft.setEnabled(false);
                break;
//            case "stop_collect_right":
//                btnCollectTrainingRight.setEnabled(true);
//                btnStopCollectingRight.setEnabled(false);
//                break;
            default:    // Initial states
                btnCollectTrainingLeft.setEnabled(true);
//                btnCollectTrainingRight.setEnabled(true);
                btnStopCollectingLeft.setEnabled(false);
//                btnStopCollectingRight.setEnabled(false);
                break;
        }

    }

    /*
     *  label: "left" or "right"
     */
    public void startDataCollection(String leg) {
        highlightButton("start_collect_" + leg);
        sensorDataClassifier.clear();

        classLabel = spModeLeft.getSelectedItem().toString().toLowerCase();

        // Send 'S' to start collection
        if (leg.equals("left")) {
            txLeft.setValue("S".getBytes(Charset.forName("UTF-8")));
            if (gattLeft.writeCharacteristic(txLeft)) {
                Log.d(TAG,"Sent to left: S");
            } else {
                Log.d(TAG,"Couldn't write S to left!");
            }
        } else if (leg.equals("right")) {
//            txRight.setValue("S".getBytes(Charset.forName("UTF-8")));
//            if (gattRight.writeCharacteristic(txRight)) {
//                Log.d(TAG, "Sent to right: S");
//            } else {
//                Log.d(TAG, "Couldn;t write S to right!");
//            }
        }
    }

    public void finishDataCollection(String leg) {
        // Send 'F' to finish collection
        if (leg.equals("left")) {
            txLeft.setValue("F".getBytes(Charset.forName("UTF-8")));
            if (gattLeft.writeCharacteristic(txLeft)) {
                Log.d(TAG, "Sent to left: F");
            } else {
                Log.d(TAG, "Couldn't write F to left!");
            }
            // Save raw data
            saveRawDataToCSV();

            // Save calculated feature sets
//        sensorDataClassifier.saveInstancesToArff(sensorDataClassifier.getInstances(), PREFIX_FEATURES + System.currentTimeMillis() + "_" + classLabel + ".txt");
            sensorDataClassifier.saveInstancesToArff(sensorDataClassifier.getInstances(), classLabel + ".txt");
        } else if (leg.equals("right")){
//            txRight.setValue("F".getBytes(Charset.forName("UTF-8")));
//            if (gattRight.writeCharacteristic(txRight)) {
//                Log.d(TAG, "Sent to right: F");
//            } else {
//                Log.d(TAG, "Couldn't write F to right!");
//            }
//            // Save raw data
//            saveRawDataToCSVRight();
//
//            // Save calculated feature sets
//            sensorDataClassifierRight.saveInstancesToArff(sensorDataClassifierRight.getInstances(), classLabel + "_right.txt");
        }

        highlightButton("stop_collect_" + leg);
    }

    public interface DataAdaptor {
        public void slidingWindowData(String classLabel, DataInstanceList dlPressure);
    }

    public DataAdaptor dataAdaptor;
//    public DataAdaptor dataAdaptorRight;

    private void processWindowBuffer() {
        if (!slidingWindowPressure.isBufferReady()) return;

        // Fetching a slice of sliding window
        DataInstanceList dlPressure = slidingWindowPressure.output();

        if (dlPressure == null) return;

        if (dataAdaptor == null) return;
        dataAdaptor.slidingWindowData(classLabel, dlPressure);
    }

//
//    private void processWindowBufferRight() {
//        if (!slidingWindowPressureRight.isBufferReady()) return;
//
//        // Fetching a slice of sliding window
//        DataInstanceList dlPressure = slidingWindowPressureRight.output();
//
//        if (dlPressure == null) return;
//
//        if (dataAdaptorRight == null) return;
//        dataAdaptorRight.slidingWindowData(classLabel, dlPressure);
//    }

    public void saveRawDataToCSV() {
        String fileNamePressure = Constants.PREFIX_RAW_DATA + System.currentTimeMillis() + "_" + classLabel + "_pressure.txt";
        dlPressure.saveToCsvFile(fileNamePressure);
    }
//
//    public void saveRawDataToCSVRight() {
//        String fileNamePressure = Constants.PREFIX_RAW_DATA + System.currentTimeMillis() + "_" + classLabel + "_right_pressure.txt";
//        dlPressureRight.saveToCsvFile(fileNamePressure);
//    }

    public void startTesting(String[] arffFileNames) {
        try {
            highlightButton("start_testing");

            classLabel = null;
            sensorDataClassifier.setClassifier(arffFileNames);
//            sensorDataClassifierRight.setClassifier(Constants.RIGHT_ARFF_FILE_NAMES);

            txLeft.setValue("S".getBytes(Charset.forName("UTF-8")));
            if (gattLeft.writeCharacteristic(txLeft)) {
                Log.d(TAG,"Sent to left: S");
            } else {
                Log.d(TAG,"Couldn't write S to left!");
            }

//            txRight.setValue("S".getBytes(Charset.forName("UTF-8")));
//            if (gattRight.writeCharacteristic(txRight)) {
//                Log.d(TAG,"Sent to left: S");
//            } else {
//                Log.d(TAG,"Couldn't write S to left!");
//            }


        } catch (FileNotFoundException e) {
            highlightButton("");
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
        }
    }

    public void saveTestResultToFile(String outputFileName, String content) {
        // Writing summary if output is set
        if (outputFileName != null) {
            // Set output file for writing result
            String filePath = Environment.getExternalStorageDirectory() + "/" + Constants.WORKING_DIR_NAME + "/" + Constants.PREFIX_RESULT + System.currentTimeMillis() + "_" + outputFileName + ".txt";
            FileWriter fw = null;
            try {
                fw = new FileWriter(filePath);
                Log.i(TAG, "Output file writer is open!");

                fw.write(content);
                fw.flush();
                fw.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.e(TAG, "setOutputFileName() error : " + e.getMessage());
            }
        }
    }

    public void finishTesting(String outputFileName) {
        highlightButton("");

        txLeft.setValue("F".getBytes(Charset.forName("UTF-8")));
        if (gattLeft.writeCharacteristic(txLeft)) {
            Log.d(TAG, "Sent to left: F");
        } else {
            Log.d(TAG, "Couldn't write F to left!");
        }
//
//        txRight.setValue("F".getBytes(Charset.forName("UTF-8")));
//        if (gattLeft.writeCharacteristic(txLeft)) {
//            Log.d(TAG, "Sent to left: F");
//        } else {
//            Log.d(TAG, "Couldn't write F to left!");
//        }

        // Save raw data
        saveRawDataToCSV();

        // Save calculated feature sets
        sensorDataClassifier.saveInstancesToArff(sensorDataClassifier.getInstances(), Constants.PREFIX_FEATURES + System.currentTimeMillis() + "_" + classLabel + ".txt");

        String resultTest = sensorDataClassifier.getResultTest();
        saveTestResultToFile(outputFileName, "\n" + resultTest);

        highlightButton("finish_testing");
    }

    @Override
    protected void onPause() {
        super.onPause();
        bluetoothAdapter.stopLeScan(scanCallbackLeft);
//        bluetoothAdapter.stopLeScan(scanCallbackRight);
    }

    @Override
    protected void onStop() {
        super.onStop();
        bluetoothAdapter.stopLeScan(scanCallbackLeft);
//        bluetoothAdapter.stopLeScan(scanCallbackRight);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.stopLeScan(scanCallbackLeft);
//        bluetoothAdapter.stopLeScan(scanCallbackRight);
    }
}
