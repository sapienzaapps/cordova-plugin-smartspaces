package it.sapienzaapps.cordova.smartspaces;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Path;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;

public interface ISmartSpacesAPI {
	
	@GET("regions")
	Call<SmartSpacesBeacon[]> getBeaconList();

	@PUT("regions/{uuid}/{major}/{minor}/{deviceid}")
	Call<Void> performEnter(@Path("deviceid") String deviceid, @Path("uuid") String uuid, @Path("major") int major, @Path("minor") int minor);

	@DELETE("regions/{uuid}/{major}/{minor}/{deviceid}")
	Call<Void> performExit(@Path("deviceid") String deviceid, @Path("uuid") String uuid, @Path("major") int major, @Path("minor") int minor);

}