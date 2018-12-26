package it.sapienzaapps.cordova.smartspaces;

import android.app.Application;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

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
	
	private RegionBootstrap regionBootstrap;
	private BackgroundPowerSaver backgroundPowerSaver;

	private ISmartSpacesAPI api;
	private String deviceId;

	@Override
	public void onCreate() {
		super.onCreate();

		deviceId = UUID.randomUUID().toString();
	}

	private void setServerURL(String url) {
		Retrofit retrofit = new Retrofit.Builder()
			.baseUrl(url)
			.addConverterFactory(GsonConverterFactory.create())
			.build();

		api = retrofit.create(ISmartSpacesAPI.class);
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
		api.getBeaconList().enqueue(new Callback<List<SmartSpacesBeacon>>() {
			@Override
			public void onResponse(Call<List<SmartSpacesBeacon>> call, Response<List<SmartSpacesBeacon>> response) {
				if (response.code() < 300) {
					beaconListReceived(response.body());
				}
			}

			@Override
			public void onFailure(Call<List<SmartSpacesBeacon>> call, Throwable t) {
				// Fail silently
			}
		});
	}

	private void beaconListReceived(List<SmartSpacesBeacon> beaconList) {
		LOG.d(TAG, "Beacon list received with " + beaconList.size() + " beacons");

		BeaconManager beaconManager = BeaconManager.getInstanceForApplication(this);
		beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
		if (backgroundPowerSaver == null) {
			backgroundPowerSaver = new BackgroundPowerSaver(this);
		}

		List<Region> regions = new ArrayList<>();
		for (int i = 0; i < beaconList.size(); i++) {
			SmartSpacesBeacon obj = beaconList.get(i);
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