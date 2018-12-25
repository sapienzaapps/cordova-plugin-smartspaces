package it.sapienzaapps.cordova.smartspaces;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ISmartSpacesAPI {
	
	@GET("regions")
	Call<List<SmartSpacesBeacon>> getBeaconList();

	@FormUrlEncoded
	@POST("enter")
	Call<Void> performEnter(@Field("deviceid") String deviceid, @Field("uuid") String uuid, @Field("major") int major, @Field("minor") int minor);

	@FormUrlEncoded
	@POST("exit")
	Call<Void> performExit(@Field("deviceid") String deviceid, @Field("uuid") String uuid, @Field("major") int major, @Field("minor") int minor);

}