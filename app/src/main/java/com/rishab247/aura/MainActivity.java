package com.rishab247.aura;

import android.Manifest;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.rishab247.aura.api.GeocodingApi;
import com.rishab247.aura.api.OpenMeteoApi;
import com.rishab247.aura.api.RetrofitClient;
import com.rishab247.aura.model.GeocodingListResponse;
import com.rishab247.aura.model.GeocodingResponse;
import com.rishab247.aura.model.WeatherResponse;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends Activity {
    private TextView locationText, tempText, conditionText, humidityText, windText, emojiText, feelsLikeText, insightText, loaderText;
    private TextView aqiValue, aqiBadge, pm25Value, pm10Value, no2Value, aqiAdvice;
    private TextView moonEmoji, moonPhaseText, moonIllumination, sunriseCompact, outfitText;
    private View weatherContent, rootLayout, loaderOverlay, aqiCard, refreshBtn;
    private AutoCompleteTextView searchInput;
    private LinearLayout savedCitiesContainer, forecastContainer, hourlyContainer;
    private ImageButton saveCityBtn;
    private boolean isBusy = false;
    private double lastLat = 28.6139, lastLon = 77.209;
    private String currentCity = "Delhi";
    private LocationManager locationManager;
    private SharedPreferences prefs;
    private Set<String> savedCities;
    
    private static final int LOCATION_PERMISSION_CODE = 1001;
    
    private static final String[] CITIES = {
        "Mumbai", "Delhi", "Bangalore", "Hyderabad", "Ahmedabad", "Chennai", "Kolkata", "Surat", 
        "Pune", "Jaipur", "Lucknow", "Kanpur", "Nagpur", "Indore", "Bhopal", "Patna", "Vadodara", 
        "Ghaziabad", "Ludhiana", "Agra", "Nashik", "Faridabad", "Varanasi", "Srinagar", "Amritsar", 
        "Ranchi", "Coimbatore", "Vijayawada", "Jodhpur", "Madurai", "Raipur", "Kota", "Guwahati", 
        "Chandigarh", "Noida", "Kochi", "Dehradun", "Mangalore", "Udaipur", "New York", "London",
        "Tokyo", "Singapore", "Dubai", "Paris", "Sydney", "Toronto", "Berlin", "Moscow"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences("aura_prefs", MODE_PRIVATE);
        savedCities = new HashSet<>(prefs.getStringSet("saved_cities", new HashSet<>()));
        
        initViews();
        setupListeners();
        updateSavedCitiesUI();
        
        // Initial load
        fetchWeatherByCoords(lastLat, lastLon, currentCity);
    }
    
    private void initViews() {
        rootLayout = findViewById(R.id.root_layout);
        loaderOverlay = findViewById(R.id.loader_overlay);
        loaderText = findViewById(R.id.loader_text);
        weatherContent = findViewById(R.id.weather_content);
        locationText = findViewById(R.id.location_text);
        tempText = findViewById(R.id.temp_text);
        conditionText = findViewById(R.id.condition_text);
        humidityText = findViewById(R.id.humidity_text);
        windText = findViewById(R.id.wind_text);
        emojiText = findViewById(R.id.condition_emoji);
        feelsLikeText = findViewById(R.id.feels_like_text);
        insightText = findViewById(R.id.insight_text);
        searchInput = findViewById(R.id.search_input);
        savedCitiesContainer = findViewById(R.id.saved_cities_container);
        forecastContainer = findViewById(R.id.forecast_container);
        hourlyContainer = findViewById(R.id.hourly_container);
        refreshBtn = findViewById(R.id.refresh_btn);
        outfitText = findViewById(R.id.outfit_text);
        sunriseCompact = findViewById(R.id.sunrise_compact);
        saveCityBtn = findViewById(R.id.save_city_btn);
        
        // Enable layout transitions for smooth container resizing
        if (weatherContent instanceof ViewGroup) {
            LayoutTransition lt = new LayoutTransition();
            lt.enableTransitionType(LayoutTransition.CHANGING);
            ((ViewGroup) weatherContent).setLayoutTransition(lt);
        }
        
        aqiCard = findViewById(R.id.aqi_card);
        aqiValue = findViewById(R.id.aqi_value);
        aqiBadge = findViewById(R.id.aqi_badge);
        pm25Value = findViewById(R.id.pm25_value);
        pm10Value = findViewById(R.id.pm10_value);
        no2Value = findViewById(R.id.no2_value);
        aqiAdvice = findViewById(R.id.aqi_advice);
        
        moonEmoji = findViewById(R.id.moon_emoji);
        moonPhaseText = findViewById(R.id.moon_phase_text);
        moonIllumination = findViewById(R.id.moon_illumination);
        
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        searchInput.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, CITIES));
    }
    
    private void setupListeners() {
        ImageButton searchBtn = findViewById(R.id.search_btn);
        ImageButton gpsBtn = findViewById(R.id.gps_btn);
        ImageButton shareBtn = findViewById(R.id.share_btn);
        
        searchBtn.setOnClickListener(v -> searchCity());
        searchInput.setOnEditorActionListener((v, actionId, event) -> { searchCity(); return true; });
        searchInput.setOnItemClickListener((p, v, pos, id) -> { searchCity(); });
        gpsBtn.setOnClickListener(v -> requestLocation());
        refreshBtn.setOnClickListener(v -> { if (!isBusy) fetchWeatherByCoords(lastLat, lastLon, currentCity); });
        
        shareBtn.setOnClickListener(v -> shareWeather());
        saveCityBtn.setOnClickListener(v -> toggleSaveCity());
    }
    
    private void searchCity() {
        if (isBusy) return;
        String city = searchInput.getText().toString().trim();
        if (!city.isEmpty()) {
            performCitySearch(city);
            hideKeyboard();
        }
    }
    
    private void performCitySearch(String query) {
        showLoader("Searching...");
        GeocodingApi api = RetrofitClient.getGeocodingApi();
        api.searchCity(query, 1, "en", "json").enqueue(new Callback<GeocodingListResponse>() {
            @Override
            public void onResponse(Call<GeocodingListResponse> call, Response<GeocodingListResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().results != null && !response.body().results.isEmpty()) {
                    GeocodingResponse place = response.body().results.get(0);
                    fetchWeatherByCoords(place.latitude, place.longitude, place.name);
                } else {
                    hideLoader();
                    Toast.makeText(MainActivity.this, "City not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeocodingListResponse> call, Throwable t) {
                hideLoader();
                Toast.makeText(MainActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void shareWeather() {
        String shareText = "🌤️ Weather in " + currentCity + "\n\n" +
            emojiText.getText() + " " + tempText.getText() + " - " + conditionText.getText() + "\n" +
            "💧 Humidity: " + humidityText.getText() + "\n" +
            "🌬️ Wind: " + windText.getText() + "\n" +
            "🌡️ Feels like: " + feelsLikeText.getText() + "\n\n" +
            "📱 Shared via Aura Weather";
        
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(intent, "Share Weather"));
    }
    
    private void toggleSaveCity() {
        if (savedCities.contains(currentCity)) {
            savedCities.remove(currentCity);
            Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else {
            if (savedCities.size() >= 8) {
                Toast.makeText(this, "Max 8 cities", Toast.LENGTH_SHORT).show();
                return;
            }
            savedCities.add(currentCity);
            Toast.makeText(this, "Added to favorites ⭐", Toast.LENGTH_SHORT).show();
        }
        prefs.edit().putStringSet("saved_cities", savedCities).apply();
        updateSavedCitiesUI();
        updateSaveIcon();
    }
    
    private void updateSaveIcon() {
        if (savedCities.contains(currentCity)) {
            saveCityBtn.setColorFilter(0xFFFFD600);
        } else {
            saveCityBtn.setColorFilter(0x66FFFFFF);
        }
    }
    
    private void updateSavedCitiesUI() {
        savedCitiesContainer.removeAllViews();
        // Animate adding children
        LayoutTransition lt = new LayoutTransition();
        savedCitiesContainer.setLayoutTransition(lt);
        
        for (String city : savedCities) {
            TextView tv = new TextView(this);
            tv.setText("⭐ " + city);
            tv.setTextColor(0xDDFFFFFF);
            tv.setBackgroundResource(R.drawable.card_bg);
            tv.setPadding(36, 16, 36, 16);
            tv.setTextSize(12);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, 12, 0);
            tv.setLayoutParams(p);
            tv.setOnClickListener(v -> { if (!isBusy) performCitySearch(city); });
            savedCitiesContainer.addView(tv);
        }
    }

    private void requestLocation() {
        if (isBusy) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }
        
        showLoader("Locating...");
        
        try {
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (loc != null) {
                fetchWeatherByCoords(loc.getLatitude(), loc.getLongitude(), "Your Location");
            } else {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                    @Override public void onLocationChanged(Location l) { fetchWeatherByCoords(l.getLatitude(), l.getLongitude(), "Your Location"); }
                    @Override public void onProviderDisabled(String p) { hideLoader(); isBusy = false; Toast.makeText(MainActivity.this, "GPS Disabled", Toast.LENGTH_SHORT).show(); }
                    @Override public void onProviderEnabled(String p) {}
                    @Override public void onStatusChanged(String p, int s, Bundle e) {}
                }, Looper.getMainLooper());
            }
        } catch (SecurityException e) {
            hideLoader();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] res) {
        if (req == LOCATION_PERMISSION_CODE && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) requestLocation();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private void showLoader(String text) {
        isBusy = true;
        loaderText.setText(text);
        loaderOverlay.setVisibility(View.VISIBLE);
        loaderOverlay.setAlpha(0f);
        loaderOverlay.animate().alpha(1f).setDuration(200).start();
    }

    private void hideLoader() {
        loaderOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> {
            loaderOverlay.setVisibility(View.GONE);
            isBusy = false;
        }).start();
    }
    
    private void fetchWeatherByCoords(double lat, double lon, String city) {
        showLoader("Forecasting...");
        lastLat = lat; lastLon = lon; currentCity = city;
        
        OpenMeteoApi api = RetrofitClient.getWeatherApi();
        api.getWeather(lat, lon, true, 
                "temperature_2m,weathercode,relativehumidity_2m,apparent_temperature", 
                "weathercode,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max", 
                "auto").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body(), city);
                } else {
                    hideLoader();
                    Toast.makeText(MainActivity.this, "API Error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                hideLoader();
                Toast.makeText(MainActivity.this, "Connection Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(WeatherResponse data, String city) {
        // Smooth transition for all text changes
        TransitionManager.beginDelayedTransition((ViewGroup) rootLayout, new AutoTransition().setDuration(400));
        
        locationText.setText(city);
        
        // Current Weather
        WeatherResponse.CurrentWeather curr = data.current;
        tempText.setText(Math.round(curr.temperature) + "°");
        conditionText.setText(textForCode(curr.weathercode).toUpperCase());
        
        // Use animation for emoji scaling
        emojiText.animate().scaleX(0f).scaleY(0f).setDuration(150).withEndAction(() -> {
            emojiText.setText(emojiForCode(curr.weathercode));
            emojiText.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
        }).start();
        
        windText.setText(Math.round(curr.windspeed) + " km/h");
        
        // Hourly Logic to find humidity/feelsLike for current hour
        int hourIndex = 0; // Default to first index (now)
        // Ideally we map 'time' string to index, but simple approximation:
        // API returns 0..168 hours starting from today 00:00. 
        // We assume index 0 is close enough or use current system hour to find offset.
        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        
        String humidity = "0";
        double feels = curr.temperature;
        
        if (data.hourly != null && data.hourly.humidity != null && data.hourly.apparentTemperature != null) {
             // Simple fallback: index matches hour if we assume data starts at midnight.
             // But OpenMeteo returns 'time' array. Let's find current hour string prefix.
             // Simplified: Just take index for current hour of day if list is long enough.
             if (currentHour < data.hourly.humidity.size()) {
                 humidity = String.valueOf(data.hourly.humidity.get(currentHour));
                 feels = data.hourly.apparentTemperature.get(currentHour);
             }
        }
        
        humidityText.setText(humidity + "%");
        feelsLikeText.setText(Math.round(feels) + "°");
        
        updateOutfit(curr.temperature, curr.weathercode, Integer.parseInt(humidity));
        updateBackground(curr.weathercode);
        updateMoon();
        
        if (data.daily != null && data.daily.sunrise != null && !data.daily.sunrise.isEmpty()) {
            updateSunTimes(data.daily.sunrise.get(0), data.daily.sunset.get(0));
        }
        
        updateSaveIcon();
        buildHourly(data.hourly, currentHour);
        buildForecast(data.daily);
        
        // Fake AQI update for now (or implement separate call)
        // parseAqi(...) 
        
        hideLoader();
        
        // Slide up animation for content
        weatherContent.setAlpha(0f);
        weatherContent.setTranslationY(100f);
        weatherContent.animate().alpha(1f).translationY(0f).setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
    }
    
    private void updateOutfit(double temp, int code, int humidity) {
        String outfit;
        
        if (code >= 95) outfit = "🌂 Carry umbrella, expect storms!";
        else if (code >= 61) outfit = "☔ Raincoat or umbrella needed";
        else if (code >= 71 && code <= 77) outfit = "🧥 Heavy jacket for snow";
        else if (temp < 10) outfit = "🧥 Warm jacket recommended";
        else if (temp < 18) outfit = "🧥 Light jacket weather";
        else if (temp < 25) outfit = "👕 Comfortable clothing";
        else if (temp < 32) outfit = "👕 Light & breathable clothes";
        else outfit = "🩳 Stay cool, drink water!";
        
        if (humidity > 80 && temp > 25) outfit = "💦 Hot & humid - light cotton wear";
        
        outfitText.setText(outfit);
    }
    
    private void updateSunTimes(String sr, String ss) {
        String sunrise = sr.contains("T") ? sr.substring(sr.indexOf("T") + 1) : "07:00";
        String sunset = ss.contains("T") ? ss.substring(ss.indexOf("T") + 1) : "18:00";
        sunriseCompact.setText("🌅 " + sunrise + "   🌇 " + sunset);
    }
    
    private void buildHourly(WeatherResponse.Hourly hourly, int currentHour) {
        hourlyContainer.removeAllViews();
        if (hourly == null) return;
        
        for (int i = 0; i < 12 && (currentHour + i) < hourly.time.size(); i++) {
            int idx = currentHour + i;
            
            String timeLabel;
            if (i == 0) {
                timeLabel = "Now";
            } else {
                try {
                    String t = hourly.time.get(idx);
                    int h = Integer.parseInt(t.substring(11, 13));
                    timeLabel = (h == 0) ? "12a" : (h < 12) ? h + "a" : (h == 12) ? "12p" : (h - 12) + "p";
                } catch (Exception e) { timeLabel = "--"; }
            }
            
            int code = hourly.weathercode.get(idx);
            int temp = (int) Math.round(hourly.temperature2m.get(idx));
            boolean isNow = (i == 0);
            
            // Card
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(android.view.Gravity.CENTER);
            
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(28);
            if (isNow) {
                bg.setColor(0x4400BFA5);
                bg.setStroke(3, 0xFF00BFA5);
            } else {
                bg.setColor(0x22FFFFFF);
            }
            card.setBackground(bg);
            card.setPadding(32, 28, 32, 28);
            
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, 14, 0);
            card.setLayoutParams(p);
            
            TextView timeTv = new TextView(this);
            timeTv.setText(timeLabel);
            timeTv.setTextColor(isNow ? 0xFF00BFA5 : 0xAAFFFFFF);
            timeTv.setTextSize(11);
            timeTv.setGravity(android.view.Gravity.CENTER);
            
            TextView emojiTv = new TextView(this);
            emojiTv.setText(emojiForCode(code));
            emojiTv.setTextSize(26);
            emojiTv.setPadding(0, 14, 0, 14);
            emojiTv.setGravity(android.view.Gravity.CENTER);
            
            TextView tempTv = new TextView(this);
            tempTv.setText(temp + "°");
            tempTv.setTextColor(0xFFFFFFFF);
            tempTv.setTextSize(16);
            tempTv.setTypeface(null, android.graphics.Typeface.BOLD);
            tempTv.setGravity(android.view.Gravity.CENTER);
            
            card.addView(timeTv);
            card.addView(emojiTv);
            card.addView(tempTv);
            
            // Staggered animation
            card.setAlpha(0f);
            card.setTranslationX(50f);
            card.animate().alpha(1f).translationX(0f).setStartDelay(i * 50L).setDuration(300).start();
            
            hourlyContainer.addView(card);
        }
    }
    
    private void buildForecast(WeatherResponse.Daily daily) {
        forecastContainer.removeAllViews();
        if (daily == null) return;
        
        String[] dayLabels = {"Today", "Tomorrow", "Day 3", "Day 4", "Day 5"};
        int count = Math.min(5, daily.time.size());
        
        for (int i = 0; i < count; i++) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setGravity(android.view.Gravity.CENTER);
            card.setBackgroundResource(R.drawable.card_bg);
            card.setPadding(36, 28, 36, 28);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, 12, 0);
            card.setLayoutParams(p);
            
            TextView dayTv = new TextView(this);
            dayTv.setText(dayLabels[i]);
            dayTv.setTextColor(i == 0 ? 0xFF00BFA5 : 0xAAFFFFFF);
            dayTv.setTextSize(11);
            dayTv.setGravity(android.view.Gravity.CENTER);
            
            TextView emojiTv = new TextView(this);
            emojiTv.setText(emojiForCode(daily.weathercode.get(i)));
            emojiTv.setTextSize(30);
            emojiTv.setPadding(0, 12, 0, 12);
            emojiTv.setGravity(android.view.Gravity.CENTER);
            
            TextView tempTv = new TextView(this);
            int max = (int) Math.round(daily.tempMax.get(i));
            int min = (int) Math.round(daily.tempMin.get(i));
            tempTv.setText(max + "° / " + min + "°");
            tempTv.setTextColor(0xFFFFFFFF);
            tempTv.setTextSize(14);
            tempTv.setTypeface(null, android.graphics.Typeface.BOLD);
            tempTv.setGravity(android.view.Gravity.CENTER);
            
            TextView rainTv = new TextView(this);
            rainTv.setText("💧 " + daily.precipitationProb.get(i) + "%");
            rainTv.setTextColor(0x88FFFFFF);
            rainTv.setTextSize(10);
            rainTv.setPadding(0, 8, 0, 0);
            rainTv.setGravity(android.view.Gravity.CENTER);
            
            card.addView(dayTv);
            card.addView(emojiTv);
            card.addView(tempTv);
            card.addView(rainTv);
            
            card.setAlpha(0f);
            card.setTranslationY(50f);
            card.animate().alpha(1f).translationY(0f).setStartDelay(i * 100L).setDuration(400).start();
            
            forecastContainer.addView(card);
        }
    }
    
    private void updateMoon() {
        Calendar cal = Calendar.getInstance();
        long days = (long)(367 * cal.get(Calendar.YEAR) - 7 * (cal.get(Calendar.YEAR) + (cal.get(Calendar.MONTH) + 10) / 12) / 4 + 275 * (cal.get(Calendar.MONTH) + 1) / 9 + cal.get(Calendar.DAY_OF_MONTH) - 730530);
        double phase = (days % 29.53) / 29.53;
        
        String emoji, name; int illum;
        if (phase < 0.0625) { emoji = "🌑"; name = "New Moon"; illum = 0; }
        else if (phase < 0.1875) { emoji = "🌒"; name = "Waxing Crescent"; illum = (int)(phase * 100); }
        else if (phase < 0.3125) { emoji = "🌓"; name = "First Quarter"; illum = 50; }
        else if (phase < 0.4375) { emoji = "🌔"; name = "Waxing Gibbous"; illum = (int)(phase * 100); }
        else if (phase < 0.5625) { emoji = "🌕"; name = "Full Moon"; illum = 100; }
        else if (phase < 0.6875) { emoji = "🌖"; name = "Waning Gibbous"; illum = (int)((1 - phase) * 100); }
        else if (phase < 0.8125) { emoji = "🌗"; name = "Last Quarter"; illum = 50; }
        else if (phase < 0.9375) { emoji = "🌘"; name = "Waning Crescent"; illum = (int)((1 - phase) * 100); }
        else { emoji = "🌑"; name = "New Moon"; illum = 0; }
        
        moonEmoji.setText(emoji);
        moonPhaseText.setText(name);
        moonIllumination.setText(illum + "% illuminated");
    }
    
    private void updateBackground(int code) {
        int s, e;
        String insight;
        
        if (code == 0) { s = 0xFFFF6B35; e = 0xFFF7931E; insight = "Clear skies! Perfect weather for outdoor activities."; }
        else if (code <= 3) { s = 0xFF4A90A4; e = 0xFF205072; insight = "Partly cloudy. Pleasant conditions overall."; }
        else if (code <= 48) { s = 0xFF536976; e = 0xFF292E49; insight = "Foggy conditions. Drive carefully!"; }
        else if (code <= 67) { s = 0xFF1A2980; e = 0xFF26D0CE; insight = "Rainy weather. Don't forget your umbrella! ☔"; }
        else if (code <= 77) { s = 0xFF4B79A1; e = 0xFF283E51; insight = "Snowfall expected. Bundle up warm! ❄️"; }
        else if (code <= 82) { s = 0xFF2C3E50; e = 0xFF3498DB; insight = "Showers expected. Keep rain gear handy."; }
        else { s = 0xFF0F2027; e = 0xFF203A43; insight = "Thunderstorms! Stay safe indoors. ⚡"; }
        
        insightText.setText(insight);
        
        // Smooth color transition could be added here with ObjectAnimator / ValueAnimator
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{s, e});
        rootLayout.setBackground(gd);
    }

    private String emojiForCode(int code) {
        if (code == 0) return "☀️";
        if (code <= 3) return "⛅";
        if (code <= 48) return "🌫️";
        if (code <= 57) return "🌧️";
        if (code <= 67) return "🌧️";
        if (code <= 77) return "❄️";
        if (code <= 82) return "🌦️";
        return "⛈️";
    }

    private String textForCode(int code) {
        if (code == 0) return "Clear Sky";
        if (code <= 3) return "Partly Cloudy";
        if (code <= 48) return "Foggy";
        if (code <= 67) return "Rainy";
        if (code <= 77) return "Snowy";
        if (code <= 82) return "Showers";
        return "Thunderstorm";
    }
}
