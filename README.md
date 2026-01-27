# 🌤️ Aura Weather

A premium, hyper-local weather application for Android, built with a focus on high-quality meteorological data and a modern, "magazine-style" UI.

![Version](https://img.shields.io/badge/version-15.0-00BFA5?style=for-the-badge)
![Platform](https://img.shields.io/badge/platform-Android-3DDC84?style=for-the-badge&logo=android)

## ✨ Features

### 🟢 Core Essentials
- **Hyper-Local Weather**: Real-time updates powered by the Open-Meteo Engine.
- **GPS Auto-Location**: One-tap detection for instant local weather.
- **Precision Search**: Worldwide city search with autocomplete (optimized for Indian cities).
- **Air Quality Index (AQI)**: Detailed breakdown of PM2.5, PM10, and NO₂ with color-coded health advice.

### 🟡 Visual Experience
- **12-Hour Timeline**: Detailed hourly forecast with a clean, scrollable UI.
- **5-Day Forecast**: High-resolution cards showing temperature ranges and precipitation.
- **Moon Phase Widget**: Real-time lunar phase, emoji representation, and illumination percentage.
- **Dynamic Backgrounds**: UI themes that shift based on current weather conditions (Clear, Rain, Cloud, etc.).

### 🔵 Premium & Social
- **Saved Cities (Favorites)**: Quick access to your most-searched locations with persistent storage.
- **Smart Outfit Suggestions**: Intelligent clothing recommendations based on temperature and conditions.
- **Weather Sharing**: Share current weather stats directly to WhatsApp or Instagram as clean text.
- **Sun Times**: Visual tracking of Sunrise and Sunset.

## 🛠️ Tech Stack

- **Language**: Native Java
- **UI**: XML with custom Vector Drawables and Gradient Systems
- **API**: Open-Meteo (Weather & Air Quality), Nominatim (Reverse Geocoding)
- **Minimum SDK**: Android 24 (7.0)
- **Target SDK**: Android 34 (14.0)

## 🚀 Build Instructions

This project is designed to be built using standard Android Build Tools without requiring a heavy IDE like Android Studio.

### Prerequisites
- JDK 17
- Android SDK (Build Tools 34.0.0+)

### Commands
```bash
# 1. Compile Resources
aapt2 compile app/src/main/res/drawable/*.xml -o res_compiled/
aapt2 compile app/src/main/res/layout/*.xml -o res_compiled/
aapt2 compile app/src/main/res/values/*.xml -o res_compiled/
aapt2 compile app/src/main/res/anim/*.xml -o res_compiled/

# 2. Link & Generate Base APK
aapt2 link -I $ANDROID_JAR --manifest app/src/main/AndroidManifest.xml -o base.apk --java app/src/main/java res_compiled/*.flat

# 3. Compile Java Source
javac -d obj -cp $ANDROID_JAR $(find app/src/main/java -name "*.java")

# 4. Dexing
d8 --lib $ANDROID_JAR --output bin $(find obj -name "*.class")

# 5. Package & Sign
jar uf base.apk -C bin classes.dex
zipalign -f -v 4 base.apk aligned.apk
apksigner sign --ks your-key.keystore --out aura-weather.apk aligned.apk
```

## 📜 License

Created by **[rishab247](https://github.com/rishab247)**. Distributed under the MIT License.
