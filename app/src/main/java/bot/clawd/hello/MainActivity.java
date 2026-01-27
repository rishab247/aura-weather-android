package bot.clawd.hello;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private TextView locationText, tempText, conditionText, humidityText, windText, emojiText, feelsLikeText, insightText, loaderText;
    private TextView aqiValue, aqiBadge, pm25Value, pm10Value, no2Value, aqiAdvice;
    private TextView moonEmoji, moonPhaseText, moonIllumination, sunriseCompact, outfitText;
    private View weatherContent, rootLayout, loaderOverlay, aqiCard, refreshBtn;
    private AutoCompleteTextView searchInput;
    private LinearLayout savedCitiesContainer, forecastContainer, hourlyContainer;
    private ImageButton saveCityBtn;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isBusy = false;
    private String lastLat = "28.6139", lastLon = "77.209", currentCity = "Delhi";
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
        fetchWeather("Delhi");
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
            fetchWeather(city);
            hideKeyboard();
        }
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
            tv.setOnClickListener(v -> { if (!isBusy) fetchWeather(city); });
            savedCitiesContainer.addView(tv);
        }
    }

    private void requestLocation() {
        if (isBusy) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            return;
        }
        
        isBusy = true;
        loaderOverlay.setVisibility(View.VISIBLE);
        loaderText.setText("Locating...");
        
        try {
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (loc != null) {
                reverseGeocode(loc.getLatitude(), loc.getLongitude());
            } else {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                    @Override public void onLocationChanged(Location l) { reverseGeocode(l.getLatitude(), l.getLongitude()); }
                    @Override public void onProviderDisabled(String p) { handler.post(() -> { loaderOverlay.setVisibility(View.GONE); isBusy = false; }); }
                    @Override public void onProviderEnabled(String p) {}
                    @Override public void onStatusChanged(String p, int s, Bundle e) {}
                }, Looper.getMainLooper());
            }
        } catch (SecurityException e) {
            loaderOverlay.setVisibility(View.GONE);
            isBusy = false;
        }
    }
    
    private void reverseGeocode(double lat, double lon) {
        executor.execute(() -> {
            try {
                String json = fetchUrl("https://nominatim.openstreetmap.org/reverse?lat=" + lat + "&lon=" + lon + "&format=json");
                String city = getVal(json, "city");
                if (city.equals("--")) city = getVal(json, "town");
                if (city.equals("--")) city = getVal(json, "state");
                if (city.equals("--")) city = "Your Location";
                final String fCity = city;
                handler.post(() -> fetchWeatherByCoords(String.valueOf(lat), String.valueOf(lon), fCity));
            } catch (Exception e) {
                handler.post(() -> { loaderOverlay.setVisibility(View.GONE); isBusy = false; });
            }
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == LOCATION_PERMISSION_CODE && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) requestLocation();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private void fetchWeather(String city) {
        if (isBusy) return;
        isBusy = true;
        loaderOverlay.setVisibility(View.VISIBLE);
        loaderText.setText("Loading...");
        
        executor.execute(() -> {
            try {
                String enc = URLEncoder.encode(city, "UTF-8");
                String geo = fetchUrl("https://geocoding-api.open-meteo.com/v1/search?name=" + enc + "&count=1&language=en");
                String lat = getVal(geo, "latitude");
                String lon = getVal(geo, "longitude");
                String name = getVal(geo, "name");
                if (name.equals("--")) name = city;
                final String fName = name;
                handler.post(() -> fetchWeatherByCoords(lat, lon, fName));
            } catch (Exception e) {
                handler.post(() -> { loaderOverlay.setVisibility(View.GONE); conditionText.setText("NOT FOUND"); isBusy = false; });
            }
        });
    }
    
    private void fetchWeatherByCoords(String lat, String lon, String city) {
        if (!isBusy) { isBusy = true; loaderOverlay.setVisibility(View.VISIBLE); }
        lastLat = lat; lastLon = lon; currentCity = city;
        
        executor.execute(() -> {
            try {
                String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + 
                    "&current_weather=true&hourly=temperature_2m,weathercode,relativehumidity_2m,apparent_temperature" +
                    "&daily=weathercode,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max&timezone=auto";
                String json = fetchUrl(url);
                
                // Current weather
                int cw = json.indexOf("\"current_weather\":");
                String curr = json.substring(cw, json.indexOf("}", cw) + 1);
                double temp = dbl(getVal(curr, "temperature"));
                double wind = dbl(getVal(curr, "windspeed"));
                String code = getVal(curr, "weathercode");
                int hour = 12;
                try { hour = Integer.parseInt(getVal(curr, "time").substring(11, 13)); } catch (Exception e) {}
                
                // Hourly
                int hi = json.indexOf("\"hourly\":");
                String hs = json.substring(hi);
                String[] hTemps = arr(hs, "temperature_2m");
                String[] hCodes = arr(hs, "weathercode");
                String[] hHumid = arr(hs, "relativehumidity_2m");
                String[] hFeels = arr(hs, "apparent_temperature");
                String[] hTimes = arr(hs, "time");
                
                String humidity = hour < hHumid.length ? hHumid[hour] : "0";
                double feels = hour < hFeels.length ? dbl(hFeels[hour]) : temp;
                
                // Daily
                int di = json.indexOf("\"daily\":");
                String ds = json.substring(di);
                String[] dCodes = arr(ds, "weathercode");
                String[] dMax = arr(ds, "temperature_2m_max");
                String[] dMin = arr(ds, "temperature_2m_min");
                String[] dRain = arr(ds, "precipitation_probability_max");
                String[] sunrise = arr(ds, "sunrise");
                String[] sunset = arr(ds, "sunset");
                
                // AQI
                String aqi = "";
                try { aqi = fetchUrl("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + lat + "&longitude=" + lon + "&current=european_aqi,pm10,pm2_5,nitrogen_dioxide"); } catch (Exception e) {}
                
                final String fAqi = aqi, fCode = code, fHumidity = humidity;
                final double fTemp = temp, fWind = wind, fFeels = feels;
                final String[] fhTemps = hTemps, fhCodes = hCodes, fhTimes = hTimes;
                final String[] fdCodes = dCodes, fdMax = dMax, fdMin = dMin, fdRain = dRain;
                final String fSunrise = sunrise.length > 0 ? sunrise[0] : "", fSunset = sunset.length > 0 ? sunset[0] : "";
                final int fHour = hour;

                handler.post(() -> {
                    currentCity = city;
                    locationText.setText(city);
                    tempText.setText(Math.round(fTemp) + "°");
                    conditionText.setText(textForCode(fCode).toUpperCase());
                    emojiText.setText(emojiForCode(fCode));
                    humidityText.setText(fHumidity + "%");
                    windText.setText(Math.round(fWind) + " km/h");
                    feelsLikeText.setText(Math.round(fFeels) + "°");
                    
                    updateOutfit(fTemp, fCode, Integer.parseInt(fHumidity));
                    updateBackground(fCode);
                    updateMoon();
                    updateSunTimes(fSunrise, fSunset);
                    updateSaveIcon();
                    buildHourly(fhTemps, fhCodes, fhTimes, fHour);
                    buildForecast(fdCodes, fdMax, fdMin, fdRain);
                    parseAqi(fAqi);
                    
                    loaderOverlay.setVisibility(View.GONE);
                    Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_slide_up);
                    weatherContent.startAnimation(anim);
                    isBusy = false;
                });
            } catch (Exception e) {
                handler.post(() -> { loaderOverlay.setVisibility(View.GONE); conditionText.setText("ERROR"); isBusy = false; });
            }
        });
    }
    
    private void updateOutfit(double temp, String code, int humidity) {
        String outfit;
        int c = num(code);
        
        if (c >= 95) outfit = "🌂 Carry umbrella, expect storms!";
        else if (c >= 61) outfit = "☔ Raincoat or umbrella needed";
        else if (c >= 71 && c <= 77) outfit = "🧥 Heavy jacket for snow";
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
    
    private void buildHourly(String[] temps, String[] codes, String[] times, int currentHour) {
        hourlyContainer.removeAllViews();
        
        for (int i = 0; i < 12 && (currentHour + i) < temps.length; i++) {
            int idx = currentHour + i;
            
            String timeLabel;
            if (i == 0) {
                timeLabel = "Now";
            } else {
                try {
                    int h = Integer.parseInt(times[idx].substring(11, 13));
                    timeLabel = (h == 0) ? "12a" : (h < 12) ? h + "a" : (h == 12) ? "12p" : (h - 12) + "p";
                } catch (Exception e) { timeLabel = "--"; }
            }
            
            String emoji = emojiForCode(codes[idx]);
            int temp = num(temps[idx]);
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
            emojiTv.setText(emoji);
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
            hourlyContainer.addView(card);
        }
    }
    
    private void buildForecast(String[] codes, String[] max, String[] min, String[] rain) {
        forecastContainer.removeAllViews();
        String[] days = {"Today", "Tomorrow", "Day 3", "Day 4", "Day 5"};
        int count = Math.min(5, codes.length);
        
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
            dayTv.setText(days[i]);
            dayTv.setTextColor(i == 0 ? 0xFF00BFA5 : 0xAAFFFFFF);
            dayTv.setTextSize(11);
            dayTv.setGravity(android.view.Gravity.CENTER);
            
            TextView emojiTv = new TextView(this);
            emojiTv.setText(emojiForCode(codes[i]));
            emojiTv.setTextSize(30);
            emojiTv.setPadding(0, 12, 0, 12);
            emojiTv.setGravity(android.view.Gravity.CENTER);
            
            TextView tempTv = new TextView(this);
            tempTv.setText(num(max[i]) + "° / " + num(min[i]) + "°");
            tempTv.setTextColor(0xFFFFFFFF);
            tempTv.setTextSize(14);
            tempTv.setTypeface(null, android.graphics.Typeface.BOLD);
            tempTv.setGravity(android.view.Gravity.CENTER);
            
            TextView rainTv = new TextView(this);
            rainTv.setText("💧 " + num(rain[i]) + "%");
            rainTv.setTextColor(0x88FFFFFF);
            rainTv.setTextSize(10);
            rainTv.setPadding(0, 8, 0, 0);
            rainTv.setGravity(android.view.Gravity.CENTER);
            
            card.addView(dayTv);
            card.addView(emojiTv);
            card.addView(tempTv);
            card.addView(rainTv);
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
    
    private void parseAqi(String json) {
        if (json == null || !json.contains("\"current\":")) { aqiCard.setVisibility(View.GONE); return; }
        
        try {
            int ci = json.indexOf("\"current\":");
            String curr = json.substring(ci);
            int aqi = num(getVal(curr, "european_aqi"));
            
            aqiValue.setText(String.valueOf(aqi));
            pm25Value.setText(String.valueOf(num(getVal(curr, "pm2_5"))));
            pm10Value.setText(String.valueOf(num(getVal(curr, "pm10"))));
            no2Value.setText(String.valueOf(num(getVal(curr, "nitrogen_dioxide"))));
            
            String level; int color; String advice;
            if (aqi <= 20) { level = "Excellent"; color = 0xFF00E676; advice = "Great for outdoor activities! 🏃"; }
            else if (aqi <= 40) { level = "Good"; color = 0xFF69F0AE; advice = "Air quality is good ☀️"; }
            else if (aqi <= 60) { level = "Moderate"; color = 0xFFFFEB3B; advice = "Sensitive groups take care"; }
            else if (aqi <= 80) { level = "Poor"; color = 0xFFFF9800; advice = "Limit outdoor exposure 😷"; }
            else if (aqi <= 100) { level = "Very Poor"; color = 0xFFFF5722; advice = "Avoid outdoor activities"; }
            else { level = "Hazardous"; color = 0xFFF44336; advice = "Stay indoors! 🚨"; }
            
            aqiBadge.setText(level);
            aqiBadge.setTextColor(color);
            aqiAdvice.setText(advice);
            aqiCard.setVisibility(View.VISIBLE);
        } catch (Exception e) { aqiCard.setVisibility(View.GONE); }
    }
    
    private void updateBackground(String code) {
        int c = num(code);
        int s, e;
        String insight;
        
        if (c == 0) { s = 0xFFFF6B35; e = 0xFFF7931E; insight = "Clear skies! Perfect weather for outdoor activities."; }
        else if (c <= 3) { s = 0xFF4A90A4; e = 0xFF205072; insight = "Partly cloudy. Pleasant conditions overall."; }
        else if (c <= 48) { s = 0xFF536976; e = 0xFF292E49; insight = "Foggy conditions. Drive carefully!"; }
        else if (c <= 67) { s = 0xFF1A2980; e = 0xFF26D0CE; insight = "Rainy weather. Don't forget your umbrella! ☔"; }
        else if (c <= 77) { s = 0xFF4B79A1; e = 0xFF283E51; insight = "Snowfall expected. Bundle up warm! ❄️"; }
        else if (c <= 82) { s = 0xFF2C3E50; e = 0xFF3498DB; insight = "Showers expected. Keep rain gear handy."; }
        else { s = 0xFF0F2027; e = 0xFF203A43; insight = "Thunderstorms! Stay safe indoors. ⚡"; }
        
        insightText.setText(insight);
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{s, e});
        rootLayout.setBackground(gd);
    }

    private double dbl(String v) { try { return Double.parseDouble(v.trim()); } catch (Exception e) { return 0; } }
    private int num(String v) { try { return (int) Math.round(Double.parseDouble(v.trim())); } catch (Exception e) { return 0; } }
    
    private String fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "AuraWeather/15");
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        return sb.toString();
    }

    private String getVal(String json, String key) {
        try {
            int i = json.indexOf("\"" + key + "\":");
            if (i < 0) return "--";
            int s = i + key.length() + 3;
            while (s < json.length() && " \"".indexOf(json.charAt(s)) >= 0) s++;
            int e = s;
            while (e < json.length() && ",}\"]\n".indexOf(json.charAt(e)) < 0) e++;
            return json.substring(s, e).trim();
        } catch (Exception e) { return "--"; }
    }
    
    private String[] arr(String json, String key) {
        try {
            int i = json.indexOf("\"" + key + "\":");
            if (i < 0) return new String[0];
            int s = json.indexOf("[", i) + 1;
            int e = json.indexOf("]", s);
            String[] parts = json.substring(s, e).split(",");
            for (int j = 0; j < parts.length; j++) parts[j] = parts[j].trim().replace("\"", "");
            return parts;
        } catch (Exception e) { return new String[0]; }
    }

    private String emojiForCode(String code) {
        int c = num(code);
        if (c == 0) return "☀️";
        if (c <= 3) return "⛅";
        if (c <= 48) return "🌫️";
        if (c <= 57) return "🌧️";
        if (c <= 67) return "🌧️";
        if (c <= 77) return "❄️";
        if (c <= 82) return "🌦️";
        return "⛈️";
    }

    private String textForCode(String code) {
        int c = num(code);
        if (c == 0) return "Clear Sky";
        if (c <= 3) return "Partly Cloudy";
        if (c <= 48) return "Foggy";
        if (c <= 67) return "Rainy";
        if (c <= 77) return "Snowy";
        if (c <= 82) return "Showers";
        return "Thunderstorm";
    }
}
