# SmartSpaces Cordova plugin

This is a native Cordova plugin for SmartSpaces Bluetooth Beacon system. It's compatible with Android (iOS support is coming).

The library has three methods:

* `registerForBeacons(String)`: start the background worker for beacon detection (the parameter is the server base URL)
* `disableBeaconDetection()`: disable the background beacon detection worker

**Note:** due to some incompatibility with other plugins that declare an android application, now it should be done manually: you can either call methods from your own application, or set the embedded one (legacy behavior) adding this to the `android` platform in your `config.xml`:

	<edit-config file="AndroidManifest.xml" target="/manifest/application" mode="merge">
		<application android:name="it.sapienzaapps.cordova.smartspaces.SmartSpacesApplication" />
	</edit-config>
