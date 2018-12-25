package it.sapienzaapps.cordova.smartspaces;

import org.apache.cordova.LOG;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

public class SmartSpacesPlugin extends CordovaPlugin {
	private static final String TAG = "SmartSpacesPlugin";

	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);

		LOG.d(TAG, "Initializing " + TAG);
	}

	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

		if ("registerForBeacons".equals(action)) {
			final SmartSpacesApplication a = (SmartSpacesApplication) this.cordova.getActivity().getApplication();
			this.cordova.getThreadPool().execute(new Runnable() {
				@Override
				public void run() {
					try {
						a.registerForBeacons(args.getString(0));
					} catch(JSONException ex) {
						LOG.e(TAG, "JSON Exception on registerForBeacons: " + ex.toString());
					}
				}
			});
		} else if ("disableBeaconDetection".equals(action)) {
			final SmartSpacesApplication a = (SmartSpacesApplication) this.cordova.getActivity().getApplication();
			this.cordova.getThreadPool().execute(new Runnable() {
				@Override
				public void run() {
					a.disableBeaconDetection();
				}
			});
		}

		return true;
	}
}
