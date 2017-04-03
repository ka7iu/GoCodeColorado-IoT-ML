#import <Wire.h>

#include <dht11.h>

dht11 DHT11;

#define DHT11PIN 13

uint8_t humidity;

void setup() {
  Wire.begin(8);                // join i2c bus with address #8
  Wire.onRequest(requestEvent); // register event
}

void loop() {
  int chk = DHT11.read(DHT11PIN);
  humidity = DHT11.humidity;
  delay(1000);
}

void requestEvent() {
  Wire.write(humidity);
}
