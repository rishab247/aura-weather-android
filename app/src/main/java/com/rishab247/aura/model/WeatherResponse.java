package com.rishab247.aura.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {
    @SerializedName("current_weather")
    public CurrentWeather current;
    
    @SerializedName("hourly")
    public Hourly hourly;
    
    @SerializedName("daily")
    public Daily daily;

    public static class CurrentWeather {
        public double temperature;
        public double windspeed;
        public int weathercode;
        public String time;
    }

    public static class Hourly {
        public List<String> time;
        @SerializedName("temperature_2m")
        public List<Double> temperature2m;
        @SerializedName("weathercode")
        public List<Integer> weathercode;
        @SerializedName("relativehumidity_2m")
        public List<Integer> humidity;
        @SerializedName("apparent_temperature")
        public List<Double> apparentTemperature;
    }

    public static class Daily {
        public List<String> time;
        @SerializedName("weathercode")
        public List<Integer> weathercode;
        @SerializedName("temperature_2m_max")
        public List<Double> tempMax;
        @SerializedName("temperature_2m_min")
        public List<Double> tempMin;
        @SerializedName("precipitation_probability_max")
        public List<Integer> precipitationProb;
        public List<String> sunrise;
        public List<String> sunset;
    }
}
