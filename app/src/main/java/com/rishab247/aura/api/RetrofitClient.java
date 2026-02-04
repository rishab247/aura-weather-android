package com.rishab247.aura.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit weatherRetrofit;
    private static Retrofit geocodingRetrofit;

    public static OpenMeteoApi getWeatherApi() {
        if (weatherRetrofit == null) {
            weatherRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.open-meteo.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return weatherRetrofit.create(OpenMeteoApi.class);
    }

    public static GeocodingApi getGeocodingApi() {
        if (geocodingRetrofit == null) {
            geocodingRetrofit = new Retrofit.Builder()
                .baseUrl("https://geocoding-api.open-meteo.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        }
        return geocodingRetrofit.create(GeocodingApi.class);
    }
}
