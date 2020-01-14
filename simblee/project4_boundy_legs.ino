#include <SimbleeBLE.h>

const int pressurePin = 3;
const int buzzerPin = 12;
int pressureValue = 0;

bool isConnected = false;
bool isCollecting = false;
bool isBouncing = false;

// Notes and their frequencies
const int C = 1046;
const int D = 1175;
const int E = 1319;
const int F = 1397;
const int G = 1568;
const int A = 1760;
const int B = 1976;
const int C1 = 2093;
const int D1 = 2349;

void setup() {
  pinMode(buzzerPin, OUTPUT);

  Serial.begin(9600);

  SimbleeBLE.advertisementInterval = 500;
  Serial.println("Simblee BLE Advertising interval 500ms");
  SimbleeBLE.deviceName = "Simblee";
  Serial.println("Simblee BLE DeviceName: Simblee");
  SimbleeBLE.txPowerLevel = -20;
  Serial.println("Simblee BLE Tx Power Level: -20dBm");


//  SimbleeBLE.customUUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
  SimbleeBLE.begin();
  Serial.println("Simblee BLE stack started");
}

void loop() {
  pressureValue = analogRead(pressurePin);
//  Serial.println(pressureValue);
//  delay(200);
  if (isConnected && isCollecting) {
    Serial.println("Sending: ");
    Serial.println(pressureValue);
    
    SimbleeBLE.sendInt(pressureValue);

    if (isBouncing) {
      tone(buzzerPin, G);
    } else {
      noTone(buzzerPin);
    }
  }
  
  delay(200);
}

void SimbleeBLE_onAdvertisement(bool start) {
  if (start)
    Serial.println("Simblee BLE advertisement started");
    
}

void SimbleeBLE_onConnect() {
  Serial.println("Simblee BLE connected");
  isConnected = true;
}

void SimbleeBLE_onDisconnect() {
  Serial.println("Simblee BLE disconnected");
  isConnected = false;
  isBouncing = false;
  noTone(buzzerPin);
}

void SimbleeBLE_onReceive(char *data, int len) {
  Serial.println("Received data over BLE");  
  Serial.println(data);
  String str((char *) data);
  if (str == "S") {
    Serial.println("Received S");
    isCollecting = true;
  } else if (str == "F") {
    Serial.println("Received F");
    isCollecting = false;  
    isBouncing = false;
    
    noTone(buzzerPin);
  } else if (str == "B") {
    Serial.println("Bouncing legs");
    // Ring buzzer   
    isBouncing = true;
  } else if (str == "X") {
    Serial.println("Stopped bouncing legs!");
    // Turn off buzzer
    isBouncing = false;
  }
}

