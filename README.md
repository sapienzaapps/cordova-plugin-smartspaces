# SmartSpaces Cordova plugin

This is a native Cordova plugin for SmartSpaces Bluetooth Beacon system. It's compatible with Android (iOS support is coming).

The library has three methods:

* `registerForBeacons(String)`: start the background worker for beacon detection (the parameter is the server base URL)
* `disableBeaconDetection()`: disable the background beacon detection worker
