package com.rishab247.aura.api;

import com.rishab247.aura.model.GeocodingListResponse;
import com.rishab247.aura.model.WeatherResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenMeteoApi {
    @GET("v1/forecast")
    Call<WeatherResponse> getWeather(
        @Query("latitude") double lat,
        @Query("longitude") double lon,
        @Query("current_weather") boolean current,
        @Query("hourly") String hourly,
        @Query("daily") String daily,
        @Query("timezone") String timezone
    );
}
