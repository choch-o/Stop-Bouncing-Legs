package com.chocho.bouncylegs;

import java.util.UUID;

public class Constants {
    // BLE information
    public final static String leftAddress = "FE:40:0F:18:6D:A4";
    public final static String rightAddress ="D7:3F:B4:05:A3:75";

    public final static UUID serviceUUID = UUID.fromString("0000fe84-0000-1000-8000-00805f9b34fb");
    public final static UUID recvUUID = UUID.fromString("2d30c082-f39f-4ce6-923f-3484ea480596");
    public final static UUID sendUUID = UUID.fromString("2d30c083-f39f-4ce6-923f-3484ea480596");
    public final static UUID clientUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Parameters
    public final static String[] CLASS_LABELS = {"still", "bouncing"};
    public final static String[] ARFF_FILE_NAMES = {"still.txt", "bouncing.txt"};
    public final static String[] RIGHT_ARFF_FILE_NAMES = {"still_right.txt", "bouncing_right.txt"};
    public final static String WORKING_DIR_NAME = "bouncyLegs";

    public final static int DURATION_THREAD_SLEEP = 200;    //ms
    public final static int WINDOW_SIZE = 1000; // ms
    public final static int STEP_SIZE = 500;    // ms

    // File name prefix
    public final static String PREFIX_RAW_DATA = "1_raw_data_";
    public final static String PREFIX_FEATURES = "2_features_";
    public final static String PREFIX_MODEL = "3_model_"; // Not used for this application
    public final static String PREFIX_RESULT = "4_result_";

    // Raw data
    public final static String HEADER_PRESSURE = "pressure";

    // Optional fields
    public final static String HEADER_CLASS_LABEL = "label";
    public final static String HEADER_UNIXTIME = "unix_time";

    // Features
    public final static String HEADER_PRESSURE_MEAN = "pressure_mean";
    public final static String HEADER_PRESSURE_MAX = "pressure_max";
    public final static String HEADER_PRESSURE_MIN = "pressure_min";
    public final static String HEADER_PRESSURE_VARIANCE = "pressure_variance";

    // List of Features
    public final static String[] LIST_FEATURES = {
            HEADER_PRESSURE_MEAN,
            HEADER_PRESSURE_MAX,
            HEADER_PRESSURE_MIN,
            HEADER_PRESSURE_VARIANCE
    };

}
