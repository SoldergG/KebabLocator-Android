# Kebab Locator - Android

Native Android app (Kotlin + Jetpack Compose) for finding kebab restaurants and convenience stores nearby.

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM (ViewModel + StateFlow)
- **Maps**: Google Maps SDK + Maps Compose
- **Location**: FusedLocationProviderClient
- **Network**: OkHttp + Retrofit + Gson
- **Images**: Coil
- **Ads**: Google AdMob
- **Backend**: Supabase (PostgreSQL + REST API)
- **Data Sources**: Google Places, Yelp, OpenStreetMap, Foursquare

## Features

- Real-time location-based kebab shop search
- Multi-source search (Yelp, Google, OSM, Foursquare)
- Interactive Google Maps view
- Advanced filtering and sorting
- Favorites/bookmarking
- User shop submissions with photo upload
- Shop verification and reporting
- Convenience store search mode
- Google AdMob banner ads
- Dark glass UI design

## Setup

1. Open in Android Studio (Hedgehog or newer)
2. Add your `google-services.json` from Firebase Console
3. Sync Gradle
4. Add Google Maps API key in `AndroidManifest.xml`
5. Run on device/emulator (API 26+)

## Build APK

```bash
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/release/app-release.apk`

## Project Structure

```
app/src/main/java/com/kebablocator/android/
├── models/          # Data models (KebabShop, enums)
├── services/        # API services (Supabase, Google, Yelp, OSM)
├── viewmodels/      # ViewModels (Location, Favorites)
├── ui/
│   ├── theme/       # Colors, typography, theme
│   ├── screens/     # All screens (Home, Explore, Map, etc.)
│   └── components/  # Reusable components (GlassCard, ShopCard)
├── utils/           # Utilities
└── MainActivity.kt  # Entry point
```


---

<div align="center">

**Built by [Lucas Solderg](https://soldergg.github.io)** · iOS & Mobile Developer from Portugal

</div>
