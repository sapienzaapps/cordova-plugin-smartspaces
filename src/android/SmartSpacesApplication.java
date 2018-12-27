package it.sapienzaapps.cordova.smartspaces;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.Context;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.google.gson.Gson;

import org.apache.cordova.LOG;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import retrofit2.Retrofit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;

public class SmartSpacesApplication extends Application implements BootstrapNotifier {
	private static final String TAG = "SmartSpacesPlugin";
	
	private BeaconManager beaconManager;
	private RegionBootstrap regionBootstrap;
	private BackgroundPowerSaver backgroundPowerSaver;

	private ISmartSpacesAPI api;
	private String deviceId;

	@Override
	public void onCreate() {
		super.onCreate();

		deviceId = UUID.randomUUID().toString();

		this.beaconManager = BeaconManager.getInstanceForApplication(this);

		this.beaconManager.setDebug(true);
		this.beaconManager.setRegionStatePersistenceEnabled(false);
		this.beaconManager.setEnableScheduledScanJobs(true);
		// beaconManager.setBackgroundBetweenScanPeriod(0);
		// beaconManager.setBackgroundScanPeriod(1100);

		this.beaconManager.getBeaconParsers().clear();
		this.beaconManager.getBeaconParsers().add(new BeaconParser().
				setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));
		this.beaconManager.getBeaconParsers().add(new BeaconParser().
				setBeaconLayout("x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"));
		this.beaconManager.getBeaconParsers().add(new BeaconParser().
				setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
		this.beaconManager.getBeaconParsers().add(new BeaconParser().
				setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"));
		this.beaconManager.getBeaconParsers().add(new BeaconParser().
				setBeaconLayout("s:0-1=fed8,m:2-2=00,p:3-3:-41,i:4-21v"));
		this.beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
		this.beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

		backgroundPowerSaver = new BackgroundPowerSaver(this);

		// Load values for background thread
		Gson gson = new Gson();
		SharedPreferences sharedPref = this.getSharedPreferences("it.sapienzaapps.cordova.smartspaces.smartspaces", Context.MODE_PRIVATE);
		String serverURL = sharedPref.getString("server_url", "");
		if (!serverURL.isEmpty()) {
			this.setServerURL(serverURL);
			SmartSpacesBeacon[] beaconList = gson.fromJson(sharedPref.getString("beacon_list", "[]"), SmartSpacesBeacon[].class);
			this.createRegionBootstrap(beaconList);
		}
	}

	private void setServerURL(String url) {
		Retrofit retrofit = new Retrofit.Builder()
			.baseUrl(url)
			.addConverterFactory(GsonConverterFactory.create())
			.build();

		api = retrofit.create(ISmartSpacesAPI.class);

		// Save URL for background thread
		SharedPreferences sharedPref = this.getSharedPreferences("it.sapienzaapps.cordova.smartspaces.smartspaces", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putString("server_url", url);
		editor.commit();

		LOG.d(TAG, "Server URL set to " + url);
	}

	public void disableBeaconDetection() {
		if (regionBootstrap == null) {
			LOG.i(TAG, "RegionBootstrap object not initialized");
			return;
		}
		LOG.d(TAG, "Disabling bluetooth beacons detection");
		this.regionBootstrap.disable();
		this.regionBootstrap = null;
	}

	/**
	 * This method should be called when the user grants necessary permissions
	 * It creates the altBeacon background thread and register each beacon
	 */
	public void registerForBeacons(String url) {
		this.setServerURL(url);
		if (api == null) {
			LOG.e(TAG, "API object not initialized");
			return;
		}
		LOG.d(TAG, "Getting beacon list");
		api.getBeaconList().enqueue(new Callback<SmartSpacesBeacon[]>() {
			@Override
			public void onResponse(Call<SmartSpacesBeacon[]> call, Response<SmartSpacesBeacon[]> response) {
				if (response.code() < 300) {
					SmartSpacesBeacon[] beaconList = response.body();
					if (beaconList == null) {
						LOG.w(TAG, "Beacon list null");
						return;
					}
					LOG.d(TAG, "Beacon list received with " + beaconList.length + " beacons");

					// Save the list for background thread
					Gson gson = new Gson();
					SharedPreferences sharedPref = SmartSpacesApplication.this.getSharedPreferences("it.sapienzaapps.cordova.smartspaces.smartspaces", Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = sharedPref.edit();
					editor.putString("beacon_list", gson.toJson(beaconList));
					editor.commit();

					createRegionBootstrap(beaconList);
				}
			}

			@Override
			public void onFailure(Call<SmartSpacesBeacon[]> call, Throwable t) {
				// Fail silently
			}
		});
	}

	private void createRegionBootstrap(SmartSpacesBeacon[] beaconList) {
		disableBeaconDetection();

		List<Region> regions = new ArrayList<>();
		for(SmartSpacesBeacon obj : beaconList) {
			Region region = new Region(
					"it.sapienzaapps.cordova.smartspaces." + obj.name,
					Identifier.parse(obj.uuid),
					Identifier.fromInt(obj.major),
					Identifier.fromInt(obj.minor)
				);
			regions.add(region);
		}
		regionBootstrap = new RegionBootstrap(this, regions);

		LOG.d(TAG, "BLE bootstrap with " + regions.size() + " beacons");
	}

	@Override
	public void didDetermineStateForRegion(int status, Region arg0) {
		LOG.d(TAG, "didDetermineStateForRegion(" + status +"):" + arg0.getUniqueId() + " " + arg0.getId1() + " " + arg0.getId2() + " " + arg0.getId3());
		if (status == 0) {
			this.didExitRegion(arg0);
		} else {
			this.didEnterRegion(arg0);
		}
	}

	@Override
	public void didEnterRegion(Region arg0) {
		LOG.d(TAG, "didEnterRegion:" + arg0.getUniqueId() + " " + arg0.getId1() + " " + arg0.getId2() + " " + arg0.getId3());

		api.performEnter(deviceId, arg0.getId1().toString(), arg0.getId2().toInt(), arg0.getId3().toInt())
			.enqueue(new Callback<Void>() {
				@Override
				public void onResponse(Call<Void> call, Response<Void> response) {
					// Ok
				}

				@Override
				public void onFailure(Call<Void> call, Throwable t) {
					// Fail silently
				}
			});
	}

	@Override
	public void didExitRegion(Region arg0) {
		LOG.d(TAG, "didExitRegion:" + arg0.getUniqueId() + " " + arg0.getId1() + " " + arg0.getId2() + " " + arg0.getId3());

		api.performExit(deviceId, arg0.getId1().toString(), arg0.getId2().toInt(), arg0.getId3().toInt())
			.enqueue(new Callback<Void>() {
				@Override
				public void onResponse(Call<Void> call, Response<Void> response) {
					// Ok
				}

				@Override
				public void onFailure(Call<Void> call, Throwable t) {
					// Fail silently
				}
			});
	}
}