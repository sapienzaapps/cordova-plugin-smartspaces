# SmartSpaces Cordova plugin

This is a native Cordova plugin for SmartSpaces Bluetooth Beacon system. It's compatible with Android (iOS support is coming).

The library has three methods:

* `setServerURL(String)`: sets the server base URL and creates the necessary HTTP Client objects. Can be called multiple times, **but at least once before other methods**.
* `registerForBeacons()`: start the background worker for beacon detection
* `disableBeaconDetection()`: disable the background beacon detection worker