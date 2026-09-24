# Mimari

Tek `:app` modülü, **feature-first** paketleme; her feature kendi
`data / domain / presentation` katmanına sahiptir. Bağımlılık yönü
`presentation → domain ← data`'dır. Domain saf Kotlin kalır;
`LayerDependencyTest` domain'de Android/Retrofit/OkHttp/serialization/Hilt
importlarını, presentation'da data importlarını yasaklar.

## Paket haritası

```
io.github.rojhatince47.weatherchan
├── HavaApplication.kt / MainActivity.kt / HavaApp.kt
├── core/
│   ├── common/        AppResult, AppError, ErrorMapper, dispatcher
│   │                  qualifier'ları, CommonModule
│   ├── di/            DataStore qualifier'ları
│   ├── network/       NetworkModule (OkHttp, Json, Open-Meteo/geocoding/
│   │                  Overpass/wttr.in Retrofit'leri), NetworkErrorMapper
│   ├── navigation/    Destinations, HavaNavHost
│   └── ui/            theme, component, state, text
└── feature/
    ├── city/
    │   ├── domain/    City, CityRepository, CityPermissions, öneri
    │   │              (seed, provider, use case)
    │   └── data/      DataStore şehir tercihleri, izinler, Overpass
    │                  kaynağı, öneri sağlayıcıları, repository
    ├── settings/
    │   ├── domain/    AppLanguage, TemperatureUnit, ThemeMode ve
    │   │              repository sözleşmeleri
    │   ├── data/      DataStore tercihleri, dil uygulayıcı, repository'ler
    │   └── presentation/ SettingsScreen, SettingsViewModel, UI modeli
    └── weather/
        ├── domain/    model (City, Coordinates, CurrentWeather, Forecast…),
        │              WeatherRepository, use case'ler, tavsiye motoru
        │              (kategori, poz, tohum), konum sözleşmeleri
        ├── data/      yerel önbellek, konum (Android provider/geocoder),
        │              Open-Meteo + wttr.in kaynakları, mapper, repository, DI
        └── presentation/ list (Main*), home (EvSayfasi*), detail
                           (ForecastContent, grafik), addcity, components
                           (arka plan, ambiyans, Weather-chan), model
```

## Veri akışı

```
Retrofit → DataSource → RepositoryImpl (Flow<AppResult<…>>)
        → UseCase → ViewModel (StateFlow<UiState>)
        → Route (collectAsStateWithLifecycle) → Screen (stateless)
```

`DataStore` yalnız tercihler ve önbellek için kullanılır; ilişkisel veritabanı
yoktur.

## Genişleme noktaları

| Senaryo | Eklenir | Değişir | Dokunulmaz |
|---|---|---|---|
| Yeni hava kaynağı | `WeatherRemoteDataSource` + DI | `WeatherDataModule` | Domain, ViewModel, ekranlar |
| Yeni şehir önerisi | `CitySuggestionProvider` + DI | `CityDataModule` | Domain, ekranlar |
| Yeni hava görünümü | `ConditionPalette` / `ConditionIcon` girdisi | Eşleme | Mapper, ekran düzeni |
| Yeni karakter pozu | `CompanionPose` + string dizileri | `WeatherAdviceEngine` eşlemesi | Hero yerleşimi |
| Yeni dil | `values-<dil>/strings.xml` | — | Motor, karakter, animasyon |

## Testler

- `LayerDependencyTest`, `StringResourceParityTest`, `CompanionStringsTest`
  mimari ve kaynak sözleşmelerini korur.
- Birim testleri `app/src/test`, enstrüman testleri `app/src/androidTest`;
  paylaşılan sahteler `app/src/testingShared` altındadır.
- `./gradlew spotlessCheck test`; cihazda `installDebugAndroidTest` ve
  `adb shell am instrument` ile enstrüman paketi çalıştırılır.
