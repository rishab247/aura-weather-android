package com.rishab247.aura.api;

import com.rishab247.aura.model.GeocodingListResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GeocodingApi {
    @GET("v1/search")
    Call<GeocodingListResponse> searchCity(
        @Query("name") String name,
        @Query("count") int count,
        @Query("language") String language,
        @Query("format") String format
    );
}
